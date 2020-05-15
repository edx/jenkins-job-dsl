#!/bin/bash

cd jenkins-configuration
source local_env.sh.sample

# Write the plugins installed by the base configuration branch to 'base_installed_plugins'
export PLUGIN_CONFIG="../configuration-base/${PLUGIN_CONFIG_FILE}"
export PLUGIN_CONFIG_KEY="${PLUGIN_CONFIG_KEY}"
make clean.ws plugins
mv installed_plugins base_installed_plugins

# Write the plugins installed by the target configuration branch to 'target_installed_plugins'
export PLUGIN_CONFIG="../configuration-target/${PLUGIN_CONFIG_FILE}"
export PLUGIN_CONFIG_KEY="${PLUGIN_CONFIG_KEY}"
make clean.ws plugins
mv installed_plugins target_installed_plugins

echo "***************************************************************"

# Compare the two outputs of the above commands to see what changes between the two
python scripts/compare_installed_plugins.py base_installed_plugins target_installed_plugins
echo "Make sure to check the Changelog for any of the plugins mentioned above"
