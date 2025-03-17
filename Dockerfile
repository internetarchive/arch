# Ubuntu (Focal) based image
ARG BASE_IMAGE=ubuntu:20.04

# Build using the command: docker build --build-arg UID=$UID . -t ait-arch


###############################################################################
# Miniconda build stage
###############################################################################

FROM $BASE_IMAGE AS build-miniconda

ARG CONDA_INSTALL_SCRIPT=Miniconda3-py310_25.1.1-2-Linux-x86_64.sh
ARG CONDA_INSTALL_SCRIPT_SHA256=7f298109ab95b5436632973a04189a125282cc948f1dd1b03fa9cb6c71443915
ARG CONDA_INSTALL_SCRIPT_URL=https://repo.anaconda.com/miniconda/$CONDA_INSTALL_SCRIPT
ARG CONDA_DIR=/root/miniconda3
ARG WHISPER_MODEL_URL=https://openaipublic.azureedge.net/main/whisper/models/25a8566e1d0c1e2231d1c762132cd20e0f96a85d16145c3a00adf5d1ac670ead/base.en.pt

# Install system packages
RUN apt update && apt install -y \
    curl \
    git

WORKDIR /root

# Install Miniconda
RUN curl --silent --remote-name $CONDA_INSTALL_SCRIPT_URL
RUN echo "$CONDA_INSTALL_SCRIPT_SHA256 $CONDA_INSTALL_SCRIPT" | sha256sum --check
RUN bash $CONDA_INSTALL_SCRIPT -b -p $CONDA_DIR
ENV PATH="/root/miniconda3/bin:$PATH"
RUN conda install --yes --channel conda-forge conda-pack
RUN conda init


###############################################################################
# Whisper job artifacts build stage
###############################################################################

FROM build-miniconda AS build-whisper-artifacts

# Build the conda env.
RUN . ./.bashrc \
    && conda create --yes --name whisper-env python=3.10 \
    && conda activate whisper-env \
    && conda install --yes 'numpy<2' pytorch==2.0.0 torchaudio==2.0.0 pytorch-cuda=11.8 -c pytorch -c nvidia \
    && pip install git+https://github.com/openai/whisper.git \
    && conda install --yes --channel conda-forge ffmpeg \
    && conda pack -n whisper-env -o conda-whisper-env.tar.gz

# Download the transcription model
RUN curl --silent $WHISPER_MODEL_URL -o base.en.pt


###############################################################################
# TrOCR job artifacts build stage
###############################################################################

FROM build-miniconda AS build-trocr-artifacts

# Build the conda env.
RUN . ./.bashrc \
    && conda create --yes --name trocr-env python=3.10 \
    && conda activate trocr-env \
    && conda install --yes 'numpy<2' pytorch==1.11.0 torchvision==0.12.0 pytorch-cuda=11.8 -c pytorch -c nvidia \
    && pip install opencv-python==4.10.0.84 scikit-image==0.24.0 transformers==4.43.4 \
    && conda pack -n trocr-env -o conda-trocr-env.tar.gz

# Create trocr-models.tar.gz
## Download craft_mlt_25k.pth
WORKDIR /root/trocr-models/weights
RUN curl -OJs 'https://drive.usercontent.google.com/download?id=1Jk4eGD7crsqCCg9C9VjCLkMN3ze8kutZ&confirm=t'
## Download craft_refiner_CTW1500.pth
RUN curl -OJs 'https://drive.usercontent.google.com/download?id=1XSaFwBkOaFOdtk4Ane3DFyJGPRw6v5bO&confirm=t'
## Install git lfs
RUN curl -L --silent --remote-name https://github.com/git-lfs/git-lfs/releases/download/v3.6.1/git-lfs-linux-amd64-v3.6.1.tar.gz \
    && tar xf git-lfs-linux-amd64-v3.6.1.tar.gz \
    && git-lfs-3.6.1/install.sh \
    && git lfs install \
    && rm -rf ./git-lfs-3.6.1 git-lfs-linux-amd64-v3.6.1.tar.gz
## Clone trocr-base-handwritten repo
WORKDIR /root/trocr-models
RUN git clone https://huggingface.co/microsoft/trocr-base-handwritten
## Create the trocr-models.tar.gz archive
WORKDIR /root
RUN tar -czvf trocr-models.tar.gz -C trocr-models .

