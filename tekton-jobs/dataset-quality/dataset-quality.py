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
from datetime import datetime
from argparse import ArgumentParser
from tempfile import NamedTemporaryFile
from subprocess import CalledProcessError
from logging import getLogger, DEBUG as LOGGING_DEBUG

from dataprofiler.config import Config
from dataprofiler.java import run_java_command
from dataprofiler.logging_helper import setup_logger_with_job_id
from dataprofiler.job_api import JobType, JobStatus, Job, JobApi

logger = getLogger('dataset-quality')
logger.setLevel(LOGGING_DEBUG)


def is_valid_config(conf: Config) -> bool:
    if not conf:
        return False
    else:
        return True


def execute(conf: Config, job: Job) -> bool:
    """
    run dataset quality, set job status to complete
    :param conf:
    :param job:
    :return:
    """
    if job.type != JobType.DATASETQUALITY:
        msg = 'Tried to execute job as a dataset quality job, but it was not a dataset quality job.'
        logger.error(msg)
        job.set_status(JobStatus.ERROR)
        job.set_status_details(msg)
        return False

    with NamedTemporaryFile(delete=False) as fd:
        logger.info('Writing out dataset quality spec...')
        logger.debug(job.details.to_json())
        fd.write(job.details.to_json().encode())
        fd.flush()
        logger.info(f'Calling dataset quality cli with {fd.name}')
        args = ["--fname", fd.name]
        class_name = "com.dataprofiler.datasetquality.cli.DatasetQualityCli"
        heap_size = '512m'
        jvm_args = ['-Xms' + heap_size, '-Xmx' + heap_size]
        try:
            run_java_command(conf, class_name, args, jvm_args=jvm_args)
        except CalledProcessError:
            msg = 'Failed to execute quality calculations (cli failed)'
            logger.exception(msg)
            job.set_status(JobStatus.ERROR)
            job.set_status_details(msg)
            return False
        logger.info('Dataset Quality Finished')
        job.set_details(job.details)
    return True


def run_job(conf: Config, job_id: str) -> None:
    """
    check that config is valid, run dataset quality, set job status to complete
    :param conf:
    :param job_id:
    :return:
    """
    job = None
    try:
        if not is_valid_config(conf):
            msg = f'Config is invalid, stopping job: {conf}'
            logger.error(msg)
            job.set_status(JobStatus.ERROR)
            job.set_status_details(msg)
            return
        api = JobApi(conf.jobsApiPath)
        job = api.job(job_id)
        if execute(conf, job) is True:
            job.set_status(JobStatus.COMPLETE)
    except Exception:
        msg = 'Unhandled exception'
        logger.exception(msg)
        if job is not None:
            job.set_status(JobStatus.ERROR)
            job.set_status_details(msg)


def get_args():
    argument_parser = ArgumentParser(description='Execute dataset quality job (typically as a tekton job)')
    argument_parser.add_argument('job_id', help='Job ID to execute')
    return argument_parser.parse_args()


if __name__ == "__main__":
    args = get_args()
    setup_logger_with_job_id(logger, args.job_id)
    c = Config()
    c.from_env()
    start_time = datetime.now()
    run_job(c, args.job_id)
    execution_time = datetime.now() - start_time
    logger.info(f'Total execution time: {execution_time}')
