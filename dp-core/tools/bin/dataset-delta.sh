#!/usr/bin/env bash

set -euf -o pipefail -x

# optionally set ~/.dataprofiler/config
# ./muster set-dev-env kube-development-env

# example run
# ./bin/compile-for-local.sh && ./bin/dataset-delta.sh ./spec-samples/dataset-delta-all-datasets.json

base_path=$( dirname "${BASH_SOURCE[0]}" )
full_path=$(pwd -P)
spec="${1:-"${base_path}/../spec-samples/dataset-delta-all-datasets.json"}"
log4j_file=${2:-"file://${full_path}/src/test/resources/log4j.properties"}
java -cp ./target/dataprofiler-tools-1.jar \
  -Dlog4j.configuration="${log4j_file}" \
  com.dataprofiler.datasetdelta.cli.DatasetDeltaCli \
  --fname "${spec}"
