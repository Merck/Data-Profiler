package com.dataprofiler.loader.config;

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

import com.beust.jcommander.Parameter;
import com.dataprofiler.loader.TableLoader;
import com.dataprofiler.util.Config;

public class LoaderConfig extends Config {

  public int iteratorStartIndex = 2;

  @Parameter(names = "--records-per-partition", description = "The number of records per partition")
  public int recordsPerPartition = 1000000;

  @Parameter(
      names = "--separate-table-schema",
      description = "Require each table to also have the filename of the schema file")
  public boolean separateTableSchema = false;

  @Parameter(
      names = "--schema-path",
      description = "External schema definition file for input data")
  public String schemaPath;

  @Parameter(
      names = "--full-dataset-load",
      description = "Treat this table as the full table for the metadata")
  public boolean fullDatasetLoad = false;

  @Parameter(
      names = "--version-id",
      description = "Unique version id to use for the metadata for this load")
  public String versionId;

  @Parameter(names = "--datasetName")
  public String datasetName;

  @Parameter(names = "--visibility")
  public String visibility;

  @Parameter(names = "--columnVisibilityExpression")
  public String columnVisibilityExpression;

  @Parameter(
      names = "--setActiveVisibilities",
      description =
          "Make an ROU call to set the provided visibility label as active. Defaults to false.")
  public boolean setActiveVisibilities = false;

  @Parameter(
      names = "--skip-repartition",
      description = "Skip repartitioning RDDs during loads. Defaults to false.")
  public boolean skipRepartition = false;

  @Parameter(
      names = "--build-hadoop-config",
      description =
          "Build a hadoop configuration based on CLI/environment variables. Defaults to false.")
  public boolean buildHadoopConfig = false;

  @Parameter(
      names = "--input-path",
      description = "Path of data to ingest. May be single file or directory.")
  public String inputPath;

  @Parameter(
      names = "--table-name",
      description = "Destination table for the data. Defaults to the basename of --input-path.")
  public String tableName;

  @Parameter(
      names = "--name",
      description = "Name of the application, as it appears in the Spark UI")
  public String appName;

  public LoaderConfig() {
    super();
  }

  public String getTableName() {
    if (this.tableName == null || this.tableName.isEmpty()) {
      return TableLoader.tableNameFromFileName(inputPath);
    } else {
      return this.tableName;
    }
  }

  public String appName(String ifUnspecified) {
    return appName == null ? ifUnspecified : appName;
  }
}
