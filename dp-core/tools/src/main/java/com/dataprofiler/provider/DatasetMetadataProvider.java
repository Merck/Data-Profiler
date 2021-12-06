package com.dataprofiler.provider;

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

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;

import com.dataprofiler.datasetdelta.model.CompleteDelta;
import com.dataprofiler.datasetperformance.model.DatasetPerformance;
import com.dataprofiler.datasetquality.model.CompleteQuality;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.MetadataVersionObject;
import com.dataprofiler.util.objects.ObjectScannerIterable;
import com.dataprofiler.util.objects.VersionedDatasetMetadata;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** provider gives helper methods to work with dataset metadata */
public class DatasetMetadataProvider extends VersionLineageService {
  private static final Logger logger = LoggerFactory.getLogger(DatasetMetadataProvider.class);

  private static final String DELTA_PROPS_KEY = "delta";
  private static final String QUALITY_PROPS_KEY = "quality";
  private static final String PERFORMANCE_PROPS_KEY = "performance";

  protected Context context;
  protected ObjectMapper objectMapper;

  public DatasetMetadataProvider() {
    super();
  }

  public DatasetMetadataProvider(Context context) {
    this();
    this.context = context;
  }

  public Set<String> fetchAllDatasets() {
    Instant start = now();
    MetadataVersionObject version = context.getCurrentMetadataVersion();
    Set<String> datasets =
        stream(new VersionedMetadataObject().scanDatasets(context, version).spliterator(), false)
            .map(VersionedMetadataObject::getDataset_name)
            .collect(toSet());
    Instant end = now();
    if (logger.isDebugEnabled()) {
      Duration duration = between(start, end);
      logger.debug(format("fetched %s dataset names time: %s", datasets.size(), duration));
    }
    return datasets;
  }

  public List<VersionedMetadataObject> fetchVersionMetadataForAllDatasets() {
    MetadataVersionObject version = context.getCurrentMetadataVersion();
    return fetchVersionMetadataForAllDatasets(version);
  }

  public List<VersionedMetadataObject> fetchVersionMetadataForAllDatasets(
      MetadataVersionObject version) {
    Instant start = now();
    List<VersionedMetadataObject> datasets =
        stream(new VersionedMetadataObject().scanDatasets(context, version).spliterator(), false)
            .collect(toList());
    Instant end = now();
    if (logger.isDebugEnabled()) {
      Duration duration = between(start, end);
      logger.debug(
          format(
              "fetched %s dataset version objects version: %s, time: %s",
              datasets.size(), version, duration));
    }
    return datasets;
  }

  public VersionedDatasetMetadata fetchDatasetMetadata(String dataset) {
    return new VersionedMetadataObject().allMetadataForDataset(context, dataset);
  }

  public VersionedDatasetMetadata fetchDatasetMetadata(
      String dataset, MetadataVersionObject version) {
    return new VersionedMetadataObject().allMetadataForDataset(context, version, dataset);
  }

  /**
   * fetch an ordered list of global metadata version (represents all datasets, tables, and columns)
   *
   * @return
   */
  public List<MetadataVersionObject> fetchGlobalVersionLineage() {
    return new MetadataVersionObject().allMetadataVersions(context);
  }

  /**
   * fetch an ordered list of metadata version of the given dataset
   *
   * @see MetadataVersionObject#allMetadataVersions
   * @param dataset
   * @param maxVersions
   * @return List<VersionedMetadataObject>
   */
  public List<VersionedMetadataObject> fetchDatasetVersionLineage(
      String dataset, Integer maxVersions) throws MetadataScanException {
    Instant start = now();
    ObjectScannerIterable<VersionedMetadataObject> scanner =
        new VersionedMetadataObject().scanDatasetAllVersions(context, dataset);
    List<VersionedMetadataObject> list = scanLineage(scanner);
    int numFound = list.size();
    list = list.subList(0, min(numFound, maxVersions));
    Instant end = now();
    if (logger.isInfoEnabled()) {
      Duration duration = between(start, end);
      logger.info(
          format(
              "dataset: %s has (%s/%s) max versions time: %s",
              dataset, list.size(), numFound, duration));
    }
    return list;
  }

  /**
   * fetch an ordered list of metadata version of the given dataset
   *
   * @param dataset
   * @return
   */
  public List<VersionedMetadataObject> fetchDatasetVersionLineage(String dataset)
      throws MetadataScanException {
    return this.fetchDatasetVersionLineage(dataset, Integer.MAX_VALUE);
  }

  /**
   * will merge given properties with existing properties and save to accumulo
   *
   * @param m
   * @param newProperties
   * @throws IOException
   * @throws BasicAccumuloException
   */
  public void addPropertiesToMetadata(VersionedMetadataObject m, Map<String, String> newProperties)
      throws IOException, BasicAccumuloException {
    // merge old properties into new properties
    // if there is a duplicate key, use the new properties
    m.getProperties().forEach((key, value) -> newProperties.merge(key, value, (v1, v2) -> v1));
    m.setProperties(newProperties);
    m.put(context);
  }

  public void addPropertiesToMetadata(String dataset, Map<String, String> newProperties)
      throws IOException, BasicAccumuloException {
    VersionedMetadataObject metadataObject = fetchDatasetMetadata(dataset).getDatasetMetadata();
    addPropertiesToMetadata(metadataObject, newProperties);
  }