# Create craft-pytorch.tar.gz
RUN git clone https://github.com/clovaai/CRAFT-pytorch.git
RUN tar -czvf craft-pytorch.tar.gz -C CRAFT-pytorch .


###############################################################################
# Final build stage
###############################################################################

FROM $BASE_IMAGE

ARG UID
ARG DEBIAN_FRONTEND=noninteractive
ARG ARCH_USER_HOME=/home/arch
ARG ARCH_INSTALL_DIR=/opt/arch

ARG SPARK_TGZ_URL=https://archive.apache.org/dist/spark/spark-2.4.5/spark-2.4.5-bin-without-hadoop-scala-2.12.tgz
ARG SPARK_TGZ_PATH=$ARCH_USER_HOME/spark-2.4.5-bin-without-hadoop-scala-2.12.tgz
ARG SPARK_TGZ_CHECKSUM=aef59f0f9074a413461894601ac1714701f0eb486ce9721b5dacaa159d82fb60

ARG CORENLP_ZIP_URL=https://huggingface.co/stanfordnlp/CoreNLP/resolve/v4.5.6/stanford-corenlp-latest.zip
ARG CORENLP_ZIP_PATH=$ARCH_USER_HOME/stanford-corenlp-4.5.6.zip
ARG CORENLP_ZIP_CHECKSUM=9ed0f1eadf2f078f83e5fd55dc95c23a08a2f8af73a63428fb459be1e9d0fab3

ARG CORENLP_CHINESE_JAR_URL=https://huggingface.co/stanfordnlp/corenlp-chinese/resolve/v4.5.6/stanford-corenlp-models-chinese.jar
ARG CORENLP_CHINESE_JAR_CHECKSUM=e624af936cda0373e20b6f44a65fdfb1bc196e8b56761dc9659728d98150d5e0

ARG SPARKLING_GIT_REPO=https://github.com/internetarchive/Sparkling
ARG SPARKLING_SHA1=a5fd21586bef6799630250c7b2625ea36ad5e5ba
ARG SPARKLING_DIR=$ARCH_USER_HOME/sparkling

ARG ARCHIVESPARK_GIT_REPO=https://github.com/internetarchive/ArchiveSpark
ARG ARCHIVESPARK_SHA1=853ff1db7b8b57858cbb652ef8788deca5b65d2c
ARG ARCHIVESPARK_DIR=$ARCH_USER_HOME/archivespark

ARG SPARKLING_JAR_PATH=$SPARKLING_DIR/target/scala-2.12/sparkling-assembly-0.3.8-SNAPSHOT.jar
ARG ARCHIVESPARK_JAR_PATH=$ARCHIVESPARK_DIR/target/scala-2.12/archivespark-assembly-3.3.8-SNAPSHOT.jar
ARG CORENLP_DIR=$ARCH_USER_HOME/stanford-corenlp-4.5.6
ARG CORENLP_JAR_PATH=$CORENLP_DIR/stanford-corenlp-4.5.6.jar
ARG CORENLP_MODELS_JAR_PATH=$CORENLP_DIR/stanford-corenlp-4.5.6-models.jar
ARG CORENLP_CHINESE_JAR_PATH=$CORENLP_DIR/stanford-corenlp-4.5.6-models-chinese.jar
ARG JOLLYDAY_JAR_PATH=$CORENLP_DIR/jollyday.jar

ARG TEST_WARC_URL=https://archive.org/download/sample-warc-file/IIPC-COVID-Announcement.warc.gz

ARG HADOOP_NODE_LOCAL_TEMP_PATH=/arch-tmp
ARG HADOOP_NODE_LOCAL_TEMP_PATH_WHISPER=$HADOOP_NODE_LOCAL_TEMP_PATH/whisper/20240807195100
ARG HADOOP_NODE_LOCAL_TEMP_PATH_TROCR=$HADOOP_NODE_LOCAL_TEMP_PATH/trocr/20240807195100
ARG HDFS_JOB_ARTIFACT_PATH=/user/helge/arch-data
ARG HDFS_JOB_ARTIFACT_PATH_WHISPER=$HDFS_JOB_ARTIFACT_PATH/whisper
ARG HDFS_JOB_ARTIFACT_PATH_TROCR=$HDFS_JOB_ARTIFACT_PATH/trocr

# Metadata
LABEL maintainer="Derek Enos <derekenos@archive.org>, Helge Holzmann <helge@archive.org>"
LABEL description="Docker image for ARCH development"
LABEL website="https://arch.archive-it.org"

