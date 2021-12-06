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
import json
import pandas as pd
import numpy as np
from scipy.stats import variation, kurtosis, skew
from termcolor import colored
from datetime import datetime
from pytz import timezone
import sys
import os
from urllib.parse import quote_plus

def element_fmt(input):
    if (str(input).find("/") > 0):
        return quote_plus(str(input))
    else:
        return str(input)

def validate(value, possible_values, value_name):
    if value == "" or value is None:
        dataset_error = "Dataset not specified"
        print(colored(
            "ERROR! " + value_name + " name [" + str(value) + "] not specified", "red"))
        return False

    elif value not in possible_values:
        dataset_error = "Dataset name not valid"
        print(colored(
            "ERROR! " + value_name + " name [" + str(value) + "] not valid", "red"))
        return False

    return True

def requestResponsePrint(response, total_run_time, verbose):
    if str(response) == "<Response [200]>":
        if verbose:
            print(colored(
                "\nDownload successful! Request completed in " + str(total_run_time), "green"))

    elif str(response) == "<Response [401]>":
        print(colored(  "\nERROR! Unauthorized. Your credentials are either invalid or expired.", 
                    "red"))

    elif str(response) == "<Response [404]>":
        print(colored("\nERROR! You don't have permission to access the resource you're \
                        trying to. If you believe this is in error, please contact the \
                        Data Profiler Team.",
                     "red"))

    elif str(response == "<Response [403]>"):
        print(colored("\nERROR! The request had an error due to programming errors or \
                        cluster component downtime. Please try again, and contact the \
                        Data Profiler Team if the problem persists.",
                    "red"))

def map_listtodict(listdict):
    ''' 
    Takes a list of dictionaries and converts to dictionary
    [{'value': 'val1', 'count': 23},{'value': 'val2', 'count': 2}, ..] 
    -> {'val1': 23, 'val2': 2}
        
        Parameters:
            listdict (list): list of dictinaries with keys as value and count only
        Returns:
            dictionary: dictionary with keys as value and value as count
    '''
    valcnt_dict = {}
    for valcnt in listdict:
        valcnt_dict[valcnt['value']] = valcnt['count']

    return valcnt_dict

