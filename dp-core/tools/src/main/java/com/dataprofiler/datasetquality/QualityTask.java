package com.dataprofiler.datasetquality;

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

import com.dataprofiler.datasetquality.model.CompleteQuality;
import com.dataprofiler.datasetquality.model.status.QualityTaskStatus;
import com.dataprofiler.datasetquality.provider.DatasetQualityProvider;
import com.dataprofiler.provider.DatasetMetadataProvider;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.objects.VersionedDatasetMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 1 task/thread per dataset */
public class QualityTask implements Callable<QualityTaskStatus> {
  private static final Logger logger = LoggerFactory.getLogger(QualityTask.class);
  private static final String QUALITY_PROPS_KEY = "quality";

  private final String dataset;
  private final DatasetMetadataProvider datasetMetadataProvider;
  private final DatasetQualityProvider datasetQualityProvider;
  private final ObjectMapper objectMapper;

  public QualityTask(
      DatasetMetadataProvider datasetMetadataProvider,
      DatasetQualityProvider datasetQualityProvider,
      ObjectMapper objectMapper,
      String dataset) {
    this.datasetMetadataProvider = datasetMetadataProvider;
    this.datasetQualityProvider = datasetQualityProvider;
    this.objectMapper = objectMapper;
    this.dataset = dataset;
  }

  /**
   * return true if this callable executed successfully
   *
   * @return Boolean true if call was successful, otherwise false
   * @throws DatasetQualityException wraps many checked exceptions
   */
  @Override
  public QualityTaskStatus call() throws DatasetQualityException {
    return execute(dataset);
  }

  protected QualityTaskStatus execute(String dataset) throws DatasetQualityException {
    if (isNull(dataset)) {
      return new QualityTaskStatus().setSuccess(false);
    }

    Instant start = now();
    CompleteQuality completeQuality = datasetQualityProvider.calculateQuality(dataset);
    VersionedDatasetMetadata metadata = datasetMetadataProvider.fetchDatasetMetadata(dataset);
    completeQuality = datasetQualityProvider.populateMetadata(completeQuality, metadata);

    if (logger.isDebugEnabled()) {
      logger.debug(format("writing dataset: %s, quality: %s", dataset, completeQuality));
    }

    // write to dataset properties
    boolean success = this.updateProperties(completeQuality, metadata);

    Instant end = now();
    Duration duration = between(start, end);
    if (logger.isInfoEnabled()) {
      logger.info(
          format(
              "dataset quality updated for dataset: %s with %s quality(ies) time: %s",
              dataset, completeQuality.getQualities().size(), duration));
    }

    return new QualityTaskStatus()
        .setSuccess(success)
        .setDataset(dataset)
        .setDuration(duration)
        .setQualitiesUpdated(completeQuality.getQualities().size());
  }

  /**
   * overwrites datasets quality properties in accumulo
   *
   * @param completeQuality CompleteQuality
   * @param metadata VersionedDatasetMetadata latest metadata
   * @return true if success, including noop success
   * @throws DatasetQualityException wraps many checked exceptions
   */
  protected boolean updateProperties(
      CompleteQuality completeQuality, VersionedDatasetMetadata metadata)
      throws DatasetQualityException {
    try {
      if (isNull(metadata) || isNull(completeQuality)) {
        throw new IllegalArgumentException(
            "metadata or quality is null, cannot update properties!");
      }

      String json = objectMapper.writeValueAsString(completeQuality);
      Map<String, String> properties = new HashMap<>();
      properties.put(QUALITY_PROPS_KEY, json);
      // write dataset properties to accumulo
      datasetMetadataProvider.addPropertiesToMetadata(metadata.getDatasetMetadata(), properties);
    } catch (IOException | BasicAccumuloException e) {
      throw new DatasetQualityException(e);
    }
    return true;
  }
}
