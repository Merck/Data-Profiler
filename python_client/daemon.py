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
#!/usr/bin/env python3

import argparse
import sys
from time import sleep
import requests
from datetime import datetime
from logging import getLogger, DEBUG as LOGGING_DEBUG

from dataprofiler import job_api
from dataprofiler.config import Config
from dataprofiler.datasets import Datasets
from dataprofiler.job_api import JobStatus
from dataprofiler.spark_rest import SessionState, SparkRestClient
from dataprofiler.tools.metadata_committer import MetadataCommitter
from dataprofiler.logging_helper import setup_logger

LOADER_BACKUPS = "/loader_backups/"
MAX_DOWNLOAD_JOBS = 15
MAX_SQLSYNC_JOBS = 5
MAX_DATASETPERFORMANCE_JOBS = 2
MAX_DATASETDELTA_JOBS = 2
MAX_DATASETQUALITY_JOBS = 2

daemon_logger = getLogger('dl-daemon')
daemon_logger.setLevel(LOGGING_DEBUG)


class DPSyncDaemon:
    @classmethod
    def id_from_session(cls, session_name):
        job_id = session_name.split('-')[-1]
        return int(job_id)

    def __init__(self, jobs_api: job_api.JobApi, config: Config = Config(),
                 spark_client: SparkRestClient = SparkRestClient()):
        self.config: Config = config
        self.jobs_api: job_api.JobApi = jobs_api
        self.dp_job_cache = dict()
        self.spark_client: SparkRestClient = spark_client
        # Async metadata committer
        # Attempting to discover existing jobs will incur a short bootup cost
        self.metadata_committer = MetadataCommitter(work_init=self._discover_existing_commit_jobs())
        metadata_commiter_logger = getLogger('md_commiter')
        self.metadata_committer.start()
        self.logger = getLogger('dp-commit-load')
        self.logger.setLevel(LOGGING_DEBUG)
        setup_logger(self.logger)
        setup_logger(metadata_commiter_logger)

    def drivers_for_jobs(self) -> dict:
        spark_snapshot = self.spark_client.spark_ui_status()
        all_drivers = dict()
        all_drivers.update([(driver.id, driver) for driver in spark_snapshot.activedrivers])
        all_drivers.update([(driver.id, driver) for driver in spark_snapshot.completeddrivers])
        drivers_for_jobs = {driver_id: driver for (driver_id, driver) in all_drivers.items() if
                            driver_id in self.dp_job_cache}
        self.logger.debug(f'Returning {len(drivers_for_jobs)} drivers for our current spark status')
        return drivers_for_jobs

    def running_dp_jobs(self):
        running_jobs = self.jobs_api.jobs(type='tableLoad', status='running')
        running_jobs.extend(self.jobs_api.jobs(type='tableLoad', status='starting'))
        for job in running_jobs:
            self.dp_job_cache[job.id] = job
            self.dp_job_cache[job.details.driverId] = job
        return running_jobs

    """
    For every session in `spark_apps`, this function
    will grab the associated DP job from the API (it may
    be cached locally in `dp_job_cache`). If the DP job
    can be found, and its status is different from what
    Spark reports, its status will be updated.

    Note: to prune the cache and keep it current, we make
    a new local `dict` to store all relevant DP Job
    instances. We replace the current `self.dp_job_cache`
    with this new cache. This makes sure we don't keep around
    DP Job instances we no longer intend to update, so it
    *shouldn't* grow unbounded.
    """

    def update_job_status(self, running_jobs):
        spark_apps = self.drivers_for_jobs()
        for dp_job in running_jobs:
            spark_app = spark_apps.get(dp_job.details.driverId, None)
            if spark_app:
                spark_status: JobStatus = SessionState(spark_app.state.lower()).to_job_status()
                if spark_status != dp_job.status:
                    dp_job.set_status(spark_status)
                if spark_status.is_terminal():
                    self.dp_job_cache.pop(dp_job.id)
                    self.dp_job_cache.pop(dp_job.details.driverId)
            else:
                self.logger.debug(f'Jobs API says {dp_job.id} is running, but we can\'t find a spark application for it.')
                if int((datetime.now() - dp_job.updated_at).seconds/60) >= 30:
                    self.logger.error(f'Job {dp_job.id} has been in starting for 30m or longer, erroring out')
                    dp_job.set_status(JobStatus.ERROR)
                    dp_job.set_status_details('Job has been in starting state for 30m or longer')


    """
    Input is a list of job_api.Job instances
    """
    def _discover_existing_commit_jobs(self):
        """
        Discovers previously in running and queued jobs to execute in the instance that the daemon is down
        Will rerun a running job and requeue the queued jobs
        :return: list; may be empty or have jobs
        """
        # NOTE: The jobs api sorts the response of jobs by the created time stamp! It will order the responses as such here with the 0th item being the most recently submitted job
        to_run = []
        # Check for 'running'
        previously_running_jobs = self.jobs_api.jobs(type='commit', status='running')  # There should only be 1 potentially in the running state
        to_run += previously_running_jobs
        # Check for 'queued'
        queued_jobs = self.jobs_api.jobs(type='commit', status='queued')  # Limit is 1000, hopefully never is that high, reverse to put oldest job in queue first as was submitted firs
        queued_jobs.reverse()
        to_run += queued_jobs
        return to_run

    def start_commit_jobs(self, commit_job):
        """
        Tells the committer daemon to run a commit; will run FIFO
        """
        if commit_job:
            self.logger.info(f'Found commit job: Adding job id {commit_job.id}')
            commit_job.set_status(JobStatus.QUEUED)
            self.metadata_committer.add_job(commit_job)

    def tick(self):
        running_jobs = self.running_dp_jobs()
        self.update_job_status(running_jobs)
        max_sessions = 32
        active_sessions = len(running_jobs)
        if active_sessions <= max_sessions:
            datasets = Datasets()
            slots = max_sessions - active_sessions
            self.logger.debug(f'Have {slots} open slots')
            jobs_started = 0
            for slot in range(slots):
                job: job_api.Job = datasets.start_jobs()
                if not job:
                    self.logger.debug(f'Drained jobs API after starting {jobs_started} jobs')
                    break
                else:
                    self.dp_job_cache[job.id] = job
                    self.dp_job_cache[job.details.driverId] = job
                    jobs_started += 1
        else:
            self.logger.error('Not starting new job, at max capacity')

        self.start_commit_jobs(self.jobs_api.executable_job(["commit"]))


