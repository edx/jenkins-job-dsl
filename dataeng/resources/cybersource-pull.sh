#!/usr/bin/env bash

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  DailyProcessFromCybersourceTask --local-scheduler \
    --merchant-id ${MERCHANT_ID} \
    --output-root ${OUTPUT_ROOT} \
    --run-date $(date +%Y-%m-%d -d "$RUN_DATE")
