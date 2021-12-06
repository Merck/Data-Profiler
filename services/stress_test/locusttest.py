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
from locust import HttpUser, User, TaskSet, SequentialTaskSet, task, between
from http import cookiejar
import configparser
import random
import json
import urllib3
import pprint

# Suppress TLS warnings See: https://urllib3.readthedocs.io/en/latest/advanced-usage.html#ssl-warnings
urllib3.disable_warnings()

# Endpoints
TREEMAP_ENDPOINT = '/api/treemap'
COLUMN_COUNTS_ENDPOINT = '/api/colcounts'
ROWS_ENDPOINT = '/api/data/rows'
DATASET_ENDPOINT = '/api/v1/datasets'
TABLES_ENDPOINT = '/api/v1/tables'
COLUMNS_ENDPOINT = '/api/v1/columns'
SAMPLES_ENDPOINT = '/api/samples'
ANNOTATIONS_DATASET_ENDPOINT = '/api/annotations/hierarchy/dataset'
ANNOTATION_TABLE_ENDPOINT = '/api/annotations/hierarchy/table'

# This is here because cookies are saved within a session and for some reason the PlayFramework
# doesn't like using cookies passed back from a GET request for a subsequent POST request. This
# should probably be fixed in the PlayFramework, but ¯\_(ツ)_/¯


class BlockAll(cookiejar.CookiePolicy):
    return_ok = set_ok = domain_return_ok = path_return_ok = lambda self, * \
        args, **kwargs: False
    netscape = True
    rfc2965 = hide_cookie2 = False


class ColumnView(SequentialTaskSet):
    """Model the user behavior of the Column Viewer

    Since a table and dataset have already been chosen, the user must first choose which column to view. After a column is selected a user can decide to view subsequent pages or change the ordering of the column being displayed.
    """

    def on_start(self):
        self.headers = self.parent.parent.headers
        self.dataset = self.parent.parent.dataset
        self.table = self.parent.parent.table
        self.columns = self.parent.parent.columns
        self.client.cookies.set_policy(BlockAll())

    # This is the initial load of the column
    @task
    def get_column_values(self):
        self.column = random.choice(self.columns)

        data = {
            'dataset': self.dataset,
            'table': self.table,
            'column': self.column,
            'normalized': 'false',
            'start': 0,
            'end': 256,
            'sort': 'CNT_DESC'
        }

        response = self.client.post(COLUMN_COUNTS_ENDPOINT,
                                    json=data,
                                    headers=self.headers,
                                    allow_redirects=False,
                                    name=COLUMN_COUNTS_ENDPOINT).json()

    @task
    class ColumnViewOptions(TaskSet):
        """Model the user behavior from within the Column View

        From within the column viewer, the user has the ability to change the ordering of the column or view subsequent pages of the selected column.
        """

        def on_start(self):
            self.headers = self.parent.headers
            self.dataset = self.parent.dataset
            self.table = self.parent.table
            self.column = self.parent.column
            self.client.cookies.set_policy(BlockAll())

        @task(2)
        def change_column_sort(self):
            sort_order = random.choice(
                ['CNT_DESC', 'CNT_ASC', 'VAL_DESC', 'VAL_ASC'])

            data = {
                'dataset': self.dataset,
                'table': self.table,
                'column': self.column,
                'normalized': 'false',
                'start': 0,
                'end': 256,
                'sort': sort_order
            }

            response = self.client.post(COLUMN_COUNTS_ENDPOINT,
                                        json=data,
                                        headers=self.headers,
                                        allow_redirects=False,
                                        name=COLUMN_COUNTS_ENDPOINT)

        @task(2)
        def get_new_page(self):
            start_idx = random.randint(0, 1000) * 100
            end_idx = start_idx + 100

            data = {
                'dataset': self.dataset,
                'table': self.table,
                'column': self.column,
                'normalized': 'false',
                'start': start_idx,
                'end': end_idx,
                'sort': 'CNT_DESC'
            }

            response = self.client.post(COLUMN_COUNTS_ENDPOINT,
                                        json=data,
                                        headers=self.headers,
                                        allow_redirects=False,
                                        name=COLUMN_COUNTS_ENDPOINT).json()

        @task
        def stop(self):
            # Exit ColumnViewOptions
            self.interrupt()

    @task
    def stop(self):
        # Exit ColumnView
        self.interrupt()