class Column():
    def __init__(self, environment, dataset_name, table_name, column_name, filters={}):
        self.column_name = column_name
        self.table_name = table_name
        self.dataset_name = dataset_name
        self.env = environment
        self.filters = filters

        validated = self.validateData()

        self.metadata = self.getColumnMetadata()

        if self.filters != {}:
            validate_filter = self.validateFilters()
            if validate_filter==False:
                print (colored("ERROR: this is not valid input", "red"))

        if validated==False:
            print (colored("ERROR: this is not valid input", "red"))

    ##### data about the column itself #####
    def getColumnMetadata(self):
        url = self.env.url + '/v1/columns/{}/{}'.format(self.dataset_name, self.table_name)
        response = requests.get(url, headers=self.env.header)

        return response.json()[self.column_name]

    ##### setting filters for listing columns counts #####
    def setFilters(self, filters):
        self.filters = filters

        self.validateData()

    ##### retrieving data stored within the column metadata #####
    def getColumnDataType(self):
        return self.metadata['data_type']

    def getValueCount(self):
        return self.metadata['num_values']

    def getUniqueValueCount(self):
        return self.metadata['num_unique_values']

    def getVisibility(self):
        return self.metadata['visibility']

    def getUserAccessList(self):
        url = self.env.url + "/rules_of_use"
        post_data = json.dumps({
            "query":"{usersWithAttribute(value:\""+self.getVisibility()+"\"){username}}"
        })
        response = requests.post(url, headers=self.env.header, data=post_data)
        if response.status_code == 401 or response.status_code == 403:
            print (colored("Unauthorized: You are not authorized to perform this request,\
                            please contact the Data Profiler team", "red"))
            return None

        try:
            usernames = [x["username"] for x in json.loads(response.text)["data"]["usersWithAttribute"]]
            return usernames
        except:
            print (colored("There was a {} error processing your \
                            request".format(response.status_code), "red"))
            return None

    ##### lists a dictionary of column counts with the structures as follows #####
    ##### [{'value':'value1', 'count':'count1'},...]                         #####
    def listColumnCounts(self):
        ## filters do not work for this endpoints
        post_data = json.dumps({
            "dataset": self.dataset_name,
            "table": self.table_name,
            "column": self.column_name,
            "limit": 0, 
            "sort": "CNT_DESC"
        })
        url = self.env.url + '/v1/colCounts'
        response = requests.post(url, headers=self.env.header, data=post_data)

        if response.status_code == 401 or response.status_code == 403:
            print (colored("Unauthorized: You are not authorized to perform this request," + \
                            " please contact the Data Profiler team", "red"))
            return None
        try:
            text_data = response.text
            text_data = text_data[:-1]
            json_data = json.loads(text_data)
            return json_data

        except:
            print (colored(f"There was a {response.status_code}" +
                            " error processing your request", "red"))
            return None

    ## get a dictionary of the listed column values and their
    ## counts that are within the provided range of values
    ## returns empty list if errors or no values exist
    ## returns list of dicts: [{"value": "val_1", "count":"count_1"}, 
    ## {"value": "val_2", "count":"count_2"}]
    def getColumnValuesInRange(self, min_val, max_val):
        try:
            range_len = float(max_val) - float(min_val)
        except:
            print (colored("Range values must be numbers", "red"))
            return []
        
        if float(max_val) <= float(min_val):
            print (colored("Max range value must be greater than the min", "red"))
            return []
        
        all_value_count = self.listColumnCounts()

        values_in_range = []
        for value in all_value_count:
            try:
                if (float(value["value"]) >= float(min_val) and \
                    float(value["value"]) < float(max_val)):
                    values_in_range.append(value)
            except:
                continue 

        return values_in_range

    def __isint(self, ignore={np.nan, '', ' ', '-', None}, threshold='0.70'):
            
        conversion = {'integer','long'}
        dt = self.getColumnDataType()
        cnt = 0
        icnt = 0
        if dt == 'string':
            all_value_count = self.listColumnCounts()

            for valdict in all_value_count:
                
                if valdict['value'] not in ignore:
                    cnt += valdict['count']  
                    try :  
                        int(valdict['value'])
                        icnt += valdict['count']  
                    except:
                        pass
            try:               
                if icnt/cnt >= float(threshold):
                    return True
                else:
                    return False
            except:
                print (colored("Range values must be numbers", "red"))
                return None

        else:
            if dt in conversion:
                return True
            else:
                return False
                    

    def __isfloat(self, ignore={np.nan, '', ' ', '-', None}, threshold='0.70'):
        
        conversion = {'integer','float','long'}
        dt = self.getColumnDataType()
        cnt = 0
        fcnt = 0
        if dt == 'string':
            all_value_count = self.listColumnCounts()

            for valdict in all_value_count:
                
                if valdict['value'] not in ignore:
                    cnt += valdict['count']
                    try :  
                        float(valdict['value'])
                        fcnt += valdict['count']  
                    except:
                        pass               
            try:               
                if fcnt/cnt >= float(threshold):
                    return True
                else:
                    return False
            except:
                print (colored("Range values must be numbers", "red"))
                return None

        else:
            if dt in conversion:
                return True
            else:
                return False

    def __getdatatype(self):
        if self.isint():
            return int
        elif self.isfloat():
            return float
        elif self.getColumnDataType()  == 'string':
            return str
        else:
            return self.getColumnDataType() 

    ##### Lists of valid datasets, tables, and columns #####
    def validDatasets(self):
        return self.env.getDatasetList()

    def validTables(self):
        url = self.env.url + '/v1/tables/{}'.format(self.dataset_name)
        response = requests.get(url, headers=self.env.header)

        return list(response.json().keys())

    def validColumns(self):
        url = self.env.url + '/v1/columns/{}/{}'.format(self.dataset_name, self.table_name)
        response = requests.get(url, headers=self.env.header)

        return list(response.json().keys())

    ##### validates the dataset, table, and column specified on initialization #####
    def validateData(self):
        valid_datasets = self.validDatasets()
        dataset_valid = validate(self.dataset_name, valid_datasets, "Dataset")

        if dataset_valid:
            valid_tables = self.validTables()
            table_valid = validate(self.table_name, valid_tables, "Table")

            if table_valid:
                valid_columns = self.validColumns()
                column_valid = validate(self.column_name, self.validColumns(), "Column")

        return dataset_valid & table_valid & column_valid

    ##### validates the filters the user can choose to set #####
    def validateFilters(self):

        if self.filters != {}:
            filter_keys = [x for x in self.filters]

            for key in filter_keys:
                valid_filter = validate(key, self.validColumns(), "Filter Column")
                if valid_filter==False:
                    return False

        return True

    # Check for number of missing/blank values in the column 
    def __getNAscount(self,blank_types = {'',' ','-',None, np.nan}):
        '''
        Find missing values present in selected column

            Parameters:
                    blank_types (set): what constitutes missing values 
            Returns:
                    int: Returns the number of missing values present  
        '''
        ValCount = self.listColumnCounts()
        cnt_all = 0
        for vc in ValCount:
            if vc['value'] in blank_types:
                cnt_all += vc['count']
        
        return cnt_all
        

