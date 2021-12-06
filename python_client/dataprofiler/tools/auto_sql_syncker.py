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
from functools import partial
from json import loads as jloads
from logging import getLogger, DEBUG as LOGGING_DEBUG


from ..config import Config
from ..job_api import JobApi, JobType, SqlSyncDetail
from ..table_mapper_api import TableMapperApi


logger = getLogger('auto_sql_syncker')
logger.setLevel(LOGGING_DEBUG)


def find_matching_table_load_jobs(target_metadata_version, api):
    """
    Given a commit job, find matching table load jobs via metadataVersion as there may be 1 commit but n table load
    :param target_metadata_version: str (uuid); Target metadata version to match in a Table Load job 
                                                (target should come from a commit job)
    :return list; list of table load jobs remapped to pertinent data
    [{
        'metadataVersion': <md version>,
        'datasetName': <ds name>,
        'tableName': <table name>
    }]
    """
    tl_jobs = api.jobs(type=JobType.TABLE_LOAD.value)  # Status = complete?
    return filter(
        lambda y: y['metadataVersion'] == target_metadata_version,  # Find only the ones we want
        map(lambda x: {  # Remap data out to the data we care about
                    'metadataVersion': x.details.metadataVersion, 
                    'datasetName': x.details.datasetName, 
                    'tableName': x.details.tableName}, 
            tl_jobs))


def find_unique_table_loads_from_metadata(metadata_version, japi):
    related_table_loads = find_matching_table_load_jobs(metadata_version, japi)
    unique_loads = set(map(lambda x: (x['datasetName'], x['tableName']), related_table_loads))
    logger.debug(f'Found {len(unique_loads)} unique loads for metadataVersion {metadata_version}')
    return unique_loads


def _match_dataset_and_table(job, dataset_name='', table_name=''):
    """
    Used to determine if the job has the matching dataset and table name
    """
    if len(list(filter(lambda x: (x['dataset'] == dataset_name) and (x['table'] == table_name), job.details.downloads))) > 0:
        return True
    return False


def find_unique_sql_sync_jobs(table_loads, japi):
    """
    table_loads is an interable consisting of a tuple: (datasetName, tableName)
    returns a list of details as a python dict
    """
    ss_jobs = japi.jobs(type=JobType.SQLSYNC.value)  # Hold as a cache for the moment
    job_details = set()
    for load in table_loads:
        match_func = partial(_match_dataset_and_table, dataset_name=load[0], table_name=load[1])
        job_details.update(map(lambda x: (x.details.to_json(), x.created_at),(filter(match_func, ss_jobs))))
    final_details = dict()
    for found_job in job_details:
        dict_job = jloads(found_job[0])
        timestamp = found_job[1]
        jdbc_con = dict_job.get('jdbcConnection', {}).get('url', '')
        # NOTE: If the jdbc_connection is the same between multiple exports, this will break!
        if (jdbc_con in final_details):
            # Latest timestamp is what we want to keep
            if timestamp > final_details[jdbc_con]['timestamp']:
                final_details[jdbc_con] = {
                    'j_details': dict_job,
                    'timestamp': timestamp
                }
        elif jdbc_con != '':  # We probably have a real connection instead of an empty string
            final_details[jdbc_con] = {
                'j_details': dict_job,
                'timestamp': timestamp
            }
    as_list = [x['j_details'] for _, x in final_details.items()]
    return as_list


def find_matching_sql_sync_jobs(dataset_name, table_name, api):
    ss_jobs = api.jobs(type=JobType.SQLSYNC.value)
    match_func = partial(_match_dataset_and_table, dataset_name=dataset_name, table_name=table_name)
    return filter(match_func, ss_jobs)


def is_table_mapped_and_export_enabled(dataset_name, table_name, tm_api):
    matched_exports = tm_api.tables_like(dataset_name=dataset_name, table_name=table_name)
    for export in matched_exports:  # Allows for masking the multiple tables of the same name existing
        if (export.dataset_name == dataset_name) and (export.table_name == table_name) and (export.enabled is True):
            return True
    return False


def discover_sql_sync_to_rerun(related_details, tm_api):
    rerun = list()
    for deet in related_details:
        for dl in deet['downloads']:
            if is_table_mapped_and_export_enabled(dl['dataset'], dl['table'], tm_api):
                rerun.append(deet)
                break
    return rerun


def run_list_sql_sync_from_details(to_run_details, environment, japi):
    reran = 0
    for detail in to_run_details:
        detailed = SqlSyncDetail.from_json(1, detail)
        japi.create(environment, JobType.SQLSYNC.value, '__AUTO_SQL_SYNCKER__', detailed)
        reran += 1
    return reran


def run_sql_sync_for_commit(job):
    config = Config()
    japi = JobApi(config.jobsApiPath)
    tm_api = TableMapperApi(config.tableMapperApiPath)

    target_metadata_version = job.details.metadataVersion
    unique_loads = find_unique_table_loads_from_metadata(target_metadata_version, japi)
    unique_sql_sync_jobs = find_unique_sql_sync_jobs(unique_loads, japi)
    if len(unique_sql_sync_jobs) == 0:  # No unique sqlsync jobs found, no reason to keep checking
        logger.info('No unique sql sync jobs found!')
        return 0
    to_rerun = discover_sql_sync_to_rerun(unique_sql_sync_jobs, tm_api)
    if len(to_rerun) == 0:  # No mapped tables, no reason to keep processing
        logger.info('No mapped tables found!')
        return 0
    logger.debug(f'Found details to rerurn: {to_rerun}')
    reran = run_list_sql_sync_from_details(to_rerun, config.environment, japi)
    logger.info(f'Ran {reran} sql-sync jobs related to {target_metadata_version}')
    return reran
