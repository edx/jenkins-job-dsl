#!/usr/bin/env bash

# Configure the workspace to use the Android SDK
export ANDROID_HOME="/opt/Android/Sdk"
echo "sdk.dir=/opt/Android/Sdk" > $WORKSPACE/$APP_BASE_DIR/local.properties
echo "sdk.dir=/opt/Android/Sdk" > $WORKSPACE/$APP_BASE_DIR/edx-app-android/VideoLocker/local.properties

# Build the application
cd $WORKSPACE/$APP_BASE_DIR
./build.sh

# Rename the generated apk to follow this format: edx-prod-release-<versionHash>.apk. If a has was
# not supplied by the user, determine the hash of the current commit at master (default value in the
# Jenkins job)
if [[ $HASH == "refs/heads/master" ]]; then
    HASH=`git -C $WORKSPACE/$APP_BASE_DIR rev-parse HEAD`
fi
APK_NAME="${APP_BASE_NAME}-${HASH}.apk"

# Copy the apk to 'artifacts' for ease of archiving
rm -Rf $WORKSPACE/artifacts
mkdir $WORKSPACE/artifacts
cp $WORKSPACE/$APP_BASE_DIR/edx-app-android/VideoLocker/build/outputs/apk/VideoLocker-prod-debug.apk $WORKSPACE/artifacts/$APK_NAME

exit 0