class Table():
    def __init__(self, environment, dataset_name, table_name, filters={}):
        self.table_name = table_name
        self.dataset_name = dataset_name
        self.env = environment
        self.filters = filters

        validated = self.validateData()
        if validated==False:
            print (colored("ERROR: The input data is not valid", "red"))

        self.table_info = self.getTableInfo()
        self.metadata = self.getTableMetadata()

        if self.filters != {}:
            validated_filters = validateFilters()
            if validated_filters==False:
                print (colored("ERROR: The input data is not valid", "red"))

    #### get specific information about the inside of the table #####
    def getTableInfo(self):
        url = self.env.url + '/v1/columns/{}/{}'.format(self.dataset_name, self.table_name)
        response = requests.get(url, headers=self.env.header)
        
        return response.json()

    ##### get metadata about the table #####
    def getTableMetadata(self):
        url = self.env.url + '/v1/tables/{}'.format(self.dataset_name)
        response = requests.get(url, headers=self.env.header)

        return response.json()[self.table_name]

    ##### set filters for loading table rows #####
    def setFilters(self, filters):
        self.filters = filters

        self.validateFilters()

    ##### get functions to access the information in the table #####
    def getColumnList(self):
        return list(self.table_info.keys())

    def getColumnCount(self):
        return len(self.getColumnList())

    ##### get functions to access the table metadata #####
    def getUploadDate(self):
        epoch_time = float(self.metadata['load_time'])/1000
        return datetime.fromtimestamp(epoch_time)
    
    def getUpdateDate(self):
        epoch_time = float(self.metadata['update_time'])/1000
        return datetime.fromtimestamp(epoch_time)

    def getVisibility(self):
        return self.metadata["visibility"]

    def getTableCount(self):
        return self.metadata["num_tables"]

    def getColumnCount(self):
        return self.metadata["num_columns"]

    def getValueCount(self):
        return self.metadata["num_values"]

    def getPullTimestamp(self):
        epoch_time = float(self.metadata["timestamp"])/1000
        return datetime.fromtimestamp(epoch_time)

    def getUserAccessList(self):
        url = self.env.url + "/rules_of_use"
        post_data = json.dumps({
            "query":"{usersWithAttribute(value:\""+self.getVisibility()+"\"){username}}"
        })
        response = requests.post(url, headers=self.env.header, data=post_data)
        if response.status_code == 401 or response.status_code == 403:
            print (colored("Unauthorized: You are not authorized to perform this request, \
                            please contact the Data Profiler team", "red"))
            return None

        try:
            usernames = [x["username"] for x in json.loads(response.text)["data"]["usersWithAttribute"]]
            return usernames
        except:
            print (colored("There was a {} error processing your \
                request".format(response.status_code), "red"))
            return None

    ##### format data for post requests #####
    ## If no sample size is given, then the limit is set to 0 which returns all rows
    def getPostData(self, sample_size=0):

        post_data = json.dumps({
                "dataset": self.dataset_name,
                "table": self.table_name,
                "sort": "CNT_DESC",
                "filters": self.filters,
                "limit": sample_size
            })

        return post_data


    ##### format data for post requests #####
    ## If no sample size is given, then the limit is set to 0 which returns all rows
    def getRowsPostData(self, start_location=None, page_size = 5000):
        if page_size >= 10000:
            raise ValueError("Rows Page Size must be less than 10,000")

        post_data = {
            "dataset": self.dataset_name,
            "table": self.table_name,
            "filters": self.filters,
            "limit": page_size,
            "start_location": start_location,
            "pageSize": page_size
        }

        # Remove None values (likely start_location)
        return json.dumps({k: v for k, v in post_data.items() if v})


    ##### load the rows from the table #####
    def loadRows(self):
        if self.filters == {}:
            print("\nDownloading ALL ROWS: {}, {}...".format(self.dataset_name, self.table_name))
        else:
            print("\nDownloading FILTERED TABLE: {} | {} \nFilter(s) \
                 applied: {}...".format(self.dataset_name, self.table_name, self.filters))

        try:
            url = self.env.url + '/v2/rows'

            # Time post request
            start_time = datetime.now(timezone('US/Eastern'))
            
            # Tracking variables
            start_location = None
            results = []

            # Run the rows endpoint through until we break
            while True:
                post_data = self.getRowsPostData(start_location)
                response = requests.post(
                    url, headers=self.env.header, data=post_data).json()
                
                results.extend(response['rows'])

                # If endLocation is null/None, we have successfully gotten all the rows
                # This is also where you'd want to check against the limit (if len(results) > limit)
                if response['endLocation'] is None:
                    break 
                
                # Update the start location and loop again
                start_location = response['endLocation']
                

            total_run_time = str(datetime.now(
                timezone('US/Eastern')) - start_time)
            requestResponsePrint(response, total_run_time, self.env.verbose)

            if len(results) == 0:
                if self.env.verbose:
                    print(colored("Data request empty!", "red"))
                ## returns an empty dataframe with the column structure of the table itself
                df = pd.DataFrame(columns=self.getColumnList())
            else:
                df = pd.DataFrame(results)

            return df

        except ValueError:  # includes simplejson.decoder.JSONDecodeError
            print(
                colored("\nError - check response message or text to json parser.", "red"))
            return None

    ## loads a subset of the table rows 
    ## it will default to 100 rows, but a user can specfiy a number of rows
    def loadTableSample(self, sample_size=100):
        print((colored("Note: This endpoint is experimental - expect longer load times \
                        for larger datasets", "cyan")))

        if self.filters == {}:
            print("\nDownloading {} ROWS: {}, {}...".format(sample_size, self.dataset_name,
                                                            self.table_name))
        else:
            print("\nDownloading {} FILTERED ROWS: {} | {} \nFilter(s) \
                 applied: {} ...".format(sample_size, self.dataset_name, self.table_name,
                                        self.filters))

        try:
            url = self.env.url + '/v1/rows'

            post_data = self.getPostData(sample_size)

            # Time post request
            start_time = datetime.now(timezone('US/Eastern'))
            response = requests.post(
                url, headers=self.env.header, data=post_data)
            total_run_time = str(datetime.now(
                timezone('US/Eastern')) - start_time)
            requestResponsePrint(response, total_run_time, self.env.verbose)

            # Convert to text to remove extra "$" character
            response.encoding = 'utf-8'
            text_data = response.text
            text_data = text_data[:-1]

            if len(text_data) < 2:
                if self.env.verbose:
                    print(colored("Data request empty!", "red"))
                ## returns an empty dataframe with the column structure of the table itself
                df = pd.DataFrame(columns=self.getColumnList())
            else:
                json_data = json.loads(text_data)
                df = pd.DataFrame(json_data)

            return df

        except ValueError:  # includes simplejson.decoder.JSONDecodeError
            print(
                colored("\nError - check response message or text to json parser.", "red"))
            return None


    ## get match locations for a given substring in a given column
    ## returns: list of match locations
    def getSubstringValueMatches(self, substring, column):
        ## data for substring post request
        post_data = json.dumps({
            "begins_with": False,
            "substring_match": True,
            "term": [substring],
            "dataset": self.dataset_name,
            "table": self.table_name,
            "column": column
        })
        ## call endpoint to retrieve location responses
        value_search_endpoint = self.env.url + '/search'
        response = requests.post(value_search_endpoint, headers=self.env.header, data=post_data)
        response_str = response.text

        try:
            response_list = json.loads(response_str)
            return response_list
        except:
            print (colored("There were no value results for your query", "red"))
            return []

    ## loads all the rows in a given table that match the provided substring filters
    ## these filters are structured like {'column':['subtring1',...],...} just
    ## like the ordinary filters
    def loadRowsWithSubstringMatches(self, substring_filters):
        filters = {}
        for col in substring_filters:
            vals = []
            for filt in substring_filters[col]:
                response_list = self.getSubstringValueMatches(filt, col)
                for r in response_list:
                    vals.append(r["value"])
                    
            filters[col] = vals

        self.setFilters(filters)
        df = self.loadRows()

        return df

    ## This loads all the rows within a range of min and max
    ## values for the designated column
    def loadRowsInRange(self, min_val, max_val, column):
        ## get all unique values
        def listColumnCounts(column):
            post_data = json.dumps({
                "dataset": self.dataset_name,
                "table": self.table_name,
                "column": column,
                "limit": 0, 
                "sort": "CNT_DESC"
            })
            url = self.env.url + '/v1/colCounts'
            response = requests.post(url, headers=self.env.header, data=post_data)

            if response.status_code == 401 or response.status_code == 403:
                print (colored("Unauthorized: You are not authorized to perform this request," + \
                                " please contact the Data Profiler team", "red"))
                return None
            try:
                text_data = response.text
                text_data = text_data[:-1]
                json_data = json.loads(text_data)
                return json_data

            except:
                print (colored(f"There was a {response.status_code}" +
                                " error processing your request", "red"))
                return None

        if self.validateRangeInput(min_val, max_val, column):
            value_count_df = pd.DataFrame(listColumnCounts(column))
            value_list = list(value_count_df["value"])

            ## compare to min and max to get all unique values in range
            min_float = float(min_val)
            max_float = float(max_val)

            values_in_range = []
            for val in value_list:
                try:
                    if float(val) >= min_float and float(val) < max_float:
                        values_in_range.append(val)
                except:
                    continue

            ## set as the filter to load rows
            filters = {column: values_in_range}
            self.setFilters(filters)

            ## return results
            match_df = self.loadRows()
            return match_df
        else:
            return None

    ## This allows for the user to load all of the rows of a dataset
    ## except for those that match the exception filter
    ## exception filter structure: {"column": ["value_to_be_excluded"]}
    def loadRowsWithException(self, exception_filter):
        orig_filters = self.filters

        ## get all unique values for that column
        def listColumnCounts(column):
            post_data = json.dumps({
                "dataset": self.dataset_name,
                "table": self.table_name,
                "column": column,
                "limit": 0, 
                "sort": "CNT_DESC"
            })
            url = self.env.url + '/v1/colCounts'
            response = requests.post(url, headers=self.env.header, data=post_data)

            if response.status_code == 401 or response.status_code == 403:
                print (colored("Unauthorized: You are not authorized to perform this request," + \
                                " please contact the Data Profiler team", "red"))
                return None
            try:
                text_data = response.text
                text_data = text_data[:-1]
                json_data = json.loads(text_data)
                return json_data

            except:
                print (colored(f"There was a {response.status_code}" +
                                " error processing your request", "red"))
                return None

        ## creates the filter for all values that should be included
        filters = {}
        for col in exception_filter:
            vals = exception_filter[col]
            value_count_df = pd.DataFrame(listColumnCounts(col))
            value_list = list(value_count_df.loc[~value_count_df["value"].isin(vals)]["value"])
            filters[col] = value_list

        ## sets filters and retrieves the data
        if self.env.verbose:
            print (colored("New filters are being set just for this operation", "cyan"))
        self.setFilters(filters)
        df = self.loadRows()
        self.setFilters(orig_filters)

        return df

    # Check for number of missing/blank values in the table    
    def __getNAscount(self, blank_types={'',' ','-',None,np.nan}):
        '''
        Find missing values present in columns of selected table

            Parameters:
                    blank_types (set): what constitutes missing values for selected table
            Returns:
                    dictionary: Returns dictionary of missing values present across all columns 
        '''

        df = self.loadRows()
        blank_vals = dict( df.apply(lambda x: x[x.isin(blank_types)].shape[0],0) ) 
        return blank_vals 

    # Get quantile values of numeric columns     
    def __getQuantiles(self, qvals={'0','.25','.5','.75','1'}, columns=set()):
        '''
        Get quantile values of numeric columns from selected or all columns

            Parameters:
                    qvals (set): list of values between 0 <= q <= 1, the quantile(s) to compute
                    columns (set): list of columns to compute the quantiles for
            Returns:
                    dictionary: dictionary of all the quantile values. Raises KeyError 
                    if selected column doesn't exist in table. Returns None in case of Error or 
                    datatype of selected column is not numeric

                    ex: quantiles['column1'] = { 0 : val1 , 0.25 : val2 , ..}
        '''

        df = self.loadRows()
        Tableinfo = self.getTableInfo()

        if len(columns) != 0:
            for column in columns:
                try:
                    if Tableinfo[column]['data_type'] not in {'integer','long','float','double'}:
                        
                        print(colored("\nError - Select columns with data type as" \
                        "integer, float, double or long.", "red")) 

                        return None  
                except KeyError:
                    print(colored("\nError - Column {} doesn't exist \
                        in table.".format(column),"red"))
                    return None
        else:
            quantiles = {}
            for column in Tableinfo:
                #check for acceptable data types
                if Tableinfo[column]['data_type'] in {'integer','long','float','double'}: 
                    columns.add(column)

        if len(columns) == 0:
            print('The table doesn\'t contain columns with float or integer data type')
            return None
        else:
            columns = list(columns)
            try:
                qvals = list(float(q) for q in qvals)
            except:
                print(colored("quantile values must be numbers", "red"))

            df[columns] = df[columns].apply( lambda x: pd.to_numeric(x,errors = 'coerce'),\
            axis = 0)
            quantiles = {}
            for column in columns:
                quantiles[column] = dict(zip(qvals, 
                                            list(np.quantile(df[column],qvals)))) 
            return quantiles


    # Descriptive Stats for numeric columns 
    
    def __getDescriptiveStats(self, columns=set(), decimals=2):
        '''
        Get descriptive statistics like mean, mode, standard deviation, 
        sum, median absolute deviation, coefficient of variation, kurtosis, 
        skewness of numeric columns from selected or all columns

            Parameters:
                    columns (set): list of columns to compute the quantiles for
                    decimals (int): Number of decimal places to round each column to
            Returns:
                    dictionary: dictionary of all the descriptive statistics
                    of all the relevant columns. Raises KeyError 
                    if selected column doesn't exist in table. Returns None in case of Error or 
                    datatype of selected column is not numeric
                    
                    ex: Dstats['column1'] = { 'stat1': val1 , 'stat2': val2 }
        '''

        df = self.loadRows()
        Tableinfo = self.getTableInfo()
        if columns != set():
            for column in columns:
                try:
                    if Tableinfo[column]['data_type'] not in {'integer', 
                    'long', 'float', 'double'}:
                        print(colored("\nError - Select columns with data type as" /
                        " integer, float, double or long.", "red"))
                        return None  
                except KeyError:
                    print(colored("\nError - Column {} doesn't exist \
                                in table.".format(column),"red"))
                    return None
        else:
            for column in Tableinfo:
                if Tableinfo[column]['data_type'] in {'integer','long','float','double'}:
                    columns.add(column)
        if len(columns) == 0:
            print('The table doesn\'t contain columns with numeric data type')
            return None
        else:
            columns = list(columns)
            df[columns] = df[columns].apply( lambda x: pd.to_numeric(x,errors = 'coerce'), 
            axis = 0)
            
            Dstats = {}
            for col in columns:
                Dstats[col] = {}
            
                Dstats[col]['mean'] = round(df[col].mean(skipna = True), decimals)
                Dstats[col]['median'] = round(df[col].median(skipna = True), decimals)
                Dstats[col]['mode'] = round(df[col].mode(dropna = True), decimals)
                Dstats[col]['summation'] = round(df[col].sum(skipna = True), decimals)
                Dstats[col]['standard_deviation'] = round(df[col].std(skipna = True),decimals)
                Dstats[col]['mean_absolute_deviation'] = round(df[col].mad(skipna = True),
                                                               decimals)
                Dstats[col]['variation'] = round(variation(df[col]), decimals)
                Dstats[col]['kurtosis'] = round(kurtosis(df[col]), decimals)
                Dstats[col]['skewness'] = round(skew(df[col]), decimals)

            return Dstats

    ##### get list of valid datasets and tables for verification of input #####
    def validDatasets(self):
        valid_datasets = self.env.getDatasetList()

        return valid_datasets

    def validTables(self):
        url = self.env.url + '/v1/tables/{}'.format(self.dataset_name)
        response = requests.get(url, headers=self.env.header)
        valid_tables = list(response.json().keys())

        return valid_tables

    ##### validate the dataset and table inputs #####
    def validateData(self):
        valid_datasets = self.validDatasets()
        dataset_valid = validate(self.dataset_name, valid_datasets, "Dataset")

        if dataset_valid:
            valid_tables = self.validTables()
            table_valid = validate(self.table_name, valid_tables, "Table")

        return dataset_valid & table_valid

    ##### validate the columns chosen for filtering #####
    def validateFilters(self):
        valid_columns = self.getColumnList()

        if self.filters != {}:
            filter_keys = [x for x in self.filters]

            for key in filter_keys:
                valid_filter = validate(key, valid_columns, "Filter Column")
                if valid_filter == False:
                    return False

        return True
    
    ## validate that the inputs into the range search query are valid
    ## returns: validity boolean
    def validateRangeInput(self, min_val, max_val, column):
        try:
            float(min_val)
            float(max_val)
        except:
            print (colored("Min and max values must be integers or floats","red"))
            return False

        if column in self.getColumnList():
            return True
        else:
            print (colored("Column must be valid column in table", "red"))
            return False

