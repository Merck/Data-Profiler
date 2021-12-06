package com.dataprofiler.loader;

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
import com.dataprofiler.loader.config.JsonFileParams;
import com.dataprofiler.loader.config.LoaderConfig;
import com.dataprofiler.loader.datatypes.JsonLoader;
import com.dataprofiler.loader.datatypes.LoaderException;
import com.dataprofiler.util.Const;
import org.apache.log4j.Logger;
import org.apache.spark.sql.SparkSession;

public class JsonDataLoader {
  private static final Logger logger = Logger.getLogger(JsonDataLoader.class);
  private static final String APP_NAME = "Json_Data_Loader";

  public static void main(String[] args) throws Exception {
    JsonDataLoader loader = new JsonDataLoader();
    loader.execute(args);
  }

  public void execute(String[] args) throws Exception {
    LoaderConfig config = new LoaderConfig();
    config.parse(args);

    DPSparkContext context = new DPSparkContext(config, config.appName(APP_NAME));

    SparkSession spark = context.createSparkSession();

    String datasetName = config.datasetName;
    String visibility = config.visibility;
    String columnVisibilityExpression = config.columnVisibilityExpression;
    String rowVisibilityColumnName = config.rowVisibilityColumnName;
    String tableName = config.getTableName();
    JsonFileParams params = new JsonFileParams();
    params.setInputFilename(config.inputPath);
    boolean loaded =
        new JsonLoader(
                context,
                spark,
                datasetName,
                tableName,
                visibility,
                columnVisibilityExpression,
                rowVisibilityColumnName,
                params)
            .recordsPerPartition(config.recordsPerPartition)
            .origin(Const.Origin.UPLOAD)
            .printSchema()
            .fullDatasetLoad(config.fullDatasetLoad)
            .versionId(config.versionId)
            .load();

    if (!loaded) {
      throw new LoaderException(
          String.format("File '%s' did not contain any records", params.getInputFilename()));
    }
  }
}
