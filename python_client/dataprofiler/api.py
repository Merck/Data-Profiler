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
#
# API - for calling our API
#
import datetime
import json
import os
import string

import requests
from dotmap import DotMap
from . import config


class PartialFormatter(string.Formatter):
    def __init__(self, missing='~~', bad_fmt='!!'):
        self.missing, self.bad_fmt = missing, bad_fmt

    def get_field(self, field_name, args, kwargs):
        # Handle a key not found
        try:
            #val=super(PartialFormatter, self).get_field(field_name, args, kwargs)
            val = super().get_field(field_name, args, kwargs)
        except (KeyError, AttributeError):
            val = None, field_name
        return val

    def format_field(self, value, spec):
        # handle an invalid format
        if value == None:
            return self.missing
        try:
            return super(PartialFormatter, self).format_field(value, spec)
        except ValueError:
            if self.bad_fmt is not None:
                return self.bad_fmt
            else:
                raise


class ApiException(Exception):
    pass


class Api:
    CRED_FILES = [os.path.expanduser('~/.dataprofiler/credentials'),
                  '/etc/dataprofiler.credentials']

    UPLOAD_JOB = 'upload'
    DOWNLOAD_JOB = 'download'
    COMMAND_JOB = 'command'
    MAKE_JOB = 'make'
    ALL_JOBS = 'all'

    JOB_TYPES = [UPLOAD_JOB, DOWNLOAD_JOB, COMMAND_JOB, MAKE_JOB, ALL_JOBS]

    ANY_JOB = 'any'
    NEW_JOB = 'new'
    IN_PROGRESS_JOB = 'in-progress'
    COMPLETE_JOB = 'complete'
    CANCELLED_JOB = 'cancelled'
    ERROR_JOB = 'error'

    ANY_JOB_ID = -1
    NEW_JOB_ID = 0
    IN_PROGRESS_JOB_ID = 6
    COMPLETE_JOB_ID = 7
    CANCELLED_JOB_ID = 8
    ERROR_JOB_ID = 9

    JOB_STATUS_NAME_MAP = {
        ANY_JOB: ANY_JOB_ID,
        NEW_JOB: NEW_JOB_ID,
        IN_PROGRESS_JOB: IN_PROGRESS_JOB_ID,
        COMPLETE_JOB: COMPLETE_JOB_ID,
        CANCELLED_JOB: CANCELLED_JOB_ID,
        ERROR_JOB: ERROR_JOB_ID
    }

    JOB_STATUS_ID_MAP = {y: x for x, y in JOB_STATUS_NAME_MAP.items()}

    JOB_STATUS_NAMES = JOB_STATUS_NAME_MAP.keys()
    JOB_STATUS_IDS = JOB_STATUS_NAME_MAP.values()

    def __init__(self, baseurl=None, configobj=None):
        if configobj is None:
            self.config = config.Config()
        else:
            self.config = configobj

        if baseurl:
            self.url = baseurl
        else:
            self.url = self.config.userFacingApiHttpPath

        self.credentials = self.__load_config_file(self.CRED_FILES)
        if self.credentials is None:
            raise Exception("Failed to find credentials")

    def get_headers(self):
        return {'X-Api-Key': self.credentials['key'],
                'X-Username': self.credentials['username']}

    def __load_config_file(self, files):
        for file in files:
            if os.path.exists(file):
                return json.load(open(file))

        return None

    def get(self, path):
        r = requests.get(self.url + path, headers=self.get_headers())
        if r.status_code >= 400:
            raise ApiException(str(r.status_code))
        return r

    def get_dotmap(self, path):
        return DotMap(self.get(path).json())

    def post(self, path, json):
        r = requests.post(
            self.url + path, headers=self.get_headers(), json=json)
        if r.status_code >= 400:
            print(r.text)
            raise ApiException(str(r.status_code))
        return r

    def put(self, path, json):
        r = requests.put(
            self.url + path, headers=self.get_headers(), json=json)
        if r.status_code >= 400:
            raise ApiException(str(r.status_code))
        return r

    def __remove_top_key(self, d):
        # The job dict has the jobid as the top-level key. Pull that off
        for key in d.keys():
            d = d[key]
            break

        return DotMap(d)

    def __fix_job(self, job):
        job.status = self.JOB_STATUS_ID_MAP[job.status]

        timestamp = job.submissionDateTime
        if job.submissionDateTime != 0:
            timestamp = timestamp / 1000.0
        job.submissionDateTime = datetime.datetime.fromtimestamp(timestamp)

        return job

    def __prepare_job(self, job):
        out = job.toDict()
        out['status'] = self.JOB_STATUS_NAME_MAP[job.status]
        out['submissionDateTime'] = int(
            job.submissionDateTime.timestamp() * 1000)

        return out

    def get_jobs_for_dataset(self, dataset_name):
        jobs = self.get_jobs_list(
            job_type=self.UPLOAD_JOB, job_status=self.COMPLETE_JOB)

        jobs = [x for x in jobs if x.datasetName == dataset_name]

        return jobs

    def get_jobs(self, job_type=ALL_JOBS, job_status=ANY_JOB):
        """
        Get jobs as a dictionary of jobid: job (as DotMap dicts)
        :param job_type:
        :param job_status:
        :return: a dictionary of jobids: to jobs
        """
        jobs = self.get_dotmap('/jobs?jobtype=%s&jobstatus=%d' %
                               (job_type, self.JOB_STATUS_NAME_MAP[job_status]))
        for job in jobs.values():
            self.__fix_job(job)

        return jobs

    def get_jobs_list(self, job_type=ALL_JOBS, job_status=ANY_JOB):
        """
        Get jobs as a list of jobs (as DotMap dicts)
        :param job_type:
        :param job_status:
        :return: list of jobs
        """
        jobs = sorted(self.get_jobs(job_type=job_type, job_status=job_status).values(
        ), key=lambda j: j.submissionDateTime)

        return jobs

    def job_summary(self, job):
        time = job.submissionDateTime.strftime("%H:%M:%S %m/%d/%Y")
        if job.deleteBeforeReload:
            delete = "Delete First"
        else:
            delete = "No Delete"
        fmt = PartialFormatter()
        text = fmt.format("{time}\t{type:<10} {status:<12}\t{jobId}\t{creatingUser:<20}\t{delete:<12}", time=time,
                          delete=delete,
                          **job.toDict())
        if job.type == self.UPLOAD_JOB:
            text += " {datasetName:<30}\t{datasetProperties}".format(
                **job.toDict())

        return text

    def get_jobs_summary(self, job_type=ALL_JOBS, job_status=ANY_JOB):
        return '\n'.join([self.job_summary(x) for x in self.get_jobs_list(job_type=job_type, job_status=job_status)])

    def get_job(self, jobid):
        return self.__fix_job(self.__remove_top_key(self.get('/jobs/' + jobid).json()))

    def put_job(self, job):
        self.put('/jobs/' + job['jobId'], self.__prepare_job(job))

    def set_table_properties(self, dataset, table, properties):
        return self.post("/{}/{}/properties".format(dataset, table), properties)

    def set_dataset_properties(self, dataset, properties):
        return self.post("/{}/properties".format(dataset), properties)

    def set_column_properties(self, dataset, table, column, properties):
        return self.post("/{}/{}/{}/properties".format(dataset, table, column), properties)

    def set_all_properties(self, dataset, table, dataset_properties, table_properties, column_properties):
        if len(column_properties) > 0:
            columns = self.get_sorted_columns(dataset, table)
            for column in columns:
                if column:
                    self.set_column_properties(
                        dataset, table, column, column_properties)

        if len(table_properties):
            self.set_table_properties(dataset, table, table_properties)

        if len(dataset_properties):
            self.set_dataset_properties(dataset, dataset_properties)

    def submit_make_job(self, job):
        return self.post('/jobs/make', job).json()

    def submit_project_page_job(self, makeId, inputs=[], outputs=[], options={}, outputVisibility=''):
        body = {'makeId': makeId, 'inputs': inputs, 'outputs': outputs,
                'options': options, 'outputVisibility': outputVisibility}
        return self.submit_make_job(body)

    def get_job_detail(self, jobid) -> dict:
        return self.__remove_top_key(self.get('/jobs/' + jobid + '/detail').json())

    def user_authorizations(self, user):
        return self.get('/rules_of_use/' + user + '/authorizations').json()

    def get_sorted_columns(self, dataset, table):
        return self.get('/sortedColumns/' + dataset + '/' + table).json()

    def give_attribute_to_all_with_another_attribute(self, source, dest):
        query_payload = {
            'query': '{usersWithAttribute(value:"system.login"){username}}'}
        users = self.post(
            '/rules_of_use', query_payload).json().get('data').get('usersWithAttribute')
        for user in users:
            mutation_payload = {
                'query': 'mutation{createUpdateUser(username:"'+user.get('username')+'",attributes:["'+dest+'"]){id}}'}
            self.post('/rules_of_use', mutation_payload)
            print("Gave {} to {}".format(dest, user.get('username')))

    def create_skeleton_rou_user(self, username):
        # Check these with web/ui/src/reducers/security.js #activeSystemCapabilities
        base_attributes = ['system.admin', 'system.download', 'system.login', 'system.make',
                           'system.ui_add_tab', 'system.ui_discover_tab', 'system.ui_understand_tab', 'system.ui_use_tab']
        self.rou_create_update_user(username, base_attributes)
        print("Setup {} as a skeleton rou user. Please note that they have admin access".format(
            username))

    def rou_get_all_active_visibilities(self):
        query_payload = {'query': '{attributesActive}'}
        return self.post('/rules_of_use', query_payload)

    def rou_get_user_active_visibilities(self, username):
        query_payload = {
            'query': f'query{{usersLike(username: "{username}") {{id,username,attributes{{value, is_active}} }} }}'}
        return self.post('/rules_of_use', query_payload)

    def rou_create_update_user(self, username, attributes):
        mutation_payload = {
            'query': 'mutation{createUpdateUser(username:"' + username + '",attributes:' + json.dumps(attributes) + '){id}}'}
        return self.post('/rules_of_use', mutation_payload)

    def rou_remove_attributes_from_user(self, username, attributes):
        mutation_payload = {
            'query': 'mutation{removeAttributesFromUser(username:"' + username + '",attributes:' + json.dumps(attributes) + '){id}}'}
        return self.post('/rules_of_use', mutation_payload)

    def get_datasets(self):
        """
        Get datasets as a dictionary of dataset: dataset statistics (as DotMap dicts)
        :return: a dictionary of dataset: to dataset statistics
        """
        datasets = self.get_dotmap('/v1/datasets')
        return datasets

    def get_tables(self, dataset_name):
        """
        Get tables as a dictionary of table: table statistics (as DotMap dicts)
        :return: a dictionary of table: to table statistics
        """
        tables = self.get_dotmap('/v1/tables/%s' % dataset_name)
        return tables

    def get_columns(self, dataset_name,  table_name):
        columns = self.get_dotmap('/v1/columns/%s/%s' %
                                  (dataset_name, table_name))
        return columns

    def get_dataset_names(self):
        return '\n'.join(sorted(x for x in self.get_datasets().keys()))

    def get_table_names(self, dataset_name):
        return '\n'.join(sorted(x for x in self.get_tables(dataset_name).keys()))

    def get_rows(self, dataset_name, table_name, limit=100, filters={}):
        payload = {
            'dataset': dataset_name,
            'table': table_name,
            'limit': limit,
            'filters': filters
        }
        return self.post('/data/rows', payload)

    def get_job_logs(self, job_id):
        return self.get('/jobs/' + job_id + '/logs').content
