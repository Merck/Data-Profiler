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

import job_api
# from job_api import JobApi, ENDPOINTS
from pprint import pprint
import json
import time
import argparse
import sys


if __name__ == "__main__":


    parser = argparse.ArgumentParser()
    # parser.add_argument("jobid")

    parser.add_argument("--environment", default="development")

    options = parser.parse_args()

    api = job_api.JobApi(base_url=job_api.ENDPOINTS[options.environment])

    jobs = api.all_jobs()
    failed_commits = []
    for job in jobs:
        if job.type == job_api.JobType.COMMIT and job.status == job_api.JobStatus.ERROR:
            jrb = job.to_json()
            deets = json.loads(jrb['details'])
            if 'datasetName' in deets and 'SAILOR' in deets['datasetName']:
                failed_commits.append(jrb)


    print(f'Number of failed commits: {len(failed_commits)}')

    # Get all the ids and  sort them
    ids = []
    for fail in failed_commits:
        ids.append(fail['id'])
        # pprint(fail)
    ids.sort()

    # Set failed status to "new"
    for id in ids:
        print(f'Setting job staus for job {id}')
        api.set_job_status(id, job_api.JobStatus.NEW)

    # Check status of commits
    while True:
        success = 0
        error = 0
        new = 0
        other = 0
        for id in ids:
            jrrb = api.job(id).to_json()
            # print(jrrb['status'])
            # if jrrb['status'] == job_api.JobStatus.NEW: 
            if jrrb['status'] == 'JobStatus.NEW': 
                new+=1
            elif jrrb['status'] == 'JobStatus.SUCCESS': 
                success +=1
            elif jrrb['status'] == 'JobStatus.ERROR': 
                error+=1
            else:
                other +=1
            # print(f"{jrrb['id']}: {jrrb['status']}")

        print(f'New: {new}\tSuccess: {success}\tError: {error}\tOther:{other}')

        if len(ids) == success:
            break
        time.sleep(2)

