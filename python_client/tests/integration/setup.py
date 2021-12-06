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
from dataprofiler.job_api import JobApi, JobType, BulkIngestAndCommitDetail, TableLoadDetail
from uuid import uuid4


def load_and_commit_from_s3(file_name, ds_name, api, 
                            vis_exp='LIST.INTTESTING', 
                            s3_bucket='dataprofiler-modern-development',
                            s3_root_path='-api_testing-/integration/',
                            file_format='csv', user='test-developer', 
                            env='development'):
    print(f'Creating load for {ds_name}')
    uuid = str(uuid4())
    detail = TableLoadDetail()
    detail.datasetName = ds_name
    detail.tableName = ds_name
    detail.metadataVersion = uuid
    detail.tableVisibilityExpression = vis_exp
    detail.dataFormat = file_format
    detail.sourceS3Bucket = s3_bucket
    detail.sourceS3Key = s3_root_path + file_name
    detail.estimatedRows = 10000
    print(f'Creating commit for {ds_name}')
    cdetail = BulkIngestAndCommitDetail()
    cdetail.metadataVersion = uuid
    cdetail.datasetVisibilityExpression = vis_exp
    cdetail.dataset = ds_name
    print(f'Submitting jobs')
    api.create(env, JobType.TABLE_LOAD, user, detail)
    api.create(env, JobType.COMMIT, user, cdetail)



if __name__ == '__main__':
    api = JobApi()
    print('Setting up for test run by loading and committing table')
    load_and_commit_from_s3('basic_test_data.csv', 'int_basic_test_data', api)
    load_and_commit_from_s3('cell_level_visibilities_test_data.csv', 'int_cell_level_visibilities_test_data', api)
