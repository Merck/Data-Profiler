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
# This is to connect to the job queue daemon

import requests
from dotmap import DotMap
from pprint import pprint
from string import Template
import datetime
from typing import List, Optional
import json
from enum import Enum
import urllib.parse
from re import fullmatch as rematch
from logging import getLogger, DEBUG as LOGGING_DEBUG

logger = getLogger('dpc-job-api')
logger.setLevel(LOGGING_DEBUG)


ENDPOINTS = {
    'production': 'https://production-internal.dataprofiler.com/jobs/graphql',
    'development': 'https://development-internal.dataprofiler.com/jobs/graphql',
    'preview': 'https://preview-internal.dataprofiler..com/jobs/graphql',
    'beta': 'https://preview-internal.dataprofiler..com/jobs/graphql'
}

DEFAULT_URL = ENDPOINTS["development"]
UUID_V4_RE = r'[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89aAbB][a-f0-9]{3}-[a-f0-9]{12}'


class UnknownJobException(Exception):
    pass


class MalformedJobException(Exception):
    def __init__(self, jobid, reason):
        super().__init__(reason)
        self.jobid = jobid


class Detail:
    @classmethod
    def from_json(cls, jobid, json: dict, default_fields=True):
        o = cls()
        for k, v in json.items():
            setattr(o, k, v)

        if default_fields:
            return o

        # If we don't want to use default fields, then verify that all expected
        # (basically, string-type) parameters were set on the resulting object.
        for attr in vars(o):
            value_ref = getattr(o, attr)
            if isinstance(value_ref, str) and not value_ref:
                raise MalformedJobException(
                    jobid, f"Field {attr} was not set.")

        return o

    def to_json(self):
        return json.dumps(self.__dict__)

    # We wrap the `to_json` call in double-double quotes ("")
    # so that when the GraphQL query is constructed, it creates
    # a String with an extra double quote ("). This has the net
    # effect of making the string a blockquoted string by having
    # wraped in triple double quotes ("""). A blockquoted string
    # doesn't need any special escaping, making CSV options that
    # re-use the quote character work as expected.

    def to_json_graphql(self):
        return urllib.parse.quote(self.to_json(), safe=";,/?:@&=+$-_.!~*'()#")

    def verify(self):
        """
        Optional verification of values set on detail
        :return bool; True if verification passed, False if failed
        """
        return False


class TableLoadDetail(Detail):
    UNSET = "__unset__"

    def __init__(self):
        self.datasetName = ""
        self.tableName = ""
        self.metadataVersion = ""
        self.tableVisibilityExpression = ""
        self.columnVisibilityExpression = ""
        self.rowVisibilityColumnName = ""
        self.dataFormat = ""
        self.sourceS3Bucket = ""
        self.sourceS3Key = ""
        self.estimatedRows = -1
        self.options = {}
        self.tableProperties = {}
        self.columnProperties = {}
        # we use UNSET here because these may not exist in all jobs
        # as the schema drifts
        self.driverId = TableLoadDetail.UNSET
        self.schemaPath = TableLoadDetail.UNSET

    def is_set(self, attr_name) -> bool:
        return hasattr(self, attr_name) and not TableLoadDetail.UNSET == getattr(
            self, attr_name)

    def __repr__(self):
        return str(self.__dict__)

    def verify(self):
        """
        Light input checking for values required for detail
        :return bool; False if not valid, True if valid
        """
        if (type(self.datasetName) != str) or (len(self.datasetName) <= 0):
            logger.error(f'Value "{self.datasetName} for datasetName is invalid')
            return False
        if (type(self.tableName) != str) or (len(self.tableName) <= 0):
            logger.error(f'Value "{self.tableName} for tableName is invalid')
            return False
        if (type(self.metadataVersion) != str) or (len(self.metadataVersion) <= 0) or (rematch(UUID_V4_RE, self.metadataVersion) is None):
            logger.error(
                f'Value "{self.metadataVersion} for metadataVersion is invalid')
            return False
        if (type(self.tableVisibilityExpression) != str) or (len(self.tableVisibilityExpression) <= 0):
            logger.error('Bad visibility')
            return False
        if (type(self.dataFormat) != str) or (self.dataFormat not in ('orc', 'parquet', 'avro', 'csv', 'json')):
            logger.error(
                f'Value "{self.dataFormat} for dataFormat is invalid')
            return False
        if (type(self.sourceS3Bucket) != str) or (len(self.sourceS3Bucket) <= 0):
            logger.error(
                f'Value "{self.sourceS3Bucket} for sourceS3Bucket is invalid')
            return False
        if (type(self.sourceS3Key) != str) or (len(self.sourceS3Key) <= 0):
            logger.error(
                f'Value "{self.sourceS3Key} for sourceS3Key is invalid')
            return False
        return True


