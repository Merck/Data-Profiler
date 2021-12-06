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

import pyspark
from pyspark.sql import DataFrame


def create_spark_context(app_name) -> (pyspark.SparkContext, pyspark.sql.SparkSession):
    # Create a SparkConf with necessary serializers
    conf = [('spark.serializer', 'org.apache.spark.serializer.KryoSerializer'),
            ('spark.sql.orc.enabled', 'true'),
            ('spark.kryo.classesToRegister',
             'org.apache.accumulo.core.data.Value,org.apache.accumulo.core.data.Key')]

    spark_conf = pyspark.SparkConf()
    spark_conf.setAll(conf)
    sc = pyspark.SparkContext(appName=app_name, conf=spark_conf)

    spark = pyspark.sql.SparkSession(sc)

    return (sc, spark)


def rows_flat(t):
    return pyspark.sql.Row(**json.loads(t[1]))


def get_table(sc, dataset, table, authorizations=None) -> DataFrame:
    data_scan = {'type': 'row',
                 'dataset': dataset,
                 'table': table,
                 }
    conf = {'DataProfiler.dataScanSpec': json.dumps(data_scan)}

    if authorizations is not None:
        conf['DataProfiler.authorizationsAsJson'] = json.dumps(authorizations)

    key_converter = 'com.dataprofiler.util.mapreduce.converters.AccumuloKeyToStringConverter'
    value_converter = 'com.dataprofiler.util.mapreduce.converters.AccumuloValueToStringConverter'

    try:
        rows_rdd = sc.newAPIHadoopRDD('com.dataprofiler.util.mapreduce.RowInputFormat',
                                    'java.lang.String',
                                    'java.lang.String',
                                    keyConverter=key_converter,
                                    valueConverter=value_converter,
                                    conf=conf)

        df = rows_rdd.map(rows_flat).toDF()
        return df
    except: #py4j.protocol.Py4JJavaError 
        return sc.emptyRdd().map(rows_flat).toDF()


def store_table(sc,
                df,
                dataset,
                table,
                visibility,
                full_dataset_load=False,
                version_id=None,
                origin='make',
                commit_metadata=True,
                records_per_partition=100000):
    dp_spark_context = sc._jvm.com.dataprofiler.DPSparkContext(sc._jsc)

    return sc._jvm.com.dataprofiler.loader.PysparkDataLoader.loadFromPyspark(dp_spark_context,
                                                                                   df._jdf,
                                                                                   version_id,
                                                                                   dataset,
                                                                                   table,
                                                                                   visibility,
                                                                                   records_per_partition,
                                                                                   origin,
                                                                                   commit_metadata,
                                                                                   full_dataset_load,
                                                                                   False)


def commit_metadata(sc, version_id, full_dataset_load):
    dp_spark_context = sc._jvm.com.dataprofiler.DPSparkContext(sc._jsc)
    sc._jvm.com.dataprofiler.loader.PysparkDataLoader.commitMetadata(dp_spark_context, version_id, full_dataset_load)

def delete_dataset(sc, dataset):
    dp_spark_context = sc._jvm.com.dataprofiler.DPSparkContext(sc._jsc)
    sc._jvm.com.dataprofiler.loader.PysparkDataLoader.deleteDataset(dp_spark_context, dataset)

def delete_table(sc, dataset, table):
    dp_spark_context = sc._jvm.com.dataprofiler.DPSparkContext(sc._jsc)
    sc._jvm.com.dataprofiler.loader.PysparkDataLoader.deleteTable(dp_spark_context, dataset, table)
