source /home/webcrawl/hadoop-env/prod/setup-env.sh
export SPARK_HOME=/home/helge/bin/spark-2.4.5-bin-without-hadoop-scala-2.12
SPARK_DIST_CLASSPATH=$(hadoop classpath) /home/helge/bin/spark-2.4.5-bin-without-hadoop-scala-2.12/bin/spark-submit --jars ars-cloud-assembly-0.1.0-deps.jar --class org.archive.webservices.ars.ArsCloud ars-cloud-assembly-0.1.0.jar
