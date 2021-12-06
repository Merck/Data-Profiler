package com.dataprofiler.datasource.iterators

/*-
 * 
 * spark-sql
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

import org.apache.accumulo.core.data.{Key, Value}
import org.apache.accumulo.core.iterators.{Filter, IteratorEnvironment, SortedKeyValueIterator}
import org.apache.commons.lang.StringUtils
import org.apache.log4j.Logger

object DPColumnFilterIterator {
  val LOGGER: Logger = Logger.getLogger(this.getClass)
  val FILTER_COLUMNS_OPT = "columns"
}

class DPColumnFilterIterator extends Filter {
  import DPColumnFilterIterator._

  var requiredColumns: String = _

  override def init(source: SortedKeyValueIterator[Key, Value],
                    options: java.util.Map[String, String],
                    env: IteratorEnvironment)
  : Unit = {
    super.init(source, options, env)
    this.requiredColumns = new String(options.get(FILTER_COLUMNS_OPT))
    LOGGER.info(s"requiredColumns: $requiredColumns")
    LOGGER.info(s"env from iterator: ${env.getAuthorizations} ${env.getConfig} ${env.getIteratorScope}")
  }

  override def validateOptions(options: java.util.Map[String, String]): Boolean = {
    super.validateOptions(options)
    if (StringUtils.isBlank(options.get(FILTER_COLUMNS_OPT))) {
      LOGGER.debug(s"requiredColumns validateOptions: ${options.entrySet().toString}")
      return false
    }
    return true
  }

  override def accept(key: Key, value: Value): Boolean = {
    if (key == null || key.getSize == 0) {
      return false
    }
    LOGGER.info(s"requiredColumns: $requiredColumns")
    for (columnName: String <- requiredColumns.split(",")) {
      LOGGER.debug(s"testing for column family: $columnName")

      if (key != null && key.getColumnQualifier() != null) {
        if (key.getColumnQualifier().toString.endsWith(columnName)) {
          LOGGER.info(s"accepted key: $key, accepted column: $columnName, value: ${value.toString}")
          return true
        }
      } else {
        LOGGER.warn("key was null")
      }
    }
    return true
  }

}
