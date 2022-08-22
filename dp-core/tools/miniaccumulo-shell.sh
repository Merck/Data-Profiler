#! /bin/bash

set -x

# To run locally compile the tools project with `mvn clean install -P local -DskipTests`
# Start miniaccumulo and load csv file
# try passing csv path like; src/test/resources/basic_test_data.csv
# Or pass blank args to get some "reasonable default" data as defined in MiniAccumuloWithData#startAndLoadReasonableDefaults



# Start miniaccumulo shell
#java -cp target/dataprofiler-tools-1.jar MiniAccumuloShell "${@}"

# start miniaccumulo.sh and grab the values replace for the parameters below

CONFIG=$(ls -dtr /tmp/mini-accumulo* | tail -n 1)
BASE_CONFIG_DIR=${1:-$CONFIG}
ZOOKEEPER_PORT=${2:-2181}
ZOOKEEPERS="localhost:${ZOOKEEPER_PORT}"
CONFIG="${BASE_CONFIG_DIR}/conf/client.conf"
java -cp target/dataprofiler-tools-1.jar com.dataprofiler.shell.MiniAccumuloShell \
        -u root \
        -p "" \
        -z miniInstance "${ZOOKEEPERS}" \
        --config-file "${CONFIG}"
