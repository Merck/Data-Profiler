#! /bin/bash

set -x
# To run locally compile the tools project with `mvn clean install -P local -DskipTests`
# Start miniaccumulo and load csv file
# try passing csv path like; src/test/resources/basic_test_data.csv
# Or pass blank args to get some "reasonable default" data as defined in MiniAccumuloWithData#startAndLoadReasonableDefaults

JAR_PATH=$(find . -type f -name "dataprofiler-tools-0.1.0.jar" -print | head -n 1)
java -cp $JAR_PATH com.dataprofiler.MiniAccumuloWithData "${@}"
