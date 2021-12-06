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
#! /usr/bin/env python3

import json
from json.encoder import JSONEncoder
from typing import Dict, List
from dataprofiler import job_api
import boto3
import os
import sys
import oneshot
import uuid
from dotmap import DotMap
from pprint import pprint
import click


s3 = boto3.client("s3")

TRACKER = "jobs.txt"
ENVIRONMENT = "development"
api = None

def setup(environment, tracker):
    global ENVIRONMENT, TRACKER, api
    ENVIRONMENT = environment
    TRACKER = tracker
    api = job_api.JobApi(base_url=job_api.ENDPOINTS[ENVIRONMENT])

@click.group()
@click.option("--environment", default="development", help="Which environment to run jobs in.")
@click.option("--tracker", default="jobs.txt", help="File to track jobs with.")
def commands(environment, tracker):
    setup(environment, tracker)

def split_bucket_and_key(path):
    directory = path[-1] == '/'
    splits = path.split('/')
    assert (len(splits) >= 1)
    bucket_name = splits[0]
    prefix = None
    if len(splits) > 1:
        prefix = os.path.normpath('/'.join(splits[1:]))
        if directory:
            prefix = prefix + '/'

    return bucket_name, prefix

def s3_ls_objects(s3path, include_size=False):
    bucket_name, prefix = split_bucket_and_key(s3path)

    l = s3.list_objects(Bucket=bucket_name, Prefix=prefix,
                                Delimiter="/")
    for object in l.get('Contents'):
        value = object['Key'].split('/')[-2]
        #value = os.path.join(bucket_name, object['Key'])
        if include_size:
            yield value, object['Size']
        else:
            yield value

def s3_ls_dirs(s3path):
    bucket_name, prefix = split_bucket_and_key(s3path)

    l = s3.list_objects_v2(Bucket=bucket_name, Prefix=prefix,
                                 Delimiter="/")

    if 'CommonPrefixes' not in l:
        return
    for object in l.get('CommonPrefixes'):
        value = object['Prefix'].split('/')[-2] + '/'
        yield value

def open_tracker_for_writes(output_fname=TRACKER):
    if os.path.exists(output_fname):
        val = input(f"{output_fname} exists - overwrite [y/N]: ")
        if val != "y":
            sys.exit(1)
    return open(output_fname, "w")

@commands.command()
def play_the_blues():
    print("go to https://www.youtube.com/watch?v=-CixtG_bF28")

class Dataset:
    def __init__(self, name):
        self.metadata_version = str(uuid.uuid4())
        self.name = name
        self.tables: Dict[str, job_api.Job] = {} # table names - > job

    def check_metadata_versions(self):
        mv = None
        for job in self.jobs():
            if mv is None:
                mv = job.details.metadataVersion
                continue
            if job.details.metadataVersion != mv:
                raise Exception("Metadata Version mismatch")

        self.metadata_version = mv

    def jobs(self) -> job_api.Job:
        for job in self.tables.values():
            yield job

    def to_json(self):
        return {
            "metadata_version": self.metadata_version,
            "name": self.name,
            "tables": { k: v.to_json() for (k, v) in self.tables.items() }
        }

class Datasets:
    def __init__(self):
        self.datasets: Dict[str, Dataset] = {} # name -> dataset
        self.commits: List[job_api.Job] = []

    def from_old_file(self):
        fd = open("current_jobs.txt")
        for line in fd.readlines():
            if "DotMap" in line:
                continue
            job = api.job(line.strip())

            if job.type == job_api.JobType.COMMIT:
                self.commits.append(job)
            else:
                n = job.details.datasetName
                if not n in self.datasets:    
                    self.datasets[n] = Dataset(n)
                self.datasets[n].tables[job.details.tableName] = job
            

    def from_tracker_file(self):
        fd = open(TRACKER)
        data = json.load(fd)
        for commit in data["commits"]:
            job = api.job(commit["id"])
            self.commits.append(job)
        for k,v in data["datasets"].items():
            d = Dataset(k)
            self.datasets[k] = d
            d.metadata_version = v["metadata_version"]
            for table in v["tables"].values():
                job = api.job(table["id"])
                d.tables[job.details.tableName] = job

        for dataset in self.datasets.values():
            dataset.check_metadata_versions()

    def to_tracker_file(self, output_fname=TRACKER):
        fd = open_tracker_for_writes(output_fname)
        json.dump(self.to_json(), fd)

    def all_jobs(self):
        for job in self.commit_jobs():
            yield job

        for job in self.load_jobs():
            yield job

    def commit_jobs(self):
        for commit in self.commits:
            yield commit

    def load_jobs(self):
        for dataset in self.datasets.values():
            for job in dataset.jobs():
                yield job

    def to_json(self):
        return {
            "commits": [ x.to_json() for x in self.commits ],
            "datasets": { k:v.to_json() for (k, v) in self.datasets.items() }
        }



@commands.command()
@click.argument("visibility")
@click.option("--full-dataset-load", default=False, help="Perform a full dataset load (have this load replace all tables)")
def commit(visibility, full_dataset_load):
    datasets = Datasets()
    datasets.from_tracker_file()
    for dataset in datasets.datasets.values():
        d = DotMap()
        d.environment = ENVIRONMENT
        d.metadata_version_id = dataset.metadata_version
        d.visibility_expression = visibility
        d.full_dataset_load = full_dataset_load
        d.dataset = dataset.name
        o = oneshot.Oneshot(d)
        print(f"Creating commit job for dataset {d.dataset} [{d.metadata_version_id}]")
        j = o.create_commit_job(d)
        datasets.commits.append(j)

    datasets.to_tracker_file()

