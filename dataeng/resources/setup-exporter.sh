# Check that the DATE_MODIFIER is set to a Sunday, or if blank the current day is a Sunday
DAYOFWEEK=$(date +%u ${DATE_MODIFIER})
if [ $DAYOFWEEK != 7 ]; then
   exit 1
fi

# Create destination directory
mkdir -p /var/lib/jenkins/tmp/analytics-exporter/course-data

# Install requirements into this (exporter) virtual environment
pushd analytics-exporter/
pip install -r github_requirements.txt
pip install mysql-connector-python -e .
popd

# Configuration paths in analytics-secure
SECURE_ROOT=${WORKSPACE}/analytics-secure/analytics-exporter
CONFIG_PATH=${SECURE_ROOT}/${CONFIG_FILENAME}
GPG_KEYS_PATH=${WORKSPACE}/data-czar-keys

# Save virtualenv location and configuration paths
echo "
EXPORTER_VENV=${VIRTUAL_ENV}
CONFIG_PATH=${CONFIG_PATH}
GPG_KEYS_PATH=${GPG_KEYS_PATH}
DATE=$(date +%d ${DATE_MODIFIER})
EXTRA_OPTIONS=${EXTRA_OPTIONS}
ORG_CONFIG_PATH=${WORKSPACE}/${ORG_CONFIG}
SECURE_BRANCH=${SECURE_BRANCH}
" > exporter_vars

env | sort


# Export job configuration files
exporter-properties \
    --output-bucket=${OUTPUT_BUCKET} \
    --orgs="${ORGS}" \
    --include=platform_venv \
    --include=exporter_vars \
    ${CONFIG_PATH} \
    ${WORKSPACE}/${ORG_CONFIG} \
    organizations
