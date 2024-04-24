# Ubuntu (Focal) based image
FROM ubuntu:20.04

# Build using the command: docker build --build-arg UID=$UID . -t arch

ARG UID
ARG DEBIAN_FRONTEND=noninteractive
ARG ARCH_USER_HOME=/home/arch
ARG ARCH_INSTALL_DIR=/opt/arch

ARG SPARK_TGZ_URL=https://archive.apache.org/dist/spark/spark-2.4.5/spark-2.4.5-bin-without-hadoop-scala-2.12.tgz
ARG SPARK_TGZ_PATH=$ARCH_USER_HOME/spark-2.4.5-bin-without-hadoop-scala-2.12.tgz
ARG SPARK_TGZ_CHECKSUM=aef59f0f9074a413461894601ac1714701f0eb486ce9721b5dacaa159d82fb60

ARG CORENLP_ZIP_URL=https://huggingface.co/stanfordnlp/CoreNLP/resolve/v4.5.1/stanford-corenlp-latest.zip
ARG CORENLP_ZIP_PATH=$ARCH_USER_HOME/stanford-corenlp-4.5.1.zip
ARG CORENLP_ZIP_CHECKSUM=7d6f9d01a595d0f783ce805b9c32bcc8789cd2f299de719b2c71801bbd88605b

ARG CORENLP_CHINESE_JAR_URL=https://huggingface.co/stanfordnlp/corenlp-chinese/resolve/v4.3.1/stanford-corenlp-models-chinese.jar
ARG CORENLP_CHINESE_JAR_CHECKSUM=3a7f4feafe5cb077f92973ccbe0cca383b851d921ae0948f067ab67ef2ef0237
ARG CORENLP_CHINESE_JAR_PATH=$ARCH_USER_HOME/stanford-corenlp-models-chinese.jar

ARG SPARKLING_GIT_REPO=https://github.com/internetarchive/Sparkling
ARG SPARKLING_SHA1=d7264212eacc5df001bbec36a69b96aca4925d79
ARG SPARKLING_DIR=$ARCH_USER_HOME/sparkling

ARG ARCHIVESPARK_GIT_REPO=https://github.com/internetarchive/ArchiveSpark
ARG ARCHIVESPARK_SHA1=abb7b03603b48ec6e60b97e2c006f224ab95fa85
ARG ARCHIVESPARK_DIR=$ARCH_USER_HOME/archivespark

ARG SPARKLING_JAR_PATH=$SPARKLING_DIR/target/scala-2.12/sparkling-assembly-0.3.8-SNAPSHOT.jar
ARG ARCHIVESPARK_JAR_PATH=$ARCHIVESPARK_DIR/target/scala-2.12/archivespark-assembly-3.3.8-SNAPSHOT.jar
ARG CORENLP_DIR=$ARCH_USER_HOME/stanford-corenlp-4.5.1
ARG CORENLP_JAR_PATH=$CORENLP_DIR/stanford-corenlp-4.5.1.jar
ARG CORENLP_MODELS_JAR_PATH=$CORENLP_DIR/stanford-corenlp-4.5.1-models.jar

ARG TEST_WARC_URL=https://archive.org/download/sample-warc-file/IIPC-COVID-Announcement.warc.gz

# Metadata
LABEL maintainer="Nick Ruest <ruestn@gmail.com>, Helge Holzmann <helge@archive.org>"
LABEL description="Docker image for ARCH development"
LABEL website="https://arch.archive-it.org"

# Install required packages
RUN apt update && apt install -y \
    curl \
    gnupg \
    openjdk-8-jdk \
    git \
    unzip \
    jq

# Install maven after java 8
RUN apt install -y maven

# Set JAVA_HOME
RUN printf "JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64\n" >> /etc/environment

# Install scala v2.12.8
WORKDIR /tmp
RUN curl -sL --output /tmp/scala-2.12.8.deb http://scala-lang.org/files/archive/scala-2.12.8.deb \
    && dpkg -i /tmp/scala-2.12.8.deb

# Install sbt v1.3.8
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
RUN apt update && apt install -y sbt=1.3.8

