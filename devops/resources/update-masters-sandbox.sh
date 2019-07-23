
set -xe

pip install --upgrade pip
pip install -r requirements.txt

cd playbooks

if [ -z "${MASTERS_AUTOMATION_CLIENT_ID}" ] || [ -z "${MASTERS_AUTOMATION_CLIENT_SECRET}" ]; then
   echo "Error: Missing automation client credentials!"
   exit 1
elif [ -z "${program_uuids}" ] || [ -z "${dns_name}" ]; then
   echo "Error: Missing dns_name or program_uuids!"
   exit 2
fi

CREDENTIALS="client_id=${MASTERS_AUTOMATION_CLIENT_ID} client_secret=${MASTERS_AUTOMATION_CLIENT_SECRET}"
PARAMS="program_uuids=${program_uuids} dns_name=${dns_name}"

ansible-playbook --user ubuntu \
                 -i "${dns_name}.sandbox.edx.org," \
                 -e "${CREDENTIALS} ${PARAMS}" \
                 masters_sandbox_update.yml
