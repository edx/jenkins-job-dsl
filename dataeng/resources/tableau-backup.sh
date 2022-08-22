#!/usr/bin/env bash

# This script ssh into Tableau server. Default centos user does not have access on Tableau directories
# so shared folder is created that can be accessible from centos user. Backups are being created using
# tsm maintenance backup utility. This script creates the data and config backup and copies them into s3
# bucket and then remove backup files from tableau server.

ssh $USER_NAME@$TABLEAU_SERVER_IP /bin/bash << EOF
set -ex
sudo su - tsm_admin
set -ex

# Data backup filename in the format of table_data_backup_20220116T2348Z.tsbak
data_backup_filename="tableau_data_backup_$(date -u +%Y%m%dT%H%MZ).tsbak"

echo "Creating data backup"
tsm maintenance backup --file \$data_backup_filename --multithreaded

echo "Copying data backup file into s3"
aws s3 cp /home/tableau_backups/\$data_backup_filename s3://$S3_BUCKET/\$data_backup_filename

echo "Removing data backup file(s) from local disk"
rm /home/tableau_backups/tableau_data_backup_*.tsbak

# Config backup filename in the format of tableau_config_backup_20220116T2348Z.json
config_backup_filename="tableau_config_backup_$(date -u +%Y%m%dT%H%MZ).json"

echo "Creating config backup"
tsm settings export -f \$config_backup_filename

echo "Copying config backup file into s3"
aws s3 cp \$config_backup_filename s3://$S3_BUCKET/\$config_backup_filename

echo "Removing config backup file(s) from local disk"
rm tableau_config_backup_*.json

echo "Backup completed"

EOF
