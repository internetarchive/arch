# Archive-It Research Services Cloud

A template for the integration of Archives Unleashed (AU) with Archive-It (AIT), designed as an independent web app, communicating with AIT only via APIs and direct HDFS access.

Demo running under https://webdata.archive-it.org/ait

## Docker

1. `git clone git@github.com:helgeho/ars-cloud.git`
2. `cd ars-cloud`
3. `docker build -t ars-cloud .`
4. `docker run -it -v "/home/nruest/Projects/au/sample-data/ars-cloud:/data" -v "/home/nruest/Projects/au/ars-cloud:/ars-cloud" -p 12341:12341 ars-cloud`
