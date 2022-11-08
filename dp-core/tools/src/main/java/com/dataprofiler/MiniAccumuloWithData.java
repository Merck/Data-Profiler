package com.dataprofiler;

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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.dataprofiler.loader.TableLoader;
import com.dataprofiler.loader.config.AvroFileParams;
import com.dataprofiler.loader.config.CsvFileParams;
import com.dataprofiler.loader.config.JsonFileParams;
import com.dataprofiler.loader.config.ParquetFileParams;
import com.dataprofiler.loader.datatypes.AvroLoader;
import com.dataprofiler.loader.datatypes.CsvLoader;
import com.dataprofiler.loader.datatypes.JsonLoader;
import com.dataprofiler.loader.datatypes.Loader;
import com.dataprofiler.loader.datatypes.ParquetLoader;
import com.dataprofiler.metadata.CommitMetadata;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Config;
import com.dataprofiler.util.Const.LoadType;
import com.dataprofiler.util.Const.Origin;
import com.dataprofiler.util.MiniAccumuloContext;
import com.dataprofiler.util.objects.MetadataVersionObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.apache.log4j.Logger;
import org.apache.spark.sql.SparkSession;

/**
 * * This is a commandline program that let's you run a mini-accumulo cluster with data loaded into
 * it. We just load csvs - I would not recommend loading anything other than small test datasets.
 *
 * <p>Note that this will rewrite ~/.dataprofiler/config to point to this cluster (just changing the
 * accumulo stuff).
 */
public class MiniAccumuloWithData implements AutoCloseable {

  private static final Logger logger = Logger.getLogger(MiniAccumuloWithData.class);
  private MiniAccumuloContext context;
  private Config config;
  private String[] rootAuths;

  public MiniAccumuloWithData() {
    super();
  }

  public MiniAccumuloWithData(String[] auths) {
    this();
    this.rootAuths = auths;
  }

  public MiniAccumuloWithData(Config config, String[] auths) {
    this();
    this.config = config;
    this.rootAuths = auths;
  }

  /**
   * main <program> [csvfile]
   *
   * @csvfile a string path to a csv file
   */
  public static void main(String[] args) throws Exception {

    Config config = new Config();
    if (!config.parse(args)) {
      System.exit(1);
    }

    System.out.println("MiniAccumulo!");
    String[] rootAuths =
        new String[] {
          "LIST.PUBLIC_DATA",
          "LIST.PRIVATE_DATA"
        };
    MiniAccumuloWithData mad = new MiniAccumuloWithData(config, rootAuths);
    MiniAccumuloContext madContext;
    if (config.parameters.size() < 1) {
      madContext = mad.startAndLoadReasonableDefaults();
    } else {
      Map<String, List<String>> datasets = new HashMap<>();
      for (String inputFile : config.parameters) {
        String datasetName = friendlyDatasetNameFromPath(inputFile);

        if (!datasets.containsKey(datasetName)) {
          datasets.put(datasetName, new LinkedList<String>());
        }
        datasets.get(datasetName).add(inputFile);
      }
      madContext = mad.startAndLoadData(datasets);
    }
    logger.info("Writing mini config");
    Config miniConfig = madContext.getConfig();
    miniConfig.writeUserConfig();
    if (logger.isTraceEnabled()) {
      logger.trace(miniConfig);
    }
    Thread shutdownHook =
        new Thread(
            () -> {
              try {
                System.out.println("interrupt received, killing server...");
                mad.shutdown();
              } catch (Exception e) {
                e.printStackTrace();
              }
            });
    Runtime.getRuntime().addShutdownHook(shutdownHook);
    logger.info("Hit Ctl-C or Enter to shutdown...");
    System.in.read();
    // if we hit enter and not ctl-c, then remove the shutdown hook so to not run shutdown 2x
    Runtime.getRuntime().removeShutdownHook(shutdownHook);
    mad.shutdown();
  }

  public void shutdown() throws IOException, InterruptedException {
    logger.info("Shutting down...");
    context.close();
  }

  public void loadDataset(String datasetFriendlyName, String datasetPath) throws Exception {
    SparkSession spark = null;

    try {
      DPSparkContext context = new DPSparkContext(this.context, "test");
      spark = context.createSparkSession();
      logger.info("loading " + datasetFriendlyName);

      Loader loader =
          getLoaderBasedOnFileExtension(context, spark, datasetFriendlyName, datasetPath);

      loader.setCommitMetadata(true);

      loader.load();
      logger.info("finished loading " + datasetFriendlyName);
    } finally {
      if (spark != null) {
        spark.close();
      }
    }
  }

