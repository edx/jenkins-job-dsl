#!/usr/bin/env bash

# Prepare an Android Virtual Device (AVD) for testing the edX Android application

# ARM architecture is used instead of x86 (which is 10x faster) of the lack of support from CI due
# to complications of creating a virtual machine within a virtual machine. This may be solved
# eventually and would significantly speed some things up.
# Create the AVD for screenshot tests
$ANDROID_HOME/tools/android create avd --force --name screenshotDevice --target android-21 --abi armeabi-v7a --device "Nexus 4" --skin 768x1280 --sdcard 250M
echo "runtime.scalefactor=auto" >> $HOME/.android/avd/screenshotDevice.avd/config.ini
# Boot up the emulator in the background
$ANDROID_HOME/tools/emulator -avd screenshotDevice -no-audio -no-window &

$ANDROID_HOME/tools/adb shell input keyevent 82 &

exit 0