class BulkIngestAndCommitDetail(Detail):
    def __init__(self):
        self.metadataVersion = ""
        # I don't believe this is actually used anywhere
        # in committing the metadata or bulk loading. If
        # we get rid of it, then the metaprogramming in
        # `Detail` will throw an exception
        self.datasetVisibilityExpression = ""
        self.fullDatasetLoad = False
        self.dataset = "unknown"


class DownloadDetail(Detail):
    def __init__(self):
        self.downloads = []
        self.s3Path = None


class SqlSyncDetail(Detail):
    def __init__(self):
        self.downloads = []
        self.jdbcConnection = {}
        self.visibilities = []
        self.externalUsers = []


class DatasetPerformanceDetail(Detail):
    def __init__(self):
        self.datasets = []
        self.allDatasets = False


class DatasetDeltaDetail(Detail):
    def __init__(self):
        self.datasets = []
        self.allDatasets = False


class CancellerDetail(Detail):
    def __init__(self):
        self.to_cancel = ""


class DatasetQualityDetail(Detail):
    def __init__(self):
        self.datasets = []
        self.allDatasets = False


class ConnectionEngineDetail(Detail):
    def __init__(self):
        self.dataset = ""
        self.s3Path = ""


class JobStatus(Enum):
    # NEW is the default state from users. We
    # should not see it from Spark
    NEW = "new"

    # COMPLETE is a backwards compatibility thing
    COMPLETE = "complete"

    # These are all Livy statuses.
    UNKNOWN = "unknown"
    NOT_STARTED = "not_started"
    STARTING = "starting"
    RECOVERING = "recovering"
    IDLE = "idle"
    RUNNING = "running"
    BUSY = "busy"
    SHUTTING_DOWN = "shutting_down"
    ERROR = "error"
    DEAD = "dead"
    KILLED = "killed"
    SUCCESS = "success"
    CANCELLED = "cancelled"
    QUEUED = "queued"

    def is_terminal(self):
        if self == JobStatus.KILLED:
            return True
        elif self == JobStatus.DEAD:
            return True
        elif self == JobStatus.SUCCESS:
            return True
        elif self == JobStatus.ERROR:
            return True
        elif self == JobStatus.CANCELLED:
            return True
        else:
            return False


class JobType(Enum):
    TABLE_LOAD = "tableLoad"
    COMMIT = "commit"
    DOWNLOAD = "download"
    GENERIC = "generic"
    SQLSYNC = "sqlsync"
    DATASETPERFORMANCE = "datasetperformance"
    DATASETDELTA = "datasetdelta"
    CANCELLER = "canceller"
    DATASETQUALITY = "datasetquality"
    CONNECTIONENGINE = "connectionengine"