class Dataset():

    def __init__(self, environment, dataset_name):
        self.dataset_name = dataset_name
        self.env = environment

        self.validateData()

        self.dataset_info = self.getDatasetInfo()
        self.metadata = self.getDatasetMetadata()

    def getDatasetInfo(self):
        url = self.env.url + '/v1/tables/{}'.format(self.dataset_name)
        response = requests.get(url, headers=self.env.header)

        return response.json()

    def getDatasetMetadata(self):
        url = self.env.url + '/v1/datasets'
        response = requests.get(url, headers=self.env.header)
        
        return response.json()[self.dataset_name]

    def getTableList(self):
        return list(self.dataset_info.keys())

    def getUploadDate(self):
        epoch_time = float(self.metadata['load_time'])/1000
        return datetime.fromtimestamp(epoch_time)
    
    def getUpdateDate(self):
        epoch_time = float(self.metadata['update_time'])/1000
        return datetime.fromtimestamp(epoch_time)

    def getVisibility(self):
        return self.metadata["visibility"]

    def getTableCount(self):
        return self.metadata["num_tables"]

    def getColumnCount(self):
        return self.metadata["num_columns"]

    def getValueCount(self):
        return self.metadata["num_values"]

    def getPullTimestamp(self):
        epoch_time = float(self.metadata["timestamp"])/1000
        return datetime.fromtimestamp(epoch_time)

    def getUserAccessList(self):
        url = self.env.url + "/rules_of_use"
        post_data = json.dumps({
            "query":"{usersWithAttribute(value:\""+self.getVisibility()+"\"){username}}"
        })
        response = requests.post(url, headers=self.env.header, data=post_data)
        if response.status_code == 401 or response.status_code == 403:
            print (colored("Unauthorized: You are not authorized to perform this request, \
                            please contact the Data Profiler team", "red"))
            return None

        try:
            usernames = [x["username"] for x in json.loads(response.text)["data"]["usersWithAttribute"]]
            return usernames
        except:
            print (colored("There was a {} error processing your \
                    request".format(response.status_code), "red"))
            return None

    def getUploadCount(self):
        url = self.env.url + '/jobs'
        response = requests.get(url, headers=self.env.header)
        if response.status_code == 200:
            try:
                count = 0
                jobs = json.loads(response.text)
                for upload in jobs:
                    if (jobs[upload]["datasetName"] == self.dataset_name and \
                        self.getVisibility() in jobs[upload]["visibilities"]):
                        count += 1
                    else:
                        continue
                
                return count
            except:
                if self.env.verbose:
                    print (colored("Your request was empty", "red"))
        else:
            print (colored("There was a {} error processing your \
                            request".format(response.status_code), "red"))
        
        return None

    def getAllUploadDates(self):
        url = self.env.url + '/jobs'
        response = requests.get(url, headers=self.env.header)
        if response.status_code == 200:
            try:
                jobs = json.loads(response.text)
                dates = []
                for upload in jobs:
                    if (jobs[upload]["datasetName"] == self.dataset_name and \
                        self.getVisibility() in jobs[upload]["visibilities"]):
                        epoch_time = float(jobs[upload]["timestamp"])/1000
                        dates.append(datetime.fromtimestamp(epoch_time))
                    else:
                        continue
                
                return dates
            except:
                if self.env.verbose:
                    print (colored("Your request was empty", "red"))
        else:
            print (colored("There was a {} error processing your\
                            request".format(response.status_code), "red"))

        return None

    def validateData(self):
        valid_datasets = self.env.getDatasetList()
        dataset_valid = validate(self.dataset_name, valid_datasets, "Dataset")
        return dataset_valid
    
    def loadTable(self, table_name):
        if self.env.verbose:
            print("\nDownloading ALL ROWS: ", self.dataset_name, ",", table_name, "...")

        try:
            url = self.env.url + '/v1/rows'

            post_data = json.dumps({
                    "dataset": self.dataset_name,
                    "table": table_name,
                    "limit": 0, 
                    "sort": "CNT_DESC"
                })

            # Time post request
            start_time = datetime.now(timezone('US/Eastern'))
            response = requests.post(
                url, headers=self.env.header, data=post_data)
            total_run_time = str(datetime.now(
                timezone('US/Eastern')) - start_time)
            requestResponsePrint(response, total_run_time, self.env.verbose)

            # Convert to text to remove extra "$" character
            response.encoding = 'utf-8'
            text_data = response.text
            text_data = text_data[:-1]

            if len(text_data) < 2:
                if self.env.verbose:
                    print(colored("Data request empty!", "red"))
                ## returns an empty dataframe with the column structure of the table itself
                df = pd.DataFrame(columns=self.getColumnList())
            else:
                json_data = json.loads(text_data)
                df = pd.DataFrame(json_data)

            return df

        except ValueError:  # includes simplejson.decoder.JSONDecodeError
            print(
                colored("\nError - check response message or text to json parser.", "red"))
            return None

    def importDataset(self):
        if self.env.verbose:
            print((colored("Note: This endpoint is experimental - expect longer load times for larger datasets", "cyan")))
        tables = self.getTableList()
        all_tables = {}
        for table in tables:
            all_tables[table] = self.loadTable(table)
        
        return all_tables

