package com.dataprofiler.util;

/*-
 * 
 * dataprofiler-util
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

import static java.lang.Character.isUpperCase;
import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toSet;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.dataprofiler.util.Const.LoadType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Config implements Serializable {
  Logger logger = LoggerFactory.getLogger(Config.class);

  @Parameter public List<String> parameters = new ArrayList<>();

  @Parameter(names = "--environment", description = "name of the environment")
  public String environment = "development";

  @Parameter(
      names = "--zookeepers",
      description = "comma separate list of Zookeeper instances with port")
  public String zookeepers = "";

  @Parameter(names = "--accumulo-instance", description = "Name of the Accumulo instance")
  public String accumuloInstance = "hdp-accumulo-instance";

  @Parameter(
      names = "--accumulo-metadata-table",
      description = "Table name of the Accumulo table holding metadata")
  public String accumuloMetadataTable = "curr.metadata";

  @Parameter(
      names = "--accumulo-samples-table",
      description = "The name of the Accumulo table holding sample data")
  public String accumuloSamplesTable = "curr.samples";

  @Parameter(
      names = "--accumulo-column-counts-table",
      description = "The name of the Accumulo table holding column counts")
  public String accumuloColumnCountsTable = "curr.columnCounts";

  @Parameter(
      names = "--accumulo-datawave-row-table",
      description = "The name of the Accumulo table holding datawave row data")
  public String accumuloDatawaveRowsTable = "curr.datawaveRows";

  @Parameter(
      names = "--accumulo-index-table",
      description = "The name of the Accumulo table holding index data")
  public String accumuloIndexTable = "curr.index";

  @Parameter(
      names = "--data-load-jobs-table",
      description = "The name of the Accumulo table that holds Data Loader jobs info")
  public String accumuloDataLoadJobsTable = "curr.dataLoadJobs";

  @Parameter(
      names = "--accumulo-api-tokens-table",
      description = "The name of the Accumulo table that holds API Tokens")
  public String accumuloAPITokensTable = "curr.apiTokens";

  @Parameter(
      names = "--column-entities-table",
      description = "The name of the Accumulo table that holds column entities")
  public String accumuloColumnEntitiesTable = "curr.columnEntities";

  @Parameter(
      names = "--element-aliases-table",
      description = "The name of the Accumulo table that holds element aliases")
  public String accumuloElementAliasesTable = "curr.elementAliases";

  @Parameter(
      names = "--custom-annotations-table",
      description = "The name of the Accumulo table that holds manually custom annotations")
  public String accumuloCustomAnnotationsTable = "curr.customAnnotations";

  @Parameter(names = "--accumulo-user", description = "The Accumulo user")
  public String accumuloUser = "root";

  @Parameter(
      names = "--accumulo-password",
      description = "The Accumulo user password",
      password = true)
  public String accumuloPassword = "";

  @Parameter(names = "--accumulo-scanner-threads", description = "Number of threads per scanner")
  public Integer accumuloScannerThreads = 10;

  @Parameter(
      names = "--analytics-api-site-id",
      description = "Matomo API Site ID for this environment",
      password = true)
  public String analyticsApiSiteId = "";

  @Parameter(
      names = "--analytics-ui-site-id",
      description = "Matomo UI Site ID for this environment",
      password = true)
  public String analyticsUiSiteId = "";

  @Parameter(names = "--rules-of-use-api-path", description = "HTTP path for the rules of use API")
  public String rulesOfUseApiPath = "";

  @Parameter(
      names = "--rules-of-use-api-key",
      description = "API key for the rules of use API",
      password = true)
  public String rulesOfUseApiKey = "";

  @Parameter(
      names = "--matomo-api-key",
      description = "API key for the Matomo API",
      password = true)
  public String matomoApiKey = "";

  @Parameter(
      names = "--num-samples",
      description = "The number of samples to collect for each column")
  public Integer numSamples = 100;

  @Parameter(names = "--threadpool-size", description = "Number of threads in the thread pool")
  public Integer threadPoolSize = 50;

  @Parameter(names = "--statsd-host", description = "Statsd Hostname")
  public String statsdHost = "backend-docker";

  @Parameter(names = "--statsd-port", description = "Statsd Port")
  public Integer statsdPort = 8125;

  @Parameter(names = "--user-facing-api-http-path", description = "Http path for the play api")
  public String userFacingApiHttpPath = "";

  @Parameter(
      names = "--download-event-listener-url",
      description = "Url for the download event listener")
  public String downloadEventListenerUrl = "";

  // Yarn queues - we provide 5 basic levels for yarn queues: background, default, interactive,
  // upload, and download
  // These can be mapped as appropriate to queues on the different
  // clusters.

  @Parameter(
      names = "--background-yarn-queue",
      description = "Yarn queue for long running background tasks")
  public String backgroundYarnQueue = "default";

  @Parameter(names = "--default-yarn-queue", description = "Yarn queue for default tasks")
  public String defaultYarnQueue = "default";

  @Parameter(names = "--interactive-yarn-queue", description = "Yarn queue for interactive tasks")
  public String interactiveYarnQueue = "default";

  @Parameter(names = "--upload-yarn-queue", description = "Yarn queue for upload tasks")
  public String uploadYarnQueue = "default";

  @Parameter(names = "--download-yarn-queue", description = "Yarn queue for download tasks")
  public String downloadYarnQueue = "default";

  @Parameter(
      names = "--run-spark-locally",
      description = "Run Spark jobs locally instead of on the cluster")
  public boolean runSparkLocally = false;

  @Parameter(names = "--data-profiler-tools-jar", description = "Location of the dataprofiler jar")
  public String dataProfilerToolsJar = "";

  public static class LoadTypeConverter implements IStringConverter<LoadType> {

    @Override
    public LoadType convert(String value) {
      LoadType l = LoadType.forValue(value);

      if (l == null) {
        throw new ParameterException(
            "Value " + value + " cannot be converted: accepted values are live, bulk, and split");
      }

      return l;
    }
  }

  @Parameter(
      names = "--load-type",
      description =
          "How to load data (live, bulk, or split (where rfiles are written but not loaded)",
      converter = LoadTypeConverter.class)
  public LoadType loadType = LoadType.SPLIT;

  @Parameter(
      names = "--allow-accumulo-connection",
      description = "Allow an Accumulo connection, even if split-load is turned on")
  public boolean allowAccumuloConnection = false;

  @Parameter(
      names = "--loader-output-dest",
      description = "Output destination for the loader to write rFiles for bulk ingest")
  public String loadOutputDest = "";

  @Parameter(
      names = "--run-spark-from-play-framework",
      description = "Prepares the spark job being called from the play framework")
  public Boolean runSparkFromPlayFramework = false;

  @Parameter(
      names = "-spark-ingest-base-directory",
      description = "Root directory spark uses for temporary files on ingest")
  public String sparkIngestBaseDir = "/data/ingest";

  @Parameter(names = "--s3-bucket", description = "S3 bucket for the cluster")
  public String s3Bucket = "";

  @Parameter(names = "--hadoop-namenode1", description = "Hadoop namenode 1")
  public String hadoopNamenode1 = "";

  @Parameter(names = "--hadoop-namenode2", description = "Hadoop namenode 2")
  public String hadoopNamenode2 = "";

  @Parameter(names = "--hadoop-default-fs", description = "Hadoop default fs")
  public String hadoopDefaultFs = "";

  @Parameter(names = "--jobs-api-path", description = "URL for the jobs api")
  public String jobsApiUrl = "";

  @Parameter(
      names = "--table-mapper-api-path",
      description = "URL for the table mapper api - sql sync visibilities")
  public String tableMapperApiPath = "";

  @Parameter(names = "--rowVisibilityColumnName")
  public String rowVisibilityColumnName = "accumuloRowLevelVisibilities";

  @Parameter(names = "--help", help = true)
  public boolean help;

  @Parameter(names = "--config")
  public List<String> configFiles =
      Arrays.asList("/etc/dataprofiler.conf", "~/.dataprofiler/config");

  @Parameter(names = "--sqlsync-url", description = "URL destination to write jdbc exports")
  public String sqlsyncUrl = "";

  @Parameter(
      names = "--sqlsync-user",
      description = "User name for destination to write jdbc exports")
  public String sqlsyncUser = "";

  @Parameter(
      names = "--sqlsync-password",
      description = "Password for destination to write jdbc exports")
  public String sqlsyncPassword = "";

  @Parameter(
      names = "--sqlsync-event-listener-url",
      description = "Url for the sqlsync event listener")
  public String sqlsyncEventListenerUrl = "";

  @Parameter(names = "--postgres-url", description = "Postgres JDBC Database URL")
  public String postgresUrl = "postgres";

  @Parameter(names = "--postgres-username", description = "Postgres JDBC connection username")
  public String postgresUsername = "postgres";

  @Parameter(names = "--postgres-password", description = "Postgres JDBC connection password")
  public String postgresPassword = "";

  @Parameter(
      names = "--datasetperformance-event-listener-url",
      description = "Url for the dataset performance event listener")
  public String datasetperformanceEventListenerUrl = "";

  @Parameter(
      names = "--datasetdelta-event-listener-url",
      description = "Url for the dataset delta event listener")
  public String datasetdeltaEventListenerUrl = "";

  @Parameter(
      names = "--datasetquality-event-listener-url",
      description = "Url for the dataset quality event listener")
  public String datasetqualityEventListenerUrl = "";

  @Parameter(
      names = "--spark-allowMultipleContexts",
      description = "Allow multiple contexts in the Spark Config")
  public boolean sparkAllowMultipleContexts = false;

  @Parameter(
      names = "--canceller-event-listener-url",
      description = "Url for the job canceller event listener")
  public String cancellerEventListenerUrl = "";

  @Parameter(
      names = "--connection-engine-event-listener-url",
      description = "Url for the connection engine event listener")
  public String connectionEngineEventListenerUrl = "";

  @Parameter(names = "--slack-webhook", description = "slack webhook")
  public String slackWebhook = "";

  public Config() {}

  public Config(Config other) {
    this.environment = other.environment;
    this.parameters = other.parameters;
    this.zookeepers = other.zookeepers;
    this.accumuloInstance = other.accumuloInstance;
    this.accumuloMetadataTable = other.accumuloMetadataTable;
    this.accumuloSamplesTable = other.accumuloSamplesTable;
    this.accumuloColumnCountsTable = other.accumuloColumnCountsTable;
    this.accumuloDatawaveRowsTable = other.accumuloDatawaveRowsTable;
    this.accumuloIndexTable = other.accumuloIndexTable;
    this.accumuloDataLoadJobsTable = other.accumuloDataLoadJobsTable;
    this.accumuloAPITokensTable = other.accumuloAPITokensTable;
    this.accumuloColumnEntitiesTable = other.accumuloColumnEntitiesTable;
    this.accumuloElementAliasesTable = other.accumuloElementAliasesTable;
    this.accumuloCustomAnnotationsTable = other.accumuloCustomAnnotationsTable;
    this.accumuloUser = other.accumuloUser;
    this.accumuloPassword = other.accumuloPassword;
    this.accumuloScannerThreads = other.accumuloScannerThreads;
    this.allowAccumuloConnection = other.allowAccumuloConnection;
    this.analyticsApiSiteId = other.analyticsApiSiteId;
    this.analyticsUiSiteId = other.analyticsUiSiteId;
    this.rulesOfUseApiPath = other.rulesOfUseApiPath;
    this.rulesOfUseApiKey = other.rulesOfUseApiKey;
    this.matomoApiKey = other.matomoApiKey;
    this.numSamples = other.numSamples;
    this.threadPoolSize = other.threadPoolSize;
    this.backgroundYarnQueue = other.backgroundYarnQueue;
    this.defaultYarnQueue = other.defaultYarnQueue;
    this.interactiveYarnQueue = other.interactiveYarnQueue;
    this.uploadYarnQueue = other.uploadYarnQueue;
    this.downloadYarnQueue = other.downloadYarnQueue;
    this.runSparkLocally = other.runSparkLocally;
    this.dataProfilerToolsJar = other.dataProfilerToolsJar;
    this.loadType = other.loadType;
    this.loadOutputDest = other.loadOutputDest;
    this.runSparkFromPlayFramework = other.runSparkFromPlayFramework;
    this.sparkIngestBaseDir = other.sparkIngestBaseDir;
    this.s3Bucket = other.s3Bucket;
    this.hadoopNamenode1 = other.hadoopNamenode1;
    this.hadoopNamenode2 = other.hadoopNamenode2;
    this.hadoopDefaultFs = other.hadoopDefaultFs;
    this.jobsApiUrl = other.jobsApiUrl;
    this.tableMapperApiPath = other.tableMapperApiPath;
    this.userFacingApiHttpPath = other.userFacingApiHttpPath;
    this.downloadEventListenerUrl = other.downloadEventListenerUrl;
    this.statsdHost = other.statsdHost;
    this.statsdPort = other.statsdPort;
    this.rowVisibilityColumnName = other.rowVisibilityColumnName;
    this.help = other.help;
    this.configFiles = other.configFiles;
    this.sqlsyncUrl = other.sqlsyncUrl;
    this.sqlsyncUser = other.sqlsyncUser;
    this.sqlsyncPassword = other.sqlsyncPassword;
    this.sqlsyncEventListenerUrl = other.sqlsyncEventListenerUrl;
    this.postgresPassword = other.postgresPassword;
    this.postgresUrl = other.postgresUrl;
    this.postgresUsername = other.postgresUsername;
    this.datasetperformanceEventListenerUrl = other.datasetperformanceEventListenerUrl;
    this.datasetdeltaEventListenerUrl = other.datasetdeltaEventListenerUrl;
    this.datasetqualityEventListenerUrl = other.datasetqualityEventListenerUrl;
    this.sparkAllowMultipleContexts = other.sparkAllowMultipleContexts;
    this.cancellerEventListenerUrl = other.cancellerEventListenerUrl;
    this.connectionEngineEventListenerUrl = other.connectionEngineEventListenerUrl;
    this.slackWebhook = other.slackWebhook;
  }

  public void printHelp() {
    new JCommander(this).usage();
  }

  /**
   * Get the value of a accumulo table by the name. This allows callers to specify that they want
   * the value by a symbolic name (e.g., accumuloMetadataTable).
   *
   * @param name the name of the config paramater
   * @return value of that config parameter
   */
  public String getAccumuloTableByName(String name) {
    try {
      return (String) this.getClass().getField(name).get(this);
    } catch (Exception e) {
      return "";
    }
  }

  public List<String> getAllTables() {
    return Stream.of(
            accumuloColumnCountsTable,
            accumuloMetadataTable,
            accumuloSamplesTable,
            accumuloDatawaveRowsTable,
            accumuloIndexTable,
            accumuloDataLoadJobsTable,
            accumuloAPITokensTable,
            accumuloCustomAnnotationsTable,
            accumuloElementAliasesTable)
        .collect(Collectors.toList());
  }

  private byte[] open(String fname) {
    Path p = Paths.get(fname.replaceFirst("^~", System.getProperty("user.home"))).normalize();
    if (!Files.isReadable(p)) {
      return null;
    }

    try {
      return Files.readAllBytes(p);
    } catch (IOException e) {
      return null;
    }
  }

  private void updateFromConfigFile(String fname) throws IOException {
    byte[] jsonData = open(fname);
    if (jsonData == null) {
      logger.warn("could not open config file " + fname);
      return;
    }

    logger.info("loading config from " + fname);
    ObjectMapper mapper = new ObjectMapper();
    Config fromConfig = mapper.readValue(jsonData, Config.class);
    if (logger.isTraceEnabled()) {
      logger.trace("merging config " + fromConfig.toString());
    }
    update(fromConfig);
  }

  private void updateFromConfigFiles() throws IOException {
    for (String fname : configFiles) {
      updateFromConfigFile(fname);
    }
  }

  protected void update(Config other) {
    // What a bunch of nonsense this is. The basic idea is that I want to have
    // the defaults in the object and have those be able to be overridden by
    // config files or command line options. This method updates an
    // Config
    // object for each value that doesn't equal the default form the other option.
    // Maybe I should have made everything null by default to avoid all of these
    // comparisons. But whatever, this works fine and it makes setting the defaults
    // in the object very natural.
    //
    // The only problem is that you can't reset something to the default after it's been
    // changed in another config file.
    Config defaults = new Config();

    if (!other.environment.equals(defaults.environment)) {
      environment = other.environment;
    }

    if (!other.zookeepers.equals(defaults.zookeepers)) {
      zookeepers = other.zookeepers;
    }

    if (!other.accumuloInstance.equals(defaults.accumuloInstance)) {
      accumuloInstance = other.accumuloInstance;
    }

    if (!other.accumuloDataLoadJobsTable.equals(defaults.accumuloDataLoadJobsTable)) {
      accumuloDataLoadJobsTable = other.accumuloDataLoadJobsTable;
    }

    if (!other.accumuloAPITokensTable.equals(defaults.accumuloAPITokensTable)) {
      accumuloAPITokensTable = other.accumuloAPITokensTable;
    }

    if (!other.accumuloColumnEntitiesTable.equals(defaults.accumuloColumnEntitiesTable)) {
      accumuloColumnEntitiesTable = other.accumuloColumnEntitiesTable;
    }

    if (!other.accumuloCustomAnnotationsTable.equals(defaults.accumuloCustomAnnotationsTable)) {
      accumuloCustomAnnotationsTable = other.accumuloCustomAnnotationsTable;
    }

    if (!other.accumuloMetadataTable.equals(defaults.accumuloMetadataTable)) {
      accumuloMetadataTable = other.accumuloMetadataTable;
    }

    if (!other.accumuloSamplesTable.equals(defaults.accumuloSamplesTable)) {
      accumuloSamplesTable = other.accumuloSamplesTable;
    }

    if (!other.accumuloElementAliasesTable.equals(defaults.accumuloElementAliasesTable)) {
      accumuloElementAliasesTable = other.accumuloElementAliasesTable;
    }

    if (!other.accumuloColumnCountsTable.equals(defaults.accumuloColumnCountsTable)) {
      accumuloColumnCountsTable = other.accumuloColumnCountsTable;
    }

    if (!other.accumuloDatawaveRowsTable.equals(defaults.accumuloDatawaveRowsTable)) {
      accumuloDatawaveRowsTable = other.accumuloDatawaveRowsTable;
    }

    if (!other.accumuloIndexTable.equals(defaults.accumuloIndexTable)) {
      accumuloIndexTable = other.accumuloIndexTable;
    }

    if (!other.accumuloUser.equals(defaults.accumuloUser)) {
      accumuloUser = other.accumuloUser;
    }

    if (!other.accumuloPassword.equals(defaults.accumuloPassword)) {
      accumuloPassword = other.accumuloPassword;
    }

    if (!other.accumuloScannerThreads.equals(defaults.accumuloScannerThreads)) {
      accumuloScannerThreads = other.accumuloScannerThreads;
    }

    if (!other.analyticsApiSiteId.equals(defaults.analyticsApiSiteId)) {
      analyticsApiSiteId = other.analyticsApiSiteId;
    }

    if (!other.analyticsUiSiteId.equals(defaults.analyticsUiSiteId)) {
      analyticsUiSiteId = other.analyticsUiSiteId;
    }

    if (!other.rulesOfUseApiPath.equals(defaults.rulesOfUseApiPath)) {
      rulesOfUseApiPath = other.rulesOfUseApiPath;
    }

    if (!other.rulesOfUseApiKey.equals(defaults.rulesOfUseApiKey)) {
      rulesOfUseApiKey = other.rulesOfUseApiKey;
    }

    if (!other.matomoApiKey.equals(defaults.matomoApiKey)) {
      matomoApiKey = other.matomoApiKey;
    }

    if (!other.numSamples.equals(defaults.numSamples)) {
      numSamples = other.numSamples;
    }

    if (!other.threadPoolSize.equals(defaults.threadPoolSize)) {
      threadPoolSize = other.threadPoolSize;
    }

    if (!other.backgroundYarnQueue.equals((defaults.backgroundYarnQueue))) {
      backgroundYarnQueue = other.backgroundYarnQueue;
    }

    if (!other.defaultYarnQueue.equals((defaults.defaultYarnQueue))) {
      defaultYarnQueue = other.defaultYarnQueue;
    }

    if (!other.interactiveYarnQueue.equals((defaults.interactiveYarnQueue))) {
      interactiveYarnQueue = other.interactiveYarnQueue;
    }

    if (!other.uploadYarnQueue.equals((defaults.uploadYarnQueue))) {
      uploadYarnQueue = other.uploadYarnQueue;
    }

    if (!other.downloadYarnQueue.equals((defaults.downloadYarnQueue))) {
      downloadYarnQueue = other.downloadYarnQueue;
    }

    if (other.runSparkLocally != defaults.runSparkLocally) {
      runSparkLocally = other.runSparkLocally;
    }

    if (other.loadType != defaults.loadType) {
      loadType = other.loadType;
    }

    if (!other.loadOutputDest.equals(defaults.loadOutputDest)) {
      loadOutputDest = other.loadOutputDest;
    }

    if (!other.runSparkFromPlayFramework.equals(defaults.runSparkFromPlayFramework)) {
      runSparkFromPlayFramework = other.runSparkFromPlayFramework;
    }

    if (!other.sparkIngestBaseDir.equals(defaults.sparkIngestBaseDir)) {
      sparkIngestBaseDir = other.sparkIngestBaseDir;
    }

    if (!other.s3Bucket.equals(defaults.s3Bucket)) {
      s3Bucket = other.s3Bucket;
    }

    if (!other.hadoopNamenode1.equals(defaults.hadoopNamenode1)) {
      hadoopNamenode1 = other.hadoopNamenode1;
    }

    if (!other.hadoopNamenode2.equals(defaults.hadoopNamenode2)) {
      hadoopNamenode2 = other.hadoopNamenode2;
    }

    if (!other.hadoopDefaultFs.equals(defaults.hadoopDefaultFs)) {
      hadoopDefaultFs = other.hadoopDefaultFs;
    }

    if (!other.jobsApiUrl.equals(defaults.jobsApiUrl)) {
      jobsApiUrl = other.jobsApiUrl;
    }

    if (!other.tableMapperApiPath.equals(defaults.tableMapperApiPath)) {
      tableMapperApiPath = other.tableMapperApiPath;
    }

    if (!other.userFacingApiHttpPath.equals(defaults.userFacingApiHttpPath)) {
      userFacingApiHttpPath = other.userFacingApiHttpPath;
    }

    if (!other.downloadEventListenerUrl.equals(defaults.downloadEventListenerUrl)) {
      downloadEventListenerUrl = other.downloadEventListenerUrl;
    }

    if (!other.statsdHost.equals(defaults.statsdHost)) {
      statsdHost = other.statsdHost;
    }

    if (!other.statsdPort.equals(defaults.statsdPort)) {
      statsdPort = other.statsdPort;
    }

    if (!other.rowVisibilityColumnName.equals(defaults.rowVisibilityColumnName)) {
      rowVisibilityColumnName = other.rowVisibilityColumnName;
    }

    if (!other.dataProfilerToolsJar.equals(defaults.dataProfilerToolsJar)) {
      dataProfilerToolsJar = other.dataProfilerToolsJar;
    }

    if (!other.sqlsyncUrl.equals(defaults.sqlsyncUrl)) {
      sqlsyncUrl = other.sqlsyncUrl;
    }

    if (!other.sqlsyncUser.equals(defaults.sqlsyncUser)) {
      sqlsyncUser = other.sqlsyncUser;
    }

    if (!other.sqlsyncPassword.equals(defaults.sqlsyncPassword)) {
      sqlsyncPassword = other.sqlsyncPassword;
    }

    if (!other.sqlsyncEventListenerUrl.equals(defaults.sqlsyncEventListenerUrl)) {
      sqlsyncEventListenerUrl = other.sqlsyncEventListenerUrl;
    }

    if (!other.postgresUrl.equals(defaults.postgresUrl)) {
      postgresUrl = other.postgresUrl;
    }

    if (!other.postgresUsername.equals(defaults.postgresUsername)) {
      postgresUsername = other.postgresUsername;
    }

    if (!other.postgresPassword.equals(defaults.postgresPassword)) {
      postgresPassword = other.postgresPassword;
    }

    if (!other.datasetperformanceEventListenerUrl.equals(
        defaults.datasetperformanceEventListenerUrl)) {
      datasetperformanceEventListenerUrl = other.datasetperformanceEventListenerUrl;
    }

    if (!other.datasetdeltaEventListenerUrl.equals(defaults.datasetdeltaEventListenerUrl)) {
      datasetdeltaEventListenerUrl = other.datasetdeltaEventListenerUrl;
    }

    if (!other.datasetqualityEventListenerUrl.equals(defaults.datasetqualityEventListenerUrl)) {
      datasetqualityEventListenerUrl = other.datasetqualityEventListenerUrl;
    }

    if (other.sparkAllowMultipleContexts != defaults.sparkAllowMultipleContexts) {
      sparkAllowMultipleContexts = other.sparkAllowMultipleContexts;
    }

    if (other.allowAccumuloConnection != defaults.allowAccumuloConnection) {
      allowAccumuloConnection = other.allowAccumuloConnection;
    }

    if (!other.cancellerEventListenerUrl.equals(defaults.cancellerEventListenerUrl)) {
      cancellerEventListenerUrl = other.cancellerEventListenerUrl;
    }

    if (!other.connectionEngineEventListenerUrl.equals(defaults.connectionEngineEventListenerUrl)) {
      connectionEngineEventListenerUrl = other.connectionEngineEventListenerUrl;
    }

    if (!other.slackWebhook.equals(defaults.slackWebhook)) {
      slackWebhook = other.slackWebhook;
    }
  }

  public boolean parse(String[] argv) throws IOException {

    try {
      loadFromEnvironmentVariables();
    } catch (Exception e) {
      logger.warn(e.toString());
      System.err.println(e);
      printHelp();
      return false;
    }
    updateFromConfigFiles();

    JCommander parser = new JCommander(this);
    try {
      logger.debug(format("parsing %s arguments", argv.length));
      parser.parse(argv);
    } catch (Exception e) {
      logger.warn(e.toString());
      System.err.println(e);
      printHelp();
      return false;
    }

    if (help) {
      printHelp();
      return false;
    }

    return true;
  }

  public void writeUserConfig() throws IOException {
    String dirname = "~/.dataprofiler";
    Path d = Paths.get(dirname.replaceFirst("^~", System.getProperty("user.home"))).normalize();
    if (!Files.exists(d)) {
      Files.createDirectory(d);
    }

    Path p = Paths.get(d.toString(), "config");

    ObjectMapper mapper = new ObjectMapper();

    Files.write(p, mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(this));
  }

  public Set<String> configPropNames() {
    Field[] fields = this.getClass().getFields();
    return Stream.of(fields).map(field -> field.getName()).collect(toSet());
  }

  public void loadFromEnvironmentVariables() throws NoSuchFieldException, IllegalAccessException {
    System.out.println("loading config from environment variables");
    Set<String> filterSet = configPropNames();
    Map<String, String> env = System.getenv();
    for (String envName : env.keySet()) {
      String value = env.get(envName);
      String name = snakeToCamelCase(envName.toLowerCase());
      if (filterSet.contains(name)) {
        setValueWithReflection(name, value);
        logger.info("--" + name + "=" + value);
      }
    }
  }

  public Map<String, Object> asKeyValues() throws IllegalAccessException {
    Map<String, Object> map = new HashMap<>();
    Field[] fields = this.getClass().getFields();
    for (Field field : fields) {
      String name = field.getName();
      Object object = field.get(this);
      map.put(name, object);
    }
    return map;
  }

  public String snakeToCamelCase(String val) {
    if (isNull(val) || !val.contains("_")) {
      return val;
    }

    StringBuilder stringBuilder = new StringBuilder(val.length());
    String[] splits = val.split("_");
    for (int i = 0; i < splits.length; i++) {
      String split = splits[i];
      if (split.isEmpty()) {
        continue;
      }
      stringBuilder
          .append(i == 0 ? split.charAt(0) : toUpperCase(split.charAt(0)))
          .append(split.substring(1));
    }
    return stringBuilder.toString();
  }

  public String camelToSnakeCase(String val) {
    if (isNull(val) || val.isEmpty()) {
      return val;
    }

    char[] chars = val.toCharArray();
    StringBuilder stringBuilder = new StringBuilder(val.length() * 2);
    for (int i = 0; i < chars.length; i++) {
      char current = chars[i];
      if (isUpperCase(current)) {
        stringBuilder.append("_").append(toLowerCase(current));
      } else {
        stringBuilder.append(current);
      }
    }
    return stringBuilder.toString();
  }

  protected void setValueWithReflection(String name, String value)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = this.getClass().getField(name);
    field.setAccessible(true);
    field.set(this, value);
  }

  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }
}
