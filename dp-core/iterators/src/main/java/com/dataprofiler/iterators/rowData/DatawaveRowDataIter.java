package com.dataprofiler.iterators.rowData;

/*-
 * 
 * dataprofiler-iterators
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

import com.dataprofiler.iterators.rowData.v2.RowAggregator;
import com.dataprofiler.util.Const;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.accumulo.core.client.lexicoder.LongLexicoder;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.hadoop.io.Text;
import org.apache.htrace.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.htrace.fasterxml.jackson.databind.ObjectMapper;

public class DatawaveRowDataIter implements SortedKeyValueIterator<Key, Value> {

  private static final LongLexicoder longLex = new LongLexicoder();
  private static final Text DELIMITER = new Text("\u0000");
  private static final int DELIMITER_LEN = DELIMITER.getLength();
  // A lexicoded long is always 9 bytes long
  private static final ObjectMapper mapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private SortedKeyValueIterator<Key, Value> source;
  private Key nextKey;
  private Key currKey;
  private Value topValue;
  private boolean hasTop = false;
  private boolean hashLargeValues = true;
  private boolean includeVisibility = false;

  public DatawaveRowDataIter() {}

  private DatawaveRowDataIter(DatawaveRowDataIter other, IteratorEnvironment env) {
    // Copy the source iterator
    if (other.source != null) {
      source = other.source.deepCopy(env);
    }

    if (other.currKey != null) {
      currKey = new Key(other.currKey);
    }

    if (other.topValue != null) {
      topValue = new Value(other.topValue);
    }

    if (other.nextKey != null) {
      nextKey = new Key(other.nextKey);
    }

    hasTop = other.hasTop;
  }

  @Override
  public void init(
      SortedKeyValueIterator<Key, Value> sortedKeyValueIterator,
      Map<String, String> map,
      IteratorEnvironment iteratorEnvironment)
      throws IOException {
    this.source = sortedKeyValueIterator.deepCopy(iteratorEnvironment);
    this.currKey = new Key();
    this.nextKey = new Key();

    if (map.containsKey(RowAggregator.CONF_HASH_LARGE_VALUES)) {
      this.hashLargeValues = Boolean.parseBoolean(map.get(RowAggregator.CONF_HASH_LARGE_VALUES));
    }

    if (map.containsKey(RowAggregator.CONF_INCLUDE_VISIBILITY_WITH_VALUE)) {
      this.includeVisibility =
          Boolean.parseBoolean(map.get(RowAggregator.CONF_INCLUDE_VISIBILITY_WITH_VALUE));
    }
  }

  @Override
  public boolean hasTop() {
    return hasTop;
  }

  @Override
  public void next() throws IOException {
    buildRow();
  }

  @Override
  public void seek(Range range, Collection<ByteSequence> collection, boolean b) throws IOException {
    source.seek(range, collection, b);
    buildRow();
  }

  @Override
  public Key getTopKey() {
    return currKey;
  }

  @Override
  public Value getTopValue() {
    return topValue;
  }

  @Override
  public SortedKeyValueIterator<Key, Value> deepCopy(IteratorEnvironment iteratorEnvironment) {
    return new DatawaveRowDataIter(this, iteratorEnvironment);
  }

  private void buildRow() throws IOException {

    Map<String, String> output = new HashMap<>();

    long currIdx = -1;
    hasTop = false;

    if (source.hasTop()) {

      // If next is set and source's top key is not equal to next it means deepCopy was called and
      // we need to increment source
      if (nextKey.getLength() > 0 && !nextKey.equals(source.getTopKey())) {
        hasTop = true;
        source.next();
        return;
      }

      currKey.set(source.getTopKey());
      currIdx = rowIndexFromKey(currKey);
    }

    while (source.hasTop() && currIdx == rowIndexFromKey(source.getTopKey())) {
      currKey.set(source.getTopKey());
      String columnValue =
          this.hashLargeValues
              ? RowAggregator.transformValue(source.getTopValue()).toString()
              : source.getTopValue().toString();
      output.put(
          colNameFromKey(currKey).toString(),
          includeVisibility
              ? RowAggregator.colValueWithVis(columnValue, currKey.getColumnVisibility().toString())
              : columnValue);
      hasTop = true;
      source.next();
    }

    if (source.hasTop()) {
      nextKey.set(source.getTopKey());
    }

    topValue = new Value(mapper.writeValueAsBytes(output));
  }

  private long rowIndexFromKey(Key key) {
    return rowIndexFromColQ(key.getColumnQualifier());
  }

  private long rowIndexFromColQ(Text colq) {
    Text idx = new Text();

    idx.set(colq.getBytes(), 0, Const.LONG_LEX_LEN);
    return longLex.decode(idx.getBytes());
  }

  private Text colNameFromKey(Key key) {
    return colNameFromColQ(key.getColumnQualifier());
  }

  private Text colNameFromColQ(Text colq) {
    Text colName = new Text();
    colName.set(
        colq.getBytes(),
        Const.LONG_LEX_LEN + DELIMITER_LEN,
        colq.getBytes().length - Const.LONG_LEX_LEN - DELIMITER_LEN);
    return colName;
  }
}
