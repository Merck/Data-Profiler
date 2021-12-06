#!/usr/bin/env bash

set -euf -o pipefail -x

pwd
mvn clean package -Plocal -DskipTests
