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
* Python 3.7 or higher
* Java 8
* Maven 3.6.3

For the rest of the guide, it is assumed that the Data profiler is located in `~/Data-Profiler`.

### Minikube Configuration

After minikube is installed a cluster can be started. The number of CPUs required for the Kubernetes cluster is 4 and roughly 16GB of memory.

```shell
minikube start --cpus 4 --memory 16384 --driver=docker --mount-string="${HOME}/Data-Profiler/infrastructure/standalone/data:/dp_data" --mount
```

Next, kubeconfig must be updated with the correct IP address of the running cluster.

```shell
minikube update-context
```

Lastly, the environment must be configured to use minikube's Docker daemon. Please note, you will need to run this command in any newly opened shell.

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

After Spark has been built, the `spark-hive-thriftserver` jar needs to be copied to the Data Profiler `spark-sql-client`.

```shell
mkdir ~/Data-Profiler/spark-sql/spark-sql-client/spark_jars
cp ~/spark/dist/jars/spark-hive-thriftserver_2.11-2.4.5.jar ~/Data-Profiler/spark-sql/spark-sql-client/spark_jars/
```

### Build and Start the Standalone Instance

The following command can be used to build all the dependencies and the standalone instance.

```shell
cd ~/Data-Profiler/infrastructure/standalone/bin
./standalone.py build
```

Once the services are built, the standalone instance can be deployed to the kubernetes cluster.

```shell
./standalone.py deploy
```

Lastly, you may need to port forward the UI because of a bug within minikube.

```shell
kubectl port-forward deployment/dp-ui 8080:80 --address='0.0.0.0'
```

## Supplying data

Data files found in the 'data' directory will be loaded by the containerized dp-accumulo deployment. Different files can be added to this folder.

## API

Try hitting the api with the following curl command:

```shell
curl http://localhost:9000/v1/datasets -H 'Authorization: dp-rou-key' -H 'Accept: application/json' -H 'X-Authorization-Token: local-developer' -H '\"LIST.Public_Data\",\"LIST.PUBLIC_DATA\"'
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
