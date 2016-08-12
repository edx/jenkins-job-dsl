#!/bin/bash

# unofficial bash strict mode
set -euo pipefail

# start docker
sudo service docker start

# log in to DockerHub
sudo docker login --username ${USERNAME} --password ${PASSWORD}

# build the Dockerfile of the IDA defined by APP_NAME and tag it with its name; don't use cache
cd configuration
sudo docker build -f docker/build/${APP_NAME}/Dockerfile -t edxbuilder/test:${APP_NAME} --no-cache .


# push built image to DockerHub
cd ..
sudo docker push edxbuilder/test:${APP_NAME}
