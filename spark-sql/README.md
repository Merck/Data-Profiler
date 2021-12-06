# Spark SQL

In the beginning, there was Spark SQL.

## Building The Base Image

The base image for the controller and client is Spark 2.4.5. To build a base image, you need to first clone Spark from GitHub.

    git clone https://github.com/apache/spark.git

After the respository has been cloned, you need to create a branch on tag `v2.4.5`.

    git checkout tags/v2.4.5 -b v2.4.5-branch

Next, spark needs to be compiled with support for hadoop-2.7, hive, hive-thriftserver, and kubernetes. The following command can be run from the root of the spark project. This will build Spark and take 15-30 minutes depending on your machine.

    dev/make-distribution.sh --name k8s-spark --tgz -Phadoop-2.7 -Phive -Phive-thriftserver -Pkubernetes

Finally, the docker image can be created and pushed to our docker repository. The following commands need to be run from the `dist` directory and will create a docker image with tag `spark-hive-k8s:2.4.5`.

    cd dist
    docker build -t container-registry.dataprofiler.com/spark-hive-k8s:2.4.5 -f kubernetes/dockerfiles/spark/Dockerfile .
    docker push container-registry.dataprofiler.com/spark-hive-k8s:2.4.5

## Spark-SQL-Controller

This is the controller that handles the rest requests and starts and stops clients. It can be built with the shell script `build_and_push.sh` within its directory.

To deploy the image to a namespace on the kube cluster, the following commands can be run from the directory `kube/helm-charts/namespace-deploy`.

    # To install a new image
    helm install -n preview dp-spark-sql-controller .

    # To remove the existing image
    helm uninstall -n preview dp-spark-sql-controller

## Spark-SQL-Client

The client is a user or service's personal spark application. It can be built with the shell script `build_and_push.sh` within its directory.

## Rotating Certs

Both the controller and client need certs to support SSL. The `keetool` command can be used to generate the certs. The following are the commands on how to generate the `keystore.jks` and `truststore.jks` certs.


    keytool -genkey -alias development.dataprofilercom -keyalg RSA -keystore keystore.jks -keysize 2048
    keytool -export -alias  development.dataprofilercom -file devcom.crt -keystore keystore.jks
    keytool -import -trustcacerts -alias development.dataprofilercom -file devcom.crt -keystore truststore.jks

After the certs have been created, they need to be pushed to kube as a secret. This needs to be done for each namespace: development, preview, and production.

    kubectl -n development create secret generic dp-spark-sql-certs --from-file=./keystore.jks --from-file=./truststore.jks

## Rest Endpoints

To connect to the controller on a specific namespace, you can use the `port-forward` command to forward the connection from the controller to your local machine. The following is an example of using port-forward to forward the traffic from the spark controller in development to your localhost over port 7999.

    kubectl port-forward -n development service/dp-spark-sql-controller 7999

### Status

The status endpoint returns a list of the running Spark SQL clients

    curl --request GET \
    --url http://localhost:7999/status

### Start Instance

Starting an instance can take the following options.

    curl --request POST \
    --url http://localhost:7999/start-instance \
    --header 'Content-Type: application/json' \
    --data '{
        "driverMemory": "10g",
        "driverStorageSize": "10G",
        "executorMemory": "10g",
        "executorStorageSize": "10G",
        "lazyCache": false,
        "loadCatalog": false,
        "numExecutors": 4,
        "username": "dmdpsfuser",
        "storageClass": "efs-sc"
    }'

### Stop Instance

To stop an instance, you must pass the `stop-instance` method the UUID of the Spark SQL client you want to stop.

    curl --request POST \
      --url http://localhost:7999/stop-instance/<uuid>

### Load Catalog

To load all of the datasets and tables for a user, call the `load-catalog` method with the UUID of the Spark SQL client for the user.

    curl --request POST \
    --url http://localhost:7999/load-catalog/<uuid>

### Show Databases

The `show-databases` method can show all of the databases that are loaded for a Spark SQL client.

    curl --request POST \
    --url http://localhost:7999/show-databases/<uuid>

The `show-databases` method can show all of the databases that are loaded for a Spark SQL client.


### Show Tables

The `show-tables` method can show all of the tables for a specified database that are loaded for a Spark SQL client.

    curl --request POST \
    --url http://localhost:7999/show-tables/<uuid> \
    --header 'Content-Type: application/json' \
    --data '{
        "dataset": "dataset_name"
    }'

### Create Tables

The `create-tables` endpoint can create individual tables for a Spark SQL client.

    curl --request POST \
    --url http://localhost:7999/create-tables/<uuid> \
    --header 'Content-Type: application/json' \
    --data '[
        {
            "dataset": "Dataset 1",
            "table": "table 1"
        },
        {
            "dataset": "Dataset 1",
            "table": "table 2"
        }
    ]'

### Drop Tables

The `drop-tables` endpoint can drop individual tables for a Spark SQL client.

    curl --request POST \
    --url http://localhost:7999/drop-tables/<uuid> \
    --header 'Content-Type: application/json' \
    --data '[
        {
            "dataset": "Dataset 1",
            "table": "table 1"
        },
        {
            "dataset": "Dataset 1",
            "table": "table 2"
        }
    ]'


## Connecting with beeline

To connect with the beeline command line client, the following connection string format can be used.

    !connect jdbc:hive2://xxx.xxx.xxx.xxx:<port_of_sql_client>/;ssl=true;sslTrustStore=<full_path_to_truststore.jks>;trustStorePassword=


The following is an example of how to create a table from the beeline client.

    create table TEST_SMALL using org.apache.spark.sql.sources.accumulo.v1 options (dataScanSpec="{\"dataset\":\"test\",\"table\":\"test_small\"}",auths="LIST.TEST");
