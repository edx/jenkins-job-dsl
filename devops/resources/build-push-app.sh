#!/usr/bin/env bash

# unofficial bash strict mode
set -euxo pipefail

image_tag=edxops/${APP_NAME}:latest

# build the Dockerfile of the IDA defined by APP_NAME and tag it with its name; don't use cache
cd configuration
docker build -f docker/build/${APP_NAME}/Dockerfile -t ${image_tag} --no-cache .

# push built image to DockerHub
docker --config $(dirname ${CONFIG_JSON_FILE}) push ${image_tag}

