#!/usr/bin/env bash
set -ex


# This script ssh into Tableau server. Default centos user does not have access on Tableau directories
# so it runs commands as root. Backups are being created using tsm maintenance backup utility. This script
# copies backup file into s3 bucket and then remove backup file from tableau server.

ssh -tt $USER_NAME@$TABLEAU_SERVER_IP -o StrictHostKeyChecking=no "S3_BUCKET=$S3_BUCKET" '

# It gets the current date from system in 2021.10.05 format
DATE="$(date '+%Y.%m.%d')"
# It adds the date as prefix on backup name. Backup runs at 11:00 UTC so it also prefixed in the name.
BACKUPNAME="$DATE.11.00-tableau-data-backup.tsbak"

# Check if tableau backup file exist, exit if the file does not exist
if [[ ! -f /home/tableau_backups/$BACKUPNAME ]]
then
    echo "$BACKUPNAME Backup file does not exist. It seems to be some problem generating tableau backup. Refer to Administration of Tableau wiki docs to debug Tableau Regular Backup Automation"
    exit 1
else
    echo "Copying backup file into s3"
    sudo aws s3 cp /home/tableau_backups/$BACKUPNAME s3://$S3_BUCKET/$BACKUPNAME

    echo "Removing backup file from local disk"
    sudo rm -f /home/tableau_backups/$BACKUPNAME

fi


'
