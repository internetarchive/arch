# Ubuntu (Focal) based image
FROM ubuntu:20.04

# Metadata
LABEL maintainer="Nick Ruest <ruestn@gmail.com>, Helge Holzmann <helge@archive.org>"
LABEL description="Docker image for ars-cloud development"
LABEL website="https://archive-it.org"

EXPOSE 12341

VOLUME /app
VOLUME /data

# noninteractive + --no-install-recommends to avoid user input for package `tzdata`, which is a dependency of the following
RUN DEBIAN_FRONTEND=noninteractive apt-get update && apt-get install -y --no-install-recommends curl gnupg openjdk-11-jdk maven git wget

ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

WORKDIR /tmp

RUN wget http://scala-lang.org/files/archive/scala-2.12.8.deb && dpkg -i scala-2.12.8.deb

RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
RUN apt-get update && apt-get install -y sbt=1.3.8

# initially load dependencies before installing AUT (the other way arround causes weird conflicts and missing classes)
ADD build.sbt .
# comment out AUT dependency as it's not available at this point
RUN sed -i -e 's/"io.archivesunleashed/\/\//g' build.sbt
ADD project/build.properties project/*.sbt project/
RUN sbt update

RUN git clone https://github.com/archivesunleashed/aut.git

WORKDIR aut

RUN git fetch origin pull/510/head:ars-cloud
RUN git checkout ars-cloud
RUN mvn clean install -DskipTests

# updating sbt with AUT installed
RUN sbt update

WORKDIR /app

ENTRYPOINT ["sbt"]
CMD ["run"]
