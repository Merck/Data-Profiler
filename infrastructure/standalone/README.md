# Data Profiler Standalone

The standalone environment gives anyone the ability to run the Data Profiler for demonstration purposes or the ability develop features via running the Data Profiler application stack on your local machine via Minikube and Docker Desktop.

The following containerized deployments are currently supported:

* backend - The Data Profiler backend storage
* postgres - A postgres database
* api - Data Profiler API
* rou - Data Profiler custom access controller
* ui - Data Profiler User Interface

## Installation

### Requirements

The following is a list of required software to run the standalone image:

* Docker
* Minikube
* Python 3.7 or higher

To develop using the standalone image the following are also required:

* Java 11
* Maven 3.6.3

For the rest of the guide, it is assumed that the Data profiler is located in `~/Data-Profiler`.

### Minikube Configuration

After minikube is installed a cluster can be started. The number of CPUs required for the Kubernetes cluster is 4 and roughly 8GB of memory.

```shell
minikube start --cpus 4 --memory 8096 --driver=docker --mount-string="${HOME}/Data-Profiler/infrastructure/standalone/data:/dp_data" --mount
```

Next, kubeconfig must be updated with the correct IP address of the running cluster and ingress should be installed.

```shell
minikube update-context
minikube addons enable ingress
```

Lastly, the environment must be configured to use minikube's Docker daemon. **Please note, you will need to run this command in any newly opened shell.**

```shell
eval $(minikube docker-env)
```

### Python Virtual Environment

While not strictly necessary, it can be very helpful to create a virtual environment for python to help isolate packages needed for the Data Profiler.

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

### Standalone Configuration

Before building the standalone instance you must create the configuration file for the instance. The easiest way to start is to create a copy the existing example.

```shell
cp ~/Data-Profiler/infrastructure/standalone/conf/env/env-vars.yaml.example ~/Data-Profiler/infrastructure/standalone/conf/env/env-vars.yaml
```

## Start the Standalone Instance

If you only want to run the Data Profiler locally without doing any development, you only need to run the following command. This will download, install, and start the Data Profiler.

```shell
./standalone.py deploy
```

When the deploy is complete, you can visit the Data Profiler by following in your local browser by visiting <http://localhost:8080>.

## For Development

The following command can be used to build all the dependencies and the standalone instance.

```shell
cd ~/Data-Profiler/infrastructure/standalone/bin
./standalone.py build
```

Once the services are built, the standalone instance can be deployed to the kubernetes cluster.

```shell
./standalone.py deploy
```

When the deploy is complete, you can visit the Data Profiler by following in your local browser by visiting <http://localhost:8080>.

## Stopping the Standalone Instance

To stop the standalone instance, the following command can be used.

```shell
./standalone.py terminate
```

## Supplying Data

Data files found in the 'data' directory will be loaded by the containerized backend deployment. Different files can be added to this folder.

## API

Try hitting the api with the following curl command:

```shell
curl http://localhost:9000/v1/datasets -H 'Authorization: dp-rou-key' -H 'Accept: application/json' -H 'X-Authorization-Token: local-developer' -H '\"LIST.PUBLIC_DATA\",\"LIST.PRIVATE_DATA\"'
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

### Building Spark (Optional)

Spark is automatically pulled in for the backend container, but if you would like to build a spark image, you can do so with the following instructions.

The Data Profiler standalone image needs some Spark specific jars. These must be build from Spark 3.3.0. To clone the spark repo and build the project, the following commands can be used. Note: Spark will be cloned into the `~/spark` directory.

```shell
git clone https://github.com/apache/spark.git ~/spark
cd ~/spark
git checkout tags/v3.3.0 -b v3.3.0-branch
./dev/make-distribution.sh --name k8s-spark --tgz -Dhadoop.version=3.3.4 -Dzookeeper.version=3.4.14 -Phive -Phive-thriftserver -Pkubernetes
./bin/docker-image-tool.sh -u 0 -r big-wave-tech -t 3.3.0 -f ./dist/kubernetes/dockerfiles/spark/Dockerfile build.
```

After Spark has been built, the `spark-hive-thriftserver` jar needs to be copied to the Data Profiler `spark-sql-client`.

```shell
mkdir ~/Data-Profiler/spark-sql/spark-sql-client/spark_jars
cp ~/spark/dist/jars/spark-hive-thriftserver_2.12-3.3.0.jar ~/Data-Profiler/spark-sql/spark-sql-client/spark_jars/
```