# Install required packages
RUN apt update && apt install -y \
    curl \
    gnupg \
    openjdk-8-jdk \
    git \
    unzip \
    jq \
    tmux

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
        && ln -s $JOLLYDAY_JAR_PATH $ARCH_INSTALL_DIR/lib/`basename $JOLLYDAY_JAR_PATH` \
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

# Ensure that the default config hadoopNodeLocalTempPath path exists, to which
# we will symlink the required AI-job-related assets.
RUN mkdir $HADOOP_NODE_LOCAL_TEMP_PATH && chown arch:arch $HADOOP_NODE_LOCAL_TEMP_PATH

# Copy in the Whisper job artifacts.
WORKDIR $HDFS_JOB_ARTIFACT_PATH_WHISPER
COPY --from=build-whisper-artifacts /root/conda-whisper-env.tar.gz .
COPY --from=build-whisper-artifacts /root/base.en.pt .
COPY ./job_scripts/whisper-run.py .
RUN chown --recursive arch:arch $HDFS_JOB_ARTIFACT_PATH_WHISPER

# Symlink whisper assets into expected HADOOP_NODE_LOCAL_TEMP_PATH_WHISPER path to prevent
# ARCH from creating a copy of the files on first run.
RUN mkdir -p $HADOOP_NODE_LOCAL_TEMP_PATH_WHISPER
RUN chown arch:arch $HADOOP_NODE_LOCAL_TEMP_PATH_WHISPER
RUN ln -s $HDFS_JOB_ARTIFACT_PATH_WHISPER/conda-whisper-env.tar.gz $HADOOP_NODE_LOCAL_TEMP_PATH_WHISPER/conda-whisper-env.tar.gz
RUN ln -s $HDFS_JOB_ARTIFACT_PATH_WHISPER/whisper-run.py $HADOOP_NODE_LOCAL_TEMP_PATH_WHISPER/whisper-run.py
RUN ln -s $HDFS_JOB_ARTIFACT_PATH_WHISPER/base.en.pt $HADOOP_NODE_LOCAL_TEMP_PATH_WHISPER/base.en.pt

# Copy in the TrOCR job artifacts.
WORKDIR $HDFS_JOB_ARTIFACT_PATH_TROCR
COPY --from=build-trocr-artifacts /root/conda-trocr-env.tar.gz .
COPY --from=build-trocr-artifacts /root/trocr-models.tar.gz .
COPY --from=build-trocr-artifacts /root/craft-pytorch.tar.gz .
COPY ./job_scripts/trocr-run.py .
RUN chown --recursive arch:arch .

# Symlink trocr assets into expected HADOOP_NODE_LOCAL_TEMP_PATH_TROCR path to prevent
# ARCH from creating a copy of the files on first run.
RUN mkdir -p $HADOOP_NODE_LOCAL_TEMP_PATH_TROCR
RUN chown arch:arch $HADOOP_NODE_LOCAL_TEMP_PATH_TROCR
RUN ln -s $HDFS_JOB_ARTIFACT_PATH_TROCR/conda-trocr-env.tar.gz $HADOOP_NODE_LOCAL_TEMP_PATH_TROCR/conda-trocr-env.tar.gz
RUN ln -s $HDFS_JOB_ARTIFACT_PATH_TROCR/trocr-models.tar.gz $HADOOP_NODE_LOCAL_TEMP_PATH_TROCR/trocr-models.tar.gz
RUN ln -s $HDFS_JOB_ARTIFACT_PATH_TROCR/craft-pytorch.tar.gz $HADOOP_NODE_LOCAL_TEMP_PATH_TROCR/craft-pytorch.tar.gz
# Have to actually copy the python script which attempts to do a local python module import
RUN cp $HDFS_JOB_ARTIFACT_PATH_TROCR/trocr-run.py $HADOOP_NODE_LOCAL_TEMP_PATH_TROCR/

# Copy entrypoint script.
COPY --chown=arch entrypoint.sh /entrypoint.sh

USER arch
WORKDIR $ARCH_INSTALL_DIR

RUN ["sbt", "dev/clean", "dev/update", "dev/compile"]

ENTRYPOINT ["/entrypoint.sh"]
CMD ["sbt", "dev/run"]

EXPOSE 12341
EXPOSE 54040
