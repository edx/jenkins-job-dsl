#!/usr/bin/env bash
set -x

if [[ -z $WORKSPACE ]]; then
    echo "Environment incorrect for this wrapper script"
    env
    exit 1
fi

cd "$WORKSPACE/edx-platform"

# install requirements
# These requirements will be installed into the shinginpanda
# virtualenv on the jenkins server and are necessary to run
# management commands locally.

pip install --exists-action w -r requirements/edx/pre.txt
pip install --exists-action w -r requirements/edx/base.txt
pip install --exists-action w -r requirements/edx/post.txt
pip install --exists-action w -r requirements/edx/github.txt
pip install --exists-action w -r requirements/edx/local.txt

cd "$WORKSPACE/configuration"

pip install --exist-action w -r pre-requirements.txt
pip install --exist-action w -r requirements.txt

cd "$WORKSPACE/configuration/playbooks"

if [[ -f ${WORKSPACE}/configuration-secure/ansible/vars/${deployment}.yml ]]; then
    extra_var_args+=" -e@${WORKSPACE}/configuration-secure/ansible/vars/${deployment}.yml"
fi

if [[ -f ${WORKSPACE}/configuration-secure/ansible/vars/${environment}-${deployment}.yml ]]; then
    extra_var_args+=" -e@${WORKSPACE}/configuration-secure/ansible/vars/${environment}-${deployment}.yml"
fi

for extra_var in $extra_vars; do
    extra_var_args+=" -e@${WORKSPACE}/configuration-secure/ansible/vars/$extra_var"
done

extra_var_args+=" -e edxapp_app_dir=${WORKSPACE}"
extra_var_args+=" -e EDXAPP_CFG_DIR=${WORKSPACE}"
extra_var_args+=" -e edxapp_code_dir=${WORKSPACE}/edx-platform"
extra_var_args+=" -e edxapp_user=jenkins"

# Generate the json configuration files
ansible-playbook -c local $extra_var_args --tags edxapp_cfg -i localhost, -s -U jenkins edxapp.yml

# Run migrations and replace literal '\n' with actual newlines to make the output
# easier to read


EDX_PATH="${WORKSPACE}/edx-platform"
#DJANGO_ADMIN="${JENKINS_HOME}/.virtualenvs/mailchimp/bin/django-admin.py"
DJANGO_ADMIN="${VIRTUAL_ENV}/bin/python ${EDX_PATH}/manage.py lms --settings=production"

get_key () {
  case $1 in
      "edx"       ) ORG_KEY="7b87ccd203b973d87d0ac4423192afa6-us5";;
  esac
  echo $ORG_KEY
}


sync_announcements () {
    LIST_ID=$1
    CONFIGURATION=$2
    ORG_NAME=$3
    ORG_KEY=$(get_key ${ORG_NAME})
    CMD="${DJANGO_ADMIN} mailchimp_sync_announcements --key=${ORG_KEY} --list=${LIST_ID}"
    $CMD
}

sync_course () {
    LIST_ID=$1
    COURSE_ID=$2
    CONFIGURATION=$3
    ORG_NAME=$4
    ORG_KEY=$(get_key ${ORG_NAME})
    SEGMENTS=${5:-0}
    CMD="${DJANGO_ADMIN} mailchimp_sync_course --key=${ORG_KEY} --list=${LIST_ID} --course=${COURSE_ID} --segments=${SEGMENTS}"
    $CMD
}

OLD_IFS=${IFS}

cd ${WORKSPACE}/sysadmin/jenkins/mailchimp

while read -r line
do
    IFS=","
    set $line
    IFS=${OLD_IFS}
     sync_course $1 $2 $3 $4
done < "courses.csv"

sync_announcements 237694b56d production edx