class TektonDaemon:
    """
    Generc class for Tekton tasks, subclass this and override start_task to do something useful
    This class helps manage job id states
    """

    def __init__(self, api, config, max_jobs=10, job_type=job_api.JobType.GENERIC, logger=None):
        self.api = api
        self.config = config
        self.jobs = []
        self.max_jobs = max_jobs
        self.current_job_type = job_type
        self.bootstrap_on_startup()
        if logger is None:
            logger = getLogger(job_type.name.lower())
        self.logger = logger
        self.logger.setLevel(LOGGING_DEBUG)
        setup_logger(self.logger)


    def bootstrap_on_startup(self):
        # grab all currently running jobs on startup
        running_jobs = self.api.jobs(status=job_api.JobStatus.RUNNING, type=self.current_job_type)
        self.jobs = [x.id for x in running_jobs]

    def start_task(self, jobid):
        pass

    def trigger_task(self, jobid, event_listener_url):
        """
        start task
        :param jobid:
        :param event_listener_url:
        :return:
        """
        body = {"jobid": jobid}
        self.logger.info(f'Notifying {event_listener_url} of {jobid}')
        response = requests.post(event_listener_url, json=body)
        if response.ok:
            self.api.set_job_status(jobid, job_api.JobStatus.SUCCESS)
            self.jobs.append(jobid)
        else:
            self.logger.error(f'Failed to start jobid: {jobid}')
            # For now, just be conservative and permanently fail the job even though this might
            # be tekton being down. I'd rather do this for now than looping endlessly. In the
            # future we might want some better error handling.
            try:
                self.api.set_job_status(jobid, job_api.JobStatus.ERROR)
                self.api.set_job_status_details(jobid, f"Failed to start job with status code: {response.status_code}")
            except Exception:
                self.logger.exception(f'Failed to set job status')

    def handle_malformed_job_exception(self, error):
        # If the job is malformed, mark it as error and return
        self.api.set_job_status(error.jobid, job_api.JobStatus.ERROR, no_return=True)
        self.api.set_job_status_details(
            job_api, f"job was malformed: {str(error)}", no_return=True)

    def sync(self):
        jobs = []
        for jobid in self.jobs:
            try:
                job = self.api.job(jobid)
            except job_api.MalformedJobException as e:
                self.handle_malformed_job_exception(e)
                continue

            if job.status == job_api.JobStatus.RUNNING:
                jobs.append(job.id)
            else:
                self.logger.info(f'Job ({job.id}) completed with status {job.status}')

        self.jobs = jobs

    def tick(self):
        self.sync()

        while True:
            if len(self.jobs) >= self.max_jobs:
                return
            try:
                name = self.current_job_type.name.lower()
                self.logger.debug(f'Searching for jobs of type {name}')
                job = self.api.executable_job([name])
                if job is not None:
                    self.logger.debug(f'Found job {job.id}')
            except job_api.MalformedJobException as e:
                self.handle_malformed_job_exception(e)
                continue

            if job is None:
                break
            self.logger.info(f'Found job of type {name} to execute ({job.id})')
            self.start_task(job.id)


