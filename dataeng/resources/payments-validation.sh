#!/usr/bin/env bash

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  PaymentValidationTask --local-scheduler \
  --import-date $(date +%Y-%m-%d -d "$TO_DATE")
