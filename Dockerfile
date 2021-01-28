# Ubuntu (Focal) based image
FROM ubuntu:20.04

# Metadata
LABEL maintainer="Nick Ruest <ruestn@gmail.com>, Helge Holzmann <helge@archive.org>"
LABEL description="Docker image for ars-cloud development"
LABEL website="https://archive-it.org"

EXPOSE 12341
EXPOSE 54040

VOLUME /app
VOLUME /data

# noninteractive + --no-install-recommends to avoid user input for package `tzdata`, which is a dependency of the following
RUN DEBIAN_FRONTEND=noninteractive apt-get -qq update && apt-get -qq install -y --no-install-recommends curl gnupg openjdk-11-jdk maven git wget

ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

WORKDIR /tmp

RUN wget -q http://scala-lang.org/files/archive/scala-2.12.8.deb && dpkg -i scala-2.12.8.deb

RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
RUN apt-get -qq update && apt-get -qq install -y sbt=1.3.8

ADD ./ /app/
WORKDIR /app
RUN sbt clean update compile

ENTRYPOINT ["sbt"]
CMD ["run"]
