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
 * convert spark sql data types to postgres schema types
 *
 * @see https://spark.apache.org/docs/2.4.0/sql-reference.html#data-types
 * @see https://www.postgresql.org/docs/9.0/datatype.html
 */
public class SparkToPostgresTypeConversionProvider implements SparkTypeConversionProvider {

  private static final String DEFAULT_STRING_TYPE = "varchar(2048)";
  private static final String DEFAULT_TYPE = DEFAULT_STRING_TYPE;

  public SparkToPostgresTypeConversionProvider() {
    super();
  }

  public String convertType(DataType dataType) {
    String jdbcType = "";
    String sparkType = dataType.typeName();
    switch (sparkType) {
      case "boolean":
      case "integer":
        jdbcType = sparkType;
        break;
      case "binary":
      case "byte":
        jdbcType = "bytea";
        break;
      case "double":
        jdbcType = "decimal(32)";
        break;
      case "date":
        jdbcType = "date";
        break;
      case "float":
        jdbcType = "decimal(16)";
        break;
      case "long":
        jdbcType = "bigint";
        break;
      case "short":
        jdbcType = "smallint";
        break;
      case "string":
        // for RDS, 'text' did not work
        // driver error
        // Exception in thread "main" org.apache.spark.sql.catalyst.parser.ParseException:
        // DataType text is not supported.(line 1, pos 77)
        // cbsa_code integer, metro_division_code integer, csa_code integer, cbsa_title text,
        // -----------------------------------------------------------------------------^^^
        //         jdbcType = "text";
        jdbcType = DEFAULT_STRING_TYPE;
        break;
      case "timestamp":
        jdbcType = "timestamp";
        break;
      default:
        jdbcType = DEFAULT_TYPE;
        break;
    }
    return jdbcType;
  }
}
