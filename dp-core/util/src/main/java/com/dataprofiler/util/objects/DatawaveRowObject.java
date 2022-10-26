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

import com.dataprofiler.querylang.expr.Expression;
import com.dataprofiler.querylang.json.Expressions;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Const;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.RangeCheckingVisitor;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.accumulo.core.client.lexicoder.LongLexicoder;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.hadoop.io.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatawaveRowObject extends VersionedPurgableDatasetObject<DatawaveRowObject> {

  private static final LongLexicoder longLex = new LongLexicoder();

  private static final Text COL_FAM_DATA = new Text(Const.COL_FAM_DATA);
  private static final Text DELIMITER = new Text(Const.DELIMITER);
  private static final int DELIMITER_LEN = DELIMITER.getLength();

  private static final TypeReference<Map<String, String>> staticTypeReference =
      new TypeReference<Map<String, String>>() {};

  private final Logger log = LoggerFactory.getLogger(getClass());

  private long shard;
  private String datasetName;
  private String tableName; // This is really table id, but the name has not been updated TODO
  private String columnName;
  private String columnValue;
  private String columnVisibility;
  private long rowIdx;
  private Map<String, String> row = Collections.emptyMap();

  public DatawaveRowObject(
      long shard,
      String datasetName,
      String tableName,
      String columnName,
      String columnValue,
      long rowIdx) {
    this();
    this.shard = shard;
    this.datasetName = datasetName;
    this.tableName = tableName;
    this.columnName = columnName;
    this.columnValue = columnValue;
    this.rowIdx = rowIdx;
  }

  public DatawaveRowObject(
      long shard, String datasetName, String tableName, Map<String, String> row, long rowIdx) {
    this();
    this.shard = shard;
    this.datasetName = datasetName;
    this.tableName = tableName;
    this.row = row;
    this.rowIdx = rowIdx;
  }

  public DatawaveRowObject() {
    super(Const.ACCUMULO_DATAWAVE_ROWS_TABLE_ENV_KEY);
  }

  public static Text[] datasetTableShardFromRowId(Text rowId) {

    Text[] output = new Text[3];

    int firstDelimIdx = rowId.find(DELIMITER.toString());
    if (firstDelimIdx < 0) {
      throw new IllegalArgumentException("bad docid: " + rowId);
    }

    Text datasetName = new Text();
    datasetName.set(rowId.getBytes(), 0, firstDelimIdx);
    output[0] = datasetName;

    int secondDelimIdx = rowId.find(DELIMITER.toString(), firstDelimIdx + DELIMITER_LEN);
    if (secondDelimIdx < 0) {
      throw new IllegalArgumentException("bad docid: " + rowId);
    }

    Text tableName = new Text();
    tableName.set(
        rowId.getBytes(),
        firstDelimIdx + DELIMITER_LEN,
        secondDelimIdx - firstDelimIdx - DELIMITER_LEN);
    output[1] = tableName;

    Text shard = new Text();

    shard.set(
        rowId.getBytes(),
        secondDelimIdx + DELIMITER_LEN,
        rowId.getBytes().length - secondDelimIdx - DELIMITER_LEN);

    output[2] = shard;

    return output;
  }

  public Map<String, String> getRow() {
    return row;
  }

  public void setRow(Map<String, String> row) {
    this.row = row;
  }

  public long getShard() {
    return shard;
  }

  public void setShard(long shard) {
    this.shard = shard;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getColumnName() {
    return columnName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  public String getColumnValue() {
    return columnValue;
  }

  public void setColumnValue(String columnValue) {
    this.columnValue = columnValue;
  }

  public String getColumnVisibility() {
    return columnVisibility;
  }

  public void setColumnVisibility(String columnVisibility) {
    this.columnVisibility = columnVisibility;
  }

  public long getRowIdx() {
    return rowIdx;
  }

  public void setRowIdx(long rowIdx) {
    this.rowIdx = rowIdx;
  }

  public ObjectScannerIterable<DatawaveRowObject> find(
      Context context, VersionedDataScanSpec spec) {

    ObjectScannerIterable<DatawaveRowObject> scanner =
        super.scan(context).setBatch(spec.getPageSize() == null);

    if (spec.getPageSize() != null) {
      scanner.setMultiRangeScanner();
      TreeSet<DatawaveRowShardIndexObject> shardRanges =
          new TreeSet<>(
              DatawaveRowShardIndexObject.shardIndicesFor(
                  spec.getDataset(), spec.getTable_id(), context));
      long[] startComponents = parseStartLocationComponents(spec);
      long rowShard = startComponents[0];
      long rowIndex = startComponents[1];
      createPaginationBounds(rowShard, rowIndex, spec, shardRanges).stream()
          .map(DatawaveRowShardIndexObject::toRange)
          .forEach(scanner::addRange);
    } else {
      List<Range> ranges =
          DatawaveRowShardIndexObject.shardIndicesFor(
                  spec.getDataset(), spec.getTable_id(), context)
              .stream()
              .map(DatawaveRowShardIndexObject::toRowDataRange)
              .collect(Collectors.toList());
      if (ranges.isEmpty()) {
        throw new IllegalStateException(
            "Could not find any shard indices for " + spec.getDataset() + ":" + spec.getTable_id());
      }
      ranges.forEach(scanner::addRange);
    }

    if (spec.getV2Query() != null) {
      configureQueryIterator(scanner, spec);
    } else {
      configureRowDataIterator(scanner, spec);
    }

    return scanner;
  }

  private void configureQueryIterator(
      ObjectScannerIterable<DatawaveRowObject> scanner, VersionedDataScanSpec spec) {
    checkQuery(spec.getV2Query());
    IteratorSetting iter =
        new IteratorSetting(
            21, "QueryIterator", "com.dataprofiler.iterators.rowData.v2.RowAggregator");
    iter.addOption("query", Expressions.json(spec.getV2Query()));

    if (spec.getReturnFullValues()) {
      iter.addOption("hash_large_values", Boolean.toString((!spec.getReturnFullValues())));
    }
    if (spec.getReturnVisibilities()) {
      iter.addOption("include_visibility", Boolean.toString((spec.getReturnVisibilities())));
    }
    scanner.addScanIterator(iter);
  }

  private void configureRowDataIterator(
      ObjectScannerIterable<DatawaveRowObject> scanner, VersionedDataScanSpec spec) {
    scanner.fetchColumnFamily(Const.COL_FAM_DATA);
    IteratorSetting iter =
        new IteratorSetting(
            21, "RowDataIter", "com.dataprofiler.iterators.rowData.DatawaveRowDataIter");

    if (spec.getReturnFullValues()) {
      iter.addOption("hash_large_values", Boolean.toString((!spec.getReturnFullValues())));
    }
    if (spec.getReturnVisibilities()) {
      iter.addOption("include_visibility", Boolean.toString((spec.getReturnVisibilities())));
    }
    scanner.addScanIterator(iter);
  }

  public void checkQuery(Expression expr) {
    if (expr == null) {
      throw new IllegalArgumentException("Can't have null query");
    }

    RangeCheckingVisitor rangeCheck = new RangeCheckingVisitor();
    expr.accept(rangeCheck);
    if (!rangeCheck.getLastStatus()) {
      throw new IllegalArgumentException(
          "Requested range operation without a pivot (AND'd with a fixed term)");
    }
  }

  public static SortedSet<DatawaveRowShardIndexObject> createPaginationBounds(
      long rowShard,
      long rowIdx,
      DataScanSpec spec,
      SortedSet<DatawaveRowShardIndexObject> allShards) {

    DatawaveRowShardIndexObject startShard =
        new DatawaveRowShardIndexObject(rowShard, spec.getDataset(), spec.getTable(), rowIdx);

    SortedSet<DatawaveRowShardIndexObject> tail = allShards.tailSet(startShard);

    /* if empty, we're probably in the last shard
     *
     * otherwise, we'll need to add the starting document
     * to the head of the set, because we've skipped over
     * the starting record in its shard
     */
    if (tail.isEmpty() || !tail.first().equals(startShard)) {
      tail.add(startShard);
    }

    return tail;
  }

  private long[] parseStartLocationComponents(VersionedDataScanSpec spec) {
    long startShard = 0L;
    long startRowIdx = 0L;

    if (spec.getStartLocation() != null && spec.getStartLocation().contains("_")) {
      String[] startLocationComponents = spec.getStartLocation().split("_");
      if (startLocationComponents.length != 2) {
        throw new IllegalArgumentException("invalid 'startLocation'");
      }
      startShard = Long.parseLong(startLocationComponents[0]);
      startRowIdx = Long.parseLong(startLocationComponents[1]);
    }

    return new long[] {startShard, startRowIdx};
  }

  private String[] removeCarriageReturn(String[] array) {
    for (int i = 0; i < array.length; i++) {
      if (array[i].endsWith("\r")) {
        array[i] = array[i].substring(0, array[i].length() - 1);
      }
    }
    return array;
  }

  @Override
  public ObjectScannerIterable<DatawaveRowObject> scan(Context context) {

    ObjectScannerIterable<DatawaveRowObject> scanner = super.scan(context);

    scanner.fetchColumnFamily(Const.COL_FAM_DATA);

    IteratorSetting iter =
        new IteratorSetting(
            21, "RowDataIter", "com.dataprofiler.iterators.rowData.DatawaveRowDataIter");
    scanner.addScanIterator(iter);

    return scanner;
  }

  @Override
  public DatawaveRowObject fromEntry(Entry<Key, Value> entry) {

    Key key = entry.getKey();

    DatawaveRowObject obj = new DatawaveRowObject();

    Text[] datasetTableShard = datasetTableShardFromRowId(key.getRow());

    obj.setDatasetName(datasetTableShard[0].toString());
    obj.setTableName(datasetTableShard[1].toString());
    obj.setShard(longLex.decode(datasetTableShard[2].getBytes()));

    if (isDataRow(key)) {
      obj.setRowIdx(rowIndexFromKey(key));
    } else {
      Text[] colValRowIdxDataType = DatawaveRowIndexObject.colValRowIdxDataTypeFromKey(key);
      obj.setRowIdx(longLex.decode(colValRowIdxDataType[1].getBytes()));
    }

    obj.updatePropertiesFromEntry(entry);

    try {
      obj.setRow(mapper.readValue(entry.getValue().toString(), staticTypeReference));
    } catch (IOException e) {
      throw new InvalidDataFormat(e.toString());
    }

    return obj;
  }

  @Override
  public Key createAccumuloKey() throws InvalidDataFormat {

    byte[] rowId =
        joinKeyComponents(datasetName.getBytes(), tableName.getBytes(), longLex.encode(shard));
    byte[] colQual = joinKeyComponents(longLex.encode(rowIdx), columnName.getBytes());
    return new Key(new Text(rowId), COL_FAM_DATA, new Text(colQual), new Text(visibility));
  }

  @Override
  public Value createAccumuloValue() throws InvalidDataFormat {
    return new Value(columnValue.getBytes());
  }

  private long rowIndexFromKey(Key key) {
    Text docID = new Text();

    docID.set(key.getColumnQualifier().getBytes(), 0, Const.LONG_LEX_LEN);
    return longLex.decode(docID.getBytes());
  }

  private Text colNameFromKey(Key key) {
    Text colq = key.getColumnQualifier();
    Text colName = new Text();
    colName.set(
        colq.getBytes(),
        Const.LONG_LEX_LEN + DELIMITER_LEN,
        colq.getBytes().length - Const.LONG_LEX_LEN - DELIMITER_LEN);
    return colName;
  }

  @Override
  public void bulkPurgeTable(Context context, String datasetName, String tableName, String tableId)
      throws BasicAccumuloException {
    Text start = new Text(joinKeyComponentsEndDelimited(datasetName, tableId));
    Text end =
        new Text(
            joinKeyComponentsEndDelimited(datasetName, tableId) + longLex.encode(Long.MAX_VALUE));
    bulkPurgeRange(context, start, end);
  }

  public void bulkDeleteContainingTable(Context context) throws BasicAccumuloException {
    super.bulkDeleteContainingTable(context);
    String currTable = getTable(context);

    try {
      TableOperations tops = context.getClient().tableOperations();

      Map<String, Set<Text>> groups = new HashMap<>();

      String group = Const.COL_FAM_SHARD;
      groups.put(group, new HashSet<>(Arrays.asList(new Text(group))));

      tops.setLocalityGroups(currTable, groups);
    } catch (Exception e) {
      throw new BasicAccumuloException(e.getMessage());
    }
  }

  private boolean isDataRow(Key key) {
    Text rowIdType = new Text();
    rowIdType.set(key.getColumnFamily().getBytes(), 0, 1);

    return rowIdType.equals(COL_FAM_DATA);
  }
}
