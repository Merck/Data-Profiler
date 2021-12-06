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

import com.dataprofiler.util.Const;
import java.util.Map.Entry;
import org.apache.accumulo.core.client.lexicoder.LongLexicoder;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.hadoop.io.Text;

public class DatawaveRowIndexObject extends AccumuloObject<DatawaveRowIndexObject> {
  private static final LongLexicoder longLex = new LongLexicoder();

  private static final Text COL_FAM_INDEX = new Text(Const.COL_FAM_INDEX);
  private static final Text DELIMITER = new Text(Const.DELIMITER);
  private static final int DELIMITER_LEN = DELIMITER.getLength();

  private long shard;
  private String datasetName;
  private String tableName;
  private String columnName;
  private String columnValue;
  private String dataType;
  private long rowIdx;

  public DatawaveRowIndexObject() {
    super(Const.ACCUMULO_DATAWAVE_ROWS_TABLE_ENV_KEY);
  }

  public DatawaveRowIndexObject(
      long shard,
      String datasetName,
      String tableName,
      String columnName,
      String columnValue,
      String dataType,
      long rowIdx,
      String visibility) {
    this();
    this.shard = shard;
    this.datasetName = datasetName;
    this.tableName = tableName;
    this.columnName = columnName;
    this.columnValue = columnValue;
    this.dataType = dataType;
    this.rowIdx = rowIdx;
    this.visibility = visibility;
  }

  public DatawaveRowIndexObject(
      long shard,
      String datasetName,
      String tableName,
      String columnName,
      String columnValue,
      String dataType,
      long rowIdx) {
    this(shard, datasetName, tableName, columnName, columnValue, dataType, rowIdx, null);
  }

  public static Text[] colValRowIdxDataTypeFromKey(Key key) {

    Text colQ = key.getColumnQualifier();

    Text[] output = new Text[3];

    int firstDelimIdx = colQ.find(DELIMITER.toString());
    if (firstDelimIdx < 0) {
      throw new IllegalArgumentException("bad docid: " + colQ);
    }

    Text colVal = new Text();
    colVal.set(colQ.getBytes(), 0, firstDelimIdx);
    output[0] = colVal;

    int secondDelimIdx = firstDelimIdx + Const.LONG_LEX_LEN + DELIMITER_LEN;

    Text rowIdx = new Text();
    rowIdx.set(colQ.getBytes(), firstDelimIdx + DELIMITER_LEN, Const.LONG_LEX_LEN);
    output[1] = rowIdx;

    Text dataType = new Text();

    dataType.set(
        colQ.getBytes(),
        secondDelimIdx + DELIMITER_LEN,
        colQ.getBytes().length - secondDelimIdx - DELIMITER_LEN);

    output[2] = dataType;

    return output;
  }

  public String getDataType() {
    return dataType;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
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

  public long getRowIdx() {
    return rowIdx;
  }

  public void setRowIdx(long rowIdx) {
    this.rowIdx = rowIdx;
  }

  @Override
  public DatawaveRowIndexObject fromEntry(Entry<Key, Value> entry) {

    Key key = entry.getKey();

    DatawaveRowIndexObject obj = new DatawaveRowIndexObject();

    Text[] datasetTableShard = DatawaveRowObject.datasetTableShardFromRowId(key.getRow());
    obj.setDatasetName(datasetTableShard[0].toString());
    obj.setTableName(datasetTableShard[1].toString());
    obj.setShard(longLex.decode(datasetTableShard[2].getBytes()));

    obj.setColumnName(colNameFromKey(key).toString());

    Text[] colValRowIdxDataType = colValRowIdxDataTypeFromKey(key);
    obj.setColumnValue(colValRowIdxDataType[0].toString());
    obj.setRowIdx(longLex.decode(colValRowIdxDataType[1].getBytes()));
    obj.setDataType(colValRowIdxDataType[2].toString());

    obj.updatePropertiesFromEntry(entry);

    return obj;
  }

  @Override
  public Key createAccumuloKey() throws InvalidDataFormat {

    byte[] rowId =
        joinKeyComponents(datasetName.getBytes(), tableName.getBytes(), longLex.encode(shard));
    byte[] colFam = joinKeyComponents(COL_FAM_INDEX.getBytes(), columnName.getBytes());
    byte[] colQual =
        joinKeyComponents(columnValue.getBytes(), longLex.encode(rowIdx), dataType.getBytes());

    return new Key(new Text(rowId), new Text(colFam), new Text(colQual), new Text(visibility));
  }

  public static Text colNameFromKey(Key key) {

    Text colFam = key.getColumnFamily();

    int delimIdx = colFam.find(DELIMITER.toString());
    if (delimIdx < 0) {
      throw new IllegalArgumentException("bad docid: " + colFam);
    }

    Text datasetName = new Text();
    datasetName.set(
        colFam.getBytes(),
        delimIdx + DELIMITER_LEN,
        colFam.getBytes().length - delimIdx - DELIMITER_LEN);

    return datasetName;
  }
}