class DownloadDaemon(TektonDaemon):
    """
    download to s3 tekton task
    """

    def __init__(self, api, config, max_jobs):
        super().__init__(api, config, max_jobs, job_api.JobType.DOWNLOAD)

    def start_task(self, jobid):
        """
        start download task
        :param jobid:
        :return:
        """
        self.trigger_task(jobid, self.config.downloadEventListenerUrl)


class SqlSyncDaemon(TektonDaemon):
    """
    sql sync tekton task - accumulo to jdbc
    """

    def __init__(self, api, config, max_jobs):
        super().__init__(api, config, max_jobs, job_api.JobType.SQLSYNC)

    def start_task(self, jobid):
        """
        start sql sync task
        :param jobid:
        :return:
        """
        self.trigger_task(jobid, self.config.sqlsyncEventListenerUrl)


class DatasetPerformanceDaemon(TektonDaemon):
    """
    dataset performance tekton task
    """

    def __init__(self, api, config, max_jobs):
        super().__init__(api, config, max_jobs, job_api.JobType.DATASETPERFORMANCE)

    def start_task(self, jobid):
        """
        start dataset performance task
        :param jobid:
        :return:
        """
        self.trigger_task(jobid, self.config.datasetperformanceEventListenerUrl)


class DatasetDeltaDaemon(TektonDaemon):
    """
    dataset delta tekton task
    """

    def __init__(self, api, config, max_jobs):
        super().__init__(api, config, max_jobs, job_api.JobType.DATASETDELTA)

    def start_task(self, jobid):
        """
        start dataset delta task
        :param jobid:
        :return:
        """
        self.trigger_task(jobid, self.config.datasetdeltaEventListenerUrl)


class DatasetQualityDaemon(TektonDaemon):
    """
    dataset quality tekton task
    """

    def __init__(self, api, config, max_jobs):
        super().__init__(api, config, max_jobs, job_api.JobType.DATASETQUALITY)

    def start_task(self, jobid):
        """
        start dataset quality task
        :param jobid:
        :return:
        """
        self.trigger_task(jobid, self.config.datasetqualityEventListenerUrl)


class TektonJobCancellerDaemon(TektonDaemon):
    def __init__(self, api, config, max_jobs):
        super().__init__(api, config, max_jobs, job_api.JobType.CANCELLER)

    def start_task(self, jobid):
        self.trigger_task(jobid, self.config.cancellerEventListenerUrl)


class ConnectionEngineDaemon(TektonDaemon):
    def __init__(self, api, config, max_jobs):
        super().__init__(api, config, max_jobs=max_jobs, job_type=job_api.JobType.CONNECTIONENGINE)
    
    def start_task(self, jobid):
        self.trigger_task(jobid, self.config.connectionEngineEventListenerUrl)


def run_daemon(args):
    config = Config()
    daemon_logger.debug(config.to_dict())
    api = job_api.JobApi(base_url=config.jobsApiPath)
    dp_daemon = DPSyncDaemon(api)
    downloads = DownloadDaemon(api, config, MAX_DOWNLOAD_JOBS)
    sqlnsyncboyband = SqlSyncDaemon(api, config, MAX_SQLSYNC_JOBS)
    dataset_performance = DatasetPerformanceDaemon(api, config, MAX_DATASETPERFORMANCE_JOBS)
    dataset_delta = DatasetDeltaDaemon(api, config, MAX_DATASETDELTA_JOBS)
    dataset_quality = DatasetQualityDaemon(api, config, MAX_DATASETQUALITY_JOBS)
    canceller_of_jobs = TektonJobCancellerDaemon(api, config, MAX_DOWNLOAD_JOBS)
    con_engine = ConnectionEngineDaemon(api, config, MAX_DOWNLOAD_JOBS)

    while True:
        dp_daemon.tick()
        downloads.tick()
        sqlnsyncboyband.tick()
        dataset_performance.tick()
        dataset_delta.tick()
        dataset_quality.tick()
        canceller_of_jobs.tick()
        con_engine.tick()
        sleep(1)


def main():
    setup_logger(daemon_logger)
    parser = argparse.ArgumentParser(description="Merk Data Loading Daemon")
    subparsers = parser.add_subparsers(help="commands", dest="command")

    ## starts the daemon
    run_daemon_cmd = subparsers.add_parser("daemon", help="Run in daemon mode")
    run_daemon_cmd.set_defaults(func=run_daemon)

    args = parser.parse_args()

    if args.command is None:
        parser.print_help()
        sys.exit(1)

    args.func(args)


if __name__ == "__main__":
    main()
