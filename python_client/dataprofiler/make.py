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
import json
import os
import sys

from dataprofiler import Api


class MakeMetadata:
    PLUGINS_PATH = '/usr/lib/dataprofiler/make-plugins'
    PLUGIN_FNAME = 'plugin.py'
    METADATA_FNAME = 'plugin.json'
    JSON_FORM_FNAME = 'form.json'

    def __init__(self, name, base_plugin_path=None):
        self.base_plugin_path = base_plugin_path
        if self.base_plugin_path is None:
            self.base_plugin_path = self.PLUGINS_PATH

        self.plugin_path = os.path.join(self.base_plugin_path, name)

        if not os.path.exists(self.plugin_path):
            raise Exception("Plugin directory does not exist")

    def __get_file(self, fname):
        full_path = os.path.join(self.plugin_path, fname)
        if not os.path.exists(full_path):
            raise Exception(
                "Could not find metadata for plugin " + self.plugin_path)
        with open(full_path) as fd:
            return json.load(fd)

    def metadata(self):
        return self.__get_file(self.METADATA_FNAME)

    def json_form(self):
        return self.__get_file(self.JSON_FORM_FNAME)

    def plugin_script_path(self):
        path = os.path.join(self.plugin_path, self.PLUGIN_FNAME)
        return path

    @classmethod
    def list_plugins(cls):
        plugin_dirs = [os.path.join(cls.PLUGINS_PATH, name) for name in
                       os.listdir(cls.PLUGINS_PATH)
                       if os.path.isdir(os.path.join(cls.PLUGINS_PATH, name))]

        out = []
        for d in plugin_dirs:
            try:
                m = MakeMetadata(d)
            except:
                continue

            out.append(m.metadata())

        return out


from enum import Enum
from typing import List
import argparse


class ObjectType(Enum):
    DATASET = "dataset"
    TABLE = "table"
    COLUMN = "column"


class ObjectArgument:
    """
    For all object types we accept an ObjectArguemnt that has a pyspark datafrome
    that
    matches what this plugin should receive.

    Type can be dataset, table, table_list, column, or column_list (strings).
    """

    def __init__(self):
        self.type = None
        self.dataset_name = None
        self.table_name = None
        self.column_name = None
        self.table = None
        self.output_visibility = ''
        self.column_properties = {}
        self.table_properties = {}
        self.dataset_properties = {}

    @classmethod
    def from_json(cls, node):
        o = ObjectArgument()
        o.type = node['objectType']
        o.dataset_name = node['dataset']
        o.table_name = node['table']
        o.column_name = node['column']

        return o


class Make:
    def __init__(self):
        self.api = Api()
        self.user = None
        self.authorizations = None

    def main(self, argv):
        parser = argparse.ArgumentParser(description="Make plugin tool")

        subparsers = parser.add_subparsers(help="commands", dest="command")

        d = subparsers.add_parser("run-cluster",
                                  help="Run make plugin on the cluster")
        d.add_argument("jobid")

        args = parser.parse_args(argv[1:])

        if args.command == "run-cluster":
            self.cluster_main(args)
        else:
            print("Unknown command")
            parser.print_help()
            sys.exit(1)

    def get_tables(self, sc, args: List[ObjectArgument]):
        for arg in args:
            from dataprofiler import pyspark_api
            arg.table = pyspark_api.get_table(sc, arg.dataset_name,
                                              arg.table_name, self.authorizations)

    def store_tables(self, sc,
                     args: List[ObjectArgument], full_dataset_load=False, commit_metadata=True):
        for arg in args:
            from dataprofiler import pyspark_api
            
            if not arg.output_visibility:
                raise ValueError('{} {} does not have an output visibility'.format(arg.dataset_name, arg.table_name))

            pyspark_api.store_table(sc,
                                    arg.table,
                                    arg.dataset_name,
                                    arg.table_name,
                                    arg.output_visibility,
                                    full_dataset_load=full_dataset_load,
                                    commit_metadata=commit_metadata
                                    )
                                    
            self.api.set_all_properties(arg.dataset_name, arg.table_name, self.dataset_properties, self.table_properties, self.column_properties)

    def set_user(self, user):
        self.user = user
        if self.user == '':
            self.user = 'root'

        self.authorizations = self.api.user_authorizations(self.user)

    def cluster_main(self, args) -> None:
        """
        This is the method that is called when you are running on the cluster
        :return: None
        """
        base_job = self.api.get_job(args.jobid)

        self.set_user(base_job['creatingUser'])

        job = self.api.get_job_detail(args.jobid)

        input_args = [ObjectArgument.from_json(node) for node in job['inputs']]
        output_args = [ObjectArgument.from_json(node) for node in
                       job['outputs']]

        for arg in output_args:
            arg.output_visibility = job['outputVisibility']

        self.process(job['options'], input_args, output_args)

    def process(self, options: dict={}, inputs: List[ObjectArgument]=None, outputs: List[ObjectArgument]=None):
        pass
