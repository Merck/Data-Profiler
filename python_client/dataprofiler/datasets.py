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
import getpass
import uuid
from logging import getLogger, DEBUG as LOGGING_DEBUG
from requests.models import HTTPError

from dataprofiler.spark_rest import SparkJob, SparkRestClient, SparkParamsByFileSize

from . import config, job_api, s3fileinfo

logger = getLogger('dpc-datasets')
logger.setLevel(LOGGING_DEBUG)


class Datasets:
    config = config.Config()
    jobs = job_api.JobApi(base_url=config.jobsApiPath)
    spark_api = SparkRestClient()

    spark_params = [
        SparkParamsByFileSize(10000, executor_memory="4G", executor_gc_region="2M", driver_memory="2G", driver_gc_region="1M"),
        SparkParamsByFileSize(100000, executor_memory="6G", executor_gc_region="3M", driver_memory="4G", driver_gc_region="2M"),
        SparkParamsByFileSize(1000000, executor_memory="8G", executor_gc_region="4M", driver_memory="8G", driver_gc_region="4M"),
        SparkParamsByFileSize(10000000, executor_memory="10G", executor_gc_region="5M", driver_memory="10G", driver_gc_region="5M"),
        SparkParamsByFileSize(100000000, executor_memory="12G", executor_gc_region="6M", driver_memory="12G", driver_gc_region="6M"),
        SparkParamsByFileSize(500000000, executor_memory="16G", executor_gc_region="8M", driver_memory="16G", driver_gc_region="8M"),
        SparkParamsByFileSize(1000000000, executor_memory="24G", executor_gc_region="12M", driver_memory="24G", driver_gc_region="12M"),
        SparkParamsByFileSize(None, executor_memory="32G", executor_gc_region="16M", driver_memory="32G", driver_gc_region="16M"),
    ]

    def select_spark_params(self, estimated_rows: int):
        for param in self.spark_params:
            if param.max_file_size is None or estimated_rows <= param.max_file_size:
                return param

        raise Exception("Failed to find applicable spark params")

    def start_table_load(self, job: job_api.Job):
        details: job_api.TableLoadDetail = job.details
        if details.verify() is False:
            logger.error(f'Found bad details for job {job.id}')
            job.set_status(job_api.JobStatus.ERROR)
            job.set_status_details(f'Details were incorrect, please correct and resubmit!')
            return

        format_map = {
            "orc": "com.dataprofiler.loader.OrcDataLoader",
            "parquet": "com.dataprofiler.loader.ParquetDataLoader",
            "avro": "com.dataprofiler.loader.AvroDataLoader",
            "csv": "com.dataprofiler.loader.CsvDataLoader",
            "json": "com.dataprofiler.loader.JsonDataLoader",
        }

        dataloader_params = self.select_spark_params(int(details.estimatedRows))

        loader_args = self.config.to_commandline_args()

        data_loader_args = []
        data_loader_args.extend(["--version-id", details.metadataVersion])
        data_loader_args.extend(["--load-type", "bulk"])
        data_loader_args.extend(["--loader-output-dest", "/loader"])
        data_loader_args.extend(["--datasetName", details.datasetName])
        data_loader_args.extend(["--visibility" , details.tableVisibilityExpression])
        data_loader_args.extend(["--columnVisibilityExpression", details.columnVisibilityExpression])
        data_loader_args.extend(["--rowVisibilityColumnName", details.rowVisibilityColumnName])
        data_loader_args.extend(["--setActiveVisibilities", "true"])
        data_loader_args.extend(["--build-hadoop-config", "true"])
        # in a split load, we usually can't connect to accumulo, but
        # here we want to so the calls to setActiveVisibilities works
        data_loader_args.extend(["--allow-accumulo-connection", "true"])

        if details.schemaPath != job_api.TableLoadDetail.UNSET:
            data_loader_args.extend(["--separate-table-schema", "true"])
            data_loader_args.extend(["--schema-path", details.schemaPath])

        job_name = self.config.environment + "-" + str(job.id)
        data_loader_args.extend(["--name", job_name])

        if details.tableName:
            data_loader_args.extend(["--table-name", details.tableName])

        # TODO merge in loader consolidation code OR come up with a named argument convention
        # for input files
        input_path = "s3a://" + details.sourceS3Bucket + "/" + details.sourceS3Key

        if not s3fileinfo.key_contains_files(details.sourceS3Bucket, details.sourceS3Key):
            logger.error(f'No files found under {input_path}')

        data_format = format_map.get(details.dataFormat, None)
        if data_format is None:
            logger.error(f'Bad data format! Supplied data format {details.dataFormat}')
            job.set_status(job_api.JobStatus.ERROR)
            job.set_status_details(f'Bad data format! Supplied data format {details.dataFormat}')
            return

        if data_format == format_map.get("csv"):
            data_loader_args.extend(["--csv-file", input_path])
        else:
            # Probably broken for anything that doesn't have _just_ an input file!
            data_loader_args.extend(["--input-path", input_path])
        
        tags = {"metadata_version": details.metadataVersion}

        spark_job = SparkJob(
            format_map[details.dataFormat],
            dataloader_params,
            loader_args + data_loader_args,
            Datasets.config.dataProfilerToolsJar,
            job_name,
            tags=tags
        )

        create_response = None
        try:
            create_response = Datasets.spark_api.create(spark_job)
        except HTTPError:
            logger.exception()
            
        job.set_status(job_api.JobStatus.STARTING)
        # kind of a funny usage pattern here, but we want to add
        # the submissionId into the details, and then have
        # the details be persisted back to the jobs API
        job.details.driverId = create_response.submissionId
        job.set_details(job.details)

    def start_jobs(self):
        while True:
            # just try and find a good job to run
            try:
                job = self.jobs.executable_job(["tableLoad"])
            except job_api.MalformedJobException as e:
                # If the job is malformed, mark it as error and return
                self.jobs.set_job_status(e.jobid, job_api.JobStatus.ERROR, no_return=True)
                self.jobs.set_job_status_details(
                    job_api, f"job was malformed: {str(e)}", no_return=True)
                continue
            break

        if job is None:
            return None
        elif job.type == job_api.JobType.TABLE_LOAD:
            self.start_table_load(job)
            return job
        else:
            assert False

    def create_table_load(self, s3path, dataset_name, table_name, format, viz, column_viz, row_viz_cname):
        details = job_api.TableLoadDetail()
        details.datasetName = dataset_name
        details.tableName = table_name
        details.metadataVersion = str(uuid.uuid4())
        details.tableVisibilityExpression = viz
        details.columnVisibilityExpression = column_viz
        details.rowVisibilityColumnName = row_viz_cname
        details.dataFormat = format
        bucket, key = s3fileinfo.split_bucket_and_key(s3path)
        details.sourceS3Bucket = bucket
        details.sourceS3Key = key
        details.estimatedRows = 10000

        self.jobs.create(self.config.environment, job_api.JobType.TABLE_LOAD, getpass.getuser(), details)

    def create_table_commit(self, viz, version_id):
        details = job_api.BulkIngestAndCommitDetail()
        details.metadataVersion = version_id
        details.datasetVisibilityExpression = viz
        self.jobs.create(self.config.environment, job_api.JobType.COMMIT, getpass.getuser(), details)
