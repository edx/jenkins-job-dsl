set -xe

pip3 install --upgrade pip
pip3 install -r requirements.txt

. util/jenkins/assume-role.sh
set +x
assume-role ${ROLE_ARN}
set -x

cd $WORKSPACE/playbooks
ansible-playbook -c local -i "localhost," -vv create_cname.yml -e "dns_zone=${dns_zone} dns_name=${dns_name} sandbox=${sandbox}"
