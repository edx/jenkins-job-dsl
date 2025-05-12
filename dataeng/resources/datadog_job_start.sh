
#!/bin/bash

set +x
API_KEY=$(aws secretsmanager get-secret-value --secret-id "analytics-secure/datadog" --region "us-east-1"  --output text --query 'SecretString' | jq -r '.api_key')


START_TIME="$(date +"%Y-%m-%d %H:%M:%S")"

# Send job start event to Datadog
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "https://api.datadoghq.com/api/v1/events" \
  -H "Content-Type: application/json" \
  -H "DD-API-KEY: $API_KEY" \
  -d @- <<EOF
{
    "title": "Jenkins Job $JOB_NAME Build $BUILD_NUMBER - STARTED",
    "text": "Job URL: $BUILD_URL\nStart Time: $START_TIME",
    "tags": [
        "job_name:$JOB_NAME",
        "status:started"
    ],
    "alert_type": "info",
    "aggregation_key": "jenkins_job_$JOB_NAME",
    "source_type_name": "jenkins"
}
EOF
)

if [[ "$RESPONSE" -ne 202 ]]; then
  echo "ERROR: Failed to send job start event to Datadog (HTTP $RESPONSE)"
  exit 1
fi

echo "INFO: Sent job start event for job $JOB_NAME (Build $BUILD_NUMBER)"
