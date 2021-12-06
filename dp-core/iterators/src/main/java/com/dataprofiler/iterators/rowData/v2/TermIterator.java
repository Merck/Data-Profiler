package com.dataprofiler.iterators.rowData.v2;

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

import static com.dataprofiler.iterators.rowData.v2.Common.EMPTY;
import static com.dataprofiler.iterators.rowData.v2.Common.MAX_DOC_ID;
import static com.dataprofiler.iterators.rowData.v2.Common.MIN_DOC_ID;
import static com.dataprofiler.iterators.rowData.v2.Common.TWOFIDDYSIX;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.dataprofiler.util.Const;
import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.hadoop.io.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TermIterator implements IndexIterator {
  private SortedKeyValueIterator<Key, Value> source;

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final byte[] valuePrefix;
  private final Text indexCF;
  private final Text firstCQ;
  private final Text lastCQ;

  // these buffers are used to reduce the number
  // of byte array allocations during looping
  private final Text rowBuffer = new Text();
  private final Text cqBuffer = new Text();
  private final Text docIDBuffer = new Text();

  // for use in `moveToDocument`
  private final Text minDocIDBuffer = new Text();
  private final Text currentDocIDBuffer = new Text();

  private final Multimap<String, String> indexValue;

  private Key topKey;

  public TermIterator(byte[] column, byte[] value) {
    this(column, value, null, null);
  }

  public TermIterator(
      byte[] column, byte[] value, byte[] type, SortedKeyValueIterator<Key, Value> source) {
    indexCF = new Text("I\u0000".getBytes(UTF_8));
    indexCF.append(column, 0, column.length);

    // save this off so we can reference it later
    valuePrefix = new byte[value.length + 1];
    System.arraycopy(value, 0, valuePrefix, 0, value.length);

    if (type == null) {
      firstCQ = Common.join(value, MIN_DOC_ID);
    } else {
      firstCQ = Common.join(value, MIN_DOC_ID, type);
    }

    if (type == null) {
      lastCQ = Common.join(value, MAX_DOC_ID);
    } else {
      lastCQ = Common.join(value, MAX_DOC_ID, type);
    }

    if (log.isDebugEnabled()) {
      log.debug(
          "Creating term iterator column[{}], value[{}], type[{}]",
          new Text(column),
          new Text(value),
          type == null ? "null" : new Text(type));
    }

    this.indexValue = ArrayListMultimap.create(1, 1);
    this.indexValue.put(new String(column, Charsets.UTF_8), new String(value, Charsets.UTF_8));

    this.source = source;
  }

  public Multimap<String, String> indexValue() {
    return this.indexValue;
  }

  public void setSource(SortedKeyValueIterator<Key, Value> src) {
    this.source = src;
  }

  @Override
  public Key getTopKey() {
    return topKey;
  }

  @Override
  public void next() throws IOException {
    this.topKey = null;
    source.next();
    setTopKey();
  }

  /**
   * Some indirection here. This is called from `next()` and `seek()`. The iterator contract
   * only specifies that an iterator may have data after `seek` or `next` is called.
   *
   * <p>Calling `seek` does not imply that `next` has been called, or vice-versa. This is a
   * convenience method to transform the top-key into one that only has the document ID in the
   * column qualifier.
   */
  public void setTopKey() {
    if (this.source.hasTop()) {
      Key tk = this.source.getTopKey();
      topKey =
          new Key(tk.getRow(rowBuffer), Common.DOC_ID_COLUMN_FAMILY, getRowID(tk, docIDBuffer));
    }
  }

  // we only want to get the term in the shard
  @Override
  public void seek(Range range, Collection<ByteSequence> columnFamilies, boolean inclusive)
      throws IOException {
    Key startKey = range.getStartKey();
    Key newStartKey;
    if (startKey.getColumnQualifierData().length() > 0) {
      // we're resuming a scan
      Text startingDoc = new Text(valuePrefix);
      Text resumedDoc = startKey.getColumnQualifier();
      startingDoc.append(resumedDoc.getBytes(), 0, resumedDoc.getLength());
      startingDoc.append(TWOFIDDYSIX, 0, TWOFIDDYSIX.length);
      newStartKey = new Key(startKey.getRow(), indexCF, startingDoc);
    } else {
      newStartKey = new Key(startKey.getRow(), indexCF, firstCQ);
    }
    Key newEndKey = new Key(startKey.getRow(), indexCF, lastCQ);
    Range newRange = new Range(newStartKey, false, newEndKey, false);
    source.seek(newRange, columnFamilies, inclusive);
    this.topKey = null;
    setTopKey();
  }

  @Override
  public void init(
      SortedKeyValueIterator<Key, Value> source,
      Map<String, String> options,
      IteratorEnvironment env) {
    // no-need?
  }

  @Override
  public boolean hasTop() {
    return this.topKey != null;
  }

  @Override
  public Value getTopValue() {
    return EMPTY;
  }

  @Override
  public SortedKeyValueIterator<Key, Value> deepCopy(IteratorEnvironment env) {
    return null;
  }

  private Text getRowID(Key key, Text docID) {
    Text colq = key.getColumnQualifier(cqBuffer);
    docID.set(colq.getBytes(), valuePrefix.length, Const.LONG_LEX_LEN);
    return docID;
  }

  // kinda ugly, but this is basically a macro
  // for use in `moveToDocument`
  private int compareDocIDs() {
    this.source.getTopKey().getColumnQualifier(currentDocIDBuffer);
    return minDocIDBuffer.compareTo(currentDocIDBuffer);
  }

  public void moveToDocument(Text docID) throws IOException {
    minDocIDBuffer.set(valuePrefix);
    minDocIDBuffer.append(docID.getBytes(), 0, docID.getLength());

    this.topKey = null;
    while (this.source.hasTop() && compareDocIDs() > 0) {
      this.source.next();
    }
    setTopKey();
  }
}
