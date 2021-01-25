# Archive-It Research Services Cloud

A template for the integration of Archives Unleashed (AU) with Archive-It (AIT), designed as an independent web app, communicating with AIT only via APIs and direct HDFS access.

Demo running under https://webdata.archive-it.org/ait

## Accomplishments

☑ Code compatibility with Java 11  
☑ AUT integrated ([PR](https://github.com/archivesunleashed/aut/pull/510)), first job migrated ([WebPagesExtraction](src/main/scala/org/archive/webservices/ars/processing/jobs/FileCountAndSize.scala))   
☑ Making use of Sparkling's WARC loader   
☑ Chained jobs (e.g., Spark ➡ shell post-processing)    
☑ Separation of queues (Spark, Generic, ...)  
☑ Working Dockerfile

## Docker

1. `git clone git@github.com:helgeho/ars-cloud.git`
2. `cd ars-cloud`
3. Create a config (`config/config.json`) for your Docker setup, e.g., by copying the included template: `cp config/docker.json config/config.json`
4. `docker build --no-cache -t ars-cloud .`
5. `docker run -ti --rm -p 12341:12341 -v /home/nruest/Projects/au/sample-data/ars-cloud:/data -v /home/nruest/Projects/au/ars-cloud:/app ars-cloud`
6. Open `http://localhost:12341/ait`