#!/bin/bash

# Define the location of the script in the Jenkins workspace
SCRIPT_PATH="$WORKSPACE/secrets-manager.sh"

echo "running setup"

# Write the script content to the specified location
cat <<EOF > "$SCRIPT_PATH"

#!/usr/bin/env bash

extract_value_from_json() {
    local secret_json="\$1"
    local secret_key="\$2"

    local secret_value=\$(echo "$secret_json" | jq -r ".$secret_key")
}

fetch_whole_secret() {
    local secret_name="\$1"
    local variable_name="\$2"
    echo "\$secret_name"
    echo "\$variable_name"
    SECRET_JSON=\$(aws secretsmanager get-secret-value --secret-id "\$secret_name" --region "us-east-1" --output json)
    echo "\$SECRET_JSON"
    value=\$(echo "\$SECRET_JSON" | jq -r ".SecretString" 2>/dev/null)
    echo "\$value"
    echo "\$value" > "\$WORKSPACE/\$variable_name"
    # Output the contents of the file to verify
    cat "\$WORKSPACE/\$variable_name"
    declare "\${variable_name%=*}=\${value}"    

    #declare "$variable_name=$secret_value"
    #declare "$variable_name=\"$secret_value\""
    #what brian said to do
    #declare "\${variable_name%=*}=\${value}"
}

fetch_specific_key() {
    local secret_name="\$1"
    local key="\$2"
    local secret_value=$(aws secretsmanager get-secret-value --secret-id "$secret_name" --query "SecretString" --output text)
    local extracted_value=$(extract_value_from_json "$secret_value" "$key")
    declare "${key%=*}=${extracted_value}"
}

secret_script() {
    echo "\$1"
    echo "\$2"
    echo "\$3"
    if [[ "\$1" == "-w" ]]; then
        if [ \$# -ne 3 ]; then
            echo "Usage: $0 -w <name_of_file> <name_of_variable>"
            exit 1
        fi
        fetch_whole_secret "\$2" "\$3"
    else
        if [ $# -ne 2 ]; then
            echo "Usage: $0 <name_of_file> <name_of_key>"
            exit 1
        fi
        fetch_specific_key "\$1" "\$2"
    fi
}
EOF
