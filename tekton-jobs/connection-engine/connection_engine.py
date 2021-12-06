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
from boto3 import client as aws_client
from tempfile import NamedTemporaryFile
from subprocess import CalledProcessError
from logging import getLogger, DEBUG as LOGGING_DEBUG

from dataprofiler.config import Config
from dataprofiler.java import run_spark_submit_command
from dataprofiler.logging_helper import setup_logger_with_job_id
from dataprofiler.job_api import JobType, JobStatus, Job, JobApi

logger = getLogger('connection-engine')
logger.setLevel(LOGGING_DEBUG)


CONNECTION_ENGINE_CMD = 'com.dataprofiler.connectionengine.cli.ConnectionEngineCli'
S3_EXPORT_KEY = 'exports'
S3_URL_EXPIRATION = 14 * 24 * 60 * 60  # 14 days in seconds


def is_valid_config(config: Config) -> bool:
    if not config:
        return False

    has_jobs_path = config.jobsApiPath is not None
    return has_jobs_path 


def execute(job: Job, config: Config) -> bool:
    if job.type != JobType.CONNECTIONENGINE:
        msg = 'Tried to execute job as a connection engine job, but it was not a connection engine job.'
        logger.error(msg)
        job.set_status(JobStatus.ERROR)
        job.set_status_details(msg)
        return False

    with NamedTemporaryFile(delete=False) as fd:
        logger.info('Writing out connection-engine spec...')
        fd.flush()
        try:
            run_spark_submit_command(config, CONNECTION_ENGINE_CMD, [job.details.dataset, fd.name])
        except CalledProcessError:
            msg = 'Failed to execute SuperTableCalculations'
            logger.exception(msg)
            job.set_status(JobStatus.ERROR)
            job.set_status_details(msg)
            return False
        logger.info('Completed SuperTableCalculations')

        logger.info('Uploading to S3')
        timestamp = job.created_at.strftime('%m-%d-%Y-%H-%M-%S')
        s3 = aws_client('s3')
        s3_name = f'{job.creating_user}-SuperTableCalc-{job.details.dataset}-{timestamp}.csv'
        s3_key = S3_EXPORT_KEY + '/' + s3_name.replace(' ', '_')
        logger.debug(f'File key: {s3_key}')
        s3.upload_file(fd.name, config.s3Bucket, s3_key)
        logger.info('Completed upload')

        signed_url = s3.generate_presigned_url(
            'get_object',
            Params={
                'Bucket': config.s3Bucket,
                'Key': s3_key
            },
            ExpiresIn=S3_URL_EXPIRATION
        )
        job.details.s3Path = signed_url
        job.set_details(job.details)
    return True


def run_job(config: Config, job_id: str) -> None:
    """
    export tables, apply visibilities, set job status to complete
    :param config:
    :param job_id:
    :return:
    """
    job = None
    try:
        if not is_valid_config(c):
            msg = f'Config is invalid, stopping job: {c}'
            logger.error(msg)
            job.set_status(JobStatus.ERROR)
            job.set_status_details(msg)
            return
        api = JobApi(config.jobsApiPath)
        job = api.job(job_id)
        if execute(job, config) is True:
            job.set_status(JobStatus.COMPLETE)  # Job completed successfully
    except Exception:
        msg = 'Unhandled exception'
        logger.exception(msg)
        if job is not None:
            job.set_status(JobStatus.ERROR)
            job.set_status_details(msg)


def get_args():
    argument_parser = ArgumentParser(description='Execute connection engine job (typically as a tekton job')
    argument_parser.add_argument('job_id', help='Job ID to execute')
    return argument_parser.parse_args()


if __name__ == '__main__':
    args = get_args()
    setup_logger_with_job_id(logger, args.job_id)
    c = Config()
    c.from_env()
    start_time = datetime.now()
    run_job(c, args.job_id)
    execution_time = datetime.now() - start_time
    logger.info(f'Total execution time: {execution_time}')
