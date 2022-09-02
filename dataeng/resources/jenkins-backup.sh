#!/usr/bin/env bash

env

pip install awscli

# Delete all files in the workspace
rm -rf *
# Create a directory for the job definitions
mkdir -p jenkins/jobs
# Copy global configuration files into the workspace
cp $JENKINS_HOME/*.xml jenkins/
# Copy user configuration files into the workspace
#cp -r $JENKINS_HOME/users jenkins/
# Copy job definitions into the workspace
rsync -am --include='config.xml' --include='*/' --prune-empty-dirs --exclude='*' $JENKINS_HOME/jobs/ jenkins/jobs/
# Get current UTC date and time with minutes 
CURRENT_UTC_TIME=$(TZ=UTC date -I minutes)
# jenkins backup name
BACKUP_NAME=${NODE_NAME}-build${BUILD_ID}-${CURRENT_UTC_TIME}
# Create an archive from all copied files (since the S3 plugin cannot copy folders recursively)
tar czf $BACKUP_NAME.tar.gz jenkins/
# Remove the directory so only the archive gets copied to S3
rm -rf jenkins

aws s3 cp ${BACKUP_NAME}.tar.gz $S3_BACKUP_BUCKET/jenkins-analytics/${BACKUP_NAME}.tar.gz
# Remove tar jenkins backup file after uploading to S3 
rm -rf ${BACKUP_NAME}.tar.gz
