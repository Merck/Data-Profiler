package com.dataprofiler.sqlsync.provider.conversion;

/*-
 * 
 * dataprofiler-tools
 *
 * Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
 *
 * 	Licensed to the Apache Software Foundation (ASF) under one
 * 	or more contributor license agreements. See the NOTICE file
 * 	distributed with this work for additional information
 * 	regarding copyright ownership. The ASF licenses this file
 * 	to you under the Apache License, Version 2.0 (the
 * 	"License"); you may not use this file except in compliance
 * 	with the License. You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * 	Unless required by applicable law or agreed to in writing,
 * 	software distributed under the License is distributed on an
 * 	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * 	KIND, either express or implied. See the License for the
 * 	specific language governing permissions and limitations
 * 	under the License.
 * 
 */

import org.apache.spark.sql.types.DataType;

/**
 * convert spark sql data types to redshift schema types
 *
 * @see "https://spark.apache.org/docs/2.4.0/sql-reference.html#data-types"
 * @see "https://docs.aws.amazon.com/redshift/latest/dg/c_Supported_data_types.html"
 * @see "https://docs.aws.amazon.com/redshift/latest/dg/federated-data-types.html"
 */
public class SparkToRedshiftTypeConversionProvider extends SparkToPostgresTypeConversionProvider {

  // "varchar(max)" did not work
  private static final String DEFAULT_STRING_TYPE = "varchar(65535)";
  private static final String DEFAULT_TYPE = DEFAULT_STRING_TYPE;

  /**
   * return basically the same conversion as postgres types, except for string can use varchar(max)
   *
   * @param dataType
   * @return
   */
  @Override
  public String convertType(DataType dataType) {
    String sparkType = dataType.typeName();
    if ("string".equals(sparkType)) {
      return DEFAULT_STRING_TYPE;
    } else {
      return super.convertType(dataType);
    }
  }
}
