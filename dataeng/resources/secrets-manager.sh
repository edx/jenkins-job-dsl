#!/bin/bash

extract_value_from_json() {
    local json="$1"
    local key="$2"
    local value=$(echo "$json" | jq -r ".$key")
}

fetch_whole_secret() {
    local secret_name="$1"
    local variable_name="$2"
    local secret_value=$(aws secretsmanager get-secret-value --secret-id "$secret_name" --query "SecretString" --output text)
    #set whole file as env var
    declare "${secret_name%=*}=${secret_value}"
}

fetch_specific_key() {
    local secret_name="$1"
    local key="$2"
    local secret_value=$(aws secretsmanager get-secret-value --secret-id "$secret_name" --query "SecretString" --output text)
    local extracted_value=$(extract_value_from_json "$secret_value" "$key")
    declare "${key%=*}=${extracted_value}"
}

# Main script
if [[ "$1" == "-w" ]]; then
    if [ $# -ne 3 ]; then
        echo "Usage: $0 -w <name_of_file> <name_of_variable>"
        exit 1
    fi
    fetch_whole_secret "$2" "$3"
else
    if [ $# -ne 2 ]; then
        echo "Usage: $0 <name_of_file> <name_of_key>"
        exit 1
    fi
    fetch_specific_key "$1" "$2"
fi