# API

# Local Running Instructions
The local API requires special DNS server which can be only launched in containers

`$ ./local-sbt run`

You can run other sbt commands from this wrapper as well

`$ ./local-sbt clean`


#### Other Considerations
See Setup For Local Development below.

You need to have a local `.env` file. See Configure The Environment below.  

Edit `const BASE_URL` in `data-profiler/dp-ui/src/modules/dataprofiler-api/index.js` to direct requests to the proper API environment.

# Setup For Local Development - Do This Once
```
$ brew install maven
$ cd data-profiler/dp-core && mvn clean install -Plocal -DskipTests
```

# Endpoints
* [Routes](conf/routes)


## To run against a local Accumulo
* Start miniaccumulo scripts in `tools` with a csv for test data
* Grab the dynamic zookeeper port
* Use your normal `.env` file for configurations, but replace with miniaccumulo parameters
* run the following with the updated parameters
```
sbt run -Daccumulo.instance=miniInstance -Daccumulo.password=<password> -Daccumulo.zookeepers=localhost:<dynamic zkport>
```