class Job:
    DETAIL_TYPE_MAP = {
        JobType.TABLE_LOAD: TableLoadDetail,
        JobType.COMMIT: BulkIngestAndCommitDetail,
        JobType.DOWNLOAD: DownloadDetail,
        JobType.SQLSYNC: SqlSyncDetail,
        JobType.DATASETPERFORMANCE: DatasetPerformanceDetail,
        JobType.DATASETDELTA: DatasetDeltaDetail,
        JobType.CANCELLER: CancellerDetail,
        JobType.DATASETQUALITY: DatasetQualityDetail,
        JobType.CONNECTIONENGINE: ConnectionEngineDetail
    }

    def __init__(self, api):
        self.api: JobApi = api
        self.details = None
        self.id = -1
        self.type: Optional[JobType] = None
        self.status = JobStatus.UNKNOWN
        self.status_details = ""
        self.environment = None
        self.creating_user = ""
        self.created_at = datetime.datetime.now()
        self.updated_at = datetime.datetime.now()

    @classmethod
    def from_json(cls, api, json, create_generic=False, default_fields=True):
        if json is None:
            return None
        job = cls(api)
        # 2020-05-29T20:19:37.584Z
        tformat = "%Y-%m-%dT%H:%M:%S.%fZ"
        job.created_at = datetime.datetime.strptime(json['createdAt'], tformat)
        job.updated_at = datetime.datetime.strptime(json['updatedAt'], tformat)
        job.id = json['id']

        job_type = json['type']
        try:
            job.type = JobType(job_type)
            job.details = cls.DETAIL_TYPE_MAP[job.type].from_json(job.id,
                                                                  json['details'],
                                                                  default_fields)
        except Exception as e:
            if create_generic:
                job.type = JobType.GENERIC
                job.details = DotMap(json)
            else:
                logger.error(f'Failed validating job type and details for job {job.id}')
                raise MalformedJobException(job.id, str(e))

        job.creating_user = json['creatingUser']
        job.status = JobStatus(json['status'].lower())
        job.status_details = json['statusDetails']
        job.environment = json['environment']

        return job

    def set_status(self, status: JobStatus):
        self.api.set_job_status(self.id, status)
        self.status = status

    def set_status_details(self, status_details: str):
        self.api.set_job_status_details(self.id, status_details)
        self.status_details = status_details

    def set_details(self, details):
        self.api.set_job_details(self.id, details)
        self.details = details

    def __repr__(self):
        return str(self.__dict__)

    def to_json(self):
        out = self.__dict__.copy()
        del out["api"]
        out["created_at"] = self.created_at.isoformat()
        out["updated_at"] = self.updated_at.isoformat()
        out["type"] = str(self.type)
        out["status"] = str(self.status)
        if self.details is not None:
            out["details"] = self.details.to_json()

        return out


