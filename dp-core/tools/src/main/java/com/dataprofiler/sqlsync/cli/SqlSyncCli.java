package com.dataprofiler.sqlsync.cli;

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

import static java.lang.String.format;
import static java.time.Duration.between;
import static java.time.Instant.now;

import com.dataprofiler.DPSparkContext;
import com.dataprofiler.sqlsync.SqlSyncException;
import com.dataprofiler.sqlsync.config.SqlSyncConfig;
import com.dataprofiler.sqlsync.destination.CsvRowDestination;
import com.dataprofiler.sqlsync.destination.JdbcRowDestination;
import com.dataprofiler.sqlsync.destination.JsonRowDestination;
import com.dataprofiler.sqlsync.destination.RowDestination;
import com.dataprofiler.sqlsync.provider.ExportTypedTableProvider;
import com.dataprofiler.sqlsync.spec.JdbcConnection;
import com.dataprofiler.sqlsync.spec.MultiSqlSyncSpec;
import com.dataprofiler.sqlsync.spec.SqlSyncSpec;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.objects.VersionedDatasetMetadata.MissingMetadataException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.log4j.Logger;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class SqlSyncCli {

  private static final Logger logger = Logger.getLogger(SqlSyncCli.class);

  private SqlSyncConfig config;
  private DPSparkContext context;
  private String outputPath;
  private RowDestination rowSink;

  public static void main(String[] args) {
    try {
      SqlSyncCli exporter = new SqlSyncCli();
      exporter.execute(args);
    } catch (SqlSyncException e) {
      e.printStackTrace();
      logger.warn(e);
      System.exit(1);
    }
    System.exit(0);
  }

  public void execute(String[] args) throws SqlSyncException {
    try {
      logger.debug("initializing...");
      config = parseJobArgs(args);
      MultiSqlSyncSpec spec = readSpec(config);
      initializeRowDestination(config, spec);
      context =
          new DPSparkContext(config, visibilitiesToAuths(spec.getVisibilities()), "Sql Sync Job");
      outputPath = context.getConfig().parameters.get(0);
      if (logger.isInfoEnabled()) {
        logger.info("context app name: " + context.getAppName());
        logger.info("context auths: " + context.getAuthorizations());
        logger.info("context spark config: " + context.getSparkConfig().toDebugString());
        logger.info("spark output path: " + outputPath);
      }
      executeMultiSpec(spec);
    } catch (IOException | BasicAccumuloException e) {
      throw new SqlSyncException(e);
    }
  }

  protected void executeMultiSpec(MultiSqlSyncSpec spec) throws SqlSyncException {
    if (!checkInitialized()) {
      throw new IllegalStateException();
    }
    int index = -1;
    Authorizations auths = visibilitiesToAuths(spec.getVisibilities());
    for (SqlSyncSpec sqlSyncSpec : spec.getDownloads()) {
      // We are going to use the index as an id for the files output and we expect this to be
      // interpreted by external tools (order in JSON arrays is supposed to be preserved, so this
      // should be safe). But in order for this to make sense, we are going to use the position
      // in the JSON array, so we increment this way so that the indexes are used even if we
      // hit a continue later down because the dataset is empty or something.
      index++;
      executeSpec(sqlSyncSpec, auths, index);
    }
  }

  protected Authorizations visibilitiesToAuths(Collection<String> visibilities) {
    String[] auths = visibilities.toArray(new String[0]);
    return new Authorizations(auths);
  }

  protected void executeSpec(SqlSyncSpec spec, Authorizations auths) throws SqlSyncException {
    executeSpec(spec, auths, 0);
  }

  protected void executeSpec(SqlSyncSpec spec, Authorizations auths, int index)
      throws SqlSyncException {
    if (!checkInitialized()) {
      throw new IllegalStateException();
    }
    try {
      Instant start = now();
      logger.info("loading spec: " + spec.toString());
      SparkSession sparkSession = context.createSparkSession();
      ExportTypedTableProvider exporter = new ExportTypedTableProvider();
      Dataset<Row> rows = exporter.exportRows(context, sparkSession, spec, auths);
      if (rows == null) {
        logger.warn("Dataset was empty - skipping: " + spec);
        return;
      }

      rowSink.printHead(rows);
      if ("jdbc".equals(config.outMode)) {
        rowSink.write(rows, format("%s-%s", outputPath, index));
      } else {
        String path = format("%s/%s", outputPath, index);
        rowSink.write(rows, path);
      }

      Instant end = now();
      if (logger.isInfoEnabled()) {
        Duration duration = between(start, end);
        logger.info(
            format(
                "executed spec: %s %s total time: %s",
                spec.getDataset(), spec.getTable(), duration));
      }
    } catch (BasicAccumuloException | MissingMetadataException | IOException e) {
      throw new SqlSyncException(e);
    }
  }

  protected MultiSqlSyncSpec readSpec(SqlSyncConfig config) throws IOException {
    logger.info("fname:" + config.fname);
    logger.info("output mode:" + config.outMode);
    MultiSqlSyncSpec spec = null;
    if (config.fname != null) {
      String json = new String(Files.readAllBytes(Paths.get(config.fname)));
      spec = MultiSqlSyncSpec.fromJson(json);
    }
    logger.info("loaded spec:" + spec);
    return spec;
  }

  protected SqlSyncConfig parseJobArgs(String[] args) throws IOException, SqlSyncException {
    logger.debug("creating and verifying job config");
    SqlSyncConfig config = new SqlSyncConfig();
    if (!config.parse(args)) {
      String err =
          "Usage: <main_class> [options] --job-id jobId | --fname "
              + "multi-sql-sync-spec.json | --output-mode [csv,json,jdbc] <output_path|dest table>";
      throw new SqlSyncException(err);
    }
    logger.debug("loaded config: " + config);
    return config;
  }

  protected void initializeRowDestination(SqlSyncConfig config, MultiSqlSyncSpec sqlSyncSpec) {
    switch (config.outMode) {
      case "csv":
        rowSink = new CsvRowDestination(outputPath);
        break;
      case "json":
        rowSink = new JsonRowDestination(outputPath);
        break;
      case "jdbc":
        JdbcConnection connectionInfo = sqlSyncSpec.getJdbcConnection();
        rowSink =
            new JdbcRowDestination(
                connectionInfo.getUrl(),
                outputPath,
                connectionInfo.getUser(),
                connectionInfo.getPasswd());
        break;
      default:
        rowSink = new CsvRowDestination();
    }
  }

  protected boolean checkInitialized() {
    return !(Objects.isNull(config) && Objects.isNull(context) && Objects.isNull(outputPath));
  }
}
