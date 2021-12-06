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
from dataprofiler.job_api import JobStatus
import requests
import json
from enum import Enum
from dotmap import DotMap
from logging import getLogger, DEBUG as LOGGING_DEBUG

logger = getLogger('dpc-spark-rest')
logger.setLevel(LOGGING_DEBUG)

SPARK_MASTER_HOST = 'http://spark-master-production.dataprofiler.com'
SPARK_ADMIN_API = f'{SPARK_MASTER_HOST}:6066/v1/submissions'
SPARK_UI_API = f'{SPARK_MASTER_HOST}:8080/json/api/v1'

# val SUBMITTED, RUNNING, FINISHED, RELAUNCHING, UNKNOWN, KILLED, FAILED, ERROR = Value


class SessionState(Enum):
    SUBMITTED = "submitted"
    WAITING = "waiting"
    RUNNING = "running"
    FINISHED = "finished"
    FAILED = "failed"
    KILLED = "killed"
    UNKNOWN = "unknown"
    RELAUNCHING = "relaunching"
    ERROR = "error"

    def to_job_status(self):
        if self == SessionState.WAITING:
            return JobStatus.STARTING
        elif self == SessionState.RUNNING:
            return JobStatus.RUNNING
        elif self == SessionState.FINISHED:
            return JobStatus.SUCCESS
        elif self == SessionState.FAILED:
            return JobStatus.DEAD
        elif self == SessionState.KILLED:
            return JobStatus.KILLED
        elif self == SessionState.UNKNOWN:
            return JobStatus.UNKNOWN
        elif self == SessionState.SUBMITTED:
            return JobStatus.STARTING
        elif self == SessionState.ERROR:
            return JobStatus.ERROR
        elif self == SessionState.RELAUNCHING:
            return JobStatus.STARTING
        return JobStatus.UNKNOWN


class SparkParamsByFileSize:
    # The 'normal' what that you use this is that you create a few of these
    # holding the configuration for different size jobs that have the different
    # sizing information (number of executor cores, memory, etc). Then when
    # you want to run a job you call create_java_config or create_py_config with
    # the specifics for that particular spark job to get a dict with all of the config
    # that you need.
    #
    # This holds livy spark params and is 'magic' in that the attributes are
    # directly converted into a dictionary with the snake case names converted
    # to camel case. So name the attributes exactly like you want the spark
    # parameters to be passed into livy.
    def __init__(self,
                 max_file_size: int,
                 executor_cores: int = 1,
                 executor_memory: str = '8G',
                 executor_gc_region: str = '4M',
                 num_executors: str = None,
                 extra_conf: dict = None,
                 driver_memory='5G',
                 driver_gc_region='3M',
                 driver_cores=1):
        self._max_file_size = max_file_size
        self.name = None
        self.jars = []
        self.executor_cores = executor_cores
        self.executor_memory = executor_memory
        self.num_executors = num_executors

        self.driver_memory = driver_memory
        self.driver_cores = driver_cores
        self.proxy_user = 'spark'

        self._extra_jars = []
        self._extra_conf = {
            # 'spark.driver.maxResultSize': 0,
            'spark.kryoserializer.buffer.max': 1024,
            # https://stackoverflow.com/questions/60172792/reading-data-from-s3-using-pyspark-throws-java-lang-numberformatexception-for-i
            'spark.hadoop.fs.s3a.multipart.size': 104857600,
            'spark.hadoop.fs.s3a.threads.max': 20,
            'spark.hadoop.fs.s3a.threads.core': 15,
            'spark.hadoop.fs.s3a.block.size': 33554432,
            'spark.ui.port': 0,
            'spark.driver.extraJavaOptions': f'-XX:+UseG1GC -XX:G1HeapRegionSize={driver_gc_region} -XX:+PrintGCDetails -XX:+PrintGCTimeStamps',
            'spark.executor.extraJavaOptions': f'-XX:+UseG1GC -XX:G1HeapRegionSize={executor_gc_region} -XX:+PrintGCDetails -XX:+PrintGCTimeStamps'
        }

        if self.num_executors is None:
            self._extra_conf.update({
                'spark.dynamicAllocation.enabled': 'true',
                'spark.shuffle.service.enabled': 'true',
                'spark.dynamicAllocation.maxExecutors': 64,
                'spark.memory.fraction': 0.75,
                'spark.driver.maxResultSize': 0
            })

        if extra_conf:
            self._extra_conf.update(extra_conf)

        self._backlist = set(['max_file_size'])

    @property
    def max_file_size(self):
        return self._max_file_size


class SparkJob:

    def __init__(self, klass, params: SparkParamsByFileSize, args, jar, name, tags=None, pyfile=None, spark_master_url=SPARK_ADMIN_API):
        self.klass = klass
        self.args = args
        self.params = params  # type:SparkParamsByFileSize
        self.jar = jar
        self.name = name  # This just needs to be a unique name
        self.tags = {}
        if tags:
            self.tags.update(tags)
        self.spark_master_url = spark_master_url
        self.driver_id = None
        self.status = None

    def create_java_config(self) -> dict:
        output = dict()
        output['action'] = "CreateSubmissionRequest"
        output['mainClass'] = self.klass
        output['appArgs'] = self.args
        output['appResource'] = self.jar
        job_conf = dict(self.params._extra_conf)
        job_conf['spark.driver.memory'] = self.params.driver_memory
        job_conf['spark.driver.cores'] = self.params.driver_cores
        # if not specified, we'll be using dynamic allocation, already specified in extra_conf
        if self.params.num_executors is not None:
            job_conf['spark.num.executors'] = self.params.num_executors
        job_conf['spark.executor.cores'] = self.params.executor_cores
        job_conf['spark.executor.memory'] = self.params.executor_memory
        job_conf['spark.app.name'] = self.name
        job_conf['spark.jars'] = self.jar
        output['sparkProperties'] = job_conf
        output['clientSparkVersion'] = '2.4.5'
        output['environmentVariables'] = {'SPARK_ENV_LOADED': '1'}
        return output


class SparkRestClient:
    def __init__(self, spark_admin_url: str = SPARK_ADMIN_API, spark_ui_url: str = SPARK_UI_API):
        self.spark_admin_url = spark_admin_url
        self.spark_ui_url = spark_ui_url

    def create(self, job: SparkJob):
        create_url = self.spark_admin_url + "/create"
        logger.debug(f'Posting job config {job.create_java_config()}')
        creation = requests.post(url=create_url, json=job.create_java_config())
        logger.debug(json.dumps(creation.json(), indent=2))
        creation.raise_for_status()
        response = DotMap(creation.json())
        self.driver_id = response.submissionId
        return response

    def status(self, job: SparkJob):
        status_check = requests.get(url=f"{SPARK_MASTER_HOST}/status/{job.id}")
        status_check.raise_for_status()
        response = DotMap(status_check.json())
        job.status = SessionState(response.driverState)
        return response

    def kill(self, job: SparkJob):
        kill_cmd = requests.get(url=f"{SPARK_MASTER_HOST}/kill/{job.id}")
        kill_cmd.raise_for_status()
        response = DotMap(kill_cmd.json())
        response.raise_for_status()
        if response.success:
            job.status = SessionState.KILLED
        return response

    def spark_ui_status(self):
        list_all = requests.get(url=f'{self.spark_ui_url}/applications')
        list_all.raise_for_status()
        return DotMap(list_all.json())
