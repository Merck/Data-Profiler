#!/usr/bin/env bash

set -euf -o pipefail -x

# optionally set ~/.dataprofiler/config
# ./muster set-dev-env kube-development-env

# example run
# ./bin/compile-for-local.sh && ./bin/sql-sync.sh ./spec-samples/sql-sync-spec.json csv tmp.csv
# ./bin/compile-for-local.sh && ./bin/sql-sync.sh ./spec-samples/sql-sync-spec.json json tmp.json
# ./bin/compile-for-local.sh && ./bin/sql-sync.sh ./spec-samples/sql-sync-spec.json jdbc '"public.test-export"'

base_path=$( dirname "${BASH_SOURCE[0]}" )
spec="${1:-"${base_path}/../spec-samples/sql-sync-spec.json"}"
outputmode="${2:-csv}" # csv, json, or jdbc
outpath=${3:-tmp.out} # "public.test-export" eg schema.tablename if jdbc

full_path=$(pwd -P)
log4j_file=${4:-"file://${full_path}/src/test/resources/log4j.properties"}

#  -Djavax.net.debug=all \
java -cp ./target/dataprofiler-tools-1.jar \
  -Dlog4j.configuration="${log4j_file}" \
  com.dataprofiler.sqlsync.cli.SqlSyncCli \
  --run-spark-locally --fname "${spec}" --output-mode "${outputmode}" "${outpath}"
