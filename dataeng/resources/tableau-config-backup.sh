# This script is used to ssh into tableau instance, create config backup and store in s3. It also compares latest created backup with a
# stored baseline backup to detect any changes and creates a PR for DE members to review and approve if changes seems correct.
#!/usr/bin/env bash
set -ex

# It gets the current date from system in 2022.04.05 format
DATE="$(date '+%Y.%m.%d')"

# It adds the date as prefix on backup name. Backup runs at 09:00 UTC so it also prefixed in the name.
TABLEAU_CONFIG="${DATE}-tableau-config-backup.json"

ssh -tt $USER_NAME@$TABLEAU_SERVER_DNS -o StrictHostKeyChecking=no "S3_BUCKET=$S3_BUCKET TABLEAU_CONFIG=$TABLEAU_CONFIG" '

sudo runuser -l tsm_admin -c "tsm settings export -f /home/tableau_backups/tableau-config-backup.json"

# Check if tableau backup file exist, exit if the file does not exist
if [[ ! -f /home/tableau_backups/tableau-config-backup.json ]]
then
    echo "$TABLEAU_CONFIG Backup file does not exist. It seems to be some problem generating tableau backup. Refer to Administration of Tableau wiki docs to debug Tableau Regular Backup Automation"
    exit 1
else
    echo "Copying backup file into s3"
    aws s3 cp /home/tableau_backups/tableau-config-backup.json s3://$S3_BUCKET/$TABLEAU_CONFIG

    echo "Removing backup file from local disk"
    sudo rm -f /home/tableau_backups/$TABLEAU_CONFIG

fi

'

# Creating python 3.8 virtual environment to run python script.
PYTHON38_VENV="py38_venv"
virtualenv --python=python3.8 --clear "${PYTHON38_VENV}"
source "${PYTHON38_VENV}/bin/activate"

# Copying tableau-config-backup file from s3.
aws s3 cp s3://$S3_BUCKET/$TABLEAU_CONFIG tableau-config.json

pip install deepdiff click

# Disabling the exit on failure flag
set +e

# Comparing current tableau-config.json file with stored backup and generates an updated config file if changes are detected
python $WORKSPACE/analytics-tools/tableau/tableau_config_compare.py --input_config 'tableau-config.json' --baseline_config "$WORKSPACE/analytics-secure/tableau/tableau-config-backup-base.json" --output_file 'updated-tableau-config.json'

if [ $? -eq 0 ]; then
    echo "Tableau Config Backup is same"
else
    echo "Changes are detected in Tableau config so it is creating a pull request to update baseline backup file."
    set -e
    cp updated-tableau-config.json $WORKSPACE/analytics-secure/tableau/tableau-config-backup-base.json

    cd $WORKSPACE/analytics-secure/

    # Create a new git branch
    NOW=$(date +%Y_%m_%d_%H_%M)
    BRANCHNAME="tableau_config_$NOW"
    git checkout -b "$BRANCHNAME"

    # Update git user information
    git config --global user.email "edx-analytics-automation@edx.org"
    git config --global user.name "edX Analytics Automation"

    # Commit all changes to the new branch, making sure new files are added
    git add --all
    git commit --message "chore: Tableau Config updated at $NOW"

    # Create a PR on Github from the new branch
    HUB_PROTOCOL=ssh /snap/bin/hub pull-request --push --no-edit -r edx/edx-data-engineering
fi
