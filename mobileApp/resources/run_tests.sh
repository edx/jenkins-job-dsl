#!/usr/bin/env bash

set -e

kill_all_emus() {
    for emu_device in $(adb devices -l |grep 'device product:' |cut -d' ' -f1); do
        echo "Killing emulator: $emu_device"
        adb -s $emu_device emu kill
    done
}

# set up environment for testing
export PATH="$ANDROID_HOME/tools:$PATH"
export ADB_INSTALL_TIMEOUT=12
export ANDROID_TARGET=android-23
virtualenv $WORKSPACE/VENV
source $WORKSPACE/VENV/bin/activate
echo "org.gradle.jvmargs=-Xmx3072M" > $WORKSPACE/$APP_BASE_DIR/edx-app-android/gradle.properties
cd $WORKSPACE/$APP_BASE_DIR/edx-app-android
make requirements
make clean

# Kill all running emulators (in case there are emulators still running on
# a machine -- this will cause tests to fail)
kill_all_emus

# Set up Android emulator
make emulator

# Run linting and unit tests
make validate

# Wait a little while before querying the AVD (querying it before it has begun booting can cause the build to fail)
sleep 180

# Wait until the newly created emulator has finished booting before running tests
while true; do
    echo "Checking if the emulator is ready"
    # Due to extra characters in getprop commands, test for presence of a certain string
    DEVICE_BOOT_COMPLETE=$(adb shell getprop dev.bootcomplete |grep -c '1')
    SYS_BOOT_COMPLETE=$(adb shell getprop sys.boot_completed |grep -c '1')
    INIT_ANIM_STATE=$(adb shell getprop init.svc.bootanim |grep -c 'stopped')
    if [ $DEVICE_BOOT_COMPLETE -gt 0 ] && [ $SYS_BOOT_COMPLETE -gt 0 ] && [ $INIT_ANIM_STATE -gt 0 ]; then
        echo "emulator ready..."
        break
    fi
    sleep 30
done

adb shell input keyevent 82 &

# Run emulator tests
make e2e
make artifacts

# Again, kill all running emulators, as the Makefile spawns them as a background
# process, and leaving them running can interfere with other jobs
kill_all_emus
