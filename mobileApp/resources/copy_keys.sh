#!/usr/bin/bash env

# Copy gradle.properties and the keystore file into the application directory
# They will be used by the gradle script that builds the app
cp $GRADLE_PROPERTIES $WORKSPACE/$APP_BASE_DIR/gradle.properties
cp $KEY_STORE $WORKSPACE/$APP_BASE_DIR/$KEY_STORE_FILE
