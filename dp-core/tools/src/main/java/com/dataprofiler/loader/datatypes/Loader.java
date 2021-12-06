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

import com.dataprofiler.ActiveVisibilityManager;
import com.dataprofiler.DPSparkContext;
import com.dataprofiler.InvalidSchemaFormatException;
import com.dataprofiler.loader.TableLoader;
import com.dataprofiler.loader.config.CsvFileParams;
import com.dataprofiler.loader.config.LoaderConfig;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Const;
import com.dataprofiler.util.Const.Origin;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.MetadataVersionObject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.log4j.Logger;
import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class Loader {

  private static final Logger logger = Logger.getLogger(Loader.class);

  public static int DEFAULT_RECORDS_PER_PARTITION = 1000000;
  protected DPSparkContext context;
  protected SparkSession spark;
  protected String datasetName;
  protected String tableName;
  protected String visibility;
  protected String columnVisibilityExpression;
  protected String rowVisibilityColumnName;
  protected Integer recordsPerPartition = DEFAULT_RECORDS_PER_PARTITION;
  protected boolean printSchema = true;
  protected Dataset<Row> origTable;
  protected Origin origin = Const.Origin.UPLOAD;
  protected boolean commitMetadata = false;
  protected boolean fullDatasetLoad = false;
  protected String versionId;

  public Loader(
      DPSparkContext context,
      SparkSession spark,
      String datasetName,
      String tableName,
      String visibility,
      String columnVisibilityExpression,
      String rowVisibilityColumnName)
      throws BasicAccumuloException {
    this.context = context;
    if (context.getConfig().loadType == Const.LoadType.SPLIT
        && !context.getConfig().allowAccumuloConnection) {
      context.setNeverConnect(true);
    }
    this.spark = spark;
    this.datasetName = datasetName;
    this.tableName = tableName;
    this.visibility = visibility.toUpperCase();
    this.columnVisibilityExpression = columnVisibilityExpression;
    this.rowVisibilityColumnName = rowVisibilityColumnName;
  }

  public Loader(
      DPSparkContext context,
      SparkSession spark,
      String datasetName,
      String tableName,
      String visibility,
      String columnVisibilityExpression,
      String rowVisibilityColumnName,
      Dataset<Row> rows) {
    this.context = context;
    this.spark = spark;
    this.datasetName = datasetName;
    this.tableName = tableName;
    this.visibility = visibility.toUpperCase();
    this.columnVisibilityExpression = columnVisibilityExpression;
    this.rowVisibilityColumnName = rowVisibilityColumnName;
    origTable = rows;
  }

  public void setCommitMetadata(boolean commit) {
    this.commitMetadata = commit;
  }

  /**
   * Determine if a file or directory has a size greater than 0. This is an easy check to see if the
   * path or file provided can be used by the loader.
   *
   * @param fileName The absolute path to the file or directory
   * @return True is file or directory has a size greater than 0; False otherwise
   * @throws URISyntaxException
   * @throws IOException
   */
  public boolean inputContainsFiles(String fileName) throws URISyntaxException, IOException {
    URI uri = new URI(fileName);
    FileSystem fs = FileSystem.get(uri, this.context.getHadoopConfiguration());

    long size = 0;
    RemoteIterator<LocatedFileStatus> iter = fs.listFiles(new Path(fileName), false);
    while (iter.hasNext()) {
      LocatedFileStatus fileStatus = iter.next();
      size += fileStatus.getBlockSize();
    }

    return size > 0;
  }

  public static Dataset<Row> applySchemaFromFile(
      SparkSession spark, Dataset<Row> rows, String schemaFilename)
      throws InvalidSchemaFormatException {
    List<Row> columnNamesFromCsv = getColumnNamesFromCsv(spark, schemaFilename);
    logger.warn("Applying schema from file: " + schemaFilename);
    if (rows.columns().length != columnNamesFromCsv.size()) {
      String err =
          String.format(
              "Schema from schema file %s has a different number of columns "
                  + "than data: %d columns "
                  + "vs %d names",
              schemaFilename, rows.columns().length, columnNamesFromCsv.size());
      logger.warn(err);
      throw new InvalidSchemaFormatException(err);
    }

    for (int i = 0; i < rows.columns().length; i++) {
      String columnName = columnNamesFromCsv.get(i).getString(0);
      logger.warn(String.format("Renaming column %d to %s.", i, columnName));
      rows = rows.withColumnRenamed(rows.columns()[i], columnName);
    }

    return rows;
  }

  public static List<Row> getColumnNamesFromCsv(SparkSession spark, String schemaFileName)
      throws InvalidSchemaFormatException {
    CsvFileParams params = new CsvFileParams(schemaFileName);
    params.setHasHeader(false);
    Dataset<Row> columnNames = readCSV(spark, params);

    if (columnNames.columns().length != 1) {
      throw new InvalidSchemaFormatException("Schema files should have only 1 column");
    }
    columnNames.show();

    return columnNames.select("_c0").collectAsList();
  }

  /**
   * Load a CSV into a DataFrame.
   *
   * @return DataFrame of rows from the CSV file.
   */
  public static Dataset<Row> readCSV(SparkSession spark, CsvFileParams file)
      throws InvalidSchemaFormatException {
    DataFrameReader reader =
        spark
            .read()
            .format("csv")
            .option("mode", "DROPMALFORMED")
            .option("delimiter", file.getDelimiter())
            .option("charset", file.getCharset())
            .option("quote", file.getQuote())
            .option("escape", file.getEscape())
            .option("multiLine", file.isMultiline())
            .option("escape", "\"");

    if (file.getHasHeader()) {
      reader.option("header", "true");
    } else {
      reader.option("header", "false");
    }

    if (file.getInferSchema()) {
      reader.option("inferSchema", "true");
    }

    Dataset<Row> rows = reader.load(file.getInputFilename());

    // test for performance to avoid another call to s3
    rows.persist();

    if (file.getSchemaFilename() != null) {
      rows = applySchemaFromFile(spark, rows, file.getSchemaFilename());
    }

    return rows;
  }

  public Loader printSchema() {
    printSchema = true;
    return this;
  }

  public Loader recordsPerPartition(int numRecords) {
    recordsPerPartition = numRecords;
    return this;
  }

  public Loader origin(Const.Origin origin) {
    this.origin = origin;
    return this;
  }

  public Loader origin(String origin) {
    this.origin = Const.Origin.getEnum(origin);
    return this;
  }

  public Loader fullDatasetLoad(boolean fullDatasetLoad) {
    this.fullDatasetLoad = fullDatasetLoad;
    return this;
  }

  public DPSparkContext getContext() {
    return context;
  }

  public void setContext(DPSparkContext context) {
    this.context = context;
  }

  public SparkSession getSpark() {
    return spark;
  }

  public void setSpark(SparkSession spark) {
    this.spark = spark;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getVisibility() {
    return visibility;
  }

  public void setVisibility(String visibility) {
    this.visibility = visibility;
  }

  public String getVersionId() {
    return versionId;
  }

  public Loader versionId(String versionId) {
    this.versionId = versionId;
    return this;
  }

  public Integer getRecordsPerPartition() {
    return recordsPerPartition;
  }

  public void setRecordsPerPartition(Integer recordsPerPartition) {
    this.recordsPerPartition = recordsPerPartition;
  }

  public boolean isPrintSchema() {
    return printSchema;
  }

  public void setPrintSchema(boolean printSchema) {
    this.printSchema = printSchema;
  }

  public Dataset<Row> getOrigTable() {
    return origTable;
  }

  public void setOrigTable(Dataset<Row> origTable) {
    this.origTable = origTable;
  }

  public Const.Origin getOrigin() {
    return origin;
  }

  public void setOrigin(Const.Origin origin) {
    this.origin = origin;
  }

  public boolean isFullDatasetLoad() {
    return fullDatasetLoad;
  }

  public void setFullDatasetLoad(boolean fullDatasetLoad) {
    this.fullDatasetLoad = fullDatasetLoad;
  }

  public boolean load() throws Exception {
    MetadataVersionObject mv = null;
    if (versionId != null) {
      mv = new MetadataVersionObject(versionId);
    }
    boolean loaded =
        TableLoader.load(
            context,
            spark,
            origTable,
            mv,
            datasetName,
            tableName,
            visibility,
            columnVisibilityExpression,
            rowVisibilityColumnName,
            recordsPerPartition,
            origin,
            commitMetadata,
            fullDatasetLoad,
            printSchema);

    if (loaded
        && this.context.getConfig() instanceof LoaderConfig
        && ((LoaderConfig) this.context.getConfig()).setActiveVisibilities) {
      // we need to allow an Accumulo connection here
      Context ctx = this.context;
      if (this.context.isNeverConnect()) {
        ctx = new Context(this.context);
        ctx.setNeverConnect(false);
      }
      try (ActiveVisibilityManager avm = new ActiveVisibilityManager(ctx)) {
        avm.setVisibilitiesFromExpressionAsActive(visibility.toUpperCase());
        // Find all existing Row Visibilities
        try {
          logger.info("Activating row level visibilities......");
          TableLoader.activateRowLevelVisibilities(avm, origTable, rowVisibilityColumnName);
        } catch (Exception e) {
          logger.error("Failed to set row visibility expressions as active" + e.getMessage());
        }
      }
    }

    return loaded;
  }
}
