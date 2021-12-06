package com.dataprofiler.datasetproperties.cli;

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
import static java.nio.file.Files.readAllBytes;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.dataprofiler.datasetproperties.config.DatasetPropertiesConfig;
import com.dataprofiler.datasetproperties.provider.OrphanedDeltaCommentProvider;
import com.dataprofiler.datasetproperties.spec.Action;
import com.dataprofiler.datasetproperties.spec.DatasetPropertiesSpec;
import com.dataprofiler.provider.DatasetMetadataProvider;
import com.dataprofiler.provider.DatasetPropertiesException;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.CustomAnnotation;
import com.dataprofiler.util.objects.VersionedDatasetMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasetPropertiesCli {
  private static final Logger logger = LoggerFactory.getLogger(DatasetPropertiesCli.class);

  private DatasetPropertiesConfig config;
  private DatasetPropertiesSpec spec;
  private Context context;
  private DatasetMetadataProvider datasetMetadataProvider;
  private ObjectMapper objectMapper;
  private OrphanedDeltaCommentProvider orphanedDeltaCommentProvider;

  public DatasetPropertiesCli() {
    super();
  }

  public static void main(String[] args) {
    try {
      DatasetPropertiesCli exporter = new DatasetPropertiesCli();
      exporter.execute(args);
    } catch (DatasetPropertiesException e) {
      e.printStackTrace();
      logger.warn(e.toString());
      System.exit(1);
    }
    System.exit(0);
  }

  public void init(String[] args) throws DatasetPropertiesException {
    try {
      logger.debug("initializing...");
      config = parseJobArgs(args);
      spec = readSpec(config);
      if (logger.isDebugEnabled()) {
        logger.debug("config: " + config);
        logger.debug("spec: " + spec.toString());
      }
      //      context = new DPSparkContext(config, "Dataset Delta Job");
      context = new Context(config);
      if (logger.isDebugEnabled()) {
        logger.debug("context: " + context);
      }
      objectMapper = new ObjectMapper();
      datasetMetadataProvider = new DatasetMetadataProvider();
      datasetMetadataProvider.setContext(context);
      datasetMetadataProvider.setObjectMapper(objectMapper);
      orphanedDeltaCommentProvider = new OrphanedDeltaCommentProvider();
      orphanedDeltaCommentProvider.setContext(context);
      orphanedDeltaCommentProvider.setDatasetMetadataProvider(datasetMetadataProvider);
    } catch (Exception e) {
      throw new DatasetPropertiesException(e);
    }
  }

  public void execute(String[] args) throws DatasetPropertiesException {
    init(args);
    executeSpec(spec);
  }

  protected void executeSpec(DatasetPropertiesSpec spec) throws DatasetPropertiesException {
    if (!checkInitialized()) {
      throw new IllegalStateException();
    }
    Set<Action> actions = spec.getActions();
    if (isNull(actions) || actions.isEmpty()) {
      throw new IllegalArgumentException("no actions supplied");
    }

    Instant start = now();
    Set<String> datasets =
        spec.isAllDatasets() ? datasetMetadataProvider.fetchAllDatasets() : spec.getDatasets();
    for (String dataset : datasets) {
      VersionedDatasetMetadata versionedDatasetMetadata =
          datasetMetadataProvider.fetchDatasetMetadata(dataset);
      if (isNull(versionedDatasetMetadata)) {
        logger.warn(
            format("dataset: %s had a null versioned dataset metadata, skipping...", dataset));
        continue;
      }
      executeDataset(versionedDatasetMetadata, actions);
    }
    Instant end = now();
    if (logger.isInfoEnabled()) {
      Duration duration = between(start, end);
      logger.info(format("spec executed time: %s", duration));
    }
  }

  protected void executeDataset(
      VersionedDatasetMetadata versionedDatasetMetadata, Set<Action> actions)
      throws DatasetPropertiesException {
    for (Action action : actions) {
      executeAction(versionedDatasetMetadata, action);
    }
  }

  protected void executeAction(VersionedDatasetMetadata versionedDatasetMetadata, Action action)
      throws DatasetPropertiesException {
    Instant start = now();
    try {
      switch (action) {
        case WIPE_SYSTEM_COMMENTS:
          String dataset = versionedDatasetMetadata.getDatasetMetadata().getDataset_name();
          Set<CustomAnnotation> systemComments =
              orphanedDeltaCommentProvider.fetchDatasetSystemComments(dataset);
          orphanedDeltaCommentProvider.cleanupComments(systemComments);
          break;
        case WIPE_DELTA:
          datasetMetadataProvider.wipeDeltaProperties(versionedDatasetMetadata);
          break;
        case WIPE_QUALITY:
          //        datasetMetadataProvider.updateQualityProperties(versionedDatasetMetadata);
          //        break;
          throw new UnsupportedOperationException(action + " not implemented/tested");
        case WIPE_PERFORMANCE:
          //        datasetMetadataProvider.updatePerformanceProperties(versionedDatasetMetadata);
          //        break;
          throw new UnsupportedOperationException(action + " not implemented/tested");
        case PRUNE_DELTA_COMMENTS:
          orphanedDeltaCommentProvider.deleteOrphanedDeltaComments(
              versionedDatasetMetadata, spec.isDryRun());
          break;
        case PRUNE_SYSTEM_COMMENTS:
          orphanedDeltaCommentProvider.deleteOrphanedSystemComments(
              versionedDatasetMetadata, spec.isDryRun());
          break;
        default:
          break;
      }

    } catch (Exception e) {
      throw new DatasetPropertiesException(e);
    }
    Instant end = now();
    if (logger.isInfoEnabled()) {
      Duration duration = between(start, end);
      logger.info(
          format(
              "dataset: %s action: %s time: %s",
              versionedDatasetMetadata.getDatasetMetadata().getDataset_name(), action, duration));
    }
  }

  protected DatasetPropertiesSpec readSpec(DatasetPropertiesConfig config) throws IOException {
    logger.info("fname:" + config.fname);
    DatasetPropertiesSpec spec = null;
    if (config.fname != null) {
      String json = new String(readAllBytes(Paths.get(config.fname)));
      spec = DatasetPropertiesSpec.fromJson(json);
    }
    logger.info("loaded spec:" + spec);
    return spec;
  }

  protected DatasetPropertiesConfig parseJobArgs(String[] args)
      throws IOException, DatasetPropertiesException {
    logger.debug("creating and verifying job config");
    DatasetPropertiesConfig config = new DatasetPropertiesConfig();
    if (!config.parse(args)) {
      String err = "Usage: <main_class> [options] --fname <spec>";
      throw new DatasetPropertiesException(err);
    }
    logger.debug("loaded config: " + config);
    return config;
  }

  protected boolean checkInitialized() {
    return nonNull(config)
        && nonNull(spec)
        && nonNull(objectMapper)
        && nonNull(context)
        && nonNull(datasetMetadataProvider)
        && nonNull(datasetMetadataProvider.getContext())
        && nonNull(datasetMetadataProvider.getObjectMapper())
        && nonNull(orphanedDeltaCommentProvider)
        && nonNull(orphanedDeltaCommentProvider.getContext())
        && nonNull(orphanedDeltaCommentProvider.getDatasetMetadataProvider())
        && (spec.isAllDatasets() || nonNull(spec.getDatasets()))
        && nonNull(spec.getActions())
        && !spec.getActions().isEmpty();
  }
}
