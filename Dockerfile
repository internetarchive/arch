# Ubuntu (Focal) based image
ARG BASE_IMAGE=ubuntu:20.04

# Build using the command: docker buildx build --build-arg UID=$UID . -t ait-arch --load

###############################################################################
# Miniconda build stage
###############################################################################

FROM $BASE_IMAGE AS build-miniconda

# Architecture detection. TARGETARCH is automatically set by Docker BuildKit when using --platform
ARG TARGETARCH
ARG CONDA_INSTALL_SCRIPT=Miniconda3-py310_25.1.1-2-Linux-x86_64.sh
ARG CONDA_INSTALL_SCRIPT_SHA256=7f298109ab95b5436632973a04189a125282cc948f1dd1b03fa9cb6c71443915
# ARM_CONDA_INSTALL_SCRIPT_SHA256=5f61143e93d9d48a82aa99a1d7b1c77561f599b9a67ab954862e6e8d6a25c0cc
# Override CONDA_INSTALL_SCRIPT and CONDA_INSTALL_SCRIPT_SHA256 via build args for ARM64
# For ARM64: --build-arg CONDA_INSTALL_SCRIPT=Miniconda3-py310_25.1.1-2-Linux-aarch64.sh --build-arg CONDA_INSTALL_SCRIPT_SHA256=<sha256>
ARG CONDA_INSTALL_SCRIPT_URL=https://repo.anaconda.com/miniconda/$CONDA_INSTALL_SCRIPT
ARG CONDA_DIR=/root/miniconda3

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

# Build conda environments
## pytorch-1.11.0
# Copy both amd64 and arm64 versions, then select based on architecture
COPY job_artifacts/conda-envs/pytorch-1.11.0.yml ./pytorch-1.11.0-amd64.yml
COPY job_artifacts/conda-envs/pytorch-1.11.0-arm64.yml ./pytorch-1.11.0-arm64.yml
RUN ARCH=$TARGETARCH && \
    if [ "$ARCH" = "arm64" ] || [ "$ARCH" = "arm" ]; then \
        cp pytorch-1.11.0-arm64.yml pytorch-1.11.0.yml; \
    else \
        cp pytorch-1.11.0-amd64.yml pytorch-1.11.0.yml; \
    fi && \
    conda env create -p pytorch-1.11.0 -f pytorch-1.11.0.yml \
    && conda pack -n pytorch-1.11.0

## pytorch-2.0.0_ffmpeg-6.1.1
# Copy both amd64 and arm64 versions, then select based on architecture
COPY job_artifacts/conda-envs/pytorch-2.0.0_ffmpeg-6.1.1.yml ./pytorch-2.0.0_ffmpeg-6.1.1-amd64.yml
COPY job_artifacts/conda-envs/pytorch-2.0.0_ffmpeg-6.1.1-arm64.yml ./pytorch-2.0.0_ffmpeg-6.1.1-arm64.yml
RUN ARCH=$TARGETARCH && \
    if [ "$ARCH" = "arm64" ] || [ "$ARCH" = "arm" ]; then \
        cp pytorch-2.0.0_ffmpeg-6.1.1-arm64.yml pytorch-2.0.0_ffmpeg-6.1.1.yml; \
    else \
        cp pytorch-2.0.0_ffmpeg-6.1.1-amd64.yml pytorch-2.0.0_ffmpeg-6.1.1.yml; \
    fi && \
    conda env create -p pytorch-2.0.0_ffmpeg-6.1.1 -f pytorch-2.0.0_ffmpeg-6.1.1.yml \
    && conda pack -n pytorch-2.0.0_ffmpeg-6.1.1

###############################################################################
# Whisper job artifacts build stage
###############################################################################

FROM build-miniconda AS build-whisper-artifacts

# Download dependencies / models
COPY job_artifacts/whisper/download_deps.sh .
RUN ./download_deps.sh

###############################################################################
# TrOCR job artifacts build stage
###############################################################################

FROM build-miniconda AS build-trocr-artifacts

# Install git lfs (required for the next step)
# Use architecture-specific git-lfs binary
ARG TARGETARCH
RUN ARCH=$TARGETARCH && \
    if [ "$ARCH" = "arm64" ] || [ "$ARCH" = "arm" ]; then \
        GIT_LFS_ARCH=arm64; \
    else \
        GIT_LFS_ARCH=amd64; \
    fi && \
    curl -L --silent --remote-name https://github.com/git-lfs/git-lfs/releases/download/v3.6.1/git-lfs-linux-${GIT_LFS_ARCH}-v3.6.1.tar.gz && \
    tar xf git-lfs-linux-${GIT_LFS_ARCH}-v3.6.1.tar.gz && \
    git-lfs-3.6.1/install.sh && \
    git lfs install && \
    rm -rf ./git-lfs-3.6.1 git-lfs-linux-${GIT_LFS_ARCH}-v3.6.1.tar.gz

