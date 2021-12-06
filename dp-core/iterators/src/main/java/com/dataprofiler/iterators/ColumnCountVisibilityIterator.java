package com.dataprofiler.iterators;

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

import com.dataprofiler.util.Const;
import com.dataprofiler.util.objects.ColumnCountIndexObject;
import com.dataprofiler.util.objects.ColumnCountObject;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.OptionDescriber;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.WrappingIterator;
import org.apache.accumulo.core.iterators.user.WholeRowIterator;
import org.apache.hadoop.io.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ColumnCountVisibilityIterator extends WrappingIterator implements OptionDescriber {

  private static final Logger log = LoggerFactory.getLogger(ColumnCountVisibilityIterator.class);

  protected LinkedList<ColumnCountIndexObject> currentColumnIndexes = null;
  protected LinkedList<ColumnCountObject> currentColumnCounts = null;
  protected LinkedList<Key> keyBuffer = null;
  protected LinkedList<Value> valueBuffer = null;

  private final Text rowHolder = new Text();
  private final Text columnFamilyHolder = new Text();
  private final Text columnQualifierHolder = new Text();
  private final Text columnVisibilityHolder = new Text();

  private Range seekedRange;
  private Collection<ByteSequence> seekedColumnFamilies;
  private boolean seekedColumnFamiliesInclusive;

  private Key topKey = null;
  private Value topValue = null;

  public ColumnCountVisibilityIterator() {}

  public ColumnCountVisibilityIterator(SortedKeyValueIterator<Key, Value> source) {
    this.setSource(source);
  }

  private ColumnCountVisibilityIterator(
      ColumnCountVisibilityIterator other, IteratorEnvironment env) {
    this.currentColumnIndexes = new LinkedList<>(other.currentColumnIndexes);
    this.currentColumnCounts = new LinkedList<>(other.currentColumnCounts);

    // Copy the buffer
    this.keyBuffer = new LinkedList<>(other.keyBuffer);
    this.valueBuffer = new LinkedList<>(other.valueBuffer);

    this.seekedRange = new org.apache.accumulo.core.data.Range(other.seekedRange);
    this.seekedColumnFamilies = other.seekedColumnFamilies;
    this.seekedColumnFamiliesInclusive = other.seekedColumnFamiliesInclusive;

    // Copy the source
    this.setSource(other.getSource().deepCopy(env));
  }

  @Override
  public ColumnCountVisibilityIterator deepCopy(IteratorEnvironment env) {
    return new ColumnCountVisibilityIterator(this, env);
  }

  @Override
  public void init(
      SortedKeyValueIterator<Key, Value> source,
      Map<String, String> options,
      IteratorEnvironment env)
      throws IOException {
    log.warn("init(" + source + ", " + options + ", " + env + ")");
    super.init(source, options, env);

    currentColumnIndexes = new LinkedList<>();
    currentColumnCounts = new LinkedList<>();
    keyBuffer = new LinkedList<>();
    valueBuffer = new LinkedList<>();
  }

  @Override
  public IteratorOptions describeOptions() {
    return new IteratorOptions(
        "columnCountVisibilityIter",
        ColumnCountVisibilityIterator.class.getSimpleName()
            + " Combines column count object rows with different column visibilities",
        null,
        null);
  }

  @Override
  public boolean validateOptions(Map<String, String> options) {
    return true;
  }

  @Override
  public Key getTopKey() {
    Key wc = this.topKey;
    log.warn(" getTopKey() --> " + wc);
    // System.out.println(" getTopKeyRow() --> " + wc.getRow().toString());
    return wc;
  }

  @Override
  public Value getTopValue() {
    Value w = this.topValue;
    log.warn(" getTopValue() --> " + w);
    return w;
  }

  @Override
  public boolean hasTop() {
    boolean b = this.topKey != null;
    log.warn(" hasTop() --> " + b);
    return b;
  }

  @Override
  public void seek(Range range, Collection<ByteSequence> columnFamilies, boolean inclusive)
      throws IOException {
    log.warn("---> seek()");
    log.warn(" seek(" + range + ", " + columnFamilies + ", " + inclusive + ")");
    super.seek(range, columnFamilies, inclusive);

    seekedRange = range;
    seekedColumnFamilies = columnFamilies;
    seekedColumnFamiliesInclusive = inclusive;

    update();
  }

  @Override
  public void next() throws IOException {
    log.warn(" next()");
    clearBuffers();
    update();
  }

  protected void update() throws IOException {
    // If we have data to read, start the
    if (getSource().hasTop()) { // We found a matching row
      if (bufferRow()) { // Set the topKey to be just the row
        keyBuffer.getFirst().getRow(rowHolder);
        keyBuffer.getFirst().getColumnFamily(columnFamilyHolder);
        keyBuffer.getFirst().getColumnQualifier(columnQualifierHolder);
        keyBuffer.getFirst().getColumnVisibility(columnVisibilityHolder);
        this.topKey =
            new Key(rowHolder, columnFamilyHolder, columnQualifierHolder, columnVisibilityHolder);
        this.topValue = WholeRowIterator.encodeRow(keyBuffer, valueBuffer);
      } else {
        this.topKey = null;
        this.topValue = null;
      }
    } else {
      this.topKey = null;
      this.topValue = null;
    }
  }

  protected boolean bufferRow() throws IOException {
    log.warn("---> bufferRow()");

    while (getSource().hasTop()) {
      Key key = getSource().getTopKey();
      Value value = getSource().getTopValue();

      logKeyValue(key, value);

      switch (key.getColumnFamily().toString()) {
        case Const.COL_FAM_DATA:
          ColumnCountObject columnCount =
              new ColumnCountObject().fromEntry(Maps.immutableEntry(key, value));
          log.warn("COL_FAM_DATA columnCountObject -> " + columnCount);
          if (currentColumnCounts.isEmpty()) {
            currentColumnCounts.add(columnCount);
          } else if (!currentColumnCounts.getLast().getValue().equals(columnCount.getValue())
              || !currentColumnCounts.getLast().sortOrder.equals(columnCount.sortOrder)) {
            aggregateColumnCounts();
            if (!keyBuffer.isEmpty()) return true;
          } else {
            currentColumnCounts.add(columnCount);
          }
          break;

        case Const.INDEX_GLOBAL:
        case Const.INDEX_DATASET:
        case Const.INDEX_TABLE:
        case Const.INDEX_COLUMN:
          ColumnCountIndexObject columnCountIndex =
              new ColumnCountIndexObject().fromEntry(Maps.immutableEntry(key, value));
          log.warn("INDEX columnCountIndex -> " + columnCountIndex);
          if (currentColumnIndexes.isEmpty()) {
            currentColumnIndexes.add(columnCountIndex);
          } else if (!currentColumnIndexes.getLast().getValue().equals(columnCountIndex.getValue())
              || !currentColumnIndexes.getLast().getIndex().equals(columnCountIndex.getIndex())) {
            aggregateColumnIndexes();
            if (!keyBuffer.isEmpty()) return true;
          } else {
            currentColumnIndexes.add(columnCountIndex);
          }
          break;

        default:
          break;
      }

      getSource().next();

      // Handle the last row
      if (!getSource().hasTop()) {
        keyBuffer.add(key);
        valueBuffer.add(value);
      }
    }

    return !keyBuffer.isEmpty();
  }

  protected void aggregateColumnCounts() {
    Optional<Long> sum =
        currentColumnCounts.stream().map(ColumnCountObject::getCount).reduce(Long::sum);

    if (sum.isPresent()) {
      ColumnCountObject lastColumnCount = currentColumnCounts.getLast();
      log.warn("lastColumnCount=" + lastColumnCount);

      ColumnCountObject newColumnCount =
          new ColumnCountObject(
              lastColumnCount.getDataset(),
              lastColumnCount.getTableId(),
              lastColumnCount.getColumn(),
              lastColumnCount.getValue(),
              sum.get());

      newColumnCount.sortOrder = lastColumnCount.sortOrder;
      newColumnCount.setVisibility(lastColumnCount.getVisibility());
      log.warn("newColumnCount=" + newColumnCount);

      Key newKey = newColumnCount.createAccumuloKey();
      logKeyValue(newKey, new Value());

      // Reset buffers and column counts
      keyBuffer.clear();
      valueBuffer.clear();
      currentColumnCounts.clear();

      keyBuffer.add(newKey);
      valueBuffer.add(new Value());
    }
  }

  protected void aggregateColumnIndexes() {
    Optional<Long> sum =
        currentColumnIndexes.stream().map(ColumnCountIndexObject::getCount).reduce(Long::sum);

    if (sum.isPresent()) {
      ColumnCountIndexObject lastColumnCountIndex = currentColumnIndexes.getLast();

      ColumnCountIndexObject newColumnCountIndex =
          new ColumnCountIndexObject(
              lastColumnCountIndex.getDataset(),
              lastColumnCountIndex.getTable(),
              lastColumnCountIndex.getTableId(),
              lastColumnCountIndex.getColumn(),
              lastColumnCountIndex.getValue(),
              sum.get());

      newColumnCountIndex.setIndex(lastColumnCountIndex.getIndex());
      newColumnCountIndex.setVisibility(lastColumnCountIndex.getVisibility());
      // log.warn("newColumnCountIndex=" + newColumnCountIndex);

      Key newKey = newColumnCountIndex.createAccumuloKey();
      Value newValue = null;
      try {
        newValue = newColumnCountIndex.createAccumuloValue();
        System.out.println("newValue: " + newValue);
        logKeyValue(newKey, newValue);
      } catch (Exception e) {
        log.error(e.getMessage());
      }

      // Reset buffers and column counts
      keyBuffer.clear();
      valueBuffer.clear();
      currentColumnIndexes.clear();

      keyBuffer.add(newKey);
      valueBuffer.add(newValue);
    }
  }

  protected void
      clearBuffers() { // Clear the list if it's under a given size, otherwise just make a new
                       // object
    // and let the JVM GC clean up the mess so we don't waste a bunch of time
    // iterating over the list just to clear it.
    if (keyBuffer.size() < 10) {
      keyBuffer.clear();
      valueBuffer.clear();
    } else {
      keyBuffer = new LinkedList<>();
      valueBuffer = new LinkedList<>();
    }
  }

  protected void logKeyValue(Key key, Value value) {
    log.warn(
        "KEY -> "
            + key.getRow().toString()
            + " CF -> "
            + key.getColumnFamily().toString()
            + " CQ -> "
            + key.getColumnQualifier().toString()
            + " CV -> "
            + key.getColumnVisibility().toString()
            + " VALUE -> "
            + value.toString());
  }
}
