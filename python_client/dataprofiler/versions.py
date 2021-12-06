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
from dataprofiler import java, config

DEFAULT_DATAPROFILER_JAR = "/usr/lib/dataprofiler/tools/dataprofiler-tools-current.jar"
MANAGE_METADATA_CLASS = "com.dataprofiler.metadata.ManageMetadata"
DELETE_DATA_CLASS = "com.dataprofiler.delete.DeleteData"


def run_manage_metadata(args, jar=DEFAULT_DATAPROFILER_JAR):
    c = config.Config()
    c.from_env()
    java.run_java_command(c, MANAGE_METADATA_CLASS,
                          args, DEFAULT_DATAPROFILER_JAR)


def show_all_datasets(jar=DEFAULT_DATAPROFILER_JAR):
    run_manage_metadata(["show-all-datasets"], jar=jar)


def show_dataset(dataset_name, jar=DEFAULT_DATAPROFILER_JAR):
    run_manage_metadata(["show-dataset", "--dataset", dataset_name], jar=jar)


def show_table(dataset_name, table_name, jar=DEFAULT_DATAPROFILER_JAR):
    run_manage_metadata(
        ["show-table", "--dataset", dataset_name, "--table", table_name])


def delete_data(dataset_name, table_name=None, table_id=None, delete_previous_versions=False, delete_all_versions=False, delete_index=False, delete_aliases=False, purge=False, jar=DEFAULT_DATAPROFILER_JAR):

    c = config.Config()
    c.from_env()

    args = []

    if delete_index:
        args.append("--delete-index")

    if delete_aliases:
        args.append("--delete-aliases")

    if table_id:
        args.extend(["--table-id", table_id])

    if delete_previous_versions:
        args.append("--delete-previous-versions")

    if delete_all_versions:
        args.append("--delete-all-versions")

    if purge:
        args.append("--purge")

    args.extend([dataset_name])

    if table_name is not None:
        args.extend([table_name])

    java.run_java_command(c, DELETE_DATA_CLASS, args, DEFAULT_DATAPROFILER_JAR)


def delete_dataset(dataset_name, jar=DEFAULT_DATAPROFILER_JAR):
    delete_data(dataset_name, jar=jar)


def purge_dataset(dataset_name, delete_previous_versions=False, delete_all_versions=False, delete_index=True, delete_aliases=True, jar=DEFAULT_DATAPROFILER_JAR):
    delete_data(dataset_name, purge=True,
                delete_previous_versions=delete_previous_versions,
                delete_all_versions=delete_all_versions,
                delete_index=delete_index,
                delete_aliases=delete_aliases,
                jar=jar)


def delete_table(dataset_name, table_name, jar=DEFAULT_DATAPROFILER_JAR):
    delete_data(dataset_name, table_name=table_name, jar=jar)


def purge_table(dataset_name, table_name, table_id=None, delete_previous_versions=False, delete_all_versions=False, delete_index=True, delete_aliases=True, jar=DEFAULT_DATAPROFILER_JAR):
    delete_data(dataset_name, table_name=table_name,
                table_id=table_id,
                delete_previous_versions=delete_previous_versions,
                delete_all_versions=delete_all_versions,
                delete_index=delete_index,
                delete_aliases=delete_aliases,
                purge=True, jar=jar)
