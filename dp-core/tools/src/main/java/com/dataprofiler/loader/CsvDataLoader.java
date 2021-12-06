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
import com.dataprofiler.loader.config.CsvFileParams;
import com.dataprofiler.loader.config.CsvLoaderConfig;
import com.dataprofiler.loader.datatypes.CsvLoader;
import com.dataprofiler.loader.datatypes.LoaderException;
import com.dataprofiler.util.Const;
import org.apache.log4j.Logger;
import org.apache.spark.sql.SparkSession;

public class CsvDataLoader {

  private static final Logger logger = Logger.getLogger(CsvDataLoader.class);

  private static final String APP_NAME = "Data_Loader";
  private static final Boolean MULTILINE = true;

  public static void main(String[] args) throws Exception {
    CsvDataLoader loader = new CsvDataLoader();
    loader.execute(args);
  }

  public void execute(String[] args) throws Exception {
    CsvLoaderConfig config = new CsvLoaderConfig();

    if (!config.parse(args)) {
      System.exit(1);
    }

    DPSparkContext context =
        new DPSparkContext(
            config,
            config.appName(APP_NAME),
            DPSparkContext.createDefaultSparkConf(config, config.appName(APP_NAME)));

    String datasetName = config.datasetName;
    String visibility = config.visibility;
    String columnVisibilityExpression = config.columnVisibilityExpression;
    String rowVisibilityColumnName = config.rowVisibilityColumnName;

    SparkSession spark = context.createSparkSession();

    CsvFileParams p = config.toFileParams();
    String tableName = config.getTableName();
    logger.info(String.format("Table: %s", tableName));

    boolean loaded =
        new CsvLoader(
                context,
                spark,
                datasetName,
                tableName,
                visibility,
                columnVisibilityExpression,
                rowVisibilityColumnName,
                p)
            .recordsPerPartition(config.recordsPerPartition)
            .origin(Const.Origin.UPLOAD)
            .printSchema()
            .versionId(config.versionId)
            .fullDatasetLoad(config.fullDatasetLoad)
            .load();

    if (!loaded) {
      throw new LoaderException(
          String.format("File '%s' did not contain any records", p.getInputFilename()));
    }
  }
}
