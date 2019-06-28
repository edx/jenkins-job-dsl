
echo "$sandbox ${org_key}"
exit

set -xe

pip install --upgrade pip
pip install -r requirements.txt

cd playbooks

if [ -z "${SSH_USER}" ] || [ -z "${USER}" ] || [ "${USER}" == "jenkins" ] ; then
   exit 1
else
   ansible-playbook --user ${SSH_USER} -i "$sandbox," -e "org_key=${org_key} give_sudo=true USER_FAIL_MISSING_KEYS=true" masters_sandbox_update.yml
fi