  public void loadDatasets(Map<String, List<String>> datasets) throws Exception {
    for (String datasetFriendlyName : datasets.keySet()) {
      for (String datasetPath : datasets.get(datasetFriendlyName)) {
        loadDataset(datasetFriendlyName, datasetPath);
      }
    }
  }

  public Loader getLoaderBasedOnFileExtension(
      DPSparkContext context, SparkSession spark, String datasetFriendlyName, String datasetPath)
      throws BasicAccumuloException {
    String ext = datasetPath.substring(datasetPath.lastIndexOf(".") + 1);
    Loader loader;
    switch (ext) {
      case "avro":
        loader = getAvroLoader(context, spark, datasetFriendlyName, datasetPath);
        break;
      case "json":
        loader = getJsonLoader(context, spark, datasetFriendlyName, datasetPath);
        break;
      case "parquet":
        loader = getParquetLoader(context, spark, datasetFriendlyName, datasetPath);
        break;
      default:
        loader = getCsvLoader(context, spark, datasetFriendlyName, datasetPath);
    }
    return loader;
  }

  private Loader getAvroLoader(
      DPSparkContext context, SparkSession spark, String datasetFriendlyName, String datasetPath)
      throws BasicAccumuloException {
    AvroFileParams params = new AvroFileParams(datasetPath);
    Loader loader =
        new AvroLoader(
                context,
                spark,
                datasetFriendlyName,
                TableLoader.tableNameFromFileName(datasetPath),
                "LIST.PUBLIC_DATA",
                "",
                (getContext().getConfig()).rowVisibilityColumnName,
                params)
            .recordsPerPartition(1000)
            .origin(Origin.UPLOAD)
            .printSchema();
    return loader;
  }

  private Loader getCsvLoader(
      DPSparkContext context, SparkSession spark, String datasetFriendlyName, String datasetPath)
      throws BasicAccumuloException {
    CsvFileParams params = new CsvFileParams(datasetPath);
    Loader loader =
        new CsvLoader(
                context,
                spark,
                datasetFriendlyName,
                TableLoader.tableNameFromFileName(datasetPath),
                "LIST.PUBLIC_DATA",
                "",
                (getContext().getConfig()).rowVisibilityColumnName,
                params)
            .recordsPerPartition(1000)
            .origin(Origin.UPLOAD)
            .printSchema();
    return loader;
  }

  private Loader getJsonLoader(
      DPSparkContext context, SparkSession spark, String datasetFriendlyName, String datasetPath)
      throws BasicAccumuloException {
    JsonFileParams params = new JsonFileParams(datasetPath);
    params.setMultiLine(true);
    Loader loader =
        new JsonLoader(
                context,
                spark,
                datasetFriendlyName,
                TableLoader.tableNameFromFileName(datasetPath),
                "LIST.PUBLIC_DATA",
                "",
                (getContext().getConfig()).rowVisibilityColumnName,
                params)
            .recordsPerPartition(1000)
            .origin(Origin.UPLOAD)
            .printSchema();
    return loader;
  }

  private Loader getParquetLoader(
      DPSparkContext context, SparkSession spark, String datasetFriendlyName, String datasetPath)
      throws BasicAccumuloException {
    ParquetFileParams params = new ParquetFileParams(datasetPath);
    Loader loader =
        new ParquetLoader(
                context,
                spark,
                datasetFriendlyName,
                TableLoader.tableNameFromFileName(datasetPath),
                "LIST.PUBLIC_DATA",
                "",
                (getContext().getConfig()).rowVisibilityColumnName,
                params)
            .recordsPerPartition(1000)
            .origin(Origin.UPLOAD)
            .printSchema();
    return loader;
  }

  /**
   * start accumulo mini and load csv data found in the given paths, and leave accumulo running
   *
   * @param datasets a map of friendly names to file paths
   */
  public MiniAccumuloContext startAndLoadData(Map<String, List<String>> datasets) throws Exception {
    start();
    loadDatasets(datasets);
    return context;
  }

  public String fullCsvPath(String filename) {
    return "./dp-core/tools/src/test/resources/" + filename + ".csv";
  }