class JobApi:
    ALL_PROPERTIES = """
        id
        environment
        status
        statusDetails
        creatingUser
        type
        createdAt
        updatedAt
        details
    """
    QUERY_TEMPLATE = Template("""{
        $query {
            $properties
        }
    }
    """)

    def __init__(self, base_url=DEFAULT_URL):
        self.base_url = base_url

    def __exec_query(self, query, data_key=None, create_generic=False,
                     default_fields=True):
        if data_key is None:
            data_key = query
        raw_query = self.QUERY_TEMPLATE.substitute(
            query=query, properties=self.ALL_PROPERTIES)

        return self.__exec_raw_query(raw_query, data_key,
                                     create_generic=create_generic,
                                     default_fields=default_fields)

    def __exec_raw_query(self, raw_query, data_key, create_generic=False,
                         default_fields=True, no_return=False):
        ret = requests.post(self.base_url, json={"query": raw_query})
        ret.raise_for_status()
        if no_return:
            return None

        data = ret.json()['data'][data_key]
        if not isinstance(data, list):
            return Job.from_json(self, data)
        else:
            jobs = []
            for job in data:
                try:
                    jobs.append(Job.from_json(
                        self, job, create_generic=create_generic,
                        default_fields=default_fields))
                except Exception as e:
                    logger.exception('Failed executing query for job')
                    continue

            return jobs

    def executable_job(self, job_types: List[str]):
        """
        Return a job that is ready to execute.

        Note that this method can throw MalformedJobException. The right thing to do is probably to mark
        the job as in the error status and call this again, but that is left up to the caller. This is done
        this way because this is really just a wrapper around the GraphQL call which can return data that the
        wrapper might thing is malformed but the jobs api does not think is malformed.
        """
        jts = []
        for j in job_types:
            if isinstance(j, JobType):
                jts.append(j.value)
            else:
                jts.append(j)

        query = Template("""{
            executableJob(types: [$types]) {
                $properties
            }
        }
        """).substitute(types=",".join([f'"{x}"' for x in jts]),
                        properties=self.ALL_PROPERTIES)
        return self.__exec_raw_query(query, "executableJob")

    def all_jobs(self):
        return self.__exec_query("allJobs", create_generic=True,
                                 default_fields=True)

    def jobs(self, type=None, status=None, creating_user=None, limit=1000,
             default_fields=True):
        params = {}
        if type:
            params["type"] = type
        if status:
            params["status"] = status
        if creating_user:
            params["creatingUser"] = creating_user

        # This business with handling limit separately is because of the quoting
        param_items = ['%s: "%s"' % x for x in params.items()]
        if limit:
            param_items.append("limit: %s" % limit)

        param_string = ",".join(param_items)
        query = Template("""{
            jobs($params) {
                $properties
            }
        }""").substitute(params=param_string, properties=self.ALL_PROPERTIES)

        return self.__exec_raw_query(query, "jobs", default_fields=default_fields)

    def jobs_by_details(self, details, environment, jtype=None, status=None, creating_user=None, limit=1000, default_fields=True):
        """
        Fetches jobs by details and environment, optionally by type, status, creating user, with a limit of (default) 1000 responses.
        details is assumed to be a python dictionary
        """
        if type(details) != str:
            # Fix the escaped quotes, remove extra spaces (or can't find in jobs db)
            details = json.dumps(details).replace('"', '\\"').replace(' ', '')
        query = 'jobsByDetails( limit:%d details:"%s" environment:"%s" ' % (
            limit, details, environment)
        if jtype is not None:
            query += f'type:"{jtype}" '
        if status is not None:
            query += f'status:"{status}" '
        if creating_user is not None:
            query += f'creatingUser:"{creating_user}"" '
        query += ')'
        return self.__exec_query(query, data_key='jobsByDetails')

    def job(self, job_id, create_generic=False, default_fields=True):
        return self.__exec_query(f"job(id: {job_id})", data_key="job",
                                 create_generic=create_generic,
                                 default_fields=default_fields)

    def set_job_status(self, job_id, status: JobStatus, no_return=False):
        query = Template("""mutation {
            updateJobStatus(id: $job_id, status: "$status") {
                $properties
            }
        }""").substitute(job_id=job_id, status=str(status.value),
                         properties=self.ALL_PROPERTIES)

        return self.__exec_raw_query(query, "updateJobStatus", create_generic=True,
                                     no_return=no_return)

    def set_job_status_details(self, job_id, status_details: str,
                               no_return=False):
        query = Template("""mutation {
            updateJobStatusDetails(id: $job_id, statusDetails: "$status_details") {
                $properties
            }
        }""").substitute(job_id=job_id, status_details=status_details,
                         properties=self.ALL_PROPERTIES)

        return self.__exec_raw_query(query, "updateJobStatusDetails",
                                     create_generic=True, no_return=no_return)

    def set_job_details(self, job_id, details):
        query = Template("""mutation {
            updateJobDetails(id: $job_id, details: "$details") {
                $properties
            }
        }""").substitute(job_id=job_id,
                         details=details.to_json_graphql(),
                         properties=self.ALL_PROPERTIES)

        return self.__exec_raw_query(query, "updateJobDetails")

    def create(self, environment, job_type, creating_user, details):
        query = Template("""mutation {
            createJob(
                environment: "$environment",
                type: "$job_type",
                creatingUser: "$creating_user",
                details: "$details") {
                    $properties
                }
        }
        """).substitute(
            environment=environment,
            job_type=str(job_type.value),
            creating_user=creating_user,
            details=details.to_json_graphql(),
            properties=self.ALL_PROPERTIES
        )

        return self.__exec_raw_query(query, "createJob")

    def cancel(self, job_id):
        self.set_job_status(job_id, JobStatus.CANCELLED)


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument("jobid")
    parser.add_argument("--environment", default="development")
    parser.add_argument("--logs", default=False, action="store_true")

    options = parser.parse_args()

    api = JobApi(base_url=ENDPOINTS[options.environment])
    job = api.job(options.jobid)
    if job is None:
        print("Job not found")
    else:
        if options.logs:
            from .api import Api

            api = Api()
            print(api.get_job_logs(options.jobid))
        else:
            pprint(job.to_json())
