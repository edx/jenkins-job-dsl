FROM jenkins:latest
MAINTAINER edxops

USER root
RUN apt-get update \
      && apt-get install -y sudo python-dev python-pip libmysqlclient-dev \
      && rm -rf /var/lib/apt/lists/*
RUN echo "jenkins ALL=NOPASSWD: ALL" >> /etc/sudoers

USER jenkins
COPY plugins.txt /usr/share/jenkins/plugins.txt
RUN /usr/local/bin/plugins.sh /usr/share/jenkins/plugins.txt
