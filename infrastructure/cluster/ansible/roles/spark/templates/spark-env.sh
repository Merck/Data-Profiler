#
# Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
#
#	Licensed to the Apache Software Foundation (ASF) under one
#	or more contributor license agreements. See the NOTICE file
#	distributed with this work for additional information
#	regarding copyright ownership. The ASF licenses this file
#	to you under the Apache License, Version 2.0 (the
#	"License"); you may not use this file except in compliance
#	with the License. You may obtain a copy of the License at
#
#	http://www.apache.org/licenses/LICENSE-2.0
#
#
#	Unless required by applicable law or agreed to in writing,
#	software distributed under the License is distributed on an
#	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#	KIND, either express or implied. See the License for the
#	specific language governing permissions and limitations
#	under the License.
#
export SPARK_DIST_CLASSPATH=$({{ hadoop_home }}/bin/hadoop classpath)
export SPARK_DIST_CLASSPATH={{ hadoop_home}}/share/hadoop/tools/lib/hadoop-aws-2.9.2.jar:{{ hadoop_home }}/share/hadoop/tools/lib/aws-java-sdk-bundle-1.11.199.jar:${SPARK_DIST_CLASSPATH}
export HADOOP_CONF_DIR={{ hadoop_home }}/etc/hadoop

# Where log files are stored.(Default:${SPARK_HOME}/logs)
export SPARK_LOG_DIR={{ spark_log_directory }}

# Where the pid file is stored. (Default: /tmp)
export SPARK_PID_DIR={{ spark_pid_directory }}

# Set the working directory of worker processes
export SPARK_WORKER_DIR={{ spark_work_directory }}

# Set the master host to the FQDN to {{ dataprofiler_fqdn }}
# This has two effects:
#  1. It passes the FQDN down to the workers when running `start-all.sh`
#  2. It forces the master process to bind the interface that the FQDN resolves
#     to. NOTE: this means we should _not_ map 127.0.0.1 to the FQDN /etc/hosts 
#     as this will make the master bind to localhost and no external worker will
#     be able to connect.
export SPARK_MASTER_HOST="{{ hostvars[groups['spark_masters'][0]].dataprofiler_fqdn }}"

export SPARK_MASTER_OPTS="
  -Djava.rmi.server.hostname={{ dataprofiler_fqdn }}
  -Dcom.sun.management.jmxremote=true
  -Dcom.sun.management.jmxremote.local.only=false
  -Dcom.sun.management.jmxremote.authenticate=false
  -Dcom.sun.management.jmxremote.ssl=false
  -Dcom.sun.management.jmxremote.port=8090
  -Dcom.sun.management.jmxremote.rmi.port=8090
  -Dspark.master.rest.enabled=true
  -Dspark.master.rest.port=6066
  -Dspark.ui.retainedJobs=20000"

export SPARK_WORKER_OPTS="
  -Djava.rmi.server.hostname={{ dataprofiler_fqdn }}
  -Dcom.sun.management.jmxremote=true
  -Dcom.sun.management.jmxremote.local.only=false
  -Dcom.sun.management.jmxremote.authenticate=false
  -Dcom.sun.management.jmxremote.ssl=false
  -Dcom.sun.management.jmxremote.port=8091
  -Dcom.sun.management.jmxremote.rmi.port=8091
  -Dspark.worker.cleanup.appDataTtl=604800"

SPARK_HISTORY_OPTS="
  -Djava.rmi.server.hostname={{ dataprofiler_fqdn }}
  -Dcom.sun.management.jmxremote=true
  -Dcom.sun.management.jmxremote.local.only=false
  -Dcom.sun.management.jmxremote.authenticate=false
  -Dcom.sun.management.jmxremote.ssl=false
  -Dcom.sun.management.jmxremote.port=8092
  -Dcom.sun.management.jmxremote.rmi.port=8092"

SPARK_EXECUTOR_OPTS="
  -Djava.rmi.server.hostname={{ dataprofiler_fqdn }}
  -Dcom.sun.management.jmxremote=true
  -Dcom.sun.management.jmxremote.local.only=false
  -Dcom.sun.management.jmxremote.authenticate=false
  -Dcom.sun.management.jmxremote.ssl=false
  -Dcom.sun.management.jmxremote.port=0
  -Dcom.sun.management.jmxremote.rmi.port=0"

