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
import requests

from string import Template
import datetime
import json
import hashlib


DEFAULT_URL = "https://development-internal.dataprofiler.com/tables/graphql"


class User:

    PROPERTIES = """
        id
        username
        created_at
        updated_at
        tables {
            id
            environment
            datasetname
            tablename
            visibility
            created_at
            updated_at
        }
    """

    def __init__(self, api):
        self.api: TableMapperApi = api
        self.id = -1
        self.username = ''
        self.tables = []
        self.created_at = datetime.datetime.now()
        self.updated_at = datetime.datetime.now()

    @classmethod
    def from_json(cls, api, json):
        if json is None:
            return None

        user = cls(api)
        # 2020-05-29T20:19:37.584Z
        tformat = "%Y-%m-%dT%H:%M:%S.%fZ"
        user.created_at = datetime.datetime.strptime(
            json['created_at'], tformat)
        user.updated_at = datetime.datetime.strptime(
            json['updated_at'], tformat)

        user.id = json['id']
        user.username = json['username']

        for table in json.get('tables', []):
            user.tables.append(Table.from_json(api, table))

        return user

    def __repr__(self):
        return str(self.__dict__)


class Table:

    PROPERTIES = """
        id
        environment
        datasetname
        tablename
        external_name
        visibility
        created_at
        updated_at
        enabled
        users {
            id
            username
            created_at
            updated_at
        }
    """

    def __init__(self, api):
        self.api: TableMapperApi = api
        self.id = -1
        self.environment = None
        self.dataset_name = ''
        self.table_name = ''
        self.external_name = ''
        self.visibility = ''
        self.users = []
        self.enabled = False
        self.created_at = datetime.datetime.now()
        self.updated_at = datetime.datetime.now()

    @classmethod
    def from_json(cls, api, json):
        if json is None:
            return None

        table = cls(api)
        # 2020-05-29T20:19:37.584Z
        tformat = "%Y-%m-%dT%H:%M:%S.%fZ"
        table.created_at = datetime.datetime.strptime(
            json['created_at'], tformat)
        table.updated_at = datetime.datetime.strptime(
            json['updated_at'], tformat)

        table.id = json['id']
        table.environment = json['environment']
        table.dataset_name = json['datasetname']
        table.table_name = json['tablename']
        table.external_name = json['external_name']
        table.visibility = json['visibility']
        table.enabled = json['enabled']

        for user in json.get('users', []):
            table.users.append(User.from_json(api, user))

        return table

    def __repr__(self):
        return str(self.__dict__)