  private boolean updateProperties(String key, VersionedDatasetMetadata metadata)
      throws DatasetPropertiesException {
    if (isNull(key) || isNull(metadata)) {
      throw new IllegalArgumentException("metadata or key is null!");
    }
    try {
      Map<String, String> properties =
          metadata.getDatasetMetadata().getProperties().entrySet().stream()
              .collect(toMap(entry -> entry.getKey(), Map.Entry::getValue));
      properties.remove(key);
      // write dataset properties to accumulo
      addPropertiesToMetadata(metadata.getDatasetMetadata(), properties);
    } catch (IOException | BasicAccumuloException jpe) {
      throw new DatasetPropertiesException(jpe);
    }
    return true;
  }

  public boolean hasCompleteDelta(VersionedDatasetMetadata metadata) throws IOException {
    String json = propsJson(metadata, DELTA_PROPS_KEY);
    return (nonNull(json) && !json.isEmpty());
  }

  public CompleteDelta parseCompleteDelta(VersionedDatasetMetadata metadata) throws IOException {
    String json = propsJson(metadata, DELTA_PROPS_KEY);
    if (isNull(json) || json.isEmpty()) {
      return new CompleteDelta();
    }
    return objectMapper.readValue(json, CompleteDelta.class);
  }

  public CompleteQuality parseCompleteQuality(VersionedDatasetMetadata metadata)
      throws IOException {
    String json = propsJson(metadata, QUALITY_PROPS_KEY);
    if (isNull(json) || json.isEmpty()) {
      return new CompleteQuality();
    }
    return objectMapper.readValue(json, CompleteQuality.class);
  }

  public DatasetPerformance parseDatasetPerformance(VersionedDatasetMetadata metadata)
      throws IOException {
    String json = propsJson(metadata, PERFORMANCE_PROPS_KEY);
    if (isNull(json) || json.isEmpty()) {
      return new DatasetPerformance();
    }
    return objectMapper.readValue(json, DatasetPerformance.class);
  }

  protected String propsJson(VersionedDatasetMetadata metadata, String key) {
    Map<String, String> props = metadata.getDatasetMetadata().getProperties();
    return props.get(key);
  }

  /**
   * merges and updates the datasets delta properties in accumulo iff there are new deltas
   *
   * @param metadata
   * @return true if success, including noop success
   * @throws DatasetPropertiesException
   */
  public boolean wipeDeltaProperties(VersionedDatasetMetadata metadata)
      throws DatasetPropertiesException {
    try {
      if (isNull(metadata)) {
        throw new IllegalArgumentException("metadata is null, cannot update properties!");
      }

      Map<String, String> properties = new HashMap<>();
      properties.put(DELTA_PROPS_KEY, "");
      // write dataset properties to accumulo
      addPropertiesToMetadata(metadata.getDatasetMetadata(), properties);
    } catch (IOException | BasicAccumuloException e) {
      throw new DatasetPropertiesException(e);
    }
    return true;
  }

  public boolean replaceDeltaProperties(CompleteDelta delta) throws DatasetPropertiesException {
    String dataset = delta.getLastKnownVersions().getDataset();
    VersionedDatasetMetadata metadata = fetchDatasetMetadata(dataset);
    return replaceDeltaProperties(delta, metadata);
  }

  /**
   * replaces datasets delta properties overwriting the old value
   *
   * @param delta
   * @param metadata
   * @return true if success, including noop success
   * @throws DatasetPropertiesException
   */
  public boolean replaceDeltaProperties(CompleteDelta delta, VersionedDatasetMetadata metadata)
      throws DatasetPropertiesException {
    try {
      if (isNull(metadata) || isNull(delta)) {
        throw new IllegalArgumentException("metadata or delta is null, cannot update properties!");
      }

      String json = objectMapper.writeValueAsString(delta);
      Map<String, String> properties = new HashMap<>();
      properties.put(DELTA_PROPS_KEY, json);
      // write dataset properties to accumulo
      addPropertiesToMetadata(metadata.getDatasetMetadata(), properties);
    } catch (IOException | BasicAccumuloException e) {
      throw new DatasetPropertiesException(e);
    }
    return true;
  }

  public boolean updateDeltaProperties(CompleteDelta delta) throws DatasetPropertiesException {
    String dataset = delta.getLastKnownVersions().getDataset();
    VersionedDatasetMetadata metadata = fetchDatasetMetadata(dataset);
    return updateDeltaProperties(delta, metadata);
  }

  /**
   * merges and updates the datasets delta properties in accumulo iff there are new deltas
   *
   * @param delta
   * @param metadata
   * @return true if success, including noop success
   * @throws DatasetPropertiesException
   */
  public boolean updateDeltaProperties(CompleteDelta delta, VersionedDatasetMetadata metadata)
      throws DatasetPropertiesException {
    try {
      if (isNull(metadata) || isNull(delta)) {
        throw new IllegalArgumentException("metadata or delta is null, cannot update properties!");
      }

      delta = mergeDeltaProps(delta, metadata);
      String json = objectMapper.writeValueAsString(delta);
      Map<String, String> properties = new HashMap<>();
      properties.put(DELTA_PROPS_KEY, json);
      // write dataset properties to accumulo
      addPropertiesToMetadata(metadata.getDatasetMetadata(), properties);
    } catch (IOException | BasicAccumuloException e) {
      throw new DatasetPropertiesException(e);
    }
    return true;
  }

  /**
   * @param delta
   * @param metadata
   * @return
   * @throws IOException
   */
  public CompleteDelta mergeDeltaProps(CompleteDelta delta, VersionedDatasetMetadata metadata)
      throws IOException {
    if (hasCompleteDelta(metadata)) {
      CompleteDelta oldDelta = parseCompleteDelta(metadata);
      delta = delta.mergeDeltas(oldDelta);
    }
    return delta;
  }

  public Context getContext() {
    return context;
  }

  public void setContext(Context context) {
    this.context = context;
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public void setObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }
}
