#!/usr/bin/env bash

# Verify that a APK intended to be released is both:
#   - TODO: Signed with a certificate
#   - Not a debug apk (seems redundant, but the build script could be accidetnally changed)
DEBUGGABLE=`$ANDROID_HOME/build-tools/23.0.3/aapt dump badging $WORKSPACE/artifacts/$APK_NAME |grep -q 'application-debuggable'`

if [ $DEBUGGABLE -gt 0 ]; then
    echo "This bug is debuggable, and should NOT be. Please fix and rebuild"
    exit 1
fi

echo "This release apk apears to be signed correctly."

exit 0
