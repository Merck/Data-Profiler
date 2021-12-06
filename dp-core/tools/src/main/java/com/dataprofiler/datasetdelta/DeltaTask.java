package com.dataprofiler.datasetdelta;

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
import static java.util.Objects.isNull;

import com.dataprofiler.datasetdelta.model.CompleteDelta;
import com.dataprofiler.datasetdelta.model.DatasetDelta;
import com.dataprofiler.datasetdelta.model.DatasetVersion;
import com.dataprofiler.datasetdelta.model.DeltaEnum;
import com.dataprofiler.datasetdelta.model.TableVersion;
import com.dataprofiler.datasetdelta.model.status.DeltaTaskStatus;
import com.dataprofiler.datasetdelta.provider.DatasetDeltaProvider;
import com.dataprofiler.provider.DatasetMetadataProvider;
import com.dataprofiler.provider.DatasetPropertiesException;
import com.dataprofiler.util.objects.VersionedDatasetMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 1 task/thread per dataset */
public class DeltaTask implements Callable<DeltaTaskStatus> {
  private static final Logger logger = LoggerFactory.getLogger(DeltaTask.class);

  private final String dataset;
  private final DatasetDeltaProvider datasetDeltaProvider;
  private final DatasetMetadataProvider datasetMetadataProvider;
  private final ObjectMapper objectMapper;

  public DeltaTask(
      DatasetDeltaProvider datasetDeltaProvider,
      DatasetMetadataProvider datasetMetadataProvider,
      ObjectMapper objectMapper,
      String dataset) {
    this.datasetDeltaProvider = datasetDeltaProvider;
    this.datasetMetadataProvider = datasetMetadataProvider;
    this.objectMapper = objectMapper;
    this.dataset = dataset;
  }

  /**
   * return true if this callable executed successfully
   *
   * @return
   * @throws DatasetDeltaException
   */
  @Override
  public DeltaTaskStatus call() throws DatasetDeltaException {
    return executeDataset(dataset);
  }

  protected DeltaTaskStatus executeDataset(String dataset) throws DatasetDeltaException {
    if (isNull(dataset)) {
      return new DeltaTaskStatus().setSuccess(false);
    }

    if (logger.isDebugEnabled()) {
      logger.debug(format("building performance numbers for %s", dataset));
    }

    Instant start = now();

    CompleteDelta delta = datasetDeltaProvider.calculateRecentDelta(dataset);
    if (logger.isDebugEnabled()) {
      logger.debug(delta.getDeltas().toString());
    }

    // note: we have to calculate latest versions before merging with old deltas
    // we use the latest round of deltas to update the latest version numbers
    delta = updateLatestVersions(delta, dataset);

    boolean success = false;
    try {
      // write data to dataset properties
      VersionedDatasetMetadata metadata = datasetMetadataProvider.fetchDatasetMetadata(dataset);
      delta = datasetMetadataProvider.mergeDeltaProps(delta, metadata);
      Set<DatasetDelta> linkedDeltas =
          datasetDeltaProvider.getCommentProvider().saveDeltaComments(delta.getDeltas());
      delta.setDeltas(linkedDeltas);

      success = datasetMetadataProvider.replaceDeltaProperties(delta, metadata);
    } catch (IOException | DatasetPropertiesException e) {
      throw new DatasetDeltaException(e);
    }

    Instant end = now();
    Duration duration = between(start, end);
    if (logger.isInfoEnabled()) {
      logger.info(
          format(
              "%s dataset deltas updated for %s time: %s",
              delta.getDeltas().size(), dataset, duration));
    }

    DeltaTaskStatus status =
        new DeltaTaskStatus()
            .setSuccess(success)
            .setDataset(dataset)
            .setDeltasUpdated(delta.getDeltas().size())
            .setDuration(duration);
    return status;
  }

  /**
   * Warning: Note: this method assume the deltas passed in are only ones from the last run with the
   * latest version ids
   *
   * @param completeDelta
   * @param dataset
   */
  protected CompleteDelta updateLatestVersions(CompleteDelta completeDelta, String dataset) {
    if (isNull(completeDelta)) {
      return null;
    }

    Set<DatasetDelta> deltas = completeDelta.getDeltas();
    DatasetVersion datasetVersion = completeDelta.getLastKnownVersions();
    if (isNull(datasetVersion)) {
      datasetVersion = new DatasetVersion();
    }
    datasetVersion.setDataset(dataset);
    Map<String, TableVersion> tableMap = datasetVersion.getTables();
    if (isNull(tableMap)) {
      tableMap = new HashMap<>();
    }

    for (DatasetDelta delta : deltas) {
      if (logger.isTraceEnabled()) {
        logger.trace(format("update version for delta: %s", delta));
      }
      DeltaEnum currentEnum = delta.getDeltaEnum();
      String currentDataset = delta.getDataset();
      if (!currentDataset.equals(dataset)) {
        // we only care about one dataset at the moment
        if (logger.isInfoEnabled()) {
          logger.info(
              format(
                  "skipping enum... found dataset: %s but expected dataset: %s",
                  currentDataset, dataset));
        }
        continue;
      }

      if (currentEnum.isDatasetEnum()) {
        datasetVersion.setDataset(delta.getDataset());
        datasetVersion.setVersion(delta.getTargetVersion());
      } else if (currentEnum.isTableEnum()) {
        String table = currentEnum.isAddedEnum() ? delta.getValueTo() : delta.getValueFrom();
        TableVersion tableVersion = tableMap.getOrDefault(table, new TableVersion());
        tableVersion.setTable(table);
        tableVersion.setVersion(delta.getTargetVersion());
        tableMap.put(table, tableVersion);
        if (logger.isTraceEnabled()) {
          logger.trace(format("current tableMap: %s", tableMap));
        }
      } else if (currentEnum.isColumnEnum()) {
        String table = delta.getTable();
        String column = currentEnum.isAddedEnum() ? delta.getValueTo() : delta.getValueFrom();
        TableVersion tableVersion = tableMap.getOrDefault(table, new TableVersion());
        Map<String, String> columns = tableVersion.getColumns();
        columns.put(column, delta.getTargetVersion());
        tableVersion.setColumns(columns);
        tableMap.put(table, tableVersion);
        if (logger.isTraceEnabled()) {
          logger.trace(format("current tableMap: %s", tableMap));
        }
      } else {
        if (logger.isInfoEnabled()) {
          logger.info(
              format(
                  "unknown enum %s for dataset: %s. skipping this element for the version lookup cache",
                  currentEnum, currentDataset));
        }
      }
    }

    datasetVersion.setTables(tableMap);
    completeDelta.setLastKnownVersions(datasetVersion);
    if (logger.isDebugEnabled()) {
      logger.debug(format("complete delta: %s", completeDelta));
    }
    return completeDelta;
  }
}
