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
import com.dataprofiler.delete.DeleteData;
import com.dataprofiler.metadata.CommitMetadata;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Const;
import com.dataprofiler.util.objects.MetadataVersionObject;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

public class PysparkDataLoader {

  private static final Logger logger = Logger.getLogger(PysparkDataLoader.class);

  /**
   * Load column counts and rows from pyspark
   *
   * <p>This method can be called from pyspark using the signature <code>
   * sc._jvm.PysparkDataLoader.loadFromPyspark(...)</code>
   *
   * <p>The first argument is the DPSparkContext. To get this object in Python use the existing
   * spark context to call the DPSparkContext constructor and pass it the Java spark context:
   *
   * <p><code>dpSparkContext = sc._jvm.DPSparkContext(sc._jsc)</code>
   *
   * <p>
   *
   * <p>The second argument is the Java DataFrame. To get the Java DataFrame in Python, use the
   * <code>_jdf</code> method on the Python DataFrame, e.g. <code>dataframe._jdf</code>
   *
   * <p><code>
   * sc._jvm.PysparkDataLoader.loadFromPyspark(dpSparkContext, dataframe._jdf,
   * "dataset_name", "table_name", False, "LIST.PUBLIC_DATA", 10000, False)</code>
   *
   * @param context A DPSparkContext
   * @param data A Java DataFrame
   * @param datasetName The name of the destination dataset
   * @param tableName The name of the destination table
   * @param deleteBeforeReload Delete existing data before loading this data
   * @param visibility Visibility string fo the dataset
   * @param columnVisibilityExpression Visibility string for columns
   * @param rowVisibilityColumnName Visibility string for rows
   * @param recordsPerPartition Number of records per partition
   * @param origin Where the data is coming from, e.g. "make"
   * @param rebuildMetadata true to rebuild metadata; false otherwise
   * @param fullDatasetLoad treat this as the full dataset
   * @param printSchema Display the schema
   * @return true if succesfully complete, false otherwise
   * @throws IOException
   * @throws BasicAccumuloException
   */
  public static boolean loadFromPyspark(
      DPSparkContext context,
      Dataset<Row> data,
      String versionId,
      String datasetName,
      String tableName,
      String visibility,
      String columnVisibilityExpression,
      String rowVisibilityColumnName,
      Integer recordsPerPartition,
      String origin,
      boolean commitMetadata,
      boolean fullDatasetLoad,
      boolean printSchema)
      throws Exception {

    MetadataVersionObject version = null;
    if (versionId != null) {
      new MetadataVersionObject(versionId);
    }

    return TableLoader.load(
        context,
        context.createSparkSession(),
        data,
        version,
        datasetName,
        tableName,
        visibility,
        columnVisibilityExpression,
        rowVisibilityColumnName,
        recordsPerPartition,
        Const.Origin.getAndVerifyEnum(origin),
        commitMetadata,
        fullDatasetLoad,
        printSchema);
  }

  public static void commitMetadata(
      DPSparkContext context, String versionId, boolean fullDatasetLoad) throws Exception {
    logger.warn("Rebuilding metadata");
    CommitMetadata.commitMetadata(
        context, new MetadataVersionObject(versionId), null, fullDatasetLoad);
  }

  public static void deleteDataset(DPSparkContext context, String dataset) throws Exception {

    CommitMetadata.Deletions deletions = new CommitMetadata.Deletions();
    deletions.datasetDeletions.add(dataset);
    DeleteData.softDeleteDatasetsOrTables(context, deletions);
  }

  public static void deleteTable(DPSparkContext context, String dataset, String table)
      throws Exception {

    CommitMetadata.Deletions deletions = new CommitMetadata.Deletions();
    deletions.addTable(dataset, table);
    DeleteData.softDeleteDatasetsOrTables(context, deletions);
  }
}