# Download dependencies / models
COPY job_artifacts/trocr/download_deps.sh .
RUN ./download_deps.sh

###############################################################################
# Final build stage
###############################################################################

FROM $BASE_IMAGE

ARG UID
ARG TARGETARCH
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
ARG SPARKLING_SHA1=73cbcc95d280d3a736f6896580e208a0df186aaa
ARG SPARKLING_DIR=$ARCH_USER_HOME/sparkling

ARG ARCHIVESPARK_GIT_REPO=https://github.com/internetarchive/ArchiveSpark
ARG ARCHIVESPARK_SHA1=9ac4ac710803cb682fde8e0e832e3f1072994c01
ARG ARCHIVESPARK_DIR=$ARCH_USER_HOME/archivespark

ARG SPARKLING_JAR_PATH=$SPARKLING_DIR/target/scala-2.12/sparkling-assembly-0.3.8-SNAPSHOT.jar
ARG ARCHIVESPARK_JAR_PATH=$ARCHIVESPARK_DIR/target/scala-2.12/archivespark-assembly-3.3.8-SNAPSHOT.jar
ARG CORENLP_DIR=$ARCH_USER_HOME/stanford-corenlp-4.5.6
ARG CORENLP_JAR_PATH=$CORENLP_DIR/stanford-corenlp-4.5.6.jar
ARG CORENLP_MODELS_JAR_PATH=$CORENLP_DIR/stanford-corenlp-4.5.6-models.jar
ARG CORENLP_CHINESE_JAR_PATH=$CORENLP_DIR/stanford-corenlp-4.5.6-models-chinese.jar
ARG JOLLYDAY_JAR_PATH=$CORENLP_DIR/jollyday.jar

ARG TEST_WARC_URL=https://archive.org/download/sample-warc-file/IIPC-COVID-Announcement.warc.gz
ARG TEST_MP3_URL=https://archive.org/download/tvtunes_11560/Spongebob%20Squarepants.mp3

ARG HADOOP_NODE_LOCAL_TEMP_PATH=/arch-tmp
ARG HADOOP_NODE_LOCAL_TEMP_PATH_SMOKE=$HADOOP_NODE_LOCAL_TEMP_PATH/smoke-test
ARG HADOOP_NODE_LOCAL_TEMP_PATH_WHISPER=$HADOOP_NODE_LOCAL_TEMP_PATH/whisper/20240807195100
ARG HADOOP_NODE_LOCAL_TEMP_PATH_TROCR=$HADOOP_NODE_LOCAL_TEMP_PATH/trocr/20240807195100

# Metadata
LABEL maintainer="Derek Enos <derekenos@archive.org>, Helge Holzmann <helge@archive.org>"
LABEL description="Docker image for ARCH development"
LABEL website="https://arch.archive-it.org"

# Set default collation / encoding
ENV LC_ALL=C.UTF-8

# Install required packages
RUN apt update && apt upgrade -y && apt install -y \
    curl \
    gnupg \
    openjdk-8-jdk \
    git \
    unzip \
    jq \
    tmux \
    python3

# Install maven after java 8
RUN apt install -y maven

# Set JAVA_HOME (architecture-aware)
ARG TARGETARCH
RUN ARCH=$TARGETARCH && \
    if [ "$ARCH" = "arm64" ] || [ "$ARCH" = "arm" ]; then \
        printf "JAVA_HOME=/usr/lib/jvm/java-8-openjdk-arm64\n" >> /etc/environment; \
    else \
        printf "JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64\n" >> /etc/environment; \
    fi

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

RUN mkdir $ARCH_USER_HOME/.sbt
COPY sbt-repositories $ARCH_USER_HOME/.sbt/repositories

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

# If they don't already exist as a result of being copied in from the local arch dir, individually symlink JARs into
# $ARCH_INSTALL_DIR/lib to make sbt dev/run happy
RUN \
    test -L $ARCH_INSTALL_DIR/lib/$(basename $SPARKLING_JAR_PATH) || ln -s $SPARKLING_JAR_PATH $ARCH_INSTALL_DIR/lib/$(basename $SPARKLING_JAR_PATH) ; \
    test -L $ARCH_INSTALL_DIR/lib/$(basename $ARCHIVESPARK_JAR_PATH) || ln -s $ARCHIVESPARK_JAR_PATH $ARCH_INSTALL_DIR/lib/$(basename $ARCHIVESPARK_JAR_PATH) ; \
    test -L $ARCH_INSTALL_DIR/lib/$(basename $CORENLP_CHINESE_JAR_PATH) || ln -s $CORENLP_CHINESE_JAR_PATH $ARCH_INSTALL_DIR/lib/$(basename $CORENLP_CHINESE_JAR_PATH) ; \
    test -L $ARCH_INSTALL_DIR/lib/$(basename $CORENLP_JAR_PATH) || ln -s $CORENLP_JAR_PATH $ARCH_INSTALL_DIR/lib/$(basename $CORENLP_JAR_PATH) ; \
    test -L $ARCH_INSTALL_DIR/lib/$(basename $CORENLP_MODELS_JAR_PATH) || ln -s $CORENLP_MODELS_JAR_PATH $ARCH_INSTALL_DIR/lib/$(basename $CORENLP_MODELS_JAR_PATH) ; \
    test -L $ARCH_INSTALL_DIR/lib/$(basename $JOLLYDAY_JAR_PATH) || ln -s $JOLLYDAY_JAR_PATH $ARCH_INSTALL_DIR/lib/$(basename $JOLLYDAY_JAR_PATH)

# ARCH will happily create the job output directories as needed, but will fail if the log
# directory does not exist, so let's create it in the event that the image is run without
# a local .../shared mount.
RUN mkdir -p /opt/arch/shared/log

USER root

# Create a sendmail symlink to our dummy script
RUN chmod +x $ARCH_INSTALL_DIR/src/main/bash/sendmail && ln -s $ARCH_INSTALL_DIR/src/main/bash/sendmail /usr/sbin/sendmail

# Download a WARC (and an MP3 for automated job tests) to serve as data
# for the built-in ARCH Test Collection
RUN mkdir -p /user/arch/arch-test-collection \
    && curl -sL --output /user/arch/arch-test-collection/test.warc.gz $TEST_WARC_URL \
    && curl -sL --output /user/arch/arch-test-collection/test.mp3 $TEST_MP3_URL \
    && chown --recursive arch:arch /user

# Ensure that the default config hadoopNodeLocalTempPath path exists
RUN mkdir $HADOOP_NODE_LOCAL_TEMP_PATH && chown arch:arch $HADOOP_NODE_LOCAL_TEMP_PATH

# Initialize PySmokeTest working dir
WORKDIR $HADOOP_NODE_LOCAL_TEMP_PATH_SMOKE
COPY job_artifacts/smoke-test/test-run.py .
RUN chown -R arch:arch $HADOOP_NODE_LOCAL_TEMP_PATH_SMOKE

# Initialize whipser working dir
WORKDIR $HADOOP_NODE_LOCAL_TEMP_PATH_WHISPER
COPY --from=build-miniconda /root/pytorch-2.0.0_ffmpeg-6.1.1.tar.gz .
COPY --from=build-whisper-artifacts /root/base.en.pt .
COPY job_artifacts/whisper/pyproject.toml .uv/
COPY job_artifacts/whisper/whisper-run.py .
RUN chown -R arch:arch $HADOOP_NODE_LOCAL_TEMP_PATH_WHISPER

# Initialize TrOCR working dir
WORKDIR $HADOOP_NODE_LOCAL_TEMP_PATH_TROCR
COPY --from=build-miniconda /root/pytorch-1.11.0.tar.gz .
COPY --from=build-trocr-artifacts /root/trocr-models/ .
COPY --from=build-trocr-artifacts /root/CRAFT-pytorch/ .
RUN touch trocr-models.tar.gz._unpacked craft-pytorch.tar.gz._unpacked
COPY job_artifacts/trocr/pyproject.toml .uv/
COPY job_artifacts/trocr/trocr-run.py .
RUN chown -R arch:arch $HADOOP_NODE_LOCAL_TEMP_PATH_TROCR

# Copy entrypoint script.
COPY --chown=arch entrypoint.sh /entrypoint.sh

# Convert Windows to Linux line endings (in case the file was copied from Windows)
RUN sed -i 's/\r$//' /entrypoint.sh

USER arch
WORKDIR $ARCH_INSTALL_DIR

# Build ARCH
RUN sbt dev/clean dev/update dev/compile dev/publishLocal

# Build ARCH job plugins
RUN find job_plugins/ -mindepth 1 -maxdepth 1 -type d -exec bash -c "cd {}; sbt assembly" \;

ENTRYPOINT ["/entrypoint.sh"]
CMD ["sbt", "dev/run"]

EXPOSE 12341
EXPOSE 54040