class Environment():
    def __init__(self, api_key, url, verbose=True):
        self.api_key = api_key
        self.url = url
        self.header = {
            "Content-Type": "application/json",
            "X-Api-Key": self.api_key,
            "Accept": "application/json"
        }

        self.verbose = verbose

        self.env_info = self.getEnvironmentInfo()

    ##### gets all specific information about the contents of the environment#####
    def getEnvironmentInfo(self):
        url = self.url + '/v1/datasets'
        try:
            response = requests.get(url, headers=self.header)
            return response.json()
        except:
            print (colored("ERROR: Could not connect to DP. Check that you are connected to the VPN before retrying", "red"))
            return None

    ##### gets a list of all the datasets in the env available to the user #####
    def getDatasetList(self):
        return list(self.env_info.keys())

    ##### the number of datasets in the environment #####
    def getDatasetCount(self):
        return len(self.getDatasetList())

    def getUserAccessList(self, visibility):
        url = self.url + "/rules_of_use"
        post_data = json.dumps({
            "query":"{usersWithAttribute(value:\""+visibility+"\"){username}}"
        })
        response = requests.post(url, headers=self.header, data=post_data)
        if response.status_code == 401 or response.status_code == 403:
            print (colored("Unauthorized: You are not authorized to perform this request,\
                            please contact the Data Profiler team", "red"))
            return None

        try:
            usernames = [x["username"] for x in json.loads(response.text)["data"]["usersWithAttribute"]]
            return usernames
        except:
            print (colored("There was a {} error processing your \
                            request".format(response.status_code), "red"))
            return None

    ##### get a sample join between two tables on the two given columns #####
    def getSampleJoin(self, dataset_a, table_a, column_a, dataset_b, table_b,
                     column_b, sample_size):
        url = self.url + "/experimental/joinStats"
        post_data = json.dumps({
            "dataset_a":dataset_a,
            "table_a":table_a,
            "col_a":column_a,
            "dataset_b":dataset_b,
            "table_b":table_b,
            "col_b":column_b,
            "limit":sample_size
        })
        response = requests.post(url, headers=self.header, data=post_data)
        if response.status_code == 200:
            try:
                json_data = json.loads(response.text)
                return pd.DataFrame(json_data["sample"])
            except:
                print (colored("There was no result for your attempted join", "red"))
                return None
        else:
            print (colored("There was {} error in processing your\
                            request".format(response.status_code), "red"))
            return None

    ##### get a sample join between two tables on the two given columns #####
    def getSampleJoinMetaData(self, dataset_a, table_a, column_a, dataset_b, table_b, 
                            column_b, sample_size):
        url = self.url + "/experimental/joinStats"
        post_data = json.dumps({
            "dataset_a":dataset_a,
            "table_a":table_a,
            "col_a":column_a,
            "dataset_b":dataset_b,
            "table_b":table_b,
            "col_b":column_b,
            "limit":sample_size
        })
        response = requests.post(url, headers=self.header, data=post_data)
        if response.status_code == 200:
            try:
                json_data = json.loads(response.text)
                return json_data["meta"]
            except:
                print (colored("There was no result for your attempted join", "red"))
                return None
        else:
            print (colored("There was {} error in processing your \
                            request".format(response.status_code), "red"))
            return None


    def getAllJobs(self):
        url = self.url + '/jobs'
        response = requests.get(url, headers=self.header)
        if response.status_code == 200:
            return json.loads(response.text)
        else:
            print (colored("There was an error processing your request", "red"))
            return None

    def getSampleData(self, dataset, table, filters, sample_size):
       
        url = self.url + "/data/datawaverows"
        post_data= json.dumps({
            "dataset": dataset,
            "table": table,
            "filters": filters,
            "api_filter": False,
            "limit": sample_size
            })

        response = requests.post(url, headers=self.header, data=post_data)
        if response.status_code == 200:
            try:
                json_data = json.loads(response.text)
                return json_data['rows']
            except:
                print(colored("There was no result for your attempted join", "red"))
                return None
        else:
            print(colored("There was {} error in processing your " \
                           + "request".format(response.status_code), "red"))
            return None



