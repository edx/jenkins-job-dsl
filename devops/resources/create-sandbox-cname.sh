set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

set -xe

pip install --upgrade pip
pip install -r requirements.txt

. util/jenkins/assume-role.sh
set +x
assume-role ${ROLE_ARN}
set -x

cd $WORKSPACE/playbooks
ansible-playbook -c local -i "localhost," -vv create_cname.yml -e "dns_zone=${dns_zone} dns_name=${dns_name} sandbox=${sandbox}"
