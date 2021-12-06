package com.dataprofiler.datasetperformance;

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
import static java.util.stream.Collectors.toMap;

import com.dataprofiler.datasetperformance.model.DatasetPerformance;
import com.dataprofiler.datasetperformance.model.status.PerformanceTaskStatus;
import com.dataprofiler.provider.DatasetMetadataProvider;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.objects.VersionedDatasetMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 1 task/thread per dataset */
public class PerformanceSaveTask implements Callable<PerformanceTaskStatus> {
  private static final Logger logger = LoggerFactory.getLogger(PerformanceSaveTask.class);

  private final DatasetMetadataProvider datasetMetadataProvider;
  private final ObjectMapper objectMapper;
  private final DatasetPerformance datasetPerformance;

  public PerformanceSaveTask(
      DatasetMetadataProvider datasetMetadataProvider,
      ObjectMapper objectMapper,
      DatasetPerformance datasetPerformance) {
    this.datasetMetadataProvider = datasetMetadataProvider;
    this.objectMapper = objectMapper;
    this.datasetPerformance = datasetPerformance;
  }

  @Override
  public PerformanceTaskStatus call() throws DatasetPerformanceException {
    return execute(datasetPerformance);
  }

  protected PerformanceTaskStatus execute(DatasetPerformance datasetPerformance)
      throws DatasetPerformanceException {
    if (isNull(datasetPerformance) || isNull(datasetPerformance.getDataset())) {
      return new PerformanceTaskStatus().setSuccess(false);
    }

    Instant start = now();
    String dataset = datasetPerformance.getDataset();
    VersionedDatasetMetadata metadata = datasetMetadataProvider.fetchDatasetMetadata(dataset);
    if (logger.isDebugEnabled()) {
      logger.debug(format("saving dataset: %s performance: %s", dataset, datasetPerformance));
    }

    // write data to dataset properties
    boolean success = updateDatasetProperties(datasetPerformance, metadata);

    Instant end = now();
    Duration duration = between(start, end);
    if (logger.isInfoEnabled()) {
      logger.info(
          format("all performance metrics updated for dataset: %s time: %s", dataset, duration));
    }

    PerformanceTaskStatus status =
        new PerformanceTaskStatus()
            .setSuccess(success)
            .setDataset(dataset)
            .setDuration(duration)
            .setNumChartDays(datasetPerformance.getChartData().size())
            .setNumUsersWithAttribute(datasetPerformance.getNumUsersWithAttribute())
            .setAllTimeSearchTrendStat(datasetPerformance.getSearchAppearanceData().getAllTime());
    return status;
  }

  protected boolean updateDatasetProperties(
      DatasetPerformance performance, VersionedDatasetMetadata metadata)
      throws DatasetPerformanceException {
    try {
      Map<String, String> properties =
          metadata.getDatasetMetadata().getProperties().entrySet().stream()
              .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
      String json = objectMapper.writeValueAsString(performance);
      properties.put("performance", json);
      // write dataset properties to accumulo
      datasetMetadataProvider.addPropertiesToMetadata(metadata.getDatasetMetadata(), properties);
    } catch (IOException | BasicAccumuloException jpe) {
      throw new DatasetPerformanceException(jpe);
    }
    return true;
  }
}
