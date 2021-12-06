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
from tempfile import NamedTemporaryFile, TemporaryDirectory
from subprocess import CalledProcessError
from logging import getLogger, DEBUG as LOGGING_DEBUG

from dataprofiler.config import Config
from dataprofiler.java import run_spark_submit_command
from dataprofiler.logging_helper import setup_logger_with_job_id
from dataprofiler.table_mapper_api import TableMapperApi
from dataprofiler.job_api import JobType, JobStatus, Job, JobApi


logger = getLogger('sqlsynq')
logger.setLevel(LOGGING_DEBUG)


def is_valid_config(config: Config) -> bool:
    if not config:
        return False

    has_jobs_path = config.jobsApiPath is not None
    has_table_mapper_path = config.tableMapperApiPath is not None
    has_sql_url = config.sqlsyncUrl is not None
    has_sql_user = config.sqlsyncUser is not None
    has_sql_password = config.sqlsyncPassword is not None
    return has_jobs_path and has_table_mapper_path and has_sql_url and has_sql_user and has_sql_password


def ensure_jdbc_connection_info(job: Job, config: Config) -> Job:
    """
    ensure the job file has a jdbc connection, if missing pull from the env/config
    :param job:
    :param config:
    :return:
    """
    if job is None:
        return job

    job_details = job.details
    is_defined = job_details.jdbcConnection is not None
    has_user = 'user' in job_details.jdbcConnection
    has_passwd = 'passwd' in job_details.jdbcConnection
    has_url = 'url' in job_details.jdbcConnection
    has_empty = not has_user or not has_passwd or not has_url
    # if connection info does not exit, then grab it from the env
    if not is_defined or has_empty:
        logger.debug('jdbcConnection, user, passwd, or url was empty - populating from env')
        url = config.sqlsyncUrl
        user = config.sqlsyncUser
        passwd = config.sqlsyncPassword
        jdbc_connection = {'url': url, 'user': user, 'passwd': passwd}
        job_details.jdbcConnection = jdbc_connection

    return job


def apply_table_mapping_visibilities(dataset: str, table: str, job: Job,
                                     table_mapper_api: TableMapperApi) -> None:
    """
    for a single table, apply user and visibilities to table mapper subsystem
    :param dataset:
    :param table:
    :param job:
    :param table_mapper_api:
    :return:
    """
    environment = job.environment
    visibilities = job.details.visibilities
    external_users = job.details.externalUsers
    if not external_users:
        external_users = [job.creating_user]

    visibility_expression = "&".join(visibilities)
    table_mapper_api.create_update_table(environment, dataset, table, visibility_expression, external_users)
    msg = "applied [{}] to [{}] {}.{} for users {}".format(visibility_expression, environment, dataset, table,
                                                           external_users)
    logger.debug(msg)


def export_tables(job: Job, table_mapper_api: TableMapperApi) -> bool:
    """
    export tables, apply visibilities, set job status to complete
    :param job:
    :param table_mapper_api:
    :return:
    """
    if job.type != JobType.SQLSYNC:
        msg = 'Tried to execute job as a sqlsync job, but it was not a sqlsync job.'
        logger.error(msg)
        job.set_status(JobStatus.ERROR)
        job.set_status_details(msg)
        return False

    visibilities = job.details.visibilities
    if not visibilities:
        msg = 'Refusing to export data with no visibility labels'
        logger.error(msg)
        job.set_status(JobStatus.ERROR)
        job.set_status_details(msg)
        return False
    environment = job.environment
    visibility_expression = "&".join(visibilities)

    with NamedTemporaryFile(delete=False) as fd:
        logger.info('Writing out sql spec...')
        logger.debug(job.details.to_json())
        fd.write(job.details.to_json().encode())
        fd.flush()
        with TemporaryDirectory(dir="/tmp") as working_dir:
            for download in job.details.downloads:
            #for i in range(len(job.details.downloads)):
                dataset = download['dataset']
                table = download['table']
                source = '[{}] {}.{} [{}]'.format(environment, dataset, table, visibility_expression)
                external_table_name = table_mapper_api.generate_external_name(environment, dataset, table,
                                                                              visibility_expression)
                if not tables_exists(environment, dataset, table, external_table_name, visibility_expression, table_mapper_api):
                    logger.info(f'{source} does not exist, exporting table...')
                    args = ["--fname", fd.name, "--output-mode", "jdbc", external_table_name, working_dir]
                    try:
                        run_spark_submit_command(c, 'com.dataprofiler.sqlsync.cli.SqlSyncCli', args)
                    except CalledProcessError:
                        msg = f'Failed to execute export from {source} to {external_table_name}'
                        job.set_status(JobStatus.ERROR)
                        job.set_status_details(msg)
                        return False
                    logger.info(f'Export from {source} to {external_table_name} finished')
                apply_table_mapping_visibilities(dataset, table, job, table_mapper_api)
                logger.info(f'Applied visibility labels to {external_table_name}')
            job.set_details(job.details)
    return True


def tables_exists(environment: str, dataset: str, table: str, external_table_name: str,
                  visibility_expression: str, table_mapper_api: TableMapperApi) -> bool:
    #lookup = '[{}] {}.{} {} [{}]'.format(environment, dataset, table, external_table_name,
                                         #visibility_expression)
    tables = table_mapper_api.tables(environment=environment, dataset_name=dataset, table_name=table,
                                     external_name=external_table_name, visibility=visibility_expression)
    if not tables:
        return False

    found_table = None
    for table in tables:
        if table.external_name == external_table_name and table.created_at is not None:
            found_table = table

    if found_table:
        return True
    else:
        return False


def run_job(config: Config, job_id: str) -> None:
    """
    export tables, apply visibilities, set job status to complete
    :param config:
    :param job_id:
    :return:
    """
    job = None
    try:
        if not is_valid_config(c):
            msg = f'Config is invalid, stopping job: {c}'
            logger.error(msg)
            job.set_status(JobStatus.ERROR)
            job.set_status_details(msg)
            return
        api = JobApi(config.jobsApiPath)
        job = api.job(job_id)
        job = ensure_jdbc_connection_info(job, config)
        table_api = TableMapperApi(base_url=config.tableMapperApiPath)
        if export_tables(job, table_api) is True:
            job.set_status(JobStatus.COMPLETE)
    except Exception as e:
        msg = 'Unhandled exception'
        logger.exception(msg)
        if job is not None:
            job.set_status(JobStatus.ERROR)
            job.set_status_details(msg)


def get_args():
    argument_parser = ArgumentParser(description='Execute sqlsync job (typically as a tekton job)')
    argument_parser.add_argument('job_id', help='Job ID to execute')
    return argument_parser.parse_args()


if __name__ == "__main__":
    args = get_args()
    setup_logger_with_job_id(logger, args.job_id)
    c = Config()
    c.from_env()
    start_time = datetime.now()
    run_job(c, args.job_id)
    execution_time = datetime.now() - start_time
    logger.info(f'Totatl execution time: {execution_time}')
