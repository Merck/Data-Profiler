package com.dataprofiler.loader.datatypes;

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

import com.dataprofiler.DPSparkContext;
import com.dataprofiler.loader.config.ParquetFileParams;
import com.dataprofiler.util.BasicAccumuloException;
import org.apache.spark.sql.SparkSession;

public class ParquetLoader extends Loader {
  private final ParquetFileParams fileParams;

  public ParquetLoader(
      DPSparkContext context,
      SparkSession spark,
      String datasetName,
      String tableName,
      String visibility,
      String columnVisibilityExpression,
      String rowVisibilityColumnName,
      ParquetFileParams fileParams)
      throws BasicAccumuloException {
    super(
        context,
        spark,
        datasetName,
        tableName,
        visibility,
        columnVisibilityExpression,
        rowVisibilityColumnName);
    this.fileParams = fileParams;
  }

  public boolean load() throws Exception {

    if (!this.inputContainsFiles(fileParams.getInputFilename())) {
      throw new LoaderException(
          String.format("Input '%s' contains no files", fileParams.getInputFilename()));
    }

    origTable = getSpark().read().parquet(fileParams.getInputFilename());

    if (fileParams.getSchemaFilename() != null) {
      origTable = applySchemaFromFile(getSpark(), origTable, fileParams.getSchemaFilename());
    }

    return super.load();
  }
}
