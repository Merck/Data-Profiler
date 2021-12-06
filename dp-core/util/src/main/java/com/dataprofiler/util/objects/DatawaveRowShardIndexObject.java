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

import static com.dataprofiler.util.objects.DatawaveRowObject.datasetTableShardFromRowId;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Const;
import com.dataprofiler.util.Context;
import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import org.apache.accumulo.core.client.lexicoder.LongLexicoder;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.hadoop.io.Text;

public class DatawaveRowShardIndexObject
    extends VersionedPurgableDatasetObject<DatawaveRowShardIndexObject>
    implements Comparable<DatawaveRowShardIndexObject> {

  private static final LongLexicoder longLex = new LongLexicoder();

  private static final Text COL_FAM_SHARD = new Text(Const.COL_FAM_SHARD);
  private static final Text DELIMITER = new Text(Const.DELIMITER);
  private static final int DELIMITER_LEN = DELIMITER.getLength();

  public static Comparator<DatawaveRowShardIndexObject> comparator() {
    return (a, b) -> {
      int ret = a.getDatasetName().compareTo(b.getDatasetName());
      if (ret != 0) {
        return ret;
      }

      ret = a.getTableName().compareTo(b.getTableName());
      if (ret != 0) {
        return ret;
      }

      // we lose distance here doing a trunction from long -> int
      long diff = a.getShard() - b.getShard();
      if (diff > 0) {
        return 1;
      } else if (diff < 0) {
        return -1;
      }

      diff = a.getRowIdx() - b.getRowIdx();
      if (diff > 0) {
        return 1;
      } else if (diff < 0) {
        return -1;
      }

      return 0;
    };
  }

  private static final Comparator<DatawaveRowShardIndexObject> COMPARATOR = comparator();

  public static List<DatawaveRowShardIndexObject> shardIndicesFor(
      String datasetName, String tableID, Context context) {
    List<DatawaveRowShardIndexObject> indices = new ArrayList<>();
    DatawaveRowShardIndexObject shardIndexLookup = new DatawaveRowShardIndexObject();
    ObjectScannerIterable<DatawaveRowShardIndexObject> shardScanner =
        shardIndexLookup.scan(context).addRange(new Range(joinKeyComponents(datasetName, tableID)));
    /* Workaround for a bug where the shard IDs in the value of these entries incorrect.
     * We expect the ids to be in monotonically increasing order, starting with 0. The
     * trick here is that we count the number of entries returned by our scan, and generate
     * the row values by using the dataset + table_id prefix we already know is correct, then
     * appending the current count.
     */
    long partitionNumber = 0;
    for (DatawaveRowShardIndexObject drsi : shardScanner) {
      if (drsi.getRowIdx() == Long.MAX_VALUE) {
        continue;
      }
      drsi.setShard(partitionNumber++);
      indices.add(drsi);
    }
    return indices;
  }

  // A lexicoded long is always 9 bytes long
  private long shard;
  private String datasetName;
  private String tableName;
  private long rowIdx;

  public DatawaveRowShardIndexObject() {
    super(Const.ACCUMULO_DATAWAVE_ROWS_TABLE_ENV_KEY);
  }

  public DatawaveRowShardIndexObject(
      long shard, String datasetName, String tableName, long rowIdx) {
    this();
    this.shard = shard;
    this.datasetName = datasetName;
    this.tableName = tableName; // tablename is really tableid, TODO rename
    this.rowIdx = rowIdx;
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

  public long getRowIdx() {
    return rowIdx;
  }

  public void setRowIdx(long rowIdx) {
    this.rowIdx = rowIdx;
  }

  @Override
  public ObjectScannerIterable<DatawaveRowShardIndexObject> scan(Context context) {
    return super.scan(context).fetchColumnFamily(Const.COL_FAM_SHARD);
  }

  @Override
  public DatawaveRowShardIndexObject fromEntry(Entry<Key, Value> entry) {
    // TODO - this does not appear to be working, but is not currently used
    Key key = entry.getKey();

    DatawaveRowShardIndexObject obj = new DatawaveRowShardIndexObject();

    obj.setRowIdx(longLex.decode(key.getColumnQualifier().getBytes()));

    Text[] datasetTableShard = datasetTableShardFromRowId(new Text(entry.getValue().get()));
    obj.setDatasetName(datasetTableShard[0].toString());
    obj.setTableName(datasetTableShard[1].toString());

    obj.setShard(longLex.decode(datasetTableShard[2].getBytes()));

    obj.updatePropertiesFromEntry(entry);

    return obj;
  }

  @Override
  public Key createAccumuloKey() throws InvalidDataFormat {

    byte[] rowId = joinKeyComponents(datasetName.getBytes(), tableName.getBytes());
    byte[] colQual = longLex.encode(rowIdx);

    return new Key(new Text(rowId), COL_FAM_SHARD, new Text(colQual), new Text(visibility));
  }

  @Override
  public Value createAccumuloValue() throws InvalidDataFormat {

    return new Value(
        joinKeyComponents(datasetName.getBytes(), tableName.getBytes(), longLex.encode(shard)));
  }

  @Override
  public void bulkPurgeTable(Context context, String datasetName, String tableName, String tableId)
      throws BasicAccumuloException {
    // TODO - this isn't currently tested because we can't read these object back.
    Text start = new Text(prevVal(joinKeyComponents(datasetName, tableId)));
    Text end = new Text(joinKeyComponents(datasetName, tableId));
    bulkPurgeRange(context, start, end);
  }

  public Range toRowDataRange() {
    byte[] row =
        joinKeyComponents(
            getDatasetName().getBytes(UTF_8),
            getTableName().getBytes(UTF_8),
            longLex.encode(getShard()));
    return new Range(new Text(row));
  }

  /** Use when you might have a starting key that contains a document ID. */
  public Range toRange() {
    byte[] row =
        joinKeyComponents(
            getDatasetName().getBytes(UTF_8),
            getTableName().getBytes(UTF_8),
            longLex.encode(getShard()));

    byte[] columnFamily = "D".getBytes(UTF_8);

    byte[] documentId = longLex.encode(getRowIdx());

    byte[] maxDocumentId = longLex.encode(Long.MAX_VALUE);

    return new Range(
        /*start*/ new Key(row, columnFamily, documentId),
        /*end  */ new Key(row, columnFamily, maxDocumentId));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DatawaveRowShardIndexObject that = (DatawaveRowShardIndexObject) o;
    return getShard() == that.getShard()
        && getRowIdx() == that.getRowIdx()
        && Objects.equal(getDatasetName(), that.getDatasetName())
        && Objects.equal(getTableName(), that.getTableName());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getShard(), getDatasetName(), getTableName(), getRowIdx());
  }

  @Override
  public int compareTo(DatawaveRowShardIndexObject other) {
    return COMPARATOR.compare(this, other);
  }
}
