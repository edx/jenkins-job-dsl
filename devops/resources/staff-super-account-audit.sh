#!/bin/bash -xe

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

mysql --defaults-extra-file=${MYSQL_CONFIG_FILE} --batch -B -A wwc -e "select id, email, first_name, last_name, last_login, if(is_staff=1,'yes','no') as staff, if(is_superuser=1,'yes','no') as superuser from auth_user where 1=1 and is_staff = 1 or is_superuser = 1 order by superuser desc;" | tr '\t' ',' > ${ENVIRONMENT}_${DEPLOYMENT}_account_report.csv
