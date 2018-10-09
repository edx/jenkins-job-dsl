SHELL := /usr/bin/env bash
.DEFAULT_GOAL := help

# List of services defined in docker-compose.yml.  Guard this with a check for
# the existence of the docker-compose program since we don't want to exit
# before showing help text.
ifneq (, $(shell which docker-compose))
    DOCKER_SERVICES := $(shell docker-compose config --services)
endif

.PHONY: help clean docker.clean $(DOCKER_SERVICES:%=docker.run.%) $(DOCKER_SERVICES:%=docker.stop.%) $(DOCKER_SERVICES:%=docker.running.%) $(DOCKER_SERVICES:%=docker.configure.%)

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

# docker.configure.$service
# This target picks up any user-specified ansible overrides, then reconfigures
# and restarts the specified service container.
docker.configure.jenkins_build : docker.configure.% : docker.running.%
# copy in user-specified ansible overrides. If no overrides specified, just
# duplicate the overrides already in the container. This seems redundant,
# but prevents us from having to write more complicated targets/recipes
	@if [ -f ansible_overrides.yml ]; then\
		docker cp ansible_overrides.yml $*:/ansible_overrides_extra.yml;\
	else\
		docker exec $* cp /ansible_overrides.yml /ansible_overrides_extra.yml;\
	fi
	# The jenkins:local-dev ansible tag is specific to jenkinses that use
	# the jenkins_common role.
	docker-compose exec $* /bin/bash -c "PYTHONUNBUFFERED=1 /edx/app/edx_ansible/venvs/edx_ansible/bin/ansible-playbook \
		-v $*.yml \
		-i '127.0.0.1,' \
		-c local \
		-e@/ansible_overrides.yml \
		-e@/ansible_overrides_extra.yml \
		-t 'jenkins:local-dev' \
		-vv"
	docker-compose restart $*

# Reconfigure jenkins_tools
docker.configure.jenkins_tools:
	@echo 'This makefile currently does not support reconfiguring jenkins_tools.'
	exit 1
