/**
*  Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
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
**/
package helpers;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import com.dataprofiler.provider.ColumnMetadataProvider;
import com.dataprofiler.provider.DatasetMetadataProvider;
import com.dataprofiler.provider.TableMetadataProvider;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.MetadataVersionObject;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import org.apache.log4j.Logger;

public class MetadataVersionHistoryHelper {
  private static final Logger logger = Logger.getLogger(MetadataVersionHistoryHelper.class);

  @Inject
  public MetadataVersionHistoryHelper() {}

  public List<MetadataVersionHistoryResult> fetchAllMetadataVersionHistory(
      Context context, int maxHistoryElements) throws Exception {
    Instant start = Instant.now();
    final int max = Math.max(1, maxHistoryElements);
    DatasetMetadataProvider datasetMetadataProvider = new DatasetMetadataProvider(context);
    Set<String> datasets = datasetMetadataProvider.fetchAllDatasets();
    List<MetadataVersionHistoryResult> results = new ArrayList<>(128);
    for (String dataset : datasets) {
      MetadataVersionHistoryResult metadataVersionHistoryResult =
          fetchDatasetMetadataVersionHistory(context, dataset, max);
      results.add(metadataVersionHistoryResult);
    }
    Instant end = Instant.now();
    if (logger.isInfoEnabled()) {
      Duration duration = Duration.between(start, end);
      logger.info(
          format(
              "fetched max: %s history for all datasets: %s time: %s",
              max, results.size(), duration));
    }
    return results;
  }

  public MetadataVersionHistoryResult fetchDatasetMetadataVersionHistory(
      Context context, String dataset, int maxHistoryElements) throws Exception {
    Instant start = Instant.now();
    final int max = Math.max(1, maxHistoryElements);
    DatasetMetadataProvider datasetMetadataProvider = new DatasetMetadataProvider(context);
    MetadataVersionHistoryResult current = new MetadataVersionHistoryResult();
    current.setMaxHistoryElements(max);
    current.setDatasetHistory(datasetMetadataProvider.fetchDatasetVersionLineage(dataset, max));
    MetadataVersionHistoryResult tmp = fetchTableMetadataVersionHistory(context, dataset, max);
    current.setTableHistory(tmp.getTableHistory());
    current.setColumnHistory(tmp.getColumnHistory());
    Instant end = Instant.now();
    if (logger.isInfoEnabled()) {
      Duration duration = Duration.between(start, end);
      logger.info(
          format(
              "fetched max: %s history for dataset: %s time: %s",
              maxHistoryElements, dataset, duration));
    }
    return current;
  }

  public MetadataVersionHistoryResult fetchTableMetadataVersionHistory(
      Context context, String dataset, int maxHistoryElements) throws Exception {
    Instant start = Instant.now();
    final int max = Math.max(1, maxHistoryElements);
    MetadataVersionHistoryResult result = new MetadataVersionHistoryResult();
    result.setMaxHistoryElements(max);
    List<VersionedMetadataObject> tableHistory = new ArrayList<>(128);
    List<VersionedMetadataObject> columnHistory = new ArrayList<>(128);
    TableMetadataProvider tableMetadataProvider = new TableMetadataProvider(context);
    Set<String> tables = tableMetadataProvider.fetchAllTables(dataset);
    for (String table : tables) {
      if (logger.isInfoEnabled()) {
        logger.info(format("query table: %s", table));
      }
      tableHistory.addAll(tableMetadataProvider.fetchTableVersionLineage(dataset, table, max));
      columnHistory.addAll(
          fetchColumnMetadataVersionHistory(context, dataset, table, max).getColumnHistory());
    }

    result.setTableHistory(tableHistory);
    result.setColumnHistory(columnHistory);
    Instant end = Instant.now();
    if (logger.isInfoEnabled()) {
      Duration duration = Duration.between(start, end);
      logger.info(
          format(
              "fetched max: %s table history for dataset: %s time: %s",
              maxHistoryElements, dataset, duration));
    }
    return result;
  }

  public MetadataVersionHistoryResult fetchColumnMetadataVersionHistory(
      Context context, String dataset, String table, int maxHistoryElements) throws Exception {
    Instant start = Instant.now();
    final int max = Math.max(1, maxHistoryElements);
    MetadataVersionHistoryResult result = new MetadataVersionHistoryResult();
    result.setMaxHistoryElements(max);
    List<VersionedMetadataObject> history = new ArrayList<>(128);
    ColumnMetadataProvider columnMetadataProvider = new ColumnMetadataProvider(context);
    Set<String> columns = columnMetadataProvider.fetchAllColumns(dataset, table);
    for (String column : columns) {
      if (logger.isInfoEnabled()) {
        logger.info(format("query column: %s", column));
      }
      history.addAll(
          columnMetadataProvider.fetchColumnVersionLineage(
              dataset, table, column, maxHistoryElements));
    }
    result.setColumnHistory(history);
    Instant end = Instant.now();
    if (logger.isInfoEnabled()) {
      Duration duration = Duration.between(start, end);
      logger.info(
          format(
              "fetched max: %s column history for dataset: %s table: %s time: %s",
              maxHistoryElements, dataset, table, duration));
    }
    return result;
  }

  public static class MetadataVersionHistoryResult {
    protected int maxHistoryElements;
    protected List<VersionedMetadataObject> datasetHistory;
    protected List<VersionedMetadataObject> tableHistory;
    protected List<VersionedMetadataObject> columnHistory;

    public int getMaxHistoryElements() {
      return maxHistoryElements;
    }

    public void setMaxHistoryElements(int maxHistoryElements) {
      this.maxHistoryElements = maxHistoryElements;
    }

    public List<VersionedMetadataObject> getDatasetHistory() {
      return datasetHistory;
    }

    public void setDatasetHistory(List<VersionedMetadataObject> datasetHistory) {
      this.datasetHistory = datasetHistory;
    }

    public List<VersionedMetadataObject> getTableHistory() {
      return tableHistory;
    }

    public void setTableHistory(List<VersionedMetadataObject> tableHistory) {
      this.tableHistory = tableHistory;
    }

    public List<VersionedMetadataObject> getColumnHistory() {
      return columnHistory;
    }

    public void setColumnHistory(List<VersionedMetadataObject> columnHistory) {
      this.columnHistory = columnHistory;
    }
  }
}
