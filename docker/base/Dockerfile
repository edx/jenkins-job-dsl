FROM jenkins:1.651.3
MAINTAINER edxops

USER root
RUN apt-get update \
      && apt-get install -y sudo python-dev python-pip libmysqlclient-dev \
      && rm -rf /var/lib/apt/lists/*
RUN echo "jenkins ALL=NOPASSWD: ALL" >> /etc/sudoers

USER jenkins
COPY plugins.txt /usr/share/jenkins/plugins.txt
RUN /usr/local/bin/plugins.sh /usr/share/jenkins/plugins.txt

USER root
RUN apt-get update \
    && apt-get install -y libssl-dev openssl \
    && wget https://www.python.org/ftp/python/3.5.2/Python-3.5.2.tgz \
    && tar xzvf Python-3.5.2.tgz \
    && cd Python-3.5.2 \
    && ./configure --prefix=/usr/local \
    && make \
    && make install
