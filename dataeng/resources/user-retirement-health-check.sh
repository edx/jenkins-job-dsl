#!/usr/bin/env bash

set -euo pipefail

if [[ "${ENVIRONMENT}" != "stage-edx" ]]; then
    echo "User retirement health check is stage-only. Refusing ENVIRONMENT=${ENVIRONMENT}."
    exit 1
fi

if [[ -z "${RETIREMENT_HEALTH_CHECK_PASSWORD:-}" ]]; then
    echo "RETIREMENT_HEALTH_CHECK_PASSWORD is required."
    exit 1
fi

VENV="venv-${BUILD_NUMBER}"
NODEENV="${WORKSPACE}/nodeenv-${BUILD_NUMBER}"
NODE_VERSION="${NODE_VERSION:-24}"
virtualenv --python="python${PYTHON_VERSION}" --clear "${VENV}"
source "${VENV}/bin/activate"

export PYTHONIOENCODING=UTF-8
export LC_CTYPE=en_US.UTF-8
export TEST_ENV=stage
export RUN_HEALTH_CHECK=true

cd "${WORKSPACE}/configuration"
pip install -r util/jenkins/requirements.txt
pip install nodeenv
nodeenv --node="${NODE_VERSION}" --prebuilt "${NODEENV}"
export PATH="${NODEENV}/bin:${PATH}"
hash -r
node --version
npm --version

set +x
CONFIG_YAML=$(aws secretsmanager get-secret-value --secret-id "user-retirement-secure/${ENVIRONMENT}" --region "us-east-1" --output json | jq -r '.SecretString' | yq -y .)
TEMP_CONFIG_YAML=$(mktemp "${WORKSPACE}/${RETIREMENT_CONFIG_FILE}.XXXXXXXXXX")
echo "${CONFIG_YAML}" > "${TEMP_CONFIG_YAML}"
chmod 600 "${TEMP_CONFIG_YAML}"

cleanup() {
    rm -f "${TEMP_CONFIG_YAML}"
}
trap cleanup EXIT

cd "${WORKSPACE}/playwright-e2e"
npm ci
npx playwright install chromium
npm run test:retirement-health

cd "${WORKSPACE}/tubular"
pip install -r requirements.txt

cd "${WORKSPACE}/playwright-e2e"
python scripts/retirement_health_check.py assert-pending \
    --artifact "${RETIREMENT_ARTIFACT_PATH}" \
    --config-file "${TEMP_CONFIG_YAML}" \
    --tubular-path "${TUBULAR_PATH}" \
    --expected-environment stage

USERNAME="$(python -c 'import json, os; print(json.load(open(os.environ["RETIREMENT_ARTIFACT_PATH"], encoding="utf-8"))["username"])')"
echo "Running Tubular retirement for synthetic username: ${USERNAME}"

python "${TUBULAR_PATH}/scripts/retire_one_learner.py" \
    --username "${USERNAME}" \
    --config_file "${TEMP_CONFIG_YAML}"

python scripts/retirement_health_check.py poll-complete \
    --artifact "${RETIREMENT_ARTIFACT_PATH}" \
    --config-file "${TEMP_CONFIG_YAML}" \
    --tubular-path "${TUBULAR_PATH}" \
    --expected-environment stage \
    --timeout-seconds "${TIMEOUT_SECONDS}" \
    --poll-interval-seconds "${POLL_INTERVAL_SECONDS}"
