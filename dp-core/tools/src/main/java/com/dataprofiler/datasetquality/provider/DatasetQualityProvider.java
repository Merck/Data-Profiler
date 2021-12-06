package com.dataprofiler.datasetquality.provider;

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
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toSet;

import com.dataprofiler.datasetquality.DatasetQualityException;
import com.dataprofiler.datasetquality.MultisearchException;
import com.dataprofiler.datasetquality.model.ColumnTypeCount;
import com.dataprofiler.datasetquality.model.CompleteQuality;
import com.dataprofiler.datasetquality.model.DatasetQuality;
import com.dataprofiler.datasetquality.model.QualityEnum;
import com.dataprofiler.datasetquality.model.QualityWarningEnum;
import com.dataprofiler.datasetquality.model.multisearch.ColumnCountResult;
import com.dataprofiler.datasetquality.model.multisearch.GroupedColumnCountResult;
import com.dataprofiler.provider.ColumnMetadataProvider;
import com.dataprofiler.provider.DatasetMetadataProvider;
import com.dataprofiler.provider.TableMetadataProvider;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.DataScanSpec;
import com.dataprofiler.util.objects.DataScanSpec.Level;
import com.dataprofiler.util.objects.DataScanSpec.Type;
import com.dataprofiler.util.objects.VersionedDatasetMetadata;
import com.dataprofiler.util.objects.VersionedDatasetMetadata.MissingMetadataException;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasetQualityProvider {
  private static final Logger logger = LoggerFactory.getLogger(DatasetQualityProvider.class);
  private static final Function<String, String> normalize = el -> el.toLowerCase().trim();
  private static final String NULL_SYMBOL = "⍉";
  // FIXME: TODO: multiple search terms do not work and return 0 results
  //  private static final Set<String> DEFAULT_NULLNESS_TERMS =
  //      unmodifiableSet(Stream.of("null", "NULL", "", "null:null").collect(toSet()));
  private static final Set<String> DEFAULT_NULLNESS_TERMS =
      unmodifiableSet(Stream.of("null").collect(toSet()));

  protected Context context;
  protected MultisearchProvider multisearchProvider;
  protected ColumnMetadataProvider columnMetadataProvider;
  protected TableMetadataProvider tableMetadataProvider;
  protected DatasetMetadataProvider datasetMetadataProvider;
  protected Set<String> nullnessTerms;

  public DatasetQualityProvider() {
    super();
    nullnessTerms = new HashSet<>(DEFAULT_NULLNESS_TERMS).stream().map(normalize).collect(toSet());
  }

  /**
   * @param dataset String representing a dataset name
   * @return List<GroupedColumnCountResult>
   * @throws IllegalArgumentException if dataset is null
   * @throws IllegalStateException if this class is not initialized
   * @throws DatasetQualityException wraps many checked exceptions
   */
  public CompleteQuality calculateQuality(String dataset) throws DatasetQualityException {
    ensureInitialized();
    if (isNull(dataset)) {
      throw new IllegalArgumentException("dataset is null, cannot calculate quality");
    }

    if (logger.isInfoEnabled()) {
      logger.info(format("%s calculating quality for dataset: %s", NULL_SYMBOL, dataset));
    }
    Instant start = now();

    CompleteQuality completeQuality = new CompleteQuality();
    Set<DatasetQuality> qualities = new HashSet<>(createSearchQualities(dataset));
    qualities.add(createTypeQualities(dataset));
    completeQuality.setQualities(qualities);
    completeQuality = populateMetadata(dataset, completeQuality);

    if (logger.isDebugEnabled()) {
      logger.debug(format("%s quality (no metadata): %s", NULL_SYMBOL, completeQuality));
    }
    Instant end = now();
    if (logger.isInfoEnabled()) {
      Duration duration = between(start, end);
      logger.info(
          format(
              "%s dataset quality updated dataset: %s, time: %s", NULL_SYMBOL, dataset, duration));
    }

    return completeQuality;
  }

  /**
   * @param dataset String representing a dataset name
   * @return Set<DatasetQuality>
   */
  public Set<DatasetQuality> createSearchQualities(String dataset) throws DatasetQualityException {
    try {
      DataScanSpec spec = new DataScanSpec();
      spec.setType(Type.SEARCH);
      spec.setDataset(dataset);
      spec.setGroupingLevel(Level.TABLE);
      spec.setTerm(new ArrayList<>(nullnessTerms));
      spec.setBegins_with(false);
      spec.setSubstring_match(false);
      List<GroupedColumnCountResult> results = multisearchProvider.multiSearch(spec);
      if (logger.isDebugEnabled()) {
        logger.debug(
            format(
                "%s dataset: %s, grouped results size: %s", NULL_SYMBOL, dataset, results.size()));
        results.stream().limit(4).forEach(el -> logger.info(el.toString()));
      }
      return adaptResults(results);
    } catch (MultisearchException e) {
      throw new DatasetQualityException(e);
    }
  }

  public DatasetQuality createTypeQualities(String dataset) {
    Set<String> tables = tableMetadataProvider.fetchAllTables(dataset);
    Map<String, ColumnTypeCount> typeCounts = new HashMap<>();
    for (String table : tables) {
      List<VersionedMetadataObject> columns =
          columnMetadataProvider.fetchVersionMetadataForAllColumns(dataset, table);
      for (VersionedMetadataObject column : columns) {
        String dataType = normalizeDataType(column.data_type);
        if (!typeCounts.containsKey(dataType)) {
          typeCounts.put(dataType, new ColumnTypeCount(dataType, 1));
        } else {
          typeCounts.compute(
              dataType,
              (k1, v2) -> {
                v2.incrementCount();
                return v2;
              });
        }
      }
    }
    return toDatasetQualityType(dataset, typeCounts.values().stream().collect(toSet()));
  }

  public DatasetQuality toDatasetQualityType(String dataset, Set<ColumnTypeCount> typeCounts) {
    DatasetQuality quality = new DatasetQuality();
    quality.setDataset(dataset);
    quality.setQualityEnum(QualityEnum.COLUMN_TYPES);
    quality.setColumnTypeCounts(typeCounts);
    return quality;
  }

  /**
   * populate timestamp and count metadata on computed quality/nullness objects
   *
   * @param dataset name of the dataset
   * @param completeQuality
   * @return CompleteQuality
   * @throws DatasetQualityException wraps many checked exceptions
   * @throws IllegalArgumentException if quality or metadata are null
   */
  public CompleteQuality populateMetadata(String dataset, CompleteQuality completeQuality)
      throws DatasetQualityException {
    VersionedDatasetMetadata metadata = datasetMetadataProvider.fetchDatasetMetadata(dataset);
    return populateMetadata(completeQuality, metadata);
  }

  /**
   * populate timestamp and count metadata on computed quality/nullness objects
   *
   * @param quality
   * @param metadata
   * @return CompleteQuality
   * @throws DatasetQualityException wraps many checked exceptions
   * @throws IllegalArgumentException if quality or metadata are null
   */
  public CompleteQuality populateMetadata(
      CompleteQuality quality, VersionedDatasetMetadata metadata) throws DatasetQualityException {
    if (isNull(quality) || isNull(metadata)) {
      throw new IllegalArgumentException("quality and metadata should both be non null");
    }
    if (isNull(quality.getQualities()) || quality.getQualities().isEmpty()) {
      return quality;
    }

    Set<DatasetQuality> qualities = new HashSet<>();
    for (DatasetQuality datasetQuality : quality.getQualities()) {
      VersionedMetadataObject metadataObject = mostGranularMetadata(metadata, datasetQuality);
      long updateTime = metadataObject.getUpdate_time();
      long numValues = metadataObject.getNum_values();
      datasetQuality.setDatasetUpdatedOnMillis(updateTime);
      if (!QualityEnum.COLUMN_TYPES.equals(datasetQuality.getQualityEnum())) {
        datasetQuality.setTotalNumValues(numValues);
      }
      qualities.add(datasetQuality);
    }

    quality.setQualities(qualities);
    if (logger.isTraceEnabled()) {
      logger.trace(format("%s quality (with metadata): %s", NULL_SYMBOL, quality));
    }
    return quality;
  }

  protected VersionedMetadataObject mostGranularMetadata(
      VersionedDatasetMetadata metadata, DatasetQuality datasetQuality)
      throws DatasetQualityException {
    if (isNull(metadata) || isNull(datasetQuality)) {
      throw new IllegalArgumentException("dataset quality or metadata are null");
    }

    try {
      String table = datasetQuality.getTable();
      if (isNull(table)) {
        return metadata.getDatasetMetadata();
      }
      String column = datasetQuality.getColumn();
      if (isNull(column)) {
        return metadata.getTableMetadataByName(table);
      }
      return metadata.getColumnMetadataByName(table, column);
    } catch (MissingMetadataException e) {
      throw new DatasetQualityException(e);
    }
  }

  protected Set<DatasetQuality> adaptResults(List<GroupedColumnCountResult> results) {
    Set<DatasetQuality> qualities = new HashSet<>();

    for (GroupedColumnCountResult result : results) {
      List<String> values = result.getValues();
      if (values.stream().map(normalize).noneMatch(nullnessTerms::contains)) {
        continue;
      }
      List<ColumnCountResult> elements = result.getElements();
      for (ColumnCountResult element : elements) {
        qualities.addAll(toDatasetQualities(element));
      }
    }
    return qualities;
  }

  protected Set<DatasetQuality> toDatasetQualities(ColumnCountResult columnCountResult) {
    Set<DatasetQuality> qualities = new HashSet<>();
    List<String> values = columnCountResult.getValues();
    for (int i = 0; i < values.size(); i++) {
      String currentValue = normalize.apply(values.get(i));
      if (!nullnessTerms.contains(currentValue)) {
        continue;
      }
      DatasetQuality datasetQuality = new DatasetQuality();
      datasetQuality.setDataset(columnCountResult.getDataset());
      datasetQuality.setTable(columnCountResult.getTable());
      datasetQuality.setColumn(columnCountResult.getColumn());
      QualityEnum qualityEnum = QualityEnum.lookup(currentValue);
      datasetQuality.setQualityEnum(qualityEnum);
      datasetQuality.setQualityWarningEnum(QualityWarningEnum.UNKNOWN);
      datasetQuality.setCount(lookupCount(columnCountResult, i));
      qualities.add(datasetQuality);
    }
    return qualities;
  }

  protected long lookupCount(ColumnCountResult columnCountResult, int index) {
    if (isNull(columnCountResult)) {
      return -1;
    }

    int size = columnCountResult.getCounts().size();
    if (index < size) {
      return columnCountResult.getCounts().get(index);
    } else {
      return -1;
    }
  }

  protected void ensureInitialized() {
    if (isNull(context)
        || isNull(multisearchProvider)
        || isNull(multisearchProvider.getContext())) {
      throw new IllegalStateException("context is null");
    }
  }

  protected String normalizeDataType(String dataType) {
    if (isNull(dataType)) {
      return null;
    }

    dataType = dataType.toLowerCase().trim();
    final String DECIMAL = "decimal";
    if (dataType.startsWith(DECIMAL)) {
      // remove precision
      dataType = DECIMAL;
    }
    return dataType;
  }

  public MultisearchProvider getMultisearchProvider() {
    return multisearchProvider;
  }

  public void setMultisearchProvider(MultisearchProvider multisearchProvider) {
    this.multisearchProvider = multisearchProvider;
  }

  public Context getContext() {
    return context;
  }

  public void setContext(Context context) {
    this.context = context;
  }

  public Set<String> getNullnessTerms() {
    return nullnessTerms;
  }

  public void setNullnessTerms(Set<String> nullnessTerms) {
    if (isNull(nullnessTerms)) {
      this.nullnessTerms = null;
    }

    this.nullnessTerms = nullnessTerms.stream().map(normalize).collect(toSet());
  }

  public ColumnMetadataProvider getColumnMetadataProvider() {
    return columnMetadataProvider;
  }

  public void setColumnMetadataProvider(ColumnMetadataProvider columnMetadataProvider) {
    this.columnMetadataProvider = columnMetadataProvider;
  }

  public TableMetadataProvider getTableMetadataProvider() {
    return tableMetadataProvider;
  }

  public void setTableMetadataProvider(TableMetadataProvider tableMetadataProvider) {
    this.tableMetadataProvider = tableMetadataProvider;
  }

  public DatasetMetadataProvider getDatasetMetadataProvider() {
    return datasetMetadataProvider;
  }

  public void setDatasetMetadataProvider(DatasetMetadataProvider datasetMetadataProvider) {
    this.datasetMetadataProvider = datasetMetadataProvider;
  }
}
