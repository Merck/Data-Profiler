# Data Profiler Standalone

This development tool gives a developer the ability to run the DataProfiler application stack on their local machines via Minikube and Docker Desktop.

The following containerized deployments are currently supported:

* dp-accumulo - MiniAccumuloWithData
* dp-postgres - postgres database
* dp-api - data_profiler_api
* dp-rou - rules_of_use
* dp-spark-sql-controller - Spark SQL Thriftserver Instance Controller
* dp-ui - DataProfiler User Interface

## Installation

### Requirements

The following is a list of required software to run the standalone image:

* Docker
* Minikube
* Helm
* Python 3.7 or higher
* Java 8
* Maven 3.6.3

For the rest of the guide, it is assumed that the Data profiler is loacated in `~/Data-Profiler`.

### Minikube Configuration

After minikube is installed a cluster can be started. The number of CPUs required for the Kubernetes cluster is 4 and roughly 16GB of memory.

```shell
minikube start --cpus 4 --memory 16384 --driver=docker
```

Next, kubeconfig must be updated with the correct IP address of the running cluster.

```shell
minikube update-context
```

Lastly, the enviroment must be configured to use minikube's Docker daemon. Please note, you will need to run this command in any newly opened shell.

```shell
eval $(minikube docker-env)
```

### Python Virtual Environment

While not stricly necessary, it can be very helpful to create a virtual environment for python to help isolate packages needed for the Data Profiler.

The following commands can be used to create and activate a virtual environment for the Data Profiler.

```shell
python3 -m venv ~/.venv/data_profiler
source ~/.venv/data_profiler/bin/activate
```

Once a virtual environment has been created, the necessary python packages can be installed.

```shell
cd ~/Data-Profiler/python_client/
pip install -r python_requirements.txt
pip install wheel
```

### Spark

The Data Profiler standalone image needs some Spark specific jars. These must be build from Spark 2.4.5. To clone the spark repo and build the project, the following commands can be used. Note: Spark will be cloned into the `~/spark` directory.

```shell
git clone https://github.com/apache/spark.git ~/spark
cd ~/spark
git checkout tags/v2.4.5 -b v2.4.5-branch
dev/make-distribution.sh --name k8s-spark --tgz -Phadoop-2.7 -Phive -Phive-thriftserver -Pkubernetes
cd dist
docker build -t container-registry.dataprofiler.com/spark-hive-k8s:2.4.5 -f kubernetes/dockerfiles/spark/Dockerfile .
```

After Spark has been built, the `spark-hive-thriftserver` jar needs to be coppied to the Data Profiler `spark-sql-client`.

```shell
mkdir ~/Data-Profiler/spark-sql/spark-sql-client/spark_jars
cp ~/spark/dist/jars/spark-hive-thriftserver_2.11-2.4.5.jar ~/Data-Profiler/spark-sql/spark-sql-client/spark_jars/
```

### Spark Client and Controller

Build the Spark client

```shell
cd ~/Data-Profiler/spark-sql/spark-sql-client
docker build -t container-registry.dataprofiler.com/spark-sql-client -f ./Dockerfile ../..
```

Build the Spark controller

```shell
cd ~/Data-Profiler/spark-sql/spark-sql-controller
docker build -t container-registry.dataprofiler.com/spark-sql-controller .
```

### Build Necessary Containers

The following containers in `~/Data-Profiler/infrastructure/docker` must be built.

#### Java

```shell
cd ~/Data-Profiler/infrastructure/docker/java
docker build -t container-registry.dataprofiler.com/java .
```

#### Play Framework Base

```shell
cd ~/Data-Profiler/infrastructure/docker/playframework_base
docker build -t container-registry.dataprofiler.com/playframework_base .
```

#### Hadoop

```shell
cd ~/Data-Profiler/infrastructure/docker/hadoop
docker build -t container-registry.dataprofiler.com/hadoop .
```

#### NodePG

```shell
cd ~/Data-Profiler/infrastructure/docker/nodepg
docker build -t container-registry.dataprofiler.com/nodepg .
```

#### NodeYarn

```shell
cd ~/Data-Profiler/infrastructure/docker/nodeyarn
docker build -t container-registry.dataprofiler.com/nodeyarn .
```

### Build the API

The API must be built and the resulting jars must be copied to the proper locations.

```shell
cd ~/Data-Profiler
./build.py --api-copy
```

Copy the jars.

```shell
mkdir ~/Data-Profiler/infrastructure/standalone/conf/dp-accumulo/jars
cp ~/Data-Profiler/dp-core/tools/target/dataprofiler-tools-1.jar Data-Profiler/infrastructure/standalone/conf/dp-accumulo/jars/
```

### Build and Start the Standalone Instance

The last requirements for the standalone instance is to build the services.

```shell
cd ~/Data-Profiler/infrastructure/standalone/
./bin/standalone.py build
```

Once the service are built, the standalone instance can be deployed to the kubernetes cluster.

```shell
./bin/standalone.py run
```

Lastly, you may need to port forward the UI because of a bug within minikube with minikube.

```shell
kubectl port-forward deployment/dp-ui 8080:80
```

## standalone.py

### Usage

