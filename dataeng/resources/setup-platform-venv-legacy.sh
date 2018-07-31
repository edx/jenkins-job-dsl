# Some recent changes in edx-platform breaks the exporter.
# We are currently using edx-platform's aed/analytics-exporter-settings-hotfix(Nov 2017) which follows an old
# requirements installation strategy. This file would go away in favor of 'setup-platform-env' once we figure out the
# underlying issue.
#!/usr/bin/env bash

# Install requirements
pushd edx-platform
pip install --exists-action w -r requirements/edx/pre.txt
pip install --exists-action w -r requirements/edx/django.txt
pip install --exists-action w -r requirements/edx/base.txt
pip install --exists-action w -r requirements/edx/github.txt
pip install --exists-action w -r requirements/edx/local.txt
popd

# Save virtualenv location
echo "PLATFORM_VENV=${VIRTUAL_ENV}" > platform_venv
