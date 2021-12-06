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
from logging import getLogger, Formatter, StreamHandler, DEBUG as LOGGING_DEBUG

# The following are known loggers in the dataprofiler python client
KNOWN_LOGGER_NAMES = ('dpc-datasets', 'dpc-job-api', 'dpc-s3fileinfo', 'dpc-spark-rest', 'dpc-java')
KNOWN_LOGGERS = [getLogger(name) for name in KNOWN_LOGGER_NAMES]


class OneLineFormatter(Formatter):
    """
    Overrides standard formatting and instead adds a tab character since logstash will filter on newlines
    """
    def format(self, record):
        result = super().format(record)
        return result.replace('\n', '\t')


def setup_logger(logger, level=LOGGING_DEBUG, log_format='[%(levelname)s][%(name)s][%(asctime)s] %(message)s'):
    """
    Given a logger, creates a handler with the one line formatter (removes newlines) with the specified log format
    Returns the handler
    """
    formatter = OneLineFormatter(log_format)
    ch = StreamHandler()
    ch.setLevel(level)
    ch.setFormatter(formatter)
    logger.addHandler(ch)

    for dpc_logger in KNOWN_LOGGERS:  # This ensures that all loggers in the dataprofiler client are available if used
        if not dpc_logger.hasHandlers():  # Don't want to readd a handler, only need one
            dpc_logger.addHandler(ch)

    return ch


def setup_logger_with_job_id(logger, job_id, level=LOGGING_DEBUG):
    """
    Modifies the log format to add the job id as part of the message for later lookup
    Returns the handler
    """
    log_format = '[%(levelname)s][%(name)s][{job_id}][%(asctime)s] %(message)s'.format(job_id=job_id)
    return setup_logger(logger, level=level, log_format=log_format)


def cleanup_logger_handler(logger, handler):
    """
    Given a logger and a handler, attempts to remove said handler from the supplied logger and all the known loggers
    """
    if logger.hasHandlers():
        logger.removeHandler(handler)

    for dpc_logger in KNOWN_LOGGERS:
        if dpc_logger.hasHandlers():
            dpc_logger.removeHandler(handler)