```shell
$ ./bin/standalone.py -h        
usage: standalone.py [-h] {clean,run,build,status,push} ...

optional arguments:
-h, --help            show this help message and exit

sub-commands:
{deploy,destroy,build,status,push,expose}
```

Sub-Commands

* clean - delete cluster deployments
* build - builds libraries & images used in deployments
* run - create cluster deployments
* status - display deployment status

### Deploying / Starting Local Cluster

```shell
$ ./bin/standalone.py deploy --help

usage: standalone.py deploy [-h] [--branch BRANCH] [--app {dp-accumulo,dp-postgres,dp-api,dp-rou,dp-spark-sql-controller,dp-ui}]

optional arguments:
  -h, --help            show this help message and exit
  --branch BRANCH       branch to build from
  --app {dp-accumulo,dp-postgres,dp-api,dp-rou,dp-ui}
                        deployment name
```

Examples:

Deploy all available components and expose a few endpoints:

```shell
$ ./bin/standalone.py run

* Deploying Components
    dp-accumulo........................deployed
    dp-postgres........................deployed
    dp-api.............................deployed
    dp-rou.............................deployed
    dp-spark-sql-controller............deployed
    dp-ui..............................deployed
* Configuring Rules Of Use
  * Created api key as 'dp-rou-key'
  * Activating ROU attributes
    > activating system.admin
    > activating system.login
    > activating system.seen_joyride
    > activating LIST.PUBLIC_DATA
    > activating LIST.Public_Data
    > activating LIST.HR
    > activating LIST.EMPLOYEE
    > activating LIST.TECHMANAGER
    > activating LIST.SALESMANAGER
  * Updating ROU attributes for 'developer'

Standalone Minikube Status
* service default/dp-ui has no node port
* Starting tunnel for service dp-ui.
|-----------|-------|-------------|------------------------|
| NAMESPACE | NAME  | TARGET PORT |          URL           |
|-----------|-------|-------------|------------------------|
| default   | dp-ui |             | http://127.0.0.1:59209 |
|-----------|-------|-------------|------------------------|
http://127.0.0.1:59209
dp-api available at http://localhost:9000
dp-rou available at http://localhost:8081
dp-spark-sql-controller available at http://localhost:7999
```

Deploy a specific component:

```shell
$ ./bin/standalone.py run --app dp-ui

Deploying:
* dp-ui
  minikube cluster status
* service default/dp-ui has no node port
* Starting tunnel for service dp-ui.
  |-----------|-------|-------------|------------------------|
  | NAMESPACE | NAME  | TARGET PORT |          URL           |
  |-----------|-------|-------------|------------------------|
  | default   | dp-ui |             | http://127.0.0.1:59560 |
  |-----------|-------|-------------|------------------------|
  http://127.0.0.1:59560
  dp-api available at http://localhost:9000
  dp-rou available at http://localhost:8081
  dp-spark-sql-controller available at http://localhost:7999
```

#### API

Try hitting the api with the following curl command:

```shell
curl http://localhost:9000/v1/datasets -H 'Authorization: dp-rou-key' -H 'Accept: application/json' -H 'X-Authorization-Token: local-developer' -H '\"LIST.Public_Data\",\"LIST.PUBLIC_DATA\"'
```

#### Supplying data

Data files found in the root 'data' directory will be loaded by the containerized dp-accumulo deployment.

### Destroying / Terminating

Delete all deployments in the local kubernetes cluster

```shell
./bin/standalone.py deploy
```

Delete the specified <APP> deployment

```shell
./bin/standalone.py deploy --app <APP>
```

### Building deployments

Build all docker images only

```shell
./bin/standalone.py build
```

Build dataprofiler jars and all build docker images

```shell
./bin/standalone.py build --jars
```

Build libraries and docker images for a specific component:

```shell
./bin/standalone.py build --jars --app <component>
```

## Directory Structure

```shell
├── README.md
├── bin
│   ├── custom_formatter.py
│   ├── lib
│   ├── scripts
│   └── standalone.py
├── conf
│   ├── dp-accumulo
│   ├── dp-api
│   ├── dp-data-loading-daemon
│   ├── dp-postgres
│   ├── dp-rou
│   └── dp-ui
├── data
│   ├── basic_test_data.csv
│   ├── cell_level_visibilities_test_data.csv
│   └── tiny_test_data.csv
├── lib
│   ├── iterators
│   ├── python_packages
│   └── tools
├── sbin
│   ├── accumulo-shell.sh
│   ├── cluster-shell.sh
│   ├── postgres-shell.sh
│   └── spark-shell.sh
└── venv
```

* bin - main executables
* conf - kubernetes deployment configurations as well as any extra settings required to run on the local cluster
* data - seed data for the Accumulo instance
* sbin - helper scripts for use with the local cluster

## Utilities

### Accumulo Shell

```shell
./sbin/accumulo-shell.sh

root@miniInstance>
```

### Postgres Shell

```shell
./sbin/postgres-shell.sh

psql (13.2 (Debian 13.2-1.pgdg100+1))
Type "help" for help.

postgres=#
```

### Misc

After some time, the process of constantly building images may fill up cache space.
Run the following to free up space:

```shell
minikube ssh -- docker system prune
```
