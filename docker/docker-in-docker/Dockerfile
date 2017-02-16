FROM edxops/tools-jenkins:latest
MAINTAINER edxops

USER root
RUN apt-get update \
      && apt-get install -y sudo python-dev python-pip libmysqlclient-dev apt-transport-https ca-certificates \
      && rm -rf /var/lib/apt/lists/*
RUN apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D
COPY docker.list /etc/apt/sources.list.d/docker.list
RUN sudo apt-get update
RUN sudo apt-get install -y docker-engine
RUN echo "jenkins ALL=NOPASSWD: ALL" >> /etc/sudoers
