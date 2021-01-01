#!/usr/bin/env bash

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

set -ex

FAIL=0

e_d=${ENVIRONMENT}_${DEPLOYMENT}

if ! egrep -q -r --include *.json '{{' "${GREP_DIR}"; then
  echo "No un-expanded vars in ${e_d}"
else
  echo "Found un-expanded vars in ${e_d}"
  echo `egrep -r --include *.json '{{' "${GREP_DIR}"`
  FAIL=1
fi

if ! egrep -qi -r --include *.json \'"False"\' "${GREP_DIR}"; then
  echo "No quoted False."
else
  echo "Found a quoted boolean in ${e_d}"
  echo `egrep -qi -r --include *.json "False" "${GREP_DIR}"`
  FAIL=1
fi

if ! egrep -qi -r --include *.json '\"True\"' "${GREP_DIR}"; then
  echo "No quoted True."
else
  echo "Found a quoted boolean in ${e_d}"
  echo `egrep -qi -r --include *.json '\"True\"' "${GREP_DIR}"`
  FAIL=1
fi

for i in `find $WORKSPACE -name "*.yml" | sed '/ansible/d'`
  do
    /usr/bin/python2.7 -c "import sys,yaml; yaml.load_all(open('$i'))" $i > /dev/null
    if [[ $? -ne 0 ]]; then
      echo "ERROR parsing $i"
      FAIL=1
    else
      echo "YAML syntax verified"
    fi
  done

if [ "$FAIL" -eq 1 ] ; then
  echo "Failing..."
  exit 1
fi