class RowView(TaskSet):
    """Model the user behavior of the Row Viewer

    From within the row viewer the users first must view the rows. After the rows have been loaded the user can apply one or more filters.
    """

    def on_start(self):
        self.headers = self.parent.parent.headers
        self.dataset = self.parent.parent.dataset
        self.table = self.parent.parent.table
        self.client.cookies.set_policy(BlockAll())
        self.filters = {}

    @task
    class RowFilter(SequentialTaskSet):
        """Model the user behavior of the Row Viewer Filters

        This task retrieves the row data and then applies filters.
        """

        def on_start(self):
            self.headers = self.parent.headers
            self.dataset = self.parent.dataset
            self.table = self.parent.table
            self.client.cookies.set_policy(BlockAll())
            self.filters = self.parent.filters

        @task
        def get_rows(self):
            data = {
                'dataset': self.dataset,
                'table': self.table,
                'filters': {},
                'api_filter': False,
                'pageSize': 500,
                'limit': 500
            }

            self.rows = self.client.post(ROWS_ENDPOINT,
                                         json=data,
                                         headers=self.headers,
                                         allow_redirects=False,
                                         name=ROWS_ENDPOINT).json()

        @task
        class ApplyRowFilter(TaskSet):
            """Model filtering of the row viewer

            This class can allow 0 or more filters to be applied to the row data. To apply a filter a column must first be selected and the column data for that column will be retrieved. after selecting which values to filter on, the filter is applied and resultant row data is returned.
            """

            def on_start(self):
                self.headers = self.parent.headers
                self.dataset = self.parent.dataset
                self.table = self.parent.table
                self.rows = self.parent.rows
                self.client.cookies.set_policy(BlockAll())
                self.filters = self.parent.filters

            @task
            def get_row_filter(self):

                # Select a random column that is not already in the filter
                cols_not_filtered = [
                    k for k in self.rows['sortedColumns'] if k and k not in self.filters.keys()]

                if len(cols_not_filtered) == 1:
                    self.interrupt()

                self.column = random.choice(cols_not_filtered)

                # Get column values
                data = {
                    'dataset': self.dataset,
                    'table': self.table,
                    'column': self.column,
                    'normalized': 'false',
                    'start': 0,
                    'end': 25,
                    'sort': 'CNT_DESC'
                }

                self.column_values = self.client.post(COLUMN_COUNTS_ENDPOINT,
                                                      json=data,
                                                      headers=self.headers,
                                                      allow_redirects=False,
                                                      name=COLUMN_COUNTS_ENDPOINT).json()

                # If a column contains no values don't add a filter
                if len(self.column_values) == 0:
                    self.interrupt()

                colum_obj = random.choice(self.column_values)
                self.filters[self.column] = [colum_obj['n']]

                # Get the filtered rows response
                data = {
                    'dataset': self.dataset,
                    'table': self.table,
                    'filters': self.filters,
                    'api_filter': False,
                    'pageSize': 500,
                    'limit': 500
                }

                self.rows = self.client.post(ROWS_ENDPOINT,
                                             json=data,
                                             headers=self.headers,
                                             allow_redirects=False,
                                             name=f'{ROWS_ENDPOINT} with filters').json()

            @task
            def stop(self):
                # Exit ApplyRowFilter
                self.interrupt()

        @task
        def stop(self):
            # Exit RowFilter
            self.interrupt()

    @task
    def stop(self):
        # Exit RowView
        self.interrupt()


class UnderstandTab(SequentialTaskSet):
    """Model the users behavior of the Understand Tab

    A user that enters the Understand tab will drill down from dataset, to table, then to column. After selecting the column, the user has the choice to see the row viewer or the column viewer.
    """

    def on_start(self):
        self.datasets = self.parent.parent.datasets
        self.headers = self.parent.parent.headers
        self.client.cookies.set_policy(BlockAll())

    @task
    def select_column(self):
        self.dataset = random.choice(self.datasets)

        # Get the table
        tables_response = self.client.get(f'{TABLES_ENDPOINT}/{self.dataset}',
                                          headers=self.headers,
                                          name=f'{TABLES_ENDPOINT}/[dataset]').json()
        self.table = random.choice(list(tables_response.keys()))

        # Get the column
        columns_response = self.client.get(f'{COLUMNS_ENDPOINT}/{self.dataset}/{self.table}',
                                           headers=self.headers,
                                           name=f'{{COLUMNS_ENDPOINT}}/[dataset]/[table]').json()
        self.columns = list(columns_response.keys())

        # Get the samples
        self.samples = self.client.get(f'{SAMPLES_ENDPOINT}/{self.dataset}/{self.table}',
                                       headers=self.headers,
                                       name=f'{SAMPLES_ENDPOINT}/[dataset]/[table]')

    @task
    class RowOrColView(TaskSet):
        # A user has the following choices
        tasks = {RowView: 5, ColumnView: 3}

        @task
        def stop(self):
            # Exit RowOrColView
            self.interrupt()

    @task
    def stop(self):
        # Exit UnderstandTab
        self.interrupt()


