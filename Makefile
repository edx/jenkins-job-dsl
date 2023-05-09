SHELL := /usr/bin/env bash
.DEFAULT_GOAL := help

# List of services defined in docker-compose.yml.  Guard this with a check for
# the existence of the docker-compose program since we don't want to exit
# before showing help text.
ifneq (, $(shell which docker-compose))
    DOCKER_SERVICES := $(shell docker-compose config --services)
endif

.PHONY: help requirements upgrade clean docker.clean $(DOCKER_SERVICES:%=docker.run.%) $(DOCKER_SERVICES:%=docker.stop.%) $(DOCKER_SERVICES:%=docker.running.%) $(DOCKER_SERVICES:%=docker.configure.%)

help:
	@echo 'Makefile for launching and configuring edX-related jenkins services.'
	@echo ''
	@echo 'These jenkins services are launched via Docker, and are primarily intended for'
	@echo 'local development of jenkins jobs and features, but may also facilitate with'
	@echo 'production deploys for the OpenEdX community.'
	@echo ''
	@echo 'Available targets:'
	@echo '    help                       show this information'
	@echo '    clean                      shutdown and delete running containers, networks, and images specified in docker-compose.yml'
	@echo '    docker.run.$$service        run the specified jenkins service container'
	@echo '    docker.stop.$$service       stop the specified jenkins service container'
	@echo '    docker.running.$$service    check if specified jenkins service container is running'
	@echo '    docker.configure.$$service  configure the specified jenkins service container'
	@echo ''
	@echo '$$service can be one of the services specified in docker-compose.yml'

requirements: ## Install requirements
	pip install -qr requirements/pip-tools.txt
	pip-sync requirements/base.txt

COMMON_CONSTRAINTS_TXT=requirements/common_constraints.txt
.PHONY: $(COMMON_CONSTRAINTS_TXT)
$(COMMON_CONSTRAINTS_TXT):
	wget -O "$(@)" https://raw.githubusercontent.com/edx/edx-lint/master/edx_lint/files/common_constraints.txt || touch "$(@)"

upgrade: export CUSTOM_COMPILE_COMMAND=make upgrade
upgrade: $(COMMON_CONSTRAINTS_TXT)
	## Upgrade requirements with pip-tools
	pip install -qr requirements/pip-tools.txt
	pip-compile --allow-unsafe --rebuild --upgrade -o requirements/pip.txt requirements/pip.in
	pip-compile --upgrade -o requirements/pip-tools.txt requirements/pip-tools.in
	pip install -qr requirements/pip.txt
	pip install -qr requirements/pip-tools.txt
	pip-compile --upgrade -o requirements/base.txt requirements/base.in

clean: docker.clean

docker.clean:
	docker-compose down --rmi all
	@echo ''
	@echo 'Service conatiners, networks, and images were successfully removed.'
	@echo 'If you also wish to remove volumes, deleting all jenkins configuration and jobs,'
	@echo 'run this command:'
	@echo ''
	@echo '    docker-compose down -v'

# docker.run.$service
# This target fetches the docker image corresponding to the requested jenkins
# service and launches it into a new container.  If this is used to start
# jenkins again after stopping it, jenkins jobs and configuration will persist.
$(DOCKER_SERVICES:%=docker.run.%) : docker.run.% :
	docker-compose up -d $*

# docker.stop.$service
# Stop a given jenkins service.  They can be restarted later using
# docker.run.$service.
$(DOCKER_SERVICES:%=docker.stop.%) : docker.stop.% :
	docker-compose stop $*

# docker.running.$service
# Check if service is running.
$(DOCKER_SERVICES:%=docker.running.%) : docker.running.% :
	@docker-compose exec $* /bin/bash -c 'echo "'$*' is running"'

# Reconfigure jenkins_tools
docker.configure.jenkins_tools:
	@echo 'This makefile currently does not support reconfiguring jenkins_tools.'
	exit 1
