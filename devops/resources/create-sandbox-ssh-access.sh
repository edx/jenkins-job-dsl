set -xe

pip install --upgrade pip
pip install -r requirements.txt

cd playbooks/edx-east

if [ -z "${SSH_USER}" ] || [ -z "${USER}" ] || [ "${USER}" == "jenkins" ] ; then
   exit 1
else
   ansible-playbook --user ${SSH_USER} -i "$sandbox," -e "user=${USER} give_sudo=true" create_user.yml
fi