class Omnisearch():
    def __init__(self, environment, search_str):
        self.env = environment
        self.search_str = search_str
        self.search_str_lower = search_str.lower()

        self.postdata = json.dumps({
            "term": [self.search_str],
            "begins_with": "True",
            "limit": 1000000
        })

        ## these will be parsed as they are called by the user ##
        self.value_matches = None
        self.column_matches = None
        self.table_matches = None
        self.dataset_matches = None
        self.all_matches = None

    ## get value matches
    def getValueMatches(self):
        if self.value_matches is None:
            value_search_endpoint = self.env.url + '/v1/search'
            response = requests.post(value_search_endpoint, headers=self.env.header, 
                                    data=self.postdata)
            response_str = str(response.text)[:-1]

            try:
                response_list = json.loads(response_str)
                setattr(self, 'value_matches', response_list)
            except:
                print (colored("There were no value results for your query", "red"))
                setattr(self, 'value_matches', [])
                return []

        return self.value_matches

    ## get number of values in table that match
    def getNumValueMatches(self):
        if self.value_matches:
            return len(self.value_matches)
        else:
            return len(self.getValueMatches())

    ## get dataset matches
    def getDatasetMatches(self):
        if self.dataset_matches is None:
            matches = []
            datasets = self.env.getDatasetList()
            for dataset in datasets:
                lower_dataset = dataset.lower()
                if lower_dataset.startswith(self.search_str_lower):
                    matches.append({'dataset':dataset})

            setattr(self, 'dataset_matches', matches)
            if matches == []:
                print (colored("There were no table results for your query", "red"))

        return self.dataset_matches

    ## get number of datasets that match
    def getNumDatasetMatches(self):
        if self.dataset_matches:
            return len(self.dataset_matches)
        else:
            return len(self.getDatasetMatches())
    
    ## get table matches
    def getTableMatches(self):
        if self.table_matches is None:
            table_search_endpoint =self.env.url + '/tables/search'
            response = requests.post(table_search_endpoint, headers=self.env.header, 
                                    data=self.postdata)
            response_str = str(response.text)
            try:
                response_list = json.loads(response_str)
                setattr(self, 'table_matches', response_list)
            except:
                print (colored("There were no table results for your query", "red"))
                setattr(self, 'table_matches', [])

        return self.table_matches

    ## get number of tables that match
    def getNumTableMatches(self):
        if self.table_matches:
            return len(self.table_matches)
        else:
            return len(self.getTableMatches())
            
    ## get column matches
    def getColumnMatches(self):
        if self.column_matches is None:
            column_search_endpoint =self.env.url + '/columns/search'
            response = requests.post(column_search_endpoint, headers=self.env.header, 
                                    data=self.postdata)
            response_str = str(response.text)
            try:
                response_list = json.loads(response_str)
                setattr(self, 'column_matches', response_list)
                if response_list == []:
                    print (colored("There were no column results for your query", "red"))
            except:
                print (colored("There were no column results for your query", "red"))
                setattr(self, 'column_matches', [])

        return self.column_matches

    ## get number of columns that match
    def getNumColumnMatches(self):
        if self.column_matches:
            return len(self.column_matches)
        else:
            return len(self.getColumnMatches())

    ## get all matches
    def getAllMatches(self):
        if self.all_matches is None:
            values = self.getValueMatches()
            columns = self.getColumnMatches()
            tables = self.getTableMatches()
            datasets = self.getDatasetMatches()

            all_matches = {
                "values": values,
                "columns": columns,
                "tables": tables,
                "datasets": datasets
            }
            setattr(self, "all_matches", all_matches)
        
        return self.all_matches

    ## get the number of all matches
    def getNumAllMatches(self):
        if self.all_matches is None:
            self.getAllMatches()

        num_matches = len(self.value_matches) \
                    + len(self.column_matches) \
                    + len(self.table_matches) \
                    + len(self.dataset_matches)
        
        return num_matches