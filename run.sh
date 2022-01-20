source /home/peb/ws/hadoop-env/cm-hadoop-2-cdh5.14.4-ARCH/setup-env.sh
export SPARK_HOME=/home/helge/bin/spark-2.4.5-bin-without-hadoop-scala-2.12
SPARK_DIST_CLASSPATH=$(hadoop classpath) /home/helge/bin/spark-2.4.5-bin-without-hadoop-scala-2.12/bin/spark-submit --driver-memory 8g --jars arch-assembly-0.1.0-deps.jar,stanford-corenlp-4.3.1.jar,stanford-corenlp-4.3.1-models.jar,jollyday.jar --class org.archive.webservices.ars.Arch arch-assembly-0.1.0.jar
