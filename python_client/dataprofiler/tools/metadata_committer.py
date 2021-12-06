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
from multiprocessing import Process, Queue, get_logger
from logging import DEBUG as LOGGING_DEBUG

from ..config import Config
from ..java import run_hadoop_command
from ..job_api import JobStatus, JobType
from .auto_sql_syncker import run_sql_sync_for_commit
from ..logging_helper import setup_logger_with_job_id, cleanup_logger_handler


logger = get_logger()
logger.setLevel(LOGGING_DEBUG)


def debug_worker(work_queue):
    from time import sleep
    logger.info('Starting debug commit worker')
    config = Config()
    while True:
        job = work_queue.get(block=True, timeout=None)
        handler = setup_logger_with_job_id(logger, job.id)
        logger.info(f'Starting job with id {job.id}')
        if job.type != JobType.COMMIT:
            logger.error(f'Supplied job (id:{job.id}, type:{job.type})  was not a commit job!')
            cleanup_logger_handler(logger, handler)
            continue
        try:
            details = job.details
            jv_args = ['--next-version-id', details.metadataVersion]
            jv_args.extend(['--full-dataset-load', str(details.fullDatasetLoad)])
            logger.info(f'MD Commit of {details.metadataVersion}')
            logger.info(f'EXECUTING CommitMetadata id{job.id}')
            sleep(5)
            logger.info('SUCCESS')
        except Exception:
            logger.exception(f'Error committing {details.metadataVersion}')
            logger.info('ERROR')
        cleanup_logger_handler(logger, handler)


def worker(work_queue):
    """
    The worker process for executing a metadata commit, is intended to run 'forever'
    :param work_queue: multiprocessing.Queue; a multiprocessing (or similiar) queue to read work from
    :return: None
    """
    logger.info('Starting commit worker')
    config = Config()  # Only need to load once as this shouldn't change over time
    while True:
        job = work_queue.get(block=True, timeout=None)  # Block and wait for a job indefinitely
        handler = setup_logger_with_job_id(logger, job.id)
        logger.info(f'Starting job with id {job.id}')
        if job.type != JobType.COMMIT:
            logger.error(f'Supplied job (id:{job.id}, type:{job.type})  was not a commit job!')
            job.set_status(JobStatus.NEW)  # Job shouldn't be queued any more (current state is queued)
            cleanup_logger_handler(logger, handler)
            continue
        if job.details is None:
            logger.error(f'Supplied job (id:{job.id}, type:{job.type}) did not include details!')
            job.set_status(JobStatus.ERROR)
            cleanup_logger_handler(logger, handler)
            continue
        job.set_status(JobStatus.RUNNING)
        try:
            details = job.details
            jv_args = ['--next-version-id', details.metadataVersion]
            jv_args.extend(['--full-dataset-load', str(details.fullDatasetLoad)])
            logger.info(f'MD Commit of {details.metadataVersion}')
            run_hadoop_command(config, 'com.dataprofiler.metadata.CommitMetadata', jv_args)
            job.set_status(JobStatus.SUCCESS)
            run_sql_sync_for_commit(job)
        except Exception as e:
            logger.exception(f'Error committing {details.metadataVersion}')
            job.set_status(JobStatus.ERROR)
            job.set_status_details(e)
        cleanup_logger_handler(logger, handler)


class MetadataCommitter(object):
    def __init__(self, work_init=(), debug=False):
        """
        A multiprocessed metadata committing system to prevent main thread lockups
        :param work_init: list/tuple; A list of initial work items to accomplish. (Default empty list)
        :param debug: bool; Determines which committer worker to use, debug does not run actual commits and instead sleeps for a moment
        """
        job = worker
        if debug is True:
            job = debug_worker
        self.work_queue = Queue()
        self._work_process = Process(target=job, args=(self.work_queue,), name='MetadataCommitter-Daemon', daemon=True)
        self._init_queue(work_init)
        self.is_started = False

    def _init_queue(self, work_init):
        """
        Populates the work queue with initial work
        :param work_init: list/tuple; A list of work items to accomplish, a work item is a dataprofiler.job_api.Job
        :return: None; Work queue should be populated
        """
        if work_init:
            for work_item in work_init:
                self.work_queue.put(work_item)

    def add_job(self, job):
        """
        Places a job on the work queue
        :param job:
        :return: None
        """
        self.work_queue.put(job)

    def start(self):
        """
        Starts the worker process
        :return: None
        """
        if self.is_started is False:
            self._work_process.start()
            self.is_started = True
        else:
            logger.error('Worker already started!')


    def stop(self, blocking=False):
        """
        Kills the executing process. If this is done, please re-instantiate the class, the queue is probably broken
        :param blocking: bool; True if wanting to allow the queue to finish nicely, False if wanting immediate capable death (Default False)
        :return: None
        """
        if self.is_started is True:
            self.work_queue.close()
            if blocking:
                self.work_queue.join_thread()
            else:
                self.work_queue.cancel_join_thread()
            self._work_process.terminate()
            self.is_started = False
        else:
            logger.error('Worker is not started!')
