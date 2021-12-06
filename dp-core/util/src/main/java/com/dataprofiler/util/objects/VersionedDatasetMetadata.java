package com.dataprofiler.util.objects;

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

import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Const;
import com.dataprofiler.util.Context;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.MutationsRejectedException;

public class VersionedDatasetMetadata {
  private MetadataVersionObject version;
  private VersionedMetadataObject datasetMetadata;
  private Map<String, VersionedMetadataObject> tableMetadata = new HashMap<>();
  private Map<String, VersionedMetadataObject> columnMetadata = new HashMap<>();

  public VersionedDatasetMetadata(MetadataVersionObject version) {
    this.setVersion(version);
  }

  public VersionedDatasetMetadata(
      MetadataVersionObject version, ObjectScannerIterable<VersionedMetadataObject> iterable) {
    this.setVersion(version);
    iterable.setBatch(true);
    for (VersionedMetadataObject m : iterable) {
      putMetadata(m);
    }
  }

  public void putMetadata(VersionedMetadataObject m) {
    assert (m.version_id.equals(getVersion().getId()));
    switch (m.metadata_level) {
      case VersionedMetadataObject.DATASET:
        setDatasetMetadata(m);
        break;
      case VersionedMetadataObject.TABLE:
        getTableMetadata().put(m.table_name, m);
        break;
      case VersionedMetadataObject.COLUMN:
        getColumnMetadata().put(createColumnKey(m.table_name, m.column_name), m);
        break;
      default:
        assert (false);
    }
  }

  public void checkMetadataConsistency() throws Exception {
    if (datasetMetadata == null) {
      throw new Exception("Missing dataset metadata");
    }

    for (VersionedMetadataObject t : tableMetadata.values()) {
      if (!t.dataset_name.equals(datasetMetadata.dataset_name)) {
        throw new Exception(
            String.format(
                "Table %s has dataset %s instead of %s",
                t.table_name, t.dataset_name, datasetMetadata.dataset_name));
      }
    }

    for (VersionedMetadataObject c : columnMetadata.values()) {
      if (!c.dataset_name.equals(datasetMetadata.dataset_name)) {
        throw new Exception(
            String.format(
                "Column %s in table %s has dataset %s instead of %s",
                c.column_name, c.table_name, c.dataset_name, datasetMetadata.dataset_name));
      }

      if (!tableMetadata.containsKey(c.table_name)) {
        throw new Exception(
            String.format(
                "Column %s has table %s that is not preset in dataset %s",
                c.column_name, c.table_name, datasetMetadata.dataset_name));
      }
    }
  }

  public MetadataVersionObject getVersion() {
    return version;
  }

  public void setVersion(MetadataVersionObject version) {
    this.version = version;
  }

  /**
   * Metadata for the entire dataset - there should only ever be one since this represents a single
   * dataset.
   */
  public VersionedMetadataObject getDatasetMetadata() {
    return datasetMetadata;
  }

  public void setDatasetMetadata(VersionedMetadataObject datasetMetadata) {
    this.datasetMetadata = datasetMetadata;
  }

  /** All of the table level metadata - this is keyed by table name. */
  public Map<String, VersionedMetadataObject> getTableMetadata() {
    return tableMetadata;
  }

  public void setTableMetadata(Map<String, VersionedMetadataObject> tableMetadata) {
    this.tableMetadata = tableMetadata;
  }

  /** All of the column level metadata - this is keyed by table:column */
  public Map<String, VersionedMetadataObject> getColumnMetadata() {
    return columnMetadata;
  }

  public void setColumnMetadata(Map<String, VersionedMetadataObject> columnMetadata) {
    this.columnMetadata = columnMetadata;
  }

  public static class MissingMetadataException extends Exception {
    MissingMetadataException(String err) {
      super(err);
    }
  }

