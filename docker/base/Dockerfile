FROM jenkins:1.651.3
MAINTAINER edxops

USER root
RUN apt-get update \
      && apt-get install -y sudo python-dev python-pip libmysqlclient-dev \
      && rm -rf /var/lib/apt/lists/*
RUN echo "jenkins ALL=NOPASSWD: ALL" >> /etc/sudoers
RUN curl https://raw.githubusercontent.com/edx/configuration/master/requirements.txt > /tmp/system-ansible.txt && pip install -r /tmp/system-ansible.txt

USER jenkins
COPY plugins.txt /usr/share/jenkins/plugins.txt
RUN /usr/local/bin/plugins.sh /usr/share/jenkins/plugins.txt
