package com.dataprofiler.util.iterators;

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
    super.init(source, options, env);

    log.debug("init({}, {}, {})", source, options, env);

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
    return wc;
  }

  @Override
  public Value getTopValue() {
    Value w = this.topValue;
    return w;
  }

  @Override
  public boolean hasTop() {
    boolean b = this.topKey != null;
    return b;
  }

  @Override
  public void seek(Range range, Collection<ByteSequence> columnFamilies, boolean inclusive)
      throws IOException {
    super.seek(range, columnFamilies, inclusive);
    seekedRange = range;
    seekedColumnFamilies = columnFamilies;
    seekedColumnFamiliesInclusive = inclusive;
    update();
  }

  @Override
  public void next() throws IOException {
    clearBuffers();
    update();
  }

  protected void update() throws IOException {
    // If we have data to read
    if (getSource().hasTop()) { // We found a matching row
      if (bufferRow()) { // Set the topKey to be just the row
        keyBuffer.getFirst().getRow(rowHolder);
        keyBuffer.getFirst().getColumnFamily(columnFamilyHolder);
        keyBuffer.getFirst().getColumnQualifier(columnQualifierHolder);
        keyBuffer.getFirst().getColumnVisibility(columnVisibilityHolder);
        this.topKey =
            new Key(rowHolder, columnFamilyHolder, columnQualifierHolder, columnVisibilityHolder);
        this.topValue = new Value(valueBuffer.getFirst());
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
    while (getSource().hasTop()) {
      Key key = getSource().getTopKey();
      Value value = getSource().getTopValue();

      switch (key.getColumnFamily().toString()) {
        case Const.COL_FAM_DATA:
          ColumnCountObject colCnt =
              new ColumnCountObject().fromEntry(Maps.immutableEntry(key, value));

          if (currentColumnCounts.isEmpty()) {
            currentColumnCounts.add(colCnt);
          } else if (!isInColumnCountGroup(colCnt)) {
            aggregateColumnCounts();
            if (!keyBuffer.isEmpty()) {
              return true;
            }
          } else {
            currentColumnCounts.add(colCnt);
          }
          break;
        case Const.INDEX_GLOBAL:
        case Const.INDEX_DATASET:
        case Const.INDEX_TABLE:
        case Const.INDEX_COLUMN:
          ColumnCountIndexObject colCntIdx =
              new ColumnCountIndexObject().fromEntry(Maps.immutableEntry(key, value));
          if (currentColumnIndexes.isEmpty()) {
            currentColumnIndexes.add(colCntIdx);
          } else if (!isInColumnCountIndexGroup(colCntIdx)) {
            aggregateColumnIndexes();
            if (!keyBuffer.isEmpty()) {
              return true;
            }
          } else {
            currentColumnIndexes.add(colCntIdx);
          }
          break;
        default:
          break;
      }

      getSource().next();

      // Handle the last row
      if (!getSource().hasTop()) {
        switch (key.getColumnFamily().toString()) {
          case Const.COL_FAM_DATA:
            aggregateColumnCounts();
          case Const.INDEX_GLOBAL:
          case Const.INDEX_DATASET:
          case Const.INDEX_TABLE:
          case Const.INDEX_COLUMN:
            aggregateColumnIndexes();
            break;
          default:
        }
      }
    }
    return !keyBuffer.isEmpty();
  }

  protected void aggregateColumnCounts() {
    // Get final sum of the current counts
    Optional<Long> sum =
        currentColumnCounts.stream().map(ColumnCountObject::getCount).reduce(Long::sum);

    if (sum.isPresent()) {
      ColumnCountObject newColumnCount = cloneColumnCount(currentColumnCounts.getLast(), sum.get());
      clearBuffers();
      keyBuffer.add(newColumnCount.createAccumuloKey());
      valueBuffer.add(new Value());
    }
  }

  protected void aggregateColumnIndexes() {
    // Get final sum of the current counts
    Optional<Long> sum =
        currentColumnIndexes.stream().map(ColumnCountIndexObject::getCount).reduce(Long::sum);

    if (sum.isPresent()) {
      ColumnCountIndexObject newColCntIdx =
          cloneColumnCountIndex(currentColumnIndexes.getLast(), sum.get());

      clearBuffers();
      keyBuffer.add(newColCntIdx.createAccumuloKey());
      try {
        valueBuffer.add(newColCntIdx.createAccumuloValue());
      } catch (Exception e) {
        log.error(e.getMessage());
        valueBuffer.add(new Value());
      }
    }
  }

  private boolean isInColumnCountGroup(ColumnCountObject columnCount) {
    ColumnCountObject lastColCnt = currentColumnCounts.getLast();
    return lastColCnt.getValue().equals(columnCount.getValue())
        && lastColCnt.sortOrder.equals(columnCount.sortOrder)
        && lastColCnt.getTableId().equals(columnCount.getTableId())
        && lastColCnt.getColumn().equals(columnCount.getColumn());
  }

  private boolean isInColumnCountIndexGroup(ColumnCountIndexObject columnCountIndex) {
    ColumnCountIndexObject lastIndex = currentColumnIndexes.getLast();
    return lastIndex.getValue().equals(columnCountIndex.getValue())
        && lastIndex.getIndex().equals(columnCountIndex.getIndex())
        && lastIndex.getTableId().equals(columnCountIndex.getTableId())
        && lastIndex.getColumn().equals(columnCountIndex.getColumn());
  }

  private ColumnCountObject cloneColumnCount(ColumnCountObject other, Long count) {
    ColumnCountObject result =
        new ColumnCountObject(
            other.getDataset(), other.getTableId(), other.getColumn(), other.getValue(), count);

    result.sortOrder = other.sortOrder;
    result.setVisibility(other.getVisibility());
    return result;
  }

  private ColumnCountIndexObject cloneColumnCountIndex(ColumnCountIndexObject other, Long count) {
    ColumnCountIndexObject result =
        new ColumnCountIndexObject(
            other.getDataset(),
            other.getTable(),
            other.getTableId(),
            other.getColumn(),
            other.getValue(),
            other.getNormalizedValue(),
            count);

    result.setIndex(other.getIndex());
    result.setVisibility(other.getVisibility());
    return result;
  }

  protected void
      clearBuffers() { // Clear the list if it's under a given size, otherwise just make a new
                       // object
    // and let the JVM GC clean up the mess so we don't waste a bunch of time
    // iterating over the list just to clear it.
    if (keyBuffer.size() < 10) {
      keyBuffer.clear();
      valueBuffer.clear();
      currentColumnCounts.clear();
      currentColumnIndexes.clear();
    } else {
      keyBuffer = new LinkedList<>();
      valueBuffer = new LinkedList<>();
      currentColumnCounts = new LinkedList<>();
      currentColumnIndexes = new LinkedList<>();
    }
  }
}
