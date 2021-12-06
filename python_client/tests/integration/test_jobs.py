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
#from .fixtures import AUTH_HEADERS, BASE_API_PATH
from requests import post as reqpost, get as reqget
from os import getenv

BASE_API_PATH = getenv('USER_FACING_API_HTTP_PATH', 'http://dp-api:9000')

AUTH_HEADERS = {'X-Username': 'test-developer', 'X-Api-Key': getenv('INTTESTPASS', 'local-developer')}

# Can't test on standalone

def test_jobs_tableload():
    # routes: 470
    # JobController
    # POST  /jobs/tableload controllers.JobController.createTableLoad(request: Request)
    body = {
        'datasetName': '',
        'tableName': '',
        'metadataVersion': '',
        'tableVisibilityExpression': '',
        'columnVisibilityExpression': '',
        'rowVisiblityColumnName': '',
        'dataFormat': '',
        'sourceS3Bucket': '',
        'sourceS3Key': '',
        # 'estimatedRows': -1,
        'options': '',
        'tableProperties': '',
        'columnProperties': '',
    }
    req_path = f'{BASE_API_PATH}/jobs/tableload'
    res = reqpost(req_path, headers=AUTH_HEADERS, json=body)
    assert res.status_code == 200
    assert res.json() != {}


def test_jobs_commit():
    # routes: 485
    # JobController
    # POST  /jobs/commit controllers.JobController.createCommit(request: Request)
    body = {
        'metadataVersion': '',
        'datasetVisibilityExpression': '',
        'fullDatasetLoad': False,
    }
    req_path = f'{BASE_API_PATH}/jobs/commit'
    res = reqpost(req_path, headers=AUTH_HEADERS, json=body)
    assert res.status_code == 200
    assert res.json() != {}


def test_jobs():
    # routes: 500
    # JobController
    # GET   /jobs controllers.JobController.getAll(request: Request, jobtype: String ?= "all", jobstatus: String ?= "all", creatinguser: String ?= "all", limit: Integer ?= 10000)
    req_path = f'{BASE_API_PATH}/jobs'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200


def test_jobs_user_creatinguser():
    # routes: 515
    # JobController
    # GET   /jobs/user/:creatingUser controllers.JobController.jobsForUser(request: Request, creatingUser: String, limit: Integer ?= 25)
    creating_user = 'developer'
    req_path = f'{BASE_API_PATH}/jobs/user/{creating_user}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200


def test_jobs_jobid():
    # routes: 530
    # JobController
    # GET   /jobs/:jobId controllers.JobController.getDetails(request: Request, jobId: String)
    job_id = 1
    req_path = f'{BASE_API_PATH}/jobs/{job_id}'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.json() != {}


def test_jobs_jobid_logs():
    # routes: 545
    # SparkLogsController
    # GET   /jobs/:jobId/logs controllers.SparkLogsController.fetchLogsFromCluster(req: Request, jobId: String)
    # Assuming on a spark job as otherwise logs don't exist
    job_id = 42696
    req_path = f'{BASE_API_PATH}/jobs/{job_id}/logs'
    res = reqget(req_path, headers=AUTH_HEADERS)
    assert res.status_code == 200
    assert res.text != ''


def test_jobs_canceller():
    # routes: 620
    # JobController
    # POST /jobs/canceller controllers.JobController.createCancelJob(request: Request)
    body = {
        'to_cancel': 1
    }
    req_path = f'{BASE_API_PATH}/jobs/canceller'
    res = reqpost(req_path, headers=AUTH_HEADERS, json=body)
    assert res.status_code == 200
    assert res.json() != {}


def test_jobs_connection_engine():
    # routes: 635
    # JobController
    # POST /jobs/connection_engine controllers.JobController.createConnectionEngineJob(request: Request)
    body = {
        'dataset': '',
        's3Path': ''
    }
    req_path = f'{BASE_API_PATH}/jobs/connection_engine'
    res = reqpost(req_path, headers=AUTH_HEADERS, json=body)
    assert res.status_code == 200
    assert res.json() != {}


if __name__ == '__main__':
    test_jobs_tableload()
    test_jobs_commit()
    test_jobs()
    test_jobs_user_creatinguser()
    test_jobs_jobid()
    test_jobs_jobid_logs()
    test_jobs_canceller()
    test_jobs_connection_engine()
    print('Complete')