#!/bin/bash
set -exuo pipefail

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

HOME=/edx/var/jenkins

env
set -x

cd $WORKSPACE/configuration
pip install -r requirements.txt
pip install certbot certbot-dns-route53
. util/jenkins/assume-role.sh

assume-role ${ROLE_ARN}

CERTBOT_BASE_DIR="${WORKSPACE}/letsencrypt"
mkdir -p "$CERTBOT_BASE_DIR"

CERT_DIR="$CERTBOT_BASE_DIR/config/live/$DOMAIN"
CERT_PATH="$CERT_DIR/fullchain.pem"
KEY_PATH="$CERT_DIR/privkey.pem"

SHOULD_UPLOAD=false

set +x
if [ -d "$CERT_DIR" ]; then
    echo "Certificate already exists. Attempting renewal..."
    RENEW_OUTPUT=$(certbot renew \
        --cert-name "$DOMAIN" \
        --non-interactive \
        --dns-route53 \
        --config-dir "$CERTBOT_BASE_DIR/config" \
        --work-dir "$CERTBOT_BASE_DIR/work" \
        --logs-dir "$CERTBOT_BASE_DIR/logs")

    echo "$RENEW_OUTPUT" | grep -q "No renewals were attempted" && SHOULD_UPLOAD=false || SHOULD_UPLOAD=true
else
    echo "Certificate does not exist. Requesting a new certificate for *.$DOMAIN..."
    certbot certonly \
        --non-interactive \
        --agree-tos \
        --email "$EMAIL" \
        --dns-route53 \
        --key-type rsa \
        -d "*.$DOMAIN" \
        --config-dir "$CERTBOT_BASE_DIR/config" \
        --work-dir "$CERTBOT_BASE_DIR/work" \
        --logs-dir "$CERTBOT_BASE_DIR/logs"
    SHOULD_UPLOAD=true
fi

if [ "$SHOULD_UPLOAD" = true ]; then
    echo "Uploading certificate and private key to AWS Secrets Manager..."

    CERT_CONTENT=$(<"$CERT_PATH")
    KEY_CONTENT=$(<"$KEY_PATH")

    aws secretsmanager update-secret \
        --region "$AWS_REGION" \
        --secret-id "sandbox-secure/ansible/certs/wildcard.$DOMAIN.pem" \
        --secret-string "$CERT_CONTENT" >/dev/null

    aws secretsmanager update-secret \
        --region "$AWS_REGION" \
        --secret-id "sandbox-secure/ansible/certs/wildcard.$DOMAIN.key" \
        --secret-string "$KEY_CONTENT" >/dev/null

    echo "Secrets updated successfully."
else
    echo "No new certificate generated. Skipping Secrets Manager upload."
fi
