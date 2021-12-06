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
import com.dataprofiler.util.Const.SortOrder;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.iterators.ColumnCountVisibilityIterator;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import org.apache.accumulo.core.client.BatchDeleter;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.lexicoder.LongLexicoder;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.commons.lang.SerializationUtils;
import org.apache.hadoop.io.Text;
import org.apache.log4j.Logger;

public class ColumnCountPaginationObject
    extends VersionedPurgableDatasetObject<ColumnCountPaginationObject> {

  public static final Logger logger = Logger.getLogger(ColumnCountPaginationObject.class);

  private static final LongLexicoder longLex = new LongLexicoder();

  private static final String accumuloTable = Const.ACCUMULO_COLUMN_COUNTS_TABLE_ENV_KEY;

  // Cell Level Visibility Iterator Setting
  private static final IteratorSetting visibilityIteratorSetting =
      new IteratorSetting(21, "colCntVisItr", ColumnCountVisibilityIterator.class);

  private ColumnCountObject columnCountObj;
  private Long index;

  public ColumnCountPaginationObject() {
    super(accumuloTable);
  }

  public ColumnCountPaginationObject(Long index, ColumnCountObject colCntObj) {
    this();
    this.index = index;
    this.columnCountObj = colCntObj;
  }

  public ObjectScannerIterable<ColumnCountPaginationObject> generateIndexes(
      Context context,
      String datasetName,
      String tableName,
      String columnName,
      SortOrder sortOrder) {

    Range range = createInclusiveRange(datasetName, tableName, columnName, sortOrder.GetCode());
    return this.generateIndexes(context, range);
  }

  @Override
  public ObjectScannerIterable<ColumnCountPaginationObject> scan(Context context) {
    return super.scan(context).fetchColumnFamily(Const.COL_FAM_INDEX);
  }

  public ObjectScannerIterable<ColumnCountPaginationObject> generateIndexes(
      Context context, Range range) {
    return scan(context)
        .fetchColumnFamily(Const.COL_FAM_DATA)
        .addRange(range)
        .addScanIterator(visibilityIteratorSetting);
  }

  public ObjectScannerIterable<ColumnCountPaginationObject> getIndicies(
      Context context, VersionedMetadataObject columnMetadata, SortOrder sortOrder) {
    logger.info("getIndicies");
    logger.info(context);
    logger.info(columnMetadata.toString());

    return scan(context)
        .addRange(
            createInclusiveRange(
                columnMetadata.dataset_name,
                columnMetadata.table_id,
                columnMetadata.column_name,
                sortOrder.GetCode()))
        .addScanIterator(visibilityIteratorSetting);
  }

  @Override
  public ColumnCountPaginationObject fromEntry(Entry<Key, Value> entry) {

    ColumnCountPaginationObject result = new ColumnCountPaginationObject();

    if (entry.getKey().getColumnFamily().toString().equals(Const.COL_FAM_DATA)) {

      ColumnCountObject colCount = new ColumnCountObject();
      result.columnCountObj = colCount.fromEntry(entry);
      result.index = Long.valueOf(entry.getValue().toString());
    } else {
      HashMap<String, byte[]> map =
          (HashMap<String, byte[]>) SerializationUtils.deserialize(entry.getValue().get());
      Key key =
          new Key(new Text(map.get("row")), new Text(Const.COL_FAM_DATA), new Text(map.get("col")));
      ColumnCountObject colCount = new ColumnCountObject();

      result.columnCountObj = colCount.fromEntry(new SimpleEntry<>(key, new Value()));
      result.index = longLex.decode(entry.getKey().getColumnQualifier().getBytes());
    }

    result.updatePropertiesFromEntry(entry);

    return result;
  }

  /**
   * Creates an Accumulo Key with the following format:
   *
   * <p>RowID: <code>dataset\x00table\x00column\x00sort_order\x00index</code>
   *
   * <p>ColFam: <code>I</code>
   *
   * <p>ColQual: <code>index</code>
   *
   * @return
   * @throws InvalidDataFormat
   */
  @Override
  public Key createAccumuloKey() throws InvalidDataFormat {
    byte[] encIdx = longLex.encode(index);

    byte[] rowID =
        joinKeyComponents(
            columnCountObj.dataset.getBytes(),
            columnCountObj.tableId.getBytes(),
            columnCountObj.column.getBytes(),
            columnCountObj.sortOrder.GetCode().getBytes(),
            encIdx);

    return new Key(
        new Text(rowID),
        new Text(Const.COL_FAM_INDEX),
        new Text(encIdx),
        new Text(columnCountObj.visibility));
  }

  @Override
  public Value createAccumuloValue() throws InvalidDataFormat {
    Key key = columnCountObj.createAccumuloKey();
    HashMap<String, byte[]> val = new HashMap<>();
    val.put("row", key.getRow().getBytes());
    val.put("col", key.getColumnQualifier().getBytes());

    return new Value(SerializationUtils.serialize(val));
  }

  @Override
  public void bulkPurgeTable(Context context, String datasetName, String tableName, String tableId)
      throws BasicAccumuloException {
    Text start = new Text(joinKeyComponentsEndDelimited(datasetName, tableId));
    Text end = new Text(joinKeyComponentsEndDelimited(datasetName, tableId) + Const.HIGH_BYTE);
    bulkPurgeRange(context, start, end);
  }

  @Override
  protected void bulkPurgeRange(Context context, Text start, Text end)
      throws BasicAccumuloException {
    String accumuloTable = getTable(context);

    BatchDeleter deleter = context.createBatchDeleter(accumuloTable);
    deleter.setRanges(Collections.singleton(new Range(start, end)));
    deleter.fetchColumnFamily(new Text(Const.COL_FAM_INDEX));

    logger.warn(
        String.format("Deleting range ('%s', '%s'] from table %s", start, end, accumuloTable));
    try {
      deleter.delete();
    } catch (Exception e) {
      throw new BasicAccumuloException(e.getMessage());
    } finally {
      deleter.close();
    }
  }

  public ColumnCountObject getColumnCountObj() {
    return columnCountObj;
  }

  public void setColumnCountObj(ColumnCountObject columnCountObj) {
    this.columnCountObj = columnCountObj;
  }

  public Long getIndex() {
    return index;
  }

  public void setIndex(Long index) {
    this.index = index;
  }
}
