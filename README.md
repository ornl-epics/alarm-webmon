Alarm System Web Monitor
========================

A simple read-only display of current alarms, similar to the Phoebus alarm table view.

Building
--------

Build with Maven:

   mvn clean package


Develop in Eclipse via File, Import, Maven, Existing Maven Projects.

**Docker**

Edit .env file with settings for git version and port number and docker/setenv.sh with your local site settings for the alarm server. Then:

```
docker-compose build
```


Running under Tomcat
--------------------

Set the following environment variables, for example in `$CATALINA_HOME/bin/setenv.sh`, `catalina.sh` or `tomcat.conf`, depending on version and installation details:

 * `ALARM_SERVER`: Kafka server host and port, defaults to `localhost:9092`.
 * `ALARM_CONFIG`: Alarm configuration root, defaults to `Accelerator`.

Place `alarm-webmon.war` in `$CATALINA_HOME/webapps`.
When tomcat starts up, the console will show something like this to
verify the settings and successful connection to Kafka.

    INFO: ===========================================
    INFO: Alarm Webmon /alarm-webmon started
    INFO: ALARM_SERVER=localhost:9092
    INFO: ALARM_CONFIG=Accelerator
    INFO: ===========================================
    INFO: Reading from start of 'Accelerator'

**Docker**

To run docker container (use -d option to run in detached mode):

```
docker-compose up
```

The status can be seen with docker ps. The status will be healthy if alarm-webmon webpage is reachable at the specified `${PORT_NUMBER}`
```
docker ps
```


Client URLs
-----------

`http://the_tomcat_host:8080/alarm-webmon` displays the alarm table,
updating at some slow, fixed period.

![Alarm Webmon](webmon.png)

The tables can be sorted by clicking on the column header.
