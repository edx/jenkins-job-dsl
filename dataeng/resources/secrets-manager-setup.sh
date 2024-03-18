#!/bin/bash

# Define the location of the script in the Jenkins workspace
SCRIPT_PATH="$WORKSPACE/secrets-manager.sh"

# Write the script content to the specified location
cat <<EOF > "$SCRIPT_PATH"
#!/usr/bin/env bash

get_secret_value() {
    local secret_to_call="\$1"
    local secret_name="\$2"
    local SECRET_JSON

    SECRET_JSON=\$(aws secretsmanager get-secret-value --secret-id "\$secret_to_call" --region "us-east-1" --output json)

    # Check the exit status of the AWS CLI command
    if [ \$? -eq 0 ]; then
        extract_and_store_secret_value "\$SECRET_JSON" "\$secret_name"
    else
        echo "AWS CLI command failed"
    fi
}

extract_and_store_secret_value() {
    local json_data="\$1"
    local secret_name="\$2"
    local value

    value=\$(echo "\$json_data" | jq -r ".SecretString | fromjson.\"\$secret_name\"" 2>/dev/null)
    if [ \$? -eq 0 ]; then
        eval "\$secret_name"='\$value'
    else
        echo "Failed to extract secret value for \$secret_name"
    fi
}
EOF
