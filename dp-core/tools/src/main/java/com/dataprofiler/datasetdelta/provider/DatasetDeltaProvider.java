package com.dataprofiler.datasetdelta.provider;

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

import static com.dataprofiler.datasetdelta.model.DeltaEnum.COLUMN_ADDED;
import static com.dataprofiler.datasetdelta.model.DeltaEnum.COLUMN_VALUES_DECREASED;
import static com.dataprofiler.datasetdelta.model.DeltaEnum.COLUMN_VALUES_INCREASED;
import static com.dataprofiler.datasetdelta.model.DeltaEnum.DATASET_VALUES_DECREASED;
import static com.dataprofiler.datasetdelta.model.DeltaEnum.DATASET_VALUES_INCREASED;
import static com.dataprofiler.datasetdelta.model.DeltaEnum.NO_OP;
import static com.dataprofiler.datasetdelta.model.DeltaEnum.TABLE_ADDED;
import static com.dataprofiler.datasetdelta.model.DeltaEnum.TABLE_VALUES_DECREASED;
import static com.dataprofiler.datasetdelta.model.DeltaEnum.TABLE_VALUES_INCREASED;
import static java.lang.String.format;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.dataprofiler.datasetdelta.DatasetDeltaException;
import com.dataprofiler.datasetdelta.diff.BuildColumnDeltas;
import com.dataprofiler.datasetdelta.diff.BuildDeltas;
import com.dataprofiler.datasetdelta.diff.BuildTableDeltas;
import com.dataprofiler.datasetdelta.model.CompleteDelta;
import com.dataprofiler.datasetdelta.model.DatasetDelta;
import com.dataprofiler.datasetdelta.model.DeltaEnum;
import com.dataprofiler.provider.DatasetMetadataProvider;
import com.dataprofiler.provider.TableMetadataProvider;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.MetadataVersionObject;
import com.dataprofiler.util.objects.VersionedDatasetMetadata;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import com.dataprofiler.util.objects.VersionedTableMetadata;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.text.diff.EditScript;
import org.apache.commons.text.diff.StringsComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasetDeltaProvider {
  private static final Logger logger = LoggerFactory.getLogger(DatasetDeltaProvider.class);

  protected static Character delimChar = Character.MAX_VALUE;
  protected Context context;
  protected DatasetMetadataProvider datasetMetadataProvider;
  protected TableMetadataProvider tableMetadataProvider;
  protected DeltaCommentProvider commentProvider;
  protected String deltaSymbol = "\uD835\uDEAB";

  public DatasetDeltaProvider() {
    super();
  }

  /**
   * fetch the latest dataset version metadata and its previous version then build a list of deltas
   *
   * @throws IllegalStateException if no previous version is found
   * @throws IllegalArgumentException is dataset is null
   * @throws DatasetDeltaException wraps any checked exception
   * @param dataset
   * @return
   */
  public CompleteDelta calculateRecentDelta(String dataset) throws DatasetDeltaException {
    if (isNull(context)) {
      throw new IllegalStateException("context was null, cannot calculate delta");
    }

    if (isNull(dataset)) {
      throw new IllegalArgumentException("dataset was null, cannot calculate delta");
    }

    try {
      Instant start = now();
      if (logger.isInfoEnabled()) {
        logger.debug(format("%s calculating delta for %s", deltaSymbol, dataset));
      }

      if (logger.isTraceEnabled()) {
        List<MetadataVersionObject> versions =
            new MetadataVersionObject().allMetadataVersions(context);
        logger.trace(
            "metdata version object table: " + new MetadataVersionObject().getTable(context));
        versions.forEach(el -> logger.trace(el.toString()));
      }

      List<VersionedMetadataObject> datasetVersions =
          datasetMetadataProvider.fetchDatasetVersionLineage(dataset, 2);
      if (datasetVersions == null || datasetVersions.isEmpty()) {
        // we are missing version metadata
        throw new IllegalStateException(format("dataset %s has no version metadata!", dataset));
      }

      if (datasetVersions.size() < 2) {
        // we only have one version, there is nothing to compare
        return new CompleteDelta();
      }

      if (logger.isInfoEnabled()) {
        logger.info(format("%s metadata version(s) found for %s", datasetVersions.size(), dataset));
        datasetVersions.stream().map(el -> el.version_id).limit(2).forEach(logger::info);
      }

      MetadataVersionObject currentVersion =
          new MetadataVersionObject(datasetVersions.get(0).version_id);
      MetadataVersionObject previousVersion =
          new MetadataVersionObject(datasetVersions.get(1).version_id);

      //      MetadataVersionObject currentVersion = datasetVersions.get(0);
      //      MetadataVersionObject previousVersion = datasetVersions.get(1);

      if (datasetVersions.size() < 2) {
        // we only have one version, there is nothing to compare
        logger.info("too few metadata to compare, nothing to do. skipping...");
        return new CompleteDelta();
      }
      VersionedDatasetMetadata current =
          datasetMetadataProvider.fetchDatasetMetadata(dataset, currentVersion);
      VersionedDatasetMetadata previous =
          datasetMetadataProvider.fetchDatasetMetadata(dataset, previousVersion);
      if (logger.isDebugEnabled()) {
        logger.debug("current version: " + current.getDatasetMetadata());
        logger.debug("previous version: " + previous.getDatasetMetadata());
      }
      CompleteDelta delta = calculateDelta(current, previous);
      Instant end = now();
      if (logger.isInfoEnabled()) {
        Duration duration = between(start, end);
        logger.info(
            format(
                "%s calculateRecentDelta finished for %s time: %s",
                deltaSymbol, dataset, duration));
      }

      return delta;
    } catch (Exception e) {
      e.printStackTrace();
      throw new DatasetDeltaException(format("error building deltas for dataset: %s", dataset), e);
    }
  }

  public CompleteDelta calculateDelta(
      VersionedDatasetMetadata currentDatasetMetadata,
      VersionedDatasetMetadata previousDatasetMetadata) {
    if (isNull(currentDatasetMetadata)
        || isNull(currentDatasetMetadata.getDatasetMetadata())
        || isNull(previousDatasetMetadata)
        || isNull(previousDatasetMetadata.getDatasetMetadata())) {
      throw new IllegalArgumentException("metadata is null, cannot calculate dataset delta");
    }

    Instant start = now();
    VersionedMetadataObject current = currentDatasetMetadata.getDatasetMetadata();
    VersionedMetadataObject previous = previousDatasetMetadata.getDatasetMetadata();
    String dataset = current.getDataset_name();
    if (logger.isInfoEnabled()) {
      logger.info(
          format(
              "%s calculating delta for %s current to previous versions: %s => %s",
              deltaSymbol, dataset, current.version_id, previous.version_id));
    }

    CompleteDelta completeDelta = new CompleteDelta();
    Set<DatasetDelta> deltas = new TreeSet<>();

    // dataset/table/column row value count checks
    deltas.addAll(checkRowCounts(currentDatasetMetadata, previousDatasetMetadata));

    // dataset name checks
    deltas.add(checkDatasetName(currentDatasetMetadata, previousDatasetMetadata));

    // table level checks
    deltas.addAll(checkTableNames(currentDatasetMetadata, previousDatasetMetadata));

    // column level checks
    deltas.addAll(checkColumnNames(currentDatasetMetadata, previousDatasetMetadata));

    completeDelta.setDeltas(
        deltas.stream()
            .filter(Objects::nonNull)
            .filter(delta -> delta.getDeltaEnum() != NO_OP)
            .collect(toSet()));

    if (logger.isDebugEnabled()) {
      logger.debug(completeDelta.getDeltas().toString());
    }
    Instant end = now();
    if (logger.isInfoEnabled()) {
      Duration duration = between(start, end);
      logger.info(
          format(
              "%s delta calculation finished for %s versions %s => %s time: %s",
              deltaSymbol, dataset, current.version_id, previous.version_id, duration));
    }
    return completeDelta;
  }

  /**
   * given current and previous dataset metadata, do a diff on the row counts
   *
   * @param current
   * @param previous
   * @return
   */
  protected List<DatasetDelta> checkRowCounts(
      VersionedDatasetMetadata current, VersionedDatasetMetadata previous) {
    logger.info(
        format(
            "%s check rows current: %s previous: %s",
            deltaSymbol, current.getVersion().getId(), previous.getVersion().getId()));
    List<DatasetDelta> deltas = new ArrayList<>();
    String datasetName = current.getDatasetMetadata().getDataset_name();

    // dataset row count deltas
    deltas.addAll(checkDatasetRowCount(current, previous));
    // table row count deltas
    deltas.addAll(checkTableRowCounts(current, previous));
    // column row count deltas
    deltas.addAll(checkColumnRowCounts(current, previous));
    // remove nulls
    deltas = deltas.stream().filter(Objects::nonNull).collect(toList());

    logger.debug(
        format(
            "%s %s total row count modifications: %s ", deltaSymbol, datasetName, deltas.size()));

    return deltas;
  }

  protected List<DatasetDelta> checkDatasetRowCount(
      VersionedDatasetMetadata current, VersionedDatasetMetadata previous) {
    List<DatasetDelta> deltas = new ArrayList<>(4);
    String datasetName = current.getDatasetMetadata().getDataset_name();
    long currentValueCount =
        nonNull(current.getDatasetMetadata().getNum_values())
            ? current.getDatasetMetadata().getNum_values()
            : 0;
    long previousValueCount =
        nonNull(previous.getDatasetMetadata().getNum_values())
            ? previous.getDatasetMetadata().getNum_values()
            : 0;
    if (currentValueCount != previousValueCount) {
      DatasetDelta delta = new DatasetDelta();
      delta.setUpdatedOnMillis(current.getDatasetMetadata().getUpdate_time());
      delta.setDeltaEnum(
          currentValueCount > previousValueCount
              ? DATASET_VALUES_INCREASED
              : DATASET_VALUES_DECREASED);
      delta.setValueFrom(Long.toString(previousValueCount));
      delta.setValueTo(Long.toString(currentValueCount));
      delta.setDataset(datasetName);
      delta.setFromVersion(previous.getVersion().getId());
      delta.setTargetVersion(current.getVersion().getId());
      delta.setDatasetUpdatedOnMillis(current.getDatasetMetadata().getUpdate_time());
      deltas.add(delta);
    }

    return deltas;
  }

  /**
   * given current and previous dataset metadata, do a diff on their tables row counts if a table
   * was added or removed in the current dataset, then it is ignored
   *
   * @param current
   * @param previous
   * @return
   */
  protected List<DatasetDelta> checkTableRowCounts(
      VersionedDatasetMetadata current, VersionedDatasetMetadata previous) {
    String datasetName = current.getDatasetMetadata().getDataset_name();

    Map<String, VersionedMetadataObject> currentTables = current.getTableMetadata();
    Map<String, VersionedMetadataObject> previousTables = previous.getTableMetadata();

    List<DatasetDelta> deltas =
        currentTables.entrySet().stream()
            .flatMap(
                tableEntry -> {
                  List<DatasetDelta> currentDeltas = new ArrayList<>(4);
                  String currentTableName = tableEntry.getKey();
                  if (previousTables.containsKey(currentTableName)) {
                    VersionedMetadataObject tableMeta = tableEntry.getValue();
                    long currentValueCount = tableMeta.getNum_values();
                    long previousValueCount = previousTables.get(currentTableName).getNum_values();
                    if (currentValueCount != previousValueCount) {
                      DatasetDelta delta = new DatasetDelta();
                      delta.setUpdatedOnMillis(current.getDatasetMetadata().getUpdate_time());
                      delta.setDeltaEnum(
                          currentValueCount > previousValueCount
                              ? TABLE_VALUES_INCREASED
                              : TABLE_VALUES_DECREASED);
                      delta.setTable(currentTableName);
                      delta.setValueFrom(Long.toString(previousValueCount));
                      delta.setValueTo(Long.toString(currentValueCount));
                      delta.setDataset(datasetName);
                      delta.setFromVersion(previous.getVersion().getId());
                      delta.setTargetVersion(current.getVersion().getId());
                      delta.setDatasetUpdatedOnMillis(
                          current.getDatasetMetadata().getUpdate_time());
                      currentDeltas.add(delta);
                    }
                  }
                  return currentDeltas.stream();
                })
            .collect(toList());
    logger.debug(
        format(
            "%s %s table row count modifications: %s ", deltaSymbol, datasetName, deltas.size()));

    return deltas;
  }

  /**
   * given current and previous dataset metadata, do a diff on their columns row counts if a column
   * was added or removed in the current dataset/table, then it is ignored
   *
   * @param current
   * @param previous
   * @return
   */
  protected List<DatasetDelta> checkColumnRowCounts(
      VersionedDatasetMetadata current, VersionedDatasetMetadata previous) {
    String datasetName = current.getDatasetMetadata().getDataset_name();

    Map<String, VersionedMetadataObject> currentColumns = current.getColumnMetadata();
    Map<String, VersionedMetadataObject> previousColumns = previous.getColumnMetadata();

    List<DatasetDelta> deltas =
        currentColumns.entrySet().stream()
            .flatMap(
                columnEntry -> {
                  List<DatasetDelta> currentDeltas = new ArrayList<>(4);
                  String currentTableColumnName = columnEntry.getKey();
                  // currentTableColumn key is in the form of 'table:column'
                  if (previousColumns.containsKey(currentTableColumnName)) {
                    VersionedMetadataObject columnMeta = columnEntry.getValue();
                    long currentValueCount = columnMeta.getNum_values();
                    long previousValueCount =
                        previousColumns.get(currentTableColumnName).getNum_values();
                    if (currentValueCount != previousValueCount) {
                      DatasetDelta delta = new DatasetDelta();
                      delta.setUpdatedOnMillis(current.getDatasetMetadata().getUpdate_time());
                      delta.setDeltaEnum(
                          currentValueCount > previousValueCount
                              ? COLUMN_VALUES_INCREASED
                              : COLUMN_VALUES_DECREASED);
                      delta.setTable(columnMeta.getTable_name());
                      delta.setColumn(columnMeta.getColumn_name());
                      delta.setValueFrom(Long.toString(previousValueCount));
                      delta.setValueTo(Long.toString(currentValueCount));
                      delta.setDataset(datasetName);
                      delta.setFromVersion(previous.getVersion().getId());
                      delta.setTargetVersion(current.getVersion().getId());
                      delta.setDatasetUpdatedOnMillis(
                          current.getDatasetMetadata().getUpdate_time());
                      currentDeltas.add(delta);
                    }
                  }
                  return currentDeltas.stream();
                })
            .collect(toList());
    logger.debug(
        format(
            "%s %s column row count modifications: %s ", deltaSymbol, datasetName, deltas.size()));

    return deltas;
  }

  /**
   * given a current and previous dataset, do a diff on the dataset friendly name
   *
   * @param current
   * @param previous
   * @return
   */
  protected DatasetDelta checkDatasetName(
      VersionedDatasetMetadata current, VersionedDatasetMetadata previous) {
    VersionedMetadataObject currentDatasetMetadata = current.getDatasetMetadata();
    String currentDisplayName = currentDatasetMetadata.dataset_display_name;
    String prevDisplayName = previous.getDatasetMetadata().dataset_display_name;
    DatasetDelta delta = new DatasetDelta();
    boolean bothNull = currentDisplayName == null && prevDisplayName == null;
    if (!(bothNull || currentDisplayName.equals(prevDisplayName))) {
      delta.setDataset(currentDisplayName);
      delta.setDeltaEnum(DeltaEnum.DATASET_RENAMED);
      delta.setValueFrom(prevDisplayName);
      delta.setValueTo(currentDisplayName);
      delta.setTargetVersion(current.getVersion().getId());
      delta.setFromVersion(previous.getVersion().getId());
      delta.setDatasetUpdatedOnMillis(current.getDatasetMetadata().getUpdate_time());
    }

    return delta;
  }

  /**
   * given a current and previous dataset, do a diff on the table names
   *
   * @param current
   * @param previous
   * @return
   */
  protected List<DatasetDelta> checkTableNames(
      VersionedDatasetMetadata current, VersionedDatasetMetadata previous) {
    List<DatasetDelta> deltas = new ArrayList<>();
    String datasetName = current.getDatasetMetadata().getDataset_name();
    Set<String> currentTableNames =
        current.getTableMetadata().keySet().stream().collect(toCollection(TreeSet::new));
    Set<String> previousTableNames =
        previous.getTableMetadata().keySet().stream().collect(toCollection(TreeSet::new));
    String currentTables = String.join(delimChar.toString(), currentTableNames);
    String previousTables = String.join(delimChar.toString(), previousTableNames);
    if (logger.isDebugEnabled()) {
      logger.debug(format("%s comparing table names for %s", deltaSymbol, datasetName));
    }
    if (logger.isTraceEnabled()) {
      logger.trace(format("%s current: %s", deltaSymbol, currentTableNames));
      logger.trace(format("%s previous: %s", deltaSymbol, previousTableNames));
    }
    StringsComparator cmp = new StringsComparator(previousTables, currentTables);
    EditScript<Character> script = cmp.getScript();
    int mod = script.getModifications();
    logger.debug(format("%s %s table modifications: %s ", deltaSymbol, datasetName, mod));
    if (mod > 0) {
      BuildDeltas buildDeltas =
          new BuildTableDeltas(
              datasetName,
              "",
              "",
              current.getDatasetMetadata().getUpdate_time(),
              previous.getVersion().getId(),
              current.getVersion().getId());
      script.visit(buildDeltas);
      logger.debug(
          format("%s %s new deltas: %s", deltaSymbol, datasetName, buildDeltas.getDeltas().size()));
      deltas.addAll(buildDeltas.getDeltas());
    }
    return deltas;
  }

  /**
   * given a current and previous dataset, compare currents columns across all tables
   *
   * @param current
   * @param previous
   * @return
   */
  protected List<DatasetDelta> checkColumnNames(
      VersionedDatasetMetadata current, VersionedDatasetMetadata previous) {
    MetadataVersionObject previousVersion = previous.getVersion();
    String datasetName = current.getDatasetMetadata().getDataset_name();
    Set<String> currentTableNames =
        current.getTableMetadata().keySet().stream().collect(toCollection(TreeSet::new));
    Set<String> previousTableNames =
        previous.getTableMetadata().keySet().stream().collect(toCollection(TreeSet::new));
    List<DatasetDelta> deltas =
        currentTableNames.stream()
            .map(
                tableName ->
                    checkTableColumnNames(
                        datasetName, tableName, previousTableNames, previousVersion, current))
            .flatMap(tableDeltas -> tableDeltas.stream())
            .collect(toList());

    return deltas;
  }

  /**
   * if the given table is new, than add all columns if the given table is not new, do a diff on all
   * the columns
   *
   * @param datasetName
   * @param tableName
   * @param previousTableNames
   * @param previousVersion
   * @return
   */
  protected List<DatasetDelta> checkTableColumnNames(
      String datasetName,
      String tableName,
      Set<String> previousTableNames,
      MetadataVersionObject previousVersion,
      VersionedDatasetMetadata current) {
    if (logger.isTraceEnabled()) {
      logger.trace(
          format(
              "tableMetadataProvider: %s, dataset: %s, table: %s, current: %s, previous: %s",
              tableMetadataProvider, datasetName, tableName, current, previousVersion));
    }
    List<DatasetDelta> deltas = new ArrayList<>();
    MetadataVersionObject currentVersion = current.getVersion();
    VersionedTableMetadata curTableMetadata =
        tableMetadataProvider.fetchTableMetadata(datasetName, tableName);
    if (logger.isDebugEnabled()) {
      logger.trace(
          format(
              "curTableMetadata table: %s columns: %s",
              curTableMetadata.getTable(), curTableMetadata.getColumns()));
      logger.debug(
          format(
              "%s %s %s previous tables: %s",
              deltaSymbol, datasetName, tableName, previousTableNames));
    }
    Set<String> currentColumnNames =
        curTableMetadata.getColumns().keySet().stream().collect(toCollection(TreeSet::new));
    long currentUpdateTime = current.getDatasetMetadata().getUpdate_time();
    if (!previousTableNames.contains(tableName)) {
      // this is a new table, all columns are additions
      for (String columnName : currentColumnNames) {
        DatasetDelta delta = new DatasetDelta();
        delta.setDataset(datasetName);
        delta.setTable(tableName);
        delta.setColumn(columnName);
        delta.setValueFrom("");
        delta.setValueTo(columnName);
        delta.setDeltaEnum(COLUMN_ADDED);
        delta.setFromVersion(previousVersion.getId());
        delta.setTargetVersion(currentVersion.getId());
        delta.setDatasetUpdatedOnMillis(currentUpdateTime);
        deltas.add(delta);
      }
      DatasetDelta delta = new DatasetDelta();
      delta.setDataset(datasetName);
      delta.setTable(tableName);
      delta.setValueFrom("");
      delta.setValueTo(tableName);
      delta.setDeltaEnum(TABLE_ADDED);
      delta.setFromVersion(previousVersion.getId());
      delta.setTargetVersion(currentVersion.getId());
      delta.setDatasetUpdatedOnMillis(currentUpdateTime);
      deltas.add(delta);
      return deltas;
    }

    VersionedTableMetadata prevTableMetadata =
        tableMetadataProvider.fetchTableMetadataByVersion(datasetName, tableName, previousVersion);
    Set<String> previousColumnNames =
        prevTableMetadata.getColumns().keySet().stream().collect(toCollection(TreeSet::new));
    String currentColumns = String.join(delimChar.toString(), currentColumnNames);
    String previousColumns = String.join(delimChar.toString(), previousColumnNames);
    if (logger.isDebugEnabled()) {
      logger.debug(
          format("%s comparing column names for %s %s", deltaSymbol, datasetName, tableName));
    }
    if (logger.isTraceEnabled()) {
      logger.trace(format("%s current: %s", deltaSymbol, currentColumnNames));
      logger.trace(format("%s previous: %s", deltaSymbol, previousColumnNames));
    }
    StringsComparator cmp = new StringsComparator(previousColumns, currentColumns);
    EditScript<Character> script = cmp.getScript();
    int mod = script.getModifications();
    logger.debug(
        format("%s %s %s column modifications: %s", deltaSymbol, datasetName, tableName, mod));
    if (mod > 0) {
      BuildDeltas buildDeltas =
          new BuildColumnDeltas(
              datasetName,
              tableName,
              "",
              currentUpdateTime,
              previousVersion.getId(),
              currentVersion.getId());
      script.visit(buildDeltas);

      logger.debug(
          format(
              "%s %s %s new deltas: %s",
              deltaSymbol, datasetName, tableName, buildDeltas.getDeltas().size()));
      deltas.addAll(buildDeltas.getDeltas());
    }
    return deltas;
  }

  public DatasetMetadataProvider getDatasetMetadataProvider() {
    return datasetMetadataProvider;
  }

  public void setDatasetMetadataProvider(DatasetMetadataProvider datasetMetadataProvider) {
    this.datasetMetadataProvider = datasetMetadataProvider;
  }

  public Context getContext() {
    return context;
  }

  public void setContext(Context context) {
    this.context = context;
  }

  public DeltaCommentProvider getCommentProvider() {
    return commentProvider;
  }

  public void setCommentProvider(DeltaCommentProvider commentProvider) {
    this.commentProvider = commentProvider;
  }

  public TableMetadataProvider getTableMetadataProvider() {
    return tableMetadataProvider;
  }

  public void setTableMetadataProvider(TableMetadataProvider tableMetadataProvider) {
    this.tableMetadataProvider = tableMetadataProvider;
  }
}