def is_table_empty(job):
    l = list(s3_ls_objects(job.details.sourceS3Bucket + "/" + job.details.sourceS3Key))
    if len(l):
        return l
    else:
        return None


@commands.command(help="Check if dead jobs actually just had empty tables")
def check_dead_not_empty():
    d = Datasets()
    d.from_tracker_file()
    for job in d.load_jobs():
        if job.status == job_api.JobStatus.DEAD:
            l = is_table_empty(job)
            if l is not None:
                print(f"{job}: {str(l)}")

@commands.command()
def kill_jobs():
    datasets = Datasets()
    datasets.from_tracker_file()
    for job in datasets.all_jobs():
        if job.status == job_api.JobStatus.NEW:
            job.set_status(job_api.JobStatus.KILLED)

@commands.command()
@click.argument("tracker")
@click.option("--skip-empty/--no-skip-empty", default=True)
@click.option("--all/--only-dead", default=False, help="Restart all regardless of status")
def restart_failed(tracker, skip_empty, all):
    datasets = Datasets()
    datasets.from_tracker_file()
    new_datasets = Datasets()
    for job in datasets.load_jobs():
        if all or job.status == job_api.JobStatus.DEAD:
            if not all and (skip_empty and is_table_empty(job) is None):
                print(f"Skipping {job.details.datasetName}:{job.details.tableName}")
                continue
            
            if job.details.datasetName not in new_datasets.datasets:
                new_datasets.datasets[job.details.datasetName] = Dataset(job.details.datasetName)
            d = new_datasets.datasets[job.details.datasetName]
            job.details.metadataVersion = d.metadata_version
            job.details.estimatedRows = 1000000000000
            job.set_details(job.details)
            job.set_status(job_api.JobStatus.NEW)

            d.tables[job.details.tableName] = job

    new_datasets.to_tracker_file(tracker)

@commands.command()
@click.option("--job-type", default="all", help="Which job types (all, commit, load)")
@click.option("--details/--no-details", default=False)
def status(job_type, details):
    datasets = Datasets()
    datasets.from_tracker_file()
    i = 0
    for job in getattr(datasets, job_type + "_jobs")():
        print(f"{job.id}:{job.status}")
        if job.status == job_api.JobStatus.DEAD or details:
            pprint(str(job))
        i += 1
    print(i)

@commands.command()
def datasets():
    datasets = Datasets()
    datasets.from_tracker_file()
    for dataset in datasets.datasets.values():
        print(dataset.name)

@commands.command()
def upgrade_tracker():
    datasets = Datasets()
    datasets.from_old_file()
    datasets.to_tracker_file()

@commands.command()
@click.option("--data-format", default="orc")
@click.option("--schema-path", default=None)
@click.option("--only-dataset", default=None)
@click.option("--only-tables", default=None, help="Limit to these table names (comma separated).")
@click.option("--exclude-datasets", default=None, help="Datasets to exclude (comma separated).")
@click.argument("s3path")
@click.argument("visibility")
def load(s3path, visibility, data_format, schema_path, only_dataset, only_tables, exclude_datasets):
    if only_tables is not None:
        only_tables = only_tables.split(",")
    datasets = Datasets()
    if exclude_datasets is not None:
        exclude_datasets = exclude_datasets.split(",")
    for dataset in s3_ls_dirs(s3path):
        dataset_name = " ".join(dataset[:-1].split("_")).title()
        if only_dataset is not None and dataset[:-1] != only_dataset:
            print(f"Skipping {dataset_name}")
            continue
        if exclude_datasets is not None and dataset[:-1] in exclude_datasets:
            print(f"Excluding dataset {dataset_name}")
            continue
        d = Dataset(dataset_name)
        datasets.datasets[dataset_name] = d
        for obj in s3_ls_dirs(s3path + dataset):
            table_name = obj[:-1]
            if only_tables is not None and table_name not in only_tables:
                print(f"Skipping table {table_name}")
                continue
            args = DotMap()
            args.environment = ENVIRONMENT
            args.dataset_name = dataset_name
            args.metadata_version_id = d.metadata_version
            args.table_name = table_name
            args.data_format = data_format
            obj_s3path = s3path + dataset + obj
            args.s3_bucket, args.s3_key = split_bucket_and_key(obj_s3path)
            args.visibility_expression = visibility
            args.rows = 100000000000
            if schema_path is not None:
                sp = os.path.join(schema_path, dataset, args.table_name) + "-schema"
                args.schema_path = sp
            else:
                args.schema_path = None

            print(args)

            o = oneshot.Oneshot(args)
            j = o.create_load_job(args)
            d.tables[args.table_name] = j

    # if we skipped tables, there might actually be empty datasets. So prune those.
    pruned_datasets = {}
    for k, d in datasets.datasets.items():
        if len(d.tables) > 0:
            pruned_datasets[k] = d
    datasets.datasets = pruned_datasets

    datasets.to_tracker_file()


if __name__ == "__main__":
    commands()