  public static String friendlyDatasetNameFromPath(String path) {
    String[] parts = path.split("/");
    if (parts.length >= 2) {
      return parts[parts.length - 2];
    } else {
      return path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
    }
  }

  /** Just load some reasonable defaults (multiple datasets,tables,visibilities etc) */
  public MiniAccumuloContext startAndLoadReasonableDefaults() throws Exception {
    start();
    SparkSession spark = null;
    try {
      DPSparkContext context = new DPSparkContext(this.context, "test");

      logger.info(String.format("Load Type: %s", this.context.getConfig().loadType));

      if (this.context.getConfig().loadType != LoadType.LIVE) {
        context.getConfig().loadOutputDest = this.context.getTempPath().toString();
        logger.info(String.format("Load output dest: %s", context.getConfig().loadOutputDest));
      }

      spark = context.createSparkSession();
      loadCsvTable(
          context,
          spark,
          fullCsvPath("basic_test_data"),
          "Dataset 1",
          "Table 1",
          "LIST.PUBLIC_DATA");
      loadCsvTable(
          context,
          spark,
          fullCsvPath("tiny_test_data_copy"),
          "Dataset 1",
          "Table 2",
          "LIST.PUBLIC_DATA");
      loadCsvTable(
          context, spark, fullCsvPath("basic_test_data"), "Dataset 1", "Table 3", "LIST.Nick_1");
      loadCsvTable(
          context,
          spark,
          fullCsvPath("annotation_test_data_ds2"),
          "Dataset 2",
          "Table 2",
          "LIST.PUBLIC_DATA");
    } finally {
      if (spark != null) {
        spark.close();
      }
    }
    return context;
  }

  public void loadCsvTable(
      DPSparkContext context,
      SparkSession spark,
      String path,
      String dataset,
      String table,
      String viz)
      throws Exception {
    String version = UUID.randomUUID().toString();

    MetadataVersionObject mvo = new MetadataVersionObject(version);
    String logString =
        String.format("Dataset '%s'\tTable '%s'\tVersion: %s", dataset, table, version);
    logger.info("Loading " + logString);

    CsvFileParams params = new CsvFileParams(path);
    Loader loader =
        new CsvLoader(context, spark, dataset, table, viz, "", "", params)
            .recordsPerPartition(1000)
            .origin(Origin.UPLOAD)
            .versionId(version)
            .printSchema();

    if (this.context.getConfig().loadType == LoadType.LIVE) {
      loader.setCommitMetadata(true);
    }

    loader.load();
    loader.getOrigTable().show(false);
    logger.info("Finished loading " + logString);

    if (this.context.getConfig().loadType != LoadType.LIVE) {
      CommitMetadata.commitMetadata(context, mvo, null, true);
    }
  }

  public MiniAccumuloContext start() throws IOException, BasicAccumuloException {

    if (this.rootAuths != null && this.config != null) {
      this.context = new MiniAccumuloContext(this.config, this.rootAuths);
    } else if (this.rootAuths != null) {
      this.context = new MiniAccumuloContext(this.rootAuths);
    } else if (this.config != null) {
      this.context = new MiniAccumuloContext(this.config);
    } else {
      this.context = new MiniAccumuloContext();
    }

    // We need to get the mini accumulo running, so we do that here by connecting. Just doing spark
    // operations does not do that.
    context.connect(); // this actually forces the

    return context;
  }

  /***
   * Create a new MiniAccumuloWithData that is suitable for use with unit testing. It does not load configuration from any of the
   * config file locations.
   *
   * @return MiniAccumuloContext
   * @throws BasicAccumuloException
   */
  public MiniAccumuloContext startForTesting() throws BasicAccumuloException, IOException {
    Config config = new Config();

    context =
        (this.rootAuths == null)
            ? new MiniAccumuloContext(config)
            : new MiniAccumuloContext(this.rootAuths);
    context.getConfig().loadType = LoadType.LIVE;
    // We do this so that more than one mini accumulo can run at once. It's really hard to guarantee
    // that there aren't more
    // than one mini accumulos running at any given time.
    context.setRandomZookeeperPort(true);

    // We need to get the mini accumulo running, so we do that here by connecting. Just doing spark
    // operations does not do that.
    context.connect(); // this actually forces the

    return context;
  }

  public void close() throws IOException, InterruptedException {
    context.close();
  }

  public MiniAccumuloContext getContext() {
    return context;
  }

  public void setContext(MiniAccumuloContext context) {
    this.context = context;
  }
}
