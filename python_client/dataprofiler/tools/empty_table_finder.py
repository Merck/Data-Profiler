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
from argparse import ArgumentParser
from datetime import timedelta, datetime
from logging import getLogger, Formatter, StreamHandler, DEBUG as LOGGING_DEBUG, INFO as LOGGING_INFO
from os import environ as os_environ
from requests import post as rpost

from ..job_api import JobApi, JobType, JobStatus, ENDPOINTS

CLUSTER = os_environ.get('CLUSTER_NAME', None)  # If full map
if CLUSTER is None:
    CLUSTER = os_environ.get('ENVIRONMENT_NAME', 'test-localhost')  # If partial map
CLUSTER = CLUSTER.split('-')[-1]


logger = getLogger('empty_table_finder')
logger.setLevel(LOGGING_DEBUG)


def post_message_to_slack_webhook(message, slack_name='Tabler', channel='#proj-alerts',
                                  webhook_url=os_environ.get('SLACK_WEBHOOK', 'localhost')):
    """
    Posts a message to the specified slack webhook
    :param message: str; message to post
    :param slack_name: str; Username for slack to display
    :param channel: str; channel to post in slack
    :param webhook_url: str; webhook fqurl
    :return: None
    """
    logger.info(f'Sending Slack Notification as {slack_name} to channel {channel}')
    response = rpost(webhook_url, 
                     json={
                         'username': slack_name,
                         'channel': channel,
                         'text': message
                     })
    if response.status_code != 200:
        logger.error('Unable to send slack notification!')


def get_timebound_jobs(job_api, job_type, bounds=timedelta(days=2)):
    """
    Gets jobs of a type in the timebound
    :param job_api: JobApi; job api handle
    :param job_type: str; typically from JobType enum
    :param bounds: timedelta; time range to check, defaults to last day
    :return: list; list of Jobs
    """
    time_bound = datetime.utcnow() - bounds
    jobs = list()
    logger.info(f'Attempting to fetch jobs of type: {job_type}')
    rjobs = job_api.jobs(type=job_type)
    logger.debug('Finding time bound jobs')
    for job in rjobs:
        if job.updated_at > time_bound:
            logger.debug('Found job')
            jobs.append(job)
    logger.info(f'Found {len(jobs)} jobs fitting specified bounds')
    return jobs


def match_metadata(table_load_jobs, commit_jobs):
    """
    Attempts to match table load job with commit job via metadata value
    :param table_load_jobs: list(Job); list of table load Jobs
    :param commit_jobs: list(Job); list of commit jobs
    :return dict; {
        'matches': [(load_job, commit_job)]
        'unmatched': {
            'load_jobs': list(load_job),
            'commit_jobs': list(commit_job)
        }
    }
    """
    to_return = {
        'matches': dict(),
        'unmatched': {
            'load_jobs': list(),
            'commit_jobs': list()
        }
    }
    for tl_job in table_load_jobs:
        tl_metadata = tl_job.details.metadataVersion
        found = list()
        for count in range(len(commit_jobs)):
            commit_job = commit_jobs[count]
            if commit_job.details.metadataVersion == tl_metadata:
                if to_return['matches'].get(tl_job, None) is None:
                    to_return['matches'][tl_job] = list()
                # check that when removing job job actually is copied (may need to be deep copied over due to python references)
                to_return['matches'][tl_job].append(commit_job)
                found.append(count)
        if len(found) == 0:
            to_return['unmatched']['load_jobs'].append(tl_job)
        else:
            for job in found:
                del commit_jobs[job]
    to_return['unmatched']['commit_jobs'] = commit_jobs  # save whatever is left as unmatched
    return to_return
        

def load_successful(table_load_job, 
                    commit_job, 
                    acceptable_errors=(JobStatus.ERROR, 
                                       JobStatus.DEAD, 
                                       JobStatus.KILLED)):
    """
    Checks the table load job and the commit job to determine if an error occurred
    :param table_load_job: Job; table load job
    :param commit_job: Job; commit job
    :param acceptable_error: list/tuple[Enum]; iterable of acceptable JobStatuses that 
                                               define an error
    :return bool; True if load successful (no errors), False if load failed
    """
    success = True
    if table_load_job.status in acceptable_errors:
        logger.info(f'Table Load Job ({table_load_job.details.metadataVersion}) was in an error state! Job ID: {table_load_job.id}')
        success = False
    if commit_job.status in acceptable_errors:
        logger.info(f'Commit Job ({commit_job.details.metadataVersion}) was in an error state! Job ID: {commit_job.id}')
        success = False

    return success


def find_failed_load_commit_jobs(days_to_look):
    job_api = JobApi(ENDPOINTS[CLUSTER])
    logger.info(f'Looking for jobs in the last {days_to_look} day(s)')
    table_load_jobs = get_timebound_jobs(job_api, JobType.TABLE_LOAD.value, timedelta(days=days_to_look))
    commits = get_timebound_jobs(job_api, JobType.COMMIT.value, timedelta(days=days_to_look))
    to_compare = match_metadata(table_load_jobs, commits)
    failed_metadata = list()
    for job in to_compare['matches']:
        for commit_job in to_compare['matches'][job]:
            if load_successful(job, commit_job) is False:
                failed_metadata.append({
                    'metadataVersion': job.details.metadataVersion,
                    'table_load_job_id': job.id,
                    'commit_job_id': commit_job.id
                })

    return failed_metadata


def setup_loggers(level=LOGGING_INFO):
    formatter = Formatter('%(asctime)s - %(levelname)s - %(message)s')
    ch = StreamHandler()
    ch.setLevel(level)
    ch.setFormatter(formatter)
    logger.addHandler(ch)


def main():
    setup_loggers()
    parser = ArgumentParser(description='Searches jobs failed related commits/table loads')
    parser.add_argument('days', type=int, default=2, help='Number of days to search previous to now')
    parser.add_argument('--slack', action='store_true', default=False, help='Post a report to the proj-alerts slack channel')
    args = parser.parse_args()
    failed_jobs = find_failed_load_commit_jobs(args.days)
    if len(failed_jobs) > 0:
        logger.info(f'Found failed jobs. Failed jobs: {failed_jobs}')
        if args.slack is True:
            post_message_to_slack_webhook(f'Failed Table Load and Commit Jobs occured! Please investigate the following: {failed_jobs}')
    else:
        logger.info('No recent failed jobs found')


if __name__ == '__main__':
    main()
