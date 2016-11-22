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
cd $WORKSPACE/$APP_BASE_DIR/edx-app-android
make requirements
make clean

# Run linting and unit tests
make validate

# Kill all running emulators (in case there are emulators still running on
# a machine -- this will cause tests to fail)
kill_all_emus

# Set up Android emulator
make emulator

# Wait until the newly created emulator has finished booting before running tests
adb wait-for-device
while true; do
    echo "Checking if the emulator is ready"
    DEVICE_BOOT_COMPLETE=`adb shell getprop dev.bootcomplete`
    SYS_BOOT_COMPLETE=`adb shell getprop sys.boot_complete`
    INIT_ANIM_STATE=`adb shell getprop init.svc.bootanim`
    if [ $DEVICE_BOOT_COMPLETE == '1' ] && [ $SYS_BOOT_COMPLETE == '1' ] && [ $INIT_ANIM_STATE == 'stopped' ]; then
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


exit 0
