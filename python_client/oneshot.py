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
import uuid
import sys
import time
import getpass

from datetime import datetime

from dataprofiler import job_api

def is_terminal(state):
    if state == job_api.JobStatus.SUCCESS:
        return True
    if state == job_api.JobStatus.KILLED:
        return True
    if state == job_api.JobStatus.DEAD:
        return True
    if state == job_api.JobStatus.ERROR:
        return True
    if state == job_api.JobStatus.COMPLETE:
        return True
    return False

class Oneshot:
    def __init__(self, user_args):
        self.jobs_api = job_api.JobApi(base_url=job_api.ENDPOINTS[user_args.environment])

    def create_load_job(self, user_args) -> job_api.Job:
        environment = user_args.environment
        loadDetail = job_api.TableLoadDetail()
        loadDetail.datasetName = user_args.dataset_name
        loadDetail.tableName = user_args.table_name
        loadDetail.metadataVersion = user_args.metadata_version_id
        loadDetail.dataFormat = user_args.data_format
        loadDetail.sourceS3Bucket = user_args.s3_bucket
        loadDetail.sourceS3Key = user_args.s3_key
        loadDetail.tableVisibilityExpression = user_args.visibility_expression
        loadDetail.estimatedRows = user_args.rows
        if user_args.schema_path is not None:
            loadDetail.schemaPath = user_args.schema_path
        return self.jobs_api.create(environment, 
                                    job_api.JobType.TABLE_LOAD,
                                    getpass.getuser(),
                                    loadDetail)

    def watch_job(self, job) -> job_api.Job:
        while not is_terminal(job.status):
            sys.stdout.write('\r')
            sys.stdout.flush()
            sys.stdout.write(f"{datetime.now()} Job {job.id} reported state {job.status}, waiting to check it again")
            sys.stdout.flush()
            time.sleep(3)
            job = self.jobs_api.job(job.id)
        return job

    def create_commit_job(self, user_args) -> job_api.Job:
        environment = user_args.environment
        loadDetails = job_api.BulkIngestAndCommitDetail()
        loadDetails.metadataVersion = user_args.metadata_version_id
        loadDetails.datasetVisibilityExpression = user_args.visibility_expression
        loadDetails.fullDatasetLoad = user_args.full_dataset_load
        loadDetails.dataset = user_args.dataset_name
        return self.jobs_api.create(environment, 
                                    job_api.JobType.COMMIT,
                                    getpass.getuser(),
                                    loadDetails)

def main():
    parser = argparse.ArgumentParser(description="Oneshot Data Loader")
    parser.add_argument('-d', '--dataset-name', metavar='dataset', type=str, required=True)
    parser.add_argument('-t', '--table-name', metavar='table_name', type=str, required=True)
    parser.add_argument('-m', '--metadata-version-id', metavar='uuid', type=str, default=str(uuid.uuid4()))
    parser.add_argument('-b', '--s3-bucket', metavar='bucket_name', type=str, required=True)
    parser.add_argument('-k', '--s3-key', metavar='s3-key', type=str, required=True)
    parser.add_argument('-f', '--data-format', metavar='format', type=str, required=True)
    parser.add_argument('-v', '--visibility-expression', metavar='expr', type=str, required=True)
    parser.add_argument('-r', '--rows', metavar='estimated-rows', type=int, default=10000)
    parser.add_argument('-e', '--environment', metavar='env', type=str, default='development')
    parser.add_argument('-s', '--schema-path', metavar='schema_path', type=str, default=None, required=False)
    parser.add_argument('-l', '--full-dataset-load', metavar="flag", type=bool, default=False)
    args = parser.parse_args()

    oneshot = Oneshot(args)

    job = oneshot.watch_job(oneshot.create_load_job(args))
    if not job.status == job_api.JobStatus.SUCCESS:
        print(f"Load job {job.id} failed with status {job.status} :(")
        sys.exit(-1)
    else:
        print(f"Job {job.id} was successful! Committing!")

    job = oneshot.watch_job(oneshot.create_commit_job(args))
    if not job.status == job_api.JobStatus.SUCCESS:
        print(f"Commit job {job.id} failed with status {job.status} :(")
        sys.exit(-1)
    else:
        print(f"Commit job {job.id} was successful! Query away!")

if __name__ == "__main__":
    main()
