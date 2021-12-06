"""
  Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
 
 	Licensed to the Apache Software Foundation (ASF) under one
 	or more contributor license agreements. See the NOTICE file
 	distributed with this work for additional information
 	regarding copyright ownership. The ASF licenses this file
 	to you under the Apache License, Version 2.0 (the
 	"License"); you may not use this file except in compliance
 	with the License. You may obtain a copy of the License at
 
 	http://www.apache.org/licenses/LICENSE-2.0
 
 
 	Unless required by applicable law or agreed to in writing,
 	software distributed under the License is distributed on an
 	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 	KIND, either express or implied. See the License for the
 	specific language governing permissions and limitations
 	under the License.
"""
# Utitlities to run java commands in various ways

from logging import getLogger, DEBUG as LOGGING_DEBUG
from typing import List
import subprocess
import os

from .config import Config

logger = getLogger('dpc-java')
logger.setLevel(LOGGING_DEBUG)

DEFAULT_TOOLS_JAR = "/opt/app/dataprofiler-tools-current.jar"

JAVA = "/usr/bin/java"

DEFAULT_EXTRA_ENV = {
    "JAVA_HOME": "/usr/lib/jvm/default-java"
}

HADOOP_HOME = "/opt/hadoop/current"
HADOOP_CMD = HADOOP_HOME + "/bin/hadoop"
YARN_CMD = HADOOP_HOME + "/bin/yarn"

SPARK_HOME = "/opt/spark/current"
SPARK_SUBMIT = SPARK_HOME + "/bin/spark-submit"
SPARK_SUBMIT_REMOTE = 'spark://spark-master-production.dataprofiler.com:7077'

DEFAULT_EXTRA_HADOOP_ENV = {
    "JAVA_HOME": "/usr/lib/jvm/default-java",
    "HADOOP_HOME": HADOOP_HOME
}

DISTCP_JAR = "/opt/hadoop/current/share/hadoop/tools/lib/hadoop-distcp-2.9.2.jar"


def run_java_command(config: Config, klass: str, args: List[str],
                     jar=DEFAULT_TOOLS_JAR, extra_env=DEFAULT_EXTRA_ENV,
                     jvm_args: List[str] = []) -> subprocess.CompletedProcess:
    env = os.environ.copy()
    env.update(extra_env)
    cmd = [JAVA, *jvm_args, "-cp", jar,
           klass] + config.to_commandline_args() + args
    completed = subprocess.run(cmd, env=env, check=True, capture_output=True)
    logger.debug(completed.stdout)
    logger.debug(completed.stderr)
    # Readded, will not be found when doing kibana searching
    print(completed.stdout)
    print(completed.stderr)
    return completed


def run_hadoop_command(config: Config, klass: str, args: List[str],
                       jar=DEFAULT_TOOLS_JAR, extra_env=DEFAULT_EXTRA_HADOOP_ENV,
                       hadoop_cmd=HADOOP_CMD) -> subprocess.CompletedProcess:
    """Run a hadoop command - by default /opt/hadoop/current/bin/hadoop. This runs a jar using the hadoop command to bring in all of the hadoop environment.

    klass can be the main class or None if the jar has a default main class (if the pom specifies a main class that will be used even if you specify a
    main class - that can be very confusing).
    """
    env = os.environ.copy()
    env.update(extra_env)
    cmd = [hadoop_cmd, "jar", jar]
    if klass is not None:
        cmd.append(klass)
    cmd.extend(config.to_commandline_args() + args)
    completed = subprocess.run(cmd, env=env, check=True, capture_output=True)
    logger.debug(completed.stdout)
    logger.debug(completed.stderr)
    # Readded, will not be found when doing kibana searching
    print(completed.stdout)
    print(completed.stderr)
    return completed


def run_spark_submit_command(config: Config, klass: str, args: List[str],
                             jar=DEFAULT_TOOLS_JAR, extra_env=DEFAULT_EXTRA_ENV,
                             spark_submit=SPARK_SUBMIT) -> subprocess.CompletedProcess:
    env = os.environ.copy()
    env.update(extra_env)
    cmd = [spark_submit, '--conf', 'spark.ui.enabled=false']
    cmd.extend(["--class", klass, jar])
    cmd.extend(config.to_commandline_args() + args)
    completed = subprocess.run(cmd, env=env, check=True, capture_output=True)
    logger.debug(completed.stdout)
    logger.debug(completed.stderr)
    # Readded, will not be found when doing kibana searching
    print(completed.stdout)
    print(completed.stderr)
    return completed


def distcp(config: Config, src_url: str, tgt_url: str):
    return run_hadoop_command(config, 'com.dataprofiler.LoaderDistCp',
                              ['--src', src_url, '--dest', tgt_url],
                              jar=DEFAULT_TOOLS_JAR, hadoop_cmd=YARN_CMD)
