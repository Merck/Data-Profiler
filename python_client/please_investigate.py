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
#! /usr/bin/python3
from dataprofiler import job_api
from pprint import pprint
from datetime import datetime, timedelta
from tabulate import tabulate
import json
import time
import argparse
import sys

time_format = '%Y-%m-%d'

if __name__ == '__main__':

    # Get all possible job statuses and types from the jobs API
    job_statuses = {s.value: s for s in job_api.JobStatus}
    job_types = {s.value: s for s in job_api.JobType}

    parser = argparse.ArgumentParser()

    parser.add_argument(
        '--environment',
        required=True,
        help='development, preview, or production')

    parser.add_argument(
        '--start-date',
        metavar='YYYY-MM-DD',
        default='1970-01-01',
        help='Start date with format: YYYY-MM-DD'
    )

    parser.add_argument(
        '--end-date',
        metavar='YYYY-MM-DD',
        default=(datetime.now() + timedelta(days=1)).strftime(time_format),
        help='End date with format: YYYY-MM-DD'
    )

    parser.add_argument(
        '--limit',
        default=1000,
        type=int,
        help='The maximum number of jobs to grab')

    parser.add_argument(
        '--yesterday',
        default=False,
        action='store_true',
        help='Only find jobs that errored in the previous day'
    )

    parser.add_argument(
        '--details',
        default=False,
        action='store_true',
        help='Display the job details'
    )

    parser.add_argument(
        '--stats',
        default=False,
        action='store_true',
        help='Display detailed job statistics'
    )

    parser.add_argument(
        '--statuses',
        metavar='status',
        default=['unknown', 'not_started', 'starting', 'recovering', 'idle', 'running', 'busy',
                 'shutting_down', 'error', 'dead', 'killed', 'success', 'cancelled', 'queued'],
        nargs='+',
        help=f'Statuses to query against. Default statuses are "error" and "dead". All possible statuses: {", ".join(job_statuses.keys())}'
    )

    parser.add_argument(
        '--job-types',
        metavar='type',
        default=['tableLoad', 'commit'],
        nargs='+',
        help=f'Job types to query against. Default types are "tableLoad" and "commit". All possible types: {", ".join(job_types.keys())}'
    )

    options = parser.parse_args()

    # Convert list of supplied status into JobStatus objects
    statuses = []
    for s in options.statuses:
        if s not in job_statuses:
            print(
                f'ERROR: "{s}" not in statuses: {", ".join(job_statuses.keys())}')
            parser.print_help()
            sys.exit()

        statuses.append(job_statuses.get(s))

    # Convert list of supplied types into JobTypes objects
    types = []
    for s in options.job_types:
        if s not in job_types:
            print(
                f'ERROR: "{s}" not in job types: {", ".join(job_types.keys())}')
            parser.print_help()
            sys.exit()

        types.append(job_types.get(s))

    # Set the start and end time for the query
    if options.yesterday:
        end_time = datetime.now()
        start_time = end_time - timedelta(days=1)
    else:
        start_time = datetime.strptime(options.start_date, time_format)
        end_time = datetime.strptime(options.end_date, time_format)

    print(f'Start Date: {start_time}')
    print(f'End Date: {end_time}')

    api = job_api.JobApi(base_url=job_api.ENDPOINTS[options.environment])
    jobs = api.jobs(limit=options.limit)

    jrrbs = []
    jrrbs_stats = {}
    for job in jobs:
        if job.type in types and job.status in statuses:
            if job.created_at >= start_time and job.created_at <= end_time:
                jrrbs.append(job.to_json())
                jrrbs_stats[job.status] = jrrbs_stats.get(
                    job.status, []) + [(job.id, job.type)]

    if options.stats:
        table = []
        for k, v in jrrbs_stats.items():
            for (id, job_type) in v:
                table.append([k.value, id, job_type.value])

        print()
        print(tabulate(table, headers=['Status', 'ID', 'Type']))

    table = []
    for status in statuses:
        table.append([status.value, len(jrrbs_stats.get(status, []))])

    print()
    print(tabulate(table, headers=['Status', 'Count']))

    # Print details
    if options.details and jrrbs:
        print('\nJob Details')
        for fail in jrrbs:
            pprint(fail)
