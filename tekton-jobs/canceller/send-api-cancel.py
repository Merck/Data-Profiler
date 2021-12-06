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
from subprocess import run as process_run
from logging import getLogger, DEBUG as LOGGING_DEBUG

from dataprofiler.config import Config
from dataprofiler.logging_helper import setup_logger_with_job_id
from dataprofiler.job_api import JobApi, JobStatus, JobType


logger = getLogger('job_canceller')
logger.setLevel(LOGGING_DEBUG)


def is_valid_job(job):
    if job.type != JobType.CANCELLER:
        error_msg = f'Tried to execute job as canceller, but type didn\'t match. Received {job.type}'
        logger.error(error_msg)
        job.set_status(JobStatus.ERROR)
        job.set_status_details(error_msg)
        return False
    return True


def cancel_job(job):
    logger.info(f'Starting processing of job id: {job.id}')
    to_cancel_id = job.details.to_cancel
    logger.info('Cancelling job with kube...')
    # NOTE: This was done with subprocess since it was faster to let kube process to the target value than to try to process in python
    std_out = process_run(['/home/cancel-tekton-job.sh', str(to_cancel_id)], text=True, capture_output=True, check=True)
    logger.debug(std_out)
    logger.info(f'Setting job {to_cancel_id} to cancelled')
    jobs_api.cancel(to_cancel_id)  # The job TO CANCEL
    job.set_status(JobStatus.COMPLETE)


def get_args():
    argument_parser = ArgumentParser(description='Attempts to cancel a tekton job and update job status to cancelled')
    argument_parser.add_argument('job_id', help='Job ID to execute (not to cancel)')
    return argument_parser.parse_args()


if __name__ == '__main__':
    args = get_args()
    setup_logger_with_job_id(logger, args.job_id)
    config = Config()
    config.from_env()

    job = None
    start_time = datetime.now()
    try:
        jobs_api = JobApi(config.jobsApiPath)
        job = jobs_api.job(args.job_id)
        if is_valid_job(job):
            cancel_job(job)
    except Exception:
        msg = 'Unhandled exception'
        logger.exception(msg)
        if job is not None:
            job.set_status(JobStatus.ERROR)
            job.set_status_details(msg)

    execution_time = datetime.now() - start_time
    logger.info(f'Total execution time: {execution_time}')
