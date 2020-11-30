source /home/helge/bin/hadoop-2.5.0-cdh5.3.1/setup-env.sh
export SPARK_HOME=/home/helge/bin/spark-2.4.5-bin-without-hadoop-scala-2.12
java -cp "/home/helge/bin/spark-2.4.5-bin-without-hadoop-scala-2.12/jars/*:`hadoop classpath`:ars-cloud-assembly-0.1.0.jar" org.archive.webservices.ars.ArsCloud | grep -E 'INFO|WARN'
