# docker-in-docker Subdirectory

The docker-in-docker subdirectory contains the Dockerfile and the Docker compose file to build a Docker container running Jenkins in which Docker can be used. 

The docker-compose.yml file in docker-in-docker differs from the top level version in that it exposes the Docker socket to the container via the volumes list. 

# Building Docker Containers

To build the docker-in-docker Docker container, run the following command from within the docker-in-docker subdirectory: `docker build .` 
