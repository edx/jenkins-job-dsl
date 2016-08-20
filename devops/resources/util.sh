#!/usr/bin/env bash

assume-role(){
    ROLE_ARN="${1}"
    SESSIONID=$(date +"%s")

    RESULT=(`aws sts assume-role --role-arn $ROLE_ARN \
            --role-session-name $SESSIONID \
            --query '[Credentials.AccessKeyId,Credentials.SecretAccessKey,Credentials.SessionToken]' \
            --output text`)

    export AWS_ACCESS_KEY_ID=${RESULT[0]}
    export AWS_SECRET_ACCESS_KEY=${RESULT[1]}
    export AWS_SECURITY_TOKEN=${RESULT[2]}
    export AWS_SESSION_TOKEN=${AWS_SECURITY_TOKEN}
}

