#!/usr/bin/env bash

set -ex

# take this out
virtualenv VENV
. $WORKSPACE/bin/activate

# set up environment for testing
export PATH="/opt/Android/Sdk/tools:$PATH"
export ADB_INSTALL_TIMEOUT=12
export ANDROID_TARGET=android-23
cd $WORKSPACE/$APP_BASE_DIR/edx-app-android
make requirements
make clean

# Run linting and unit tests
make validate

# Kill all running emulators (in case there are emulators still running on
# a machine -- this will cause tests to fail)
for emu_device in $(adb devices -l |grep 'device product:' |cut -d' ' -f1); do
    echo "Killing emulator: $emu_device"
    adb -s $emu_device emu kill
done

# Set up Android emulator and wait until it is fully booted
make emulator
sleep 1120
$ANDROID_TOOLS/adb shell input keyevent 82 &

# Run emulator tests
make e2e
make artifacts

exit 0
