set -xe

pip install --upgrade pip
pip install -r requirements.txt

. util/jenkins/assume-role.sh
set +x
assume-role ${ROLE_ARN}
set -x

cd $WORKSPACE/playbooks/edx-east
ansible-playbook -c local -i "localhost," -vv create_cname.yml -e "dns_zone=${dns_zone} dns_name=${dns_name} sandbox=${sandbox}"
