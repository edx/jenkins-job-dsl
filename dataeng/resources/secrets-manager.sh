#!/usr/bin/env bash
secret_to_call="$1"
secret_name="$2"
set +x

SECRET_JSON=$(aws secretsmanager get-secret-value --secret-id $secret_to_call --region "us-east-1" --output json)
# Check the exit status of the AWS CLI command

echo "$SECRET_JSON"
extract_and_store_secret_value() {
    
    value=$(echo "$SECRET_JSON" | jq -r ".SecretString | fromjson.$secret_name" 2>/dev/null)
    eval "$secret_name"='$value'
}

if [ $? -eq 0 ]; then
    # Use jq to extract the values from the JSON response
    extract_and_store_secret_value $SECRET_JSON $secret_name
else
    echo "AWS CLI command failed"
fi
