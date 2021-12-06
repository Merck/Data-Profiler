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
#! /usr/bin/python3
import json
import logging
import os
import subprocess
import sys
import traceback
import types
import uuid

import apscheduler.schedulers.background
from gevent.pywsgi import WSGIServer

from flask import Flask, abort, g

from dataprofiler import Api, MakeMetadata


# Yes, this is a manual worker pool. But our use case is so simple it doesn't matter. Especially since we
# already run things as separate processes, multi-processing isn't that helpful (we would need to move
# all of the loader code into the api - which we should do, but just not yet).
class WorkerQueue:
    NUM_WORKERS = 6
    REBOOT_FILE = '/var/run/data-loading/restart_needed'
    LOADER = '/usr/bin/dataprofiler_loader'
    LOG_DIR = '/var/log/data-loading/'

    def __init__(self, app):
        self.app = app
        self.workers = []
        # This should only be called on startup, so remove the restart file if it currently
        # exists.
        self.reset_restart_needed()

    def prune_completed_workers(self):
        self.workers = [x for x in self.workers if x.poll() is None]

    def has_space(self):
        if len(self.workers) >= self.NUM_WORKERS:
            return False
        else:
            return True

    def restart_needed(self):
        if os.path.exists(self.REBOOT_FILE):
            return True
        else:
            return False

    def reset_restart_needed(self):
        try:
            os.unlink(self.REBOOT_FILE)
        except FileNotFoundError:
            pass

    def get_new_jobs(self, api):
        jobs = api.get_jobs_list(job_status=Api.NEW_JOB)
        # sort the jobs to get the ordering that we want
        jobs.sort(reverse=True, key=lambda j: j.submissionDateTime)

        # always push upload jobs to the back
        upload = []
        non_upload = []
        for job in jobs:
            if job.type == Api.UPLOAD_JOB:
                upload.append(job)
            else:
                non_upload.append(job)

        return non_upload + upload

    def set_job_in_progress(self, api, job):
        self.app.logger.info("setting job in progress: " + job.jobId)
        job.status = api.IN_PROGRESS_JOB
        api.put_job(job)

    def prune_and_check_space(self):
        self.prune_completed_workers()
        if not self.has_space():
            self.app.logger.info("Waiting for space in the job queue")
            return False
        else:
            return True

    def create_log_fds(self, jobid):
        base_log_dir = os.path.join(self.LOG_DIR, jobid)
        i = 1
        while True:
            log_dir = '%s-attempt-%06d' % (base_log_dir, i)
            try:
                os.mkdir(log_dir)
            except FileExistsError:
                i += 1
                continue
            break

        stdout = open(os.path.join(log_dir, 'stdout'), mode='wb')
        stderr = open(os.path.join(log_dir, 'stderr'), mode='wb')

        return log_dir, stdout, stderr

    def run_job(self, api, job):
        cmd = [self.LOADER, 'process-job', job.jobId]

        log_dir, stdout, stderr = self.create_log_fds(job.jobId)

        self.app.logger.info("Processing job: %s, log dir: %s" %
                             (job.jobId, log_dir))
        self.set_job_in_progress(api, job)

        pid = subprocess.Popen(cmd, stdout=stdout, stderr=stderr)
        self.workers.append(pid)

    def tick(self):
        self.prune_completed_workers()

        if self.restart_needed():
            if len(self.workers) == 0:
                self.app.logger.info("exiting because restart is needed")
                self.reset_restart_needed()
                sys.exit(1)
            else:
                self.app.logger.info(
                    "waiting on jobs so that restart can be done")
                return

        if not self.prune_and_check_space():
            return

        api = Api()
        jobs = self.get_new_jobs(api)
        for job in jobs:
            self.run_job(api, job)

            if not self.prune_and_check_space():
                return


app = Flask(__name__)
app.logger.setLevel(logging.INFO)
scheduler = apscheduler.schedulers.background.BackgroundScheduler()
wq = WorkerQueue(app)


@scheduler.scheduled_job('interval', seconds=2)
def run_jobs() -> None:
    try:
        wq.tick()
    except Exception as e:
        app.logger.error(str(e))
        app.logger.error(traceback.print_exc())


@app.route('/application/go', methods=['POST'])
def go():
    # This is just here for compatibility. I'm polling much more often now (partially because I need to
    # handle the logging and partially because I need to prune completed jobs) and I just don't think
    # this is really needed to poke the work queue.
    return "pong"


@app.route('/make/plugins')
def get_make_plugins():
    plugins = MakeMetadata.list_plugins()

    return json.dumps(plugins)


@app.route('/make/plugin/<make_id>/json_form')
def get_make_plugin_json_form(make_id):
    try:
        m = MakeMetadata(make_id)
        app.logger.error("No such plugin " + make_id)
    except Exception as e:
        abort(400)
        return

    return json.dumps(m.json_form())


if __name__ == '__main__':
    print("Staring data loading daemon")
    scheduler.start()

    http_server = WSGIServer(('0.0.0.0', 7010), app)
    http_server.serve_forever()
