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
import arrow
from enum import Enum
from dataprofiler import pyspark_api
from pyspark.sql import Row
from pyspark.sql.functions import monotonically_increasing_id
from dataprofiler.api import Api


class PreviousRunData:
    def __init__(self, old_data):
        self.old_data = old_data

    def get(self,key):
        try:
            return self.old_data[key]
        except KeyError:
            return 0
        except TypeError:
            return 0

class WindowType(Enum):
    BETWEEN = "between"
    LESS_THAN = "less_than"
    GREATER_THAN = "greater_than"
    UNBOUNDED = 'unbounded'

class Window:
    def __init__(self, **kwargs):
        self.type = kwargs.get('type', WindowType.UNBOUNDED)
        self.name = kwargs.get('name', None)
        self.start = kwargs.get('start', None)
        self.end = kwargs.get('end', None)
        self.less_than = kwargs.get('less_than', None)
        self.greater_than = kwargs.get('greater_than', None)
        self.arrow_date_format = kwargs.get('arrow_date_format', None)
        self.spark_date_format = kwargs.get('spark_date_format', None)
        self.arrow_human_readable_date_format = kwargs.get(
            'arrow_human_readable_date_format', 'M/D/YY')
        self.display_name = kwargs.get('display_name')

    def to_sql(self):
        if self.type == WindowType.BETWEEN:
            return "BETWEEN to_date('{}',{}) AND to_date('{}',{})".format(self.start.format(self.arrow_date_format), self.spark_date_format, self.end.format(self.arrow_date_format), self.spark_date_format)
        if self.type == WindowType.LESS_THAN:
            return "< to_date('{}',{})".format(self.less_than.format(self.arrow_date_format), self.spark_date_format)
        if self.type == WindowType.GREATER_THAN:
            return "> to_date('{}',{})".format(self.greater_than.format(self.arrow_date_format), self.spark_date_format)
        return "IS NOT NULL"

    def to_human_readable(self):
        if self.type == WindowType.BETWEEN:
            return "{} - {}".format(self.start.format(self.arrow_human_readable_date_format), self.end.format(self.arrow_human_readable_date_format))
        if self.type == WindowType.LESS_THAN:
            return "Before {}".format(self.less_than.format(self.arrow_human_readable_date_format))
        if self.type == WindowType.GREATER_THAN:
            return "After {}".format(self.greater_than.format(self.arrow_human_readable_date_format))
        return "All with Dates"

    def preview(self):
        print("{}: {}".format(self.name, self.to_human_readable()))


class ProjectPageQuery:
    def __init__(self, **kwargs):
        self.spark_context = kwargs.get('sc', None)
        self.spark = kwargs.get('spark', None)
        self.name = kwargs.get('name', None)
        self.dataset = kwargs.get('dataset', None)
        self.table = kwargs.get('table', None)
        self.where_clause = kwargs.get('where_clause', None)
        self.window_query_column = kwargs.get('window_query_column', None)
        self.windows = kwargs.get('windows', [])
        self.spark_date_format = kwargs.get('spark_date_format', None)
        self.java_date_format = kwargs.get('java_date_format', None)
        self.visibility_expression = kwargs.get('visibility_expression', None)
        self.select_clause = kwargs.get('select_clause', "*")
        self.project_page_title = kwargs.get('project_page_title', None)
        self.overview_primary_key = '__row_index_id'
        self.primary_key = '__row_index_id'
        self.overview_headers = []
        self.overview_table = []
        self.api = Api()

    def table_string(self):
        return "`{}/{}`".format(self.dataset, self.table)

    def output_dataset(self):
        return "{}:{}".format(self.project_page_title, self.name).strip()

    def output_visibility(self):
        return "{}&{}".format("special.project_pages", self.visibility_expression)

    def remove_non_dates(self):
        return "to_date({}, {}) IS NOT NULL".format(self.window_query_column, self.spark_date_format)

    def dp_props_dict(self):
        return {"projectPage": self.project_page_title, "projectPageDateSort": self.java_date_format, "projectPageSource": "{}-{}".format(self.dataset,self.table)}

    def store(self, _df, _name, _human_name, _display_name=None, _previous_value=None):
        props = self.dp_props_dict()
        pyspark_api.store_table(self.spark_context, _df.drop(self.primary_key), self.output_dataset(
        ), _name, self.output_visibility())

        self.api.set_all_properties(self.output_dataset(), _name, props, props, props)
        
        self.overview_headers.append(_name)
        self.overview_table.append([str(_df.count()), _human_name, (
            _display_name if _display_name is not None else _human_name), _previous_value])
        return True

    def retreive_old_data(self):
        existing_dataframe = None
        try:
            existing_dataframe = pyspark_api.get_table(self.spark_context, self.output_dataset(), 'overview', [
                                                       self.visibility_expression, 'special.project_pages']).filter("__row_index_id = 0")
            if existing_dataframe.count() == 0:
                raise ValueError()
        except ValueError as e:
            return None
        except AttributeError as e:
            return None
        return existing_dataframe.collect()[0]

    def run(self):

        old_data = PreviousRunData(self.retreive_old_data())

        # Delete the existing data for this dataset
        pyspark_api.delete_dataset(self.spark_context, self.output_dataset())

        base_query = "SELECT {} FROM {} WHERE {}".format(
            self.select_clause, self.table_string(), self.where_clause)

        # starting_dataframe is all records regardless of it's "date" validity.  This table gets called 'all_no_date_filter'
        starting_dataframe = self.spark.sql(base_query).coalesce(
            1).withColumn(self.primary_key, monotonically_increasing_id())
        starting_dataframe.show()
        self.store(starting_dataframe, 'all_no_date_filter',
                   'All Records', 'All Records', old_data.get('all_no_date_filter'))

        # with_valid_dates is removes all non-date rows. This table gets called 'all'
        with_valid_dates_dataframe = starting_dataframe.filter(
            self.remove_non_dates()).coalesce(1)
        with_valid_dates_dataframe.show()
        self.store(with_valid_dates_dataframe, 'all',
                   'All With Dates', 'All With Dates', old_data.get('all'))

        # rejected is (starting_dataframe - with_valid_dates) This table gets called 'rejected'
        non_rejected = [str(row[self.primary_key])
                        for row in with_valid_dates_dataframe.collect()]
        rejected = starting_dataframe.filter(
            ~starting_dataframe[self.primary_key].isin(non_rejected)).coalesce(1)
        rejected.show()
        self.store(rejected, 'rejected', 'All Without Dates',
                   'All Without Dates', old_data.get('rejected'))

        for window in self.windows:
            window_query = "to_date({},{}) {}".format(
                self.window_query_column, self.spark_date_format, window.to_sql())

            result = with_valid_dates_dataframe.filter(
                window_query).coalesce(1)
            result.show()
            self.store(result, window.name, window.to_human_readable(),
                       window.display_name, old_data.get(window.name))

        R = Row(*(self.overview_headers))
        S = [R(*r) for r in zip(*(self.overview_table))]
        overview_dataframe = self.spark_context.parallelize(S, 1).toDF().withColumn(
            self.overview_primary_key, monotonically_increasing_id())
        overview_dataframe.show()
        pyspark_api.store_table(self.spark_context, overview_dataframe, self.output_dataset(
        ), 'overview', self.output_visibility())

        props = self.dp_props_dict()
        self.api.set_all_properties(self.output_dataset(), 'overview', props, props, props)