# Create the arch user
RUN useradd --create-home --home-dir=$ARCH_USER_HOME --uid $UID arch
USER arch

# Download Spark
RUN curl -sL --output $SPARK_TGZ_PATH $SPARK_TGZ_URL \
    && echo "$SPARK_TGZ_CHECKSUM  $SPARK_TGZ_PATH" | sha256sum --check \
    && tar -xzf $SPARK_TGZ_PATH -C `dirname $SPARK_TGZ_PATH`

# Download Standford CoreNLP
RUN curl -sL --output $CORENLP_ZIP_PATH $CORENLP_ZIP_URL \
    && echo "$CORENLP_ZIP_CHECKSUM  $CORENLP_ZIP_PATH" | sha256sum --check \
    && unzip $CORENLP_ZIP_PATH -d $ARCH_USER_HOME \
    && rm $CORENLP_ZIP_PATH

# Download Standford CoreNLP Chinese model
RUN curl -sL --output $CORENLP_CHINESE_JAR_PATH $CORENLP_CHINESE_JAR_URL \
    && echo "$CORENLP_CHINESE_JAR_CHECKSUM  $CORENLP_CHINESE_JAR_PATH" | sha256sum --check

# Clone and build the Sparkling assembly
WORKDIR $SPARKLING_DIR
RUN git clone $SPARKLING_GIT_REPO . \
    && git reset --hard $SPARKLING_SHA1 \
    && sbt clean assembly publishLocal

# Clone and build the ArchiveSpark assembly
WORKDIR $ARCHIVESPARK_DIR
RUN git clone $ARCHIVESPARK_GIT_REPO . \
    && git reset --hard $ARCHIVESPARK_SHA1 \
    && sbt clean assembly publishLocal

# Copy in the ARCH source
COPY --chown=arch ./ $ARCH_INSTALL_DIR
WORKDIR $ARCH_INSTALL_DIR

# If they don't already exist as a result of being copied in from the local arch dir, symlink JARs into
# $ARCH_INSTALL_DIR/lib to make sbt dev/run happy
RUN test -L $ARCH_INSTALL_DIR/lib/`basename $SPARKLING_JAR_PATH` || ( \
        ln -s $SPARKLING_JAR_PATH $ARCH_INSTALL_DIR/lib/`basename $SPARKLING_JAR_PATH` \
        && ln -s $ARCHIVESPARK_JAR_PATH $ARCH_INSTALL_DIR/lib/`basename $ARCHIVESPARK_JAR_PATH` \
        && ln -s $CORENLP_CHINESE_JAR_PATH $ARCH_INSTALL_DIR/lib/`basename $CORENLP_CHINESE_JAR_PATH` \
        && ln -s $CORENLP_JAR_PATH $ARCH_INSTALL_DIR/lib/`basename $CORENLP_JAR_PATH` \
        && ln -s $CORENLP_MODELS_JAR_PATH $ARCH_INSTALL_DIR/lib/`basename $CORENLP_MODELS_JAR_PATH` \
    )

# ARCH will happily create the job output directories as needed, but will fail if the log
# directory does not exist, so let's create it in the event that the image is run without
# a local .../shared mount.
RUN mkdir -p /opt/arch/shared/log

USER root

# Create a sendmail symlink to our dummy script
RUN chmod +x $ARCH_INSTALL_DIR/src/main/bash/sendmail && ln -s $ARCH_INSTALL_DIR/src/main/bash/sendmail /usr/sbin/sendmail

# Download a WARC to serve as data for the built-in ARCH Test Collection
RUN mkdir -p /user/helge/arch-test-collection \
    && curl -sL --output /user/helge/arch-test-collection/test.warc.gz $TEST_WARC_URL \
    && chown --recursive arch:arch /user

# Copy entrypoint script.
COPY --chown=arch entrypoint.sh /entrypoint.sh

USER arch

RUN ["sbt", "dev/clean", "dev/update", "dev/compile"]

ENTRYPOINT ["/entrypoint.sh"]
CMD ["sbt", "dev/run"]

EXPOSE 12341
EXPOSE 54040
