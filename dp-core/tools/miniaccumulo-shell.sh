#! /bin/bash

set -x
# Start miniaccumulo shell
#java -cp target/dataprofiler-tools-1.jar MiniAccumuloShell "${@}"

# start miniaccumulo.sh and grab the values replace for the parameters below
config_dir=${1:-/tmp/mini-accumulo14640390082738673465}
zookeeper1_port=${2:-2181}
zookeepers="localhost:${zookeeper1_port}"
config="${config_dir}/conf/client.conf"
java -cp target/dataprofiler-tools-1.jar com.dataprofiler.shell.MiniAccumuloShell \
        -u root \
        -p secret \
        -z miniInstance "${zookeepers}" \
        --config-file "${config}"
#\
#	"${@}"
