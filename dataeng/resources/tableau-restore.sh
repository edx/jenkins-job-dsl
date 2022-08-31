#!/usr/bin/env bash

if [ -z $BACKUP_TIMESTAMP ]; then
	aws s3 ls $S3_PATH | sort -r
else
    ssh -o StrictHostKeyChecking=accept-new $USER_NAME@$TABLEAU_SERVER_HOST /bin/bash << EOF
    set -ex
    sudo su - -s /bin/bash $TABLEAU_ADMIN_USER
    set -ex
    config_backup_file=tableau_config_backup_$BACKUP_TIMESTAMP.json
    data_backup_file=tableau_data_backup_$BACKUP_TIMESTAMP.tsbak

    # Restore config backup
    aws s3 cp $S3_PATH\$config_backup_file .
    tsm settings import -f \$config_backup_file
    tsm pending-changes apply
    tsm restart
    rm \$config_backup_file

    # Restore data backup
    # tsm maintainence restore command expects a backup file in the directory
    # defined in the TSM basefilepath.backuprestore variable
    tablea_backup_dir=$(tsm configuration get -k basefilepath.backuprestore)
    aws s3 cp $S3_PATH\$data_backup_file \$tablea_backup_dir
    tsm stop
    tsm maintenance restore --file \$data_backup_file
    tsm start
    rm \$tablea_backup_dir/\$data_backup_file
EOF
fi