  public void calcuateTableStatistics() throws MissingMetadataException {
    getTableMetadata()
        .values()
        .forEach(
            table -> {
              table.load_time = 0L;
              table.update_time = 0L;
              table.num_columns = 0L;
              table.num_values = 0L;
            });

    for (VersionedMetadataObject v : getColumnMetadata().values()) {
      if (!getTableMetadata().containsKey(v.table_name)) {
        throw new MissingMetadataException(
            "Missing table level metadata for a column wth the table " + v.table_name);
      }

      VersionedMetadataObject table = getTableMetadata().get(v.table_name);

      table.load_time = Long.max(v.load_time, table.load_time);
      table.update_time = Long.max(v.update_time, table.update_time);
      table.num_columns += 1;
      table.num_values += v.num_values;

      String colOrigin =
          v.properties.getOrDefault(Const.METADATA_PROPERTY_ORIGIN, Const.Origin.UPLOAD.getCode());
      String tabOrigin =
          table.properties.getOrDefault(
              Const.METADATA_PROPERTY_ORIGIN, Const.Origin.UPLOAD.getCode());

      if (!tabOrigin.equals(colOrigin)) {
        table.properties.put(Const.METADATA_PROPERTY_ORIGIN, Const.Origin.MIXED.getCode());
      } else {
        table.properties.put(Const.METADATA_PROPERTY_ORIGIN, tabOrigin);
      }
    }
  }

  public void calculateDatasetStatistics() {
    getDatasetMetadata().load_time = 0L;
    getDatasetMetadata().update_time = 0L;
    getDatasetMetadata().num_tables = 0L;
    getDatasetMetadata().num_values = 0L;
    getDatasetMetadata().num_columns = 0L;

    for (VersionedMetadataObject t : getTableMetadata().values()) {
      getDatasetMetadata().load_time = Long.max(t.load_time, getDatasetMetadata().load_time);
      getDatasetMetadata().update_time = Long.max(t.update_time, getDatasetMetadata().update_time);
      getDatasetMetadata().num_tables += 1;
      getDatasetMetadata().num_columns += t.num_columns;
      getDatasetMetadata().num_values += t.num_values;

      String tableOrigin =
          t.properties.getOrDefault(Const.METADATA_PROPERTY_ORIGIN, Const.Origin.UPLOAD.getCode());
      String datasetOrigin =
          getDatasetMetadata()
              .properties
              .getOrDefault(Const.METADATA_PROPERTY_ORIGIN, Const.Origin.UPLOAD.getCode());

      if (!datasetOrigin.equals(tableOrigin)) {
        getDatasetMetadata()
            .properties
            .put(Const.METADATA_PROPERTY_ORIGIN, Const.Origin.MIXED.getCode());
      } else {
        getDatasetMetadata().properties.put(Const.METADATA_PROPERTY_ORIGIN, datasetOrigin);
      }
    }
  }

  /** Calculate the table and dataset level statistics. */
  public void calculateTableAndDatasetStatistics() throws MissingMetadataException {
    calcuateTableStatistics();
    calculateDatasetStatistics();
  }

  public void visitAll(Consumer<VersionedMetadataObject> f) {
    if (getDatasetMetadata() != null) {
      f.accept(getDatasetMetadata());
    }
    getTableMetadata().values().forEach(f);
    getColumnMetadata().values().forEach(f);
  }

  public void setVersionForAll(MetadataVersionObject version) {
    this.version = version;
    visitAll(m -> m.version_id = version.id);
  }

  public void replaceTablesAndColumns(VersionedDatasetMetadata other) {
    // Replace tables and collect up the table names
    HashSet<String> tableSet = new HashSet<>();
    for (Entry<String, VersionedMetadataObject> table : other.getTableMetadata().entrySet()) {
      getTableMetadata().put(table.getKey(), table.getValue());
      tableSet.add(table.getKey());
    }

    // First remove the columns associated with the tables that we replaced
    setColumnMetadata(
        getColumnMetadata().entrySet().stream()
            .filter(entry -> !(tableSet.contains(entry.getValue().table_name)))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));

