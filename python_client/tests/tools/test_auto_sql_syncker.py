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
from dataprofiler.job_api import Job, JobType, JobStatus, TableLoadDetail, SqlSyncDetail, BulkIngestAndCommitDetail
from dataprofiler.table_mapper_api import Table
from dataprofiler.tools.auto_sql_syncker import run_sql_sync_for_commit


def test_run_sql_sync_for_commit(mocker):
    mocked_config = mocker.patch('dataprofiler.tools.auto_sql_syncker.Config')
    mocked_japi = mocker.patch('dataprofiler.tools.auto_sql_syncker.JobApi.jobs')
    mocked_tapi = mocker.patch('dataprofiler.tools.auto_sql_syncker.TableMapperApi.tables_like')
    mocked_reruner = mocker.patch('dataprofiler.tools.auto_sql_syncker.run_list_sql_sync_from_details')

    faked_api = 'http://localhost/testtest'
    faked_env = 'testtesttest'
    faked_user = 'test123'
    target_md_version = 'testtesttesttesttest'
    target_datsetname = 'camtest'
    target_tablename = 'testcam'
    mocked_config.environment = faked_api
    # Job Api
    # 1) Call to table loads
    # 2) Call to sql syncs
    table_load1 = Job(faked_api)  # matches all criteria for a run
    table_load1.id = 1
    table_load1.type = JobType.TABLE_LOAD
    table_load1.status = JobStatus.COMPLETE
    table_load1.environment = faked_env
    table_load1.creating_user = faked_user
    table_load1.details = TableLoadDetail.from_json(1, {
        'datasetName': target_datsetname,
        'tableName': target_tablename,
        'metadataVersion': target_md_version,
    })
    table_load2 = Job(faked_api)
    table_load2.id = 2
    table_load2.type = JobType.TABLE_LOAD
    table_load2.status = JobStatus.COMPLETE
    table_load2.environment = faked_env
    table_load2.creating_user = faked_user
    table_load2.details = TableLoadDetail.from_json(2, {
        'datasetName': 'notcamtest',
        'tableName': 'nottestcam',
        'metadataVersion': 'nottest',
    })
    table_load3 = Job(faked_api)  # Matches metadata version and dataset name
    table_load3.id = 3
    table_load3.type = JobType.TABLE_LOAD
    table_load3.status = JobStatus.COMPLETE
    table_load3.environment = faked_env
    table_load3.creating_user = faked_user
    table_load3.details = TableLoadDetail.from_json(3, {
        'datasetName': target_datsetname,
        'tableName': 'some_table',
        'metadataVersion': target_md_version,
    })

    sql_sync1 = Job(faked_api)  # Matches sql sync completely
    sql_sync1.id = 4
    sql_sync1.type = JobType.SQLSYNC
    sql_sync1.status = JobStatus.COMPLETE
    sql_sync1.environment = faked_env
    sql_sync1.creating_user = faked_user
    sql_sync1.details = SqlSyncDetail.from_json(4, {
        'downloads': [{
            'dataset': target_datsetname,
            'table': target_tablename,
            'timestamp_format': 'dd MMM yyy hh:mm:ssa',
            'date_format': 'dd MMMyyy'
        }],
        'jdbcConnection': {
            'url': 'jdbc:redshift://localhost',
            'user': 'test',
            'passwd': 'a'
        },
        'visibilities': ['LIST.PUBLIC_DATA'],
        'externalUsers': ['testman']
    })
    sql_sync2 = Job(faked_api)
    sql_sync2.id = 5
    sql_sync2.type = JobType.SQLSYNC
    sql_sync2.status = JobStatus.COMPLETE
    sql_sync2.environment = faked_env
    sql_sync2.creating_user = faked_user
    sql_sync2.details = SqlSyncDetail.from_json(5, {
        'downloads': [{
            'dataset': 'notcamtest', 
            'table': 'nottestcam',
            'timestamp_format': 'dd MMM yyy hh:mm:ssa',
            'date_format': 'dd MMMyyy'
        }],
        'jdbcConnection': {
            'url': 'jdbc:redshift://localhost',
            'user': 'test',
            'passwd': 'a'
        },
        'visibilities': ['LIST.PUBLIC_DATA'],
        'externalUsers': ['testman']
    })
    sql_sync3 = Job(faked_api)  # Matches sql sync completely, later timestamp!
    sql_sync3.id = 5
    sql_sync3.type = JobType.SQLSYNC
    sql_sync3.status = JobStatus.COMPLETE
    sql_sync3.environment = faked_env
    sql_sync3.creating_user = faked_user
    sql_sync3.details = SqlSyncDetail.from_json(5, {
        'downloads': [{
            'dataset': target_datsetname,
            'table': target_tablename,
            'timestamp_format': 'dd MMM yyy hh:mm:ssa',
            'date_format': 'dd MMMyyy'
        }],
        'jdbcConnection': {
            'url': 'jdbc:redshift://localhost',
            'user': 'test',
            'passwd': 'a'
        },
        'visibilities': ['LIST.PUBLIC_DATA'],
        'externalUsers': ['testman']
    })
    sql_sync4 = Job(faked_api)  # Matches but with different table name
    sql_sync4.id = 6
    sql_sync4.type = JobType.SQLSYNC
    sql_sync4.status = JobStatus.COMPLETE
    sql_sync4.environment = faked_env
    sql_sync4.creating_user = faked_user
    sql_sync4.details = SqlSyncDetail.from_json(6, {
        'downloads': [{
            'dataset': target_datsetname,
            'table': 'some_table',
            'timestamp_format': 'dd MMM yyy hh:mm:ssa',
            'date_format': 'dd MMMyyy'
        }],
        'jdbcConnection': {
            'url': 'jdbc:redshift://localhost/a',
            'user': 'test',
            'passwd': 'a'
        },
        'visibilities': ['LIST.PUBLIC_DATA'],
        'externalUsers': ['testman']
    })

    mocked_japi.side_effect = iter([(table_load2,table_load1, table_load3), (sql_sync1, sql_sync2, sql_sync3, sql_sync4)])
    
    # Table Api: Called per value in download of sql sync detail
    table1 = Table(faked_api)
    table1.id = 1
    table1.environment = faked_env
    table1.dataset_name = target_datsetname
    table1.table_name = target_tablename
    table1.enabled = True
    table2 = Table(faked_api)
    table2.id = 2
    table2.environment = faked_env
    table2.dataset_name = target_datsetname
    table2.table_name = 'some_table'
    table2.enabled = True
    mocked_tapi.return_value = [table1, table2]
    #mocked_tapi.side_effect = iter([(table1, table2)])
    
    mocked_reruner.return_value = 2  # Used with the debug output, may not be representative of it actually working

    test_commit = Job(faked_api)
    test_commit.details = BulkIngestAndCommitDetail.from_json(0, {
        'metadataVersion': target_md_version
    })
    run_sql_sync_for_commit(test_commit)
    #assert False uncomment for debug output
