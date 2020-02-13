#!/bin/bash


cd jenkins-configuration
source local_env.sh.sample

export PLUGIN_OUTPUT_FILE='base-installed-plugins'
export PLUGIN_CONFIG=../${BASE_CONFIG_PLUGIN_FILE}
export PLUGIN_CONFIG_KEY=${BASE_CONFIG_PLUGIN_KEY}
make clean plugins show

export PLUGIN_OUTPUT_FILE='new-installed-plugins'
export PLUGIN_CONFIG=../${TARGET_CONFIG_PLUGIN_FILE}
export PLUGIN_CONFIG_KEY=${TARGET_CONFIG_PLUGIN_KEY}
make clean plugins show

python scripts/compare_installed_plugins base-installed-plugins new-installed-plugins