    // Copy over the columns
    for (Entry<String, VersionedMetadataObject> column : other.getColumnMetadata().entrySet()) {
      getColumnMetadata().put(column.getKey(), column.getValue());
    }
  }

  /**
   * Take any properties for the existing dataset / table / column metadata and copy it over to this
   * metadata. This simply skips any tables present in other that aren't present in this metadata.
   *
   * <p>Origin is skipped always as that should always be set when a column is created and be
   * calculated for table and dataset.
   *
   * @param other
   */
  public void setMergedProperties(VersionedDatasetMetadata other) {
    this.getDatasetMetadata().setMergedProperties(other.getDatasetMetadata().getProperties());

    for (Entry<String, VersionedMetadataObject> table : getTableMetadata().entrySet()) {
      if (other.getTableMetadata().containsKey(table.getKey())) {
        table
            .getValue()
            .setMergedProperties(other.getTableMetadata().get(table.getKey()).getProperties());
      }
    }

    for (Entry<String, VersionedMetadataObject> column : getColumnMetadata().entrySet()) {
      if (other.getColumnMetadata().containsKey(column.getKey())) {
        column
            .getValue()
            .setMergedProperties(other.getColumnMetadata().get(column.getKey()).getProperties());
      }
    }
  }

  public String createColumnKey(String table, String column) {
    return String.format("%s:%s", table, column);
  }

  public VersionedMetadataObject getColumnMetadataByName(String tableName, String columnName)
      throws MissingMetadataException {
    VersionedMetadataObject c = getColumnMetadata().get(createColumnKey(tableName, columnName));
    if (c == null) {
      throw new MissingMetadataException("Could not find metadata for column " + columnName);
    }

    return c;
  }

  public VersionedMetadataObject getTableMetadataByName(String tableName)
      throws MissingMetadataException {
    if (!getTableMetadata().containsKey(tableName)) {
      throw new MissingMetadataException("Could not find metadata for table " + tableName);
    }

    return getTableMetadata().get(tableName);
  }

  public List<String> sortedColumnNames(String tableName) {
    return StreamSupport.stream(getColumnMetadata().values().spliterator(), false)
        .filter(x -> x.table_name.equals(tableName))
        .sorted(Comparator.comparingInt(a -> a.column_num))
        .map(x -> x.column_name)
        .collect(Collectors.toList());
  }

  public void put(Context context, boolean dataset, boolean tables, boolean columns)
      throws BasicAccumuloException, IOException {

    BatchWriter writer = context.createBatchWriter(getDatasetMetadata().getTable(context));
    put(context, writer, dataset, tables, columns);
    try {
      writer.close();
    } catch (MutationsRejectedException e) {
      throw new BasicAccumuloException(e.toString());
    }
  }

  public void put(
      Context context, BatchWriter writer, boolean dataset, boolean tables, boolean columns)
      throws BasicAccumuloException, IOException {

    if (dataset && getDatasetMetadata() != null) {
      getDatasetMetadata().put(context, writer);
    }

    if (tables) {
      for (VersionedMetadataObject o : getTableMetadata().values()) {
        o.put(context, writer);
      }
    }

    if (columns) {
      for (VersionedMetadataObject o : getColumnMetadata().values()) {
        o.put(context, writer);
      }
    }
  }

  public void putAll(Context context) throws IOException, BasicAccumuloException {
    put(context, true, true, true);
  }

  public void putAll(Context context, BatchWriter writer)
      throws IOException, BasicAccumuloException {
    put(context, writer, true, true, true);
  }

  public void putDatasetAndTables(Context context) throws IOException, BasicAccumuloException {
    put(context, true, true, false);
  }

  public void displayInfo() {
    System.out.println("Dataset");
    System.out.println(getDatasetMetadata());

    System.out.println("Tables");
    for (VersionedMetadataObject t : getTableMetadata().values()) {
      System.out.println(t);
      for (VersionedMetadataObject c : getColumnMetadata().values()) {
        if (c.table_name.equals(t.table_name)) {
          System.out.println("\t" + c);
        }
      }
    }
  }
}
