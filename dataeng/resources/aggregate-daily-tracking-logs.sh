#!/usr/bin/env bash

group_by_date=$(date +%Y%m%d -d "$TO_DATE")
group_by_pattern=".*(tracking\.log-$group_by_date).*(.gz)"
manifest_file_name="manifest-$group_by_date.gz"
previous_manifest_path="$DEST_BUCKET_PATH/$manifest_file_name"

aws s3 ls $previous_manifest_path

if [ $? == 0 ]; then
    SHELL_COMMAND="s3-dist-cp --src=$SOURCE_BUCKET_PATH --dest=$DEST_BUCKET_PATH --groupBy='$group_by_pattern' --targetSize=$TARGET_SIZE --outputManifest=$manifest_file_name --previousManifest=$previous_manifest_path"
else
    SHELL_COMMAND="s3-dist-cp --src=$SOURCE_BUCKET_PATH --dest=$DEST_BUCKET_PATH --groupBy='$group_by_pattern' --targetSize=$TARGET_SIZE --outputManifest=$manifest_file_name"
fi

${WORKSPACE}/analytics-configuration/automation/run-shell.sh "$SHELL_COMMAND"