class TableMapperApi:

    QUERY_TEMPLATE = Template("""{
        $query {
            $properties
        }
    }
    """)

    MUTATION_TEMPLATE = Template("""
    mutation {
        $query {
            $properties
        }
    }
    """)

    def __init__(self, base_url=DEFAULT_URL):
        self.base_url = base_url

    def __exec_query(self, query, obj_class, data_key=None):
        if data_key is None:
            data_key = query

        raw_query = self.QUERY_TEMPLATE.substitute(
            query=query, properties=obj_class.PROPERTIES)
        return self.__exec_raw_query(raw_query, obj_class, data_key)

    def __exec_mutation(self, query, obj_class, data_key=None):
        if data_key is None:
            data_key = query

        raw_query = self.MUTATION_TEMPLATE.substitute(
            query=query, properties=obj_class.PROPERTIES)
        return self.__exec_raw_query(raw_query, obj_class, data_key)

    def __exec_raw_query(self, raw_query, obj_class, data_key, no_return=False, return_numeric=False):
        ret = requests.post(self.base_url, json={"query": raw_query})
        ret.raise_for_status()
        if no_return:
            return None

        data = ret.json()['data'][data_key]
        if return_numeric:
            return data
        elif not isinstance(data, list):
            return obj_class.from_json(self, data)
        else:
            objs = []
            for obj in data:
                try:
                    objs.append(obj_class.from_json(self, obj))
                except Exception as e:
                    print(e)
                    continue

            return objs

    @classmethod
    def generate_external_name(cls, environment, dataset_name, table_name, visibility):
        """
        The eternal table name is a concatenation of the environment, dataset name, table name, and
        hash of the visibility. Table names in Postgres must be under 63 chars so the dataset and
        table name are limited to 17 chars and the hash of the visibilities is 15 chars.
        """

        visibility_hash = int(hashlib.sha256(
            visibility.encode('utf-8')).hexdigest(), 16) % 10**15

        external_name = '{env}_{dataset}_{table}_{viz_hash}'.format(
            env=environment,
            dataset=dataset_name[:17],
            table=table_name[:17],
            viz_hash=str(visibility_hash))

        return external_name

    def tables(self, id=None, environment=None, dataset_name=None, table_name=None, external_name=None, visibility=None):
        params = {}

        if id:
            params['id'] = id
        if environment:
            params['environment'] = environment
        if dataset_name:
            params['datasetname'] = dataset_name
        if table_name:
            params['tablename'] = table_name
        if external_name:
            params['external_name'] = external_name
        if visibility:
            params['visibility'] = visibility

        param_items = ['%s: "%s"' % (k, v) if isinstance(
            v, str) else '%s: %s' % (k, v) for k, v in params.items()]

        param_string = ",".join(param_items)
        query = Template("""tables($params)""").substitute(params=param_string)

        return self.__exec_query(query, Table, data_key="tables")

    def all_tables(self):
        return self.__exec_query("allTables", Table)

    def tables_like(self, environment='', dataset_name='', table_name='', external_name='', visibility=''):
        query = Template("""tablesLike(
            environment: "$environment",
            datasetname: "$datasetname",
            tablename: "$tablename",
            external_name: "$external_name",
            visibility: "$visibility")
        """).substitute(
            environment=environment,
            datasetname=dataset_name,
            tablename=table_name,
            external_name=external_name,
            visibility=visibility)

        return self.__exec_query(query, Table, data_key='tablesLike')

    def create_update_table(self, environment='', dataset_name='', table_name='', visibility='', users=[]):

        external_name = self.generate_external_name(
            environment, dataset_name, table_name, visibility)

        query = Template("""createUpdateTable(
            environment: "$environment",
            datasetname: "$dataset_name",
            tablename: "$table_name",
            external_name: "$external_name",
            visibility: "$visibility",
            users: $users,
            enabled: $enabled)
        """).substitute(
            environment=environment,
            dataset_name=dataset_name,
            table_name=table_name,
            external_name=external_name,
            visibility=visibility,
            users=json.dumps(users),
            enabled='true')

        return self.__exec_mutation(query, Table, data_key='createUpdateTable')

    def delete_tables(self, environment='', dataset_name='', table_name='', external_name='', visibility=''):

        params = {}

        if environment:
            params['environment'] = environment
        if dataset_name:
            params['datasetname'] = dataset_name
        if table_name:
            params['tablename'] = table_name
        if external_name:
            params['external_name'] = external_name
        if visibility:
            params['visibility'] = visibility

        param_items = ['%s: "%s"' % (k, v) for k, v in params.items()]
        param_string = ",".join(param_items)

        query = Template("""mutation {
            deleteTables($params)
        }""").substitute(params=param_string)

        return self.__exec_raw_query(query, Table, 'deleteTables', return_numeric=True)

    def delete_all_tables(self):
        query = "deleteAllTables"
        return self.__exec_mutation(query, Table, data_key='deleteAllTables')

    def user(self, id=None, user_name=None):
        params = {}

        if id:
            params['id'] = id
        if user_name:
            params['username'] = user_name

        param_items = ['%s: "%s"' % (k, v) if isinstance(
            v, str) else '%s: %s' % (k, v) for k, v in params.items()]

        param_string = ",".join(param_items)
        query = Template("""user($params)""").substitute(params=param_string)

        return self.__exec_query(query, User, data_key="user")

    def all_users(self):
        return self.__exec_query("allUsers", User)

    def users_like(self, user_name=''):
        query = Template("""usersLike(
            username: "$username")
        """).substitute(
            username=user_name)

        return self.__exec_query(query, User, data_key='usersLike')

    def remove_users_from_table(self, environment='', dataset_name='', table_name='', external_name='', visibility='', users=[]):

        query = Template("""removeUsersFromTable(
            environment: "$environment",
            datasetname: "$dataset_name",
            tablename: "$table_name",
            external_name: "$external_name",
            visibility: "$visibility",
            users: $users)
        """).substitute(
            environment=environment,
            dataset_name=dataset_name,
            table_name=table_name,
            visibility=visibility,
            users=json.dumps(users))

        return self.__exec_mutation(query, Table, data_key='removeUsersFromTable')
