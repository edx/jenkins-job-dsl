#!/usr/bin/env bash

if [ -z "$NUM_REDUCE_TASKS" ]; then
    NUM_REDUCE_TASKS=$(( $NUM_TASK_CAPACITY * 2 ))
fi
NUM_REDUCE_TASKS=$(( $NUM_REDUCE_TASKS * 2 ))

now=$(date +%H)

name=hourly
src="$SOURCES"
dest=${DESTINATION_PREFIX}/$now
manifest=${DESTINATION_PREFIX}/$now/manifest.txt

# Remove any previous intermediate output for this hour
pip install awscli
aws s3 rm --recursive $dest || true

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  AnswerDistributionWorkflow --local-scheduler \
  --src $src --dest $dest --name $name --output-root $OUTPUT_URL \
  --include '[\"*tracking.log*20131*.gz\",\"*tracking.log*2014*.gz\",\"*tracking.log*2015*.gz\",\"*tracking.log*2016*.gz\",\"*tracking.log*2017*.gz\",\"*tracking.log*2018*.gz\",\"*tracking.log*2019*.gz\",\"*tracking.log*2020*.gz\"]' \
  --manifest "$manifest" --base-input-format "org.edx.hadoop.input.ManifestTextInputFormat" --lib-jar $LIB_JAR \
  --n-reduce-tasks $NUM_REDUCE_TASKS \
  --marker $dest/marker \
  --credentials $CREDENTIALS \
  --insert-chunk-size 1000 \
  --use-temp-table-for-overwrite \
  $EXTRA_ARGS
