# base Subdirectory

The base subdirectory contains the Dockerfile and the Docker compose file to build a Docker container running Jenkins.

# docker-in-docker Subdirectory

The docker-in-docker subdirectory contains the Dockerfile and the Docker compose file to build a Docker container running Jenkins in which Docker can be used. 


The docker-compose.yml file in docker-in-docker differs from the base version in that it exposes the Docker socket to the container via the volumes list. 

The Dockerfile in docker-in-docker differs from the base version in that it installs Docker in the container.

# Building Docker Containers

The plugins.txt in the docker root directory is shared by base and docker-in-docker. In order for this file to be accessible when building either Docker containers, run the following commands to build the desired Docker container from the jenkins-job-dsl/docker directory:

To build the base Docker container: `docker build -f base/Dockerfile .`

To build the docker-in-docker Docker container: `docker build -f docker-in-docker/Dockerfile .`
