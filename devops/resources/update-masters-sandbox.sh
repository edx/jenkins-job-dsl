
set -xe

pip install --upgrade pip
pip install -r requirements.txt

cd playbooks

PARAMS="program_uuids=${program_uuids}"
CREDENTIALS="client_id=${MASTERS_AUTOMATION_CLIENT_ID} client_secret=${MASTERS_AUTOMATION_CLIENT_SECRET}"
MORE_VARS="give_sudo=true USER_FAIL_MISSING_KEYS=true"

if [ -z "${MASTERS_AUTOMATION_CLIENT_ID}" ] || \
   [ -z "${MASTERS_AUTOMATION_CLIENT_SECRET}" ]; \
then
   exit 1
else
   ansible-playbook --user ubuntu \
                    -i "$sandbox," \
                    -e "${PARAMS} ${CREDENTIALS} ${MORE_VARS}" \
                    masters_sandbox_update.yml
fi
