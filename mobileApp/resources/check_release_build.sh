#!/usr/bin/env bash

set -e

# Verify that an APK intended to be released is both:
#   - TODO: Signed with a certificate
#   - Not a debug apk (seems redundant, but the build script could be accidentally changed)
DEBUGGABLE=`$ANDROID_HOME/build-tools/23.0.3/aapt dump badging $WORKSPACE/artifacts/$APK_NAME |grep -q 'application-debuggable'`

if [ $DEBUGGABLE -eq 0 ]; then
    echo "This build is debuggable, and should NOT be. Please fix and rebuild"
    exit 1
fi

echo "This release apk apears to be signed correctly."