class TreeMap(SequentialTaskSet):
    """Model the users behavior of the Treemap

    The treemap has a drill down UX. When a use selects a dataset all of the tables and table annotations are loaded. After a table is selected, the columns and column annotations are loaded. From here a user is able to view the column counts for columns within the selected table.
    """

    def on_start(self):
        self.datasets = self.parent.parent.datasets
        self.headers = self.parent.parent.headers
        self.client.cookies.set_policy(BlockAll())

    @task
    def select_table(self):
        self.dataset = random.choice(self.datasets)

        # Get the table
        response_tables = self.client.get(f'{TABLES_ENDPOINT}/{self.dataset}', headers=self.headers,
                                          name=f'{TABLES_ENDPOINT}[dataset]').json()

        # Get the tableannotations
        response_annotations = self.client.get(f'{ANNOTATIONS_DATASET_ENDPOINT}/{self.dataset}/counts',
                                               headers=self.headers,
                                               name=f'{ANNOTATIONS_DATASET_ENDPOINT}/[dataset]/counts').json()

        self.table = random.choice(list(response_tables.keys()))

        # Get the columns
        response = self.client.get(f'{COLUMNS_ENDPOINT}/{self.dataset}/{self.table}',
                                   headers=self.headers,
                                   name=f'{COLUMNS_ENDPOINT}/[dataset]/[table]').json()
        self.columns = list(response.keys())

        # Get the column annotations
        response = self.client.get(f'{ANNOTATION_TABLE_ENDPOINT}/{self.dataset}/{self.table}/counts',
                                   headers=self.headers,
                                   name=f'{ANNOTATION_TABLE_ENDPOINT}/[dataset]/[table]/counts').json()

    @task
    class TreeMapColumnCounts(TaskSet):
        """View column counts within the Treemap

        This allows users to view multiple column counts before exiting.
        """

        def on_start(self):
            self.dataset = self.parent.dataset
            self.table = self.parent.table
            self.columns = self.parent.columns
            self.headers = self.parent.headers
            self.client.cookies.set_policy(BlockAll())

        @task(2)
        def get_column_counts(self):
            self.column = random.choice(self.columns)

            data = {
                'dataset': self.dataset,
                'table': self.table,
                'column': self.column,
                'normalized': 'false',
                'start': 0,
                'end': 256,
                'sort': 'CNT_DESC'
            }

            response = self.client.post(COLUMN_COUNTS_ENDPOINT,
                                        json=data,
                                        headers=self.headers,
                                        allow_redirects=False,
                                        name=COLUMN_COUNTS_ENDPOINT).json()

        @task
        def stop(self):
            # Exit TreeMapColumnCounts
            self.interrupt()

    @task
    def stop(self):
        # Exit TreeMap
        self.interrupt()


class UserBehavior(SequentialTaskSet):
    """Actions that run after a user logs in

    Upon initial load the a call to the datasets and treemap endpoint will always be made. After this data is loaded the user has the option to use the Understand Tab, Treemap, or Search.
    """

    def on_start(self):
        config = configparser.ConfigParser()
        config.read('config.txt')

        self.username = config['DEFAULT']['user']
        self.api_key = config['DEFAULT']['api_key']
        self.datasets = None

        self.headers = {'Content-Type': 'application/json',
                        # 'X-Authorization-Token': self.api_key,
                        'X-Api-Key': self.api_key,
                        'X-Username': self.username,
                        'X-Attributes-To-Reject': '[]'}
        self.client.cookies.set_policy(BlockAll())
        self.client.verify = False

    @task
    def datasets_and_treemap(self):
        datasets_response = self.client.get(
            DATASET_ENDPOINT,
            headers=self.headers,
            allow_redirects=False,
            name=DATASET_ENDPOINT).json()
        self.datasets = list(datasets_response.keys())

        treemap_response = self.client.get(
            TREEMAP_ENDPOINT,
            headers=self.headers,
            allow_redirects=False,
            name=TREEMAP_ENDPOINT).json()

    @task
    class Tabs(TaskSet):
        # After initial log in a user has the following options
        tasks = {UnderstandTab: 2, TreeMap: 3}

        @task
        def stop(self):
            # Exit Tabs
            self.interrupt()

    @task
    def stop(self):
        # Exit UserBehavior
        self.interrupt()


class WebsiteUser(HttpUser):
    tasks = [UserBehavior]
    # Wait between 1 and 10 seconds for an action
    wait_time = between(1, 10)
