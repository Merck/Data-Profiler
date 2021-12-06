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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.hadoop.io.Text;

public class OrIterator implements IndexIterator {

  private final List<IndexIterator> sources;

  private final PriorityQueue<IndexIterator> minHeap =
      new PriorityQueue<>(Comparator.comparing(SortedKeyValueIterator::getTopKey));

  private final Text bufferA = new Text();
  private final Text bufferB = new Text();

  private Key topKey;

  private Multimap<String, String> indexValue;

  public OrIterator(Collection<IndexIterator> children) {
    this.sources = new ArrayList<>(children);
  }

  @Override
  public void init(
      SortedKeyValueIterator<Key, Value> sortedKeyValueIterator,
      Map<String, String> map,
      IteratorEnvironment iteratorEnvironment) {}

  @Override
  public boolean hasTop() {
    return topKey != null;
  }

  @Override
  public void next() throws IOException {
    this.topKey = null;
    if (minHeap.isEmpty()) {
      return;
    }

    topKey = new Key(minHeap.peek().getTopKey());
    topKey.getColumnQualifier(bufferA);
    indexValue = HashMultimap.create();
    while (!minHeap.isEmpty()) {
      IndexIterator current = minHeap.peek();
      current.getTopKey().getColumnQualifier(bufferB);

      if (bufferA.equals(bufferB)) {
        current = minHeap.poll();
        indexValue.putAll(current.indexValue());
        current.next();
        if (current.hasTop()) {
          // if we still have some data left, advance the iterator and push it back on the queue
          minHeap.add(current);
        }
      } else {
        // we've consumed all the sources that have the minimum doc id! hooray!
        break;
      }
    }
  }

  @Override
  public void seek(Range range, Collection<ByteSequence> collection, boolean b) throws IOException {
    // reset the internal state when we're told to re-seek. typically means a top-level
    // iterator reset
    this.minHeap.clear();
    for (IndexIterator src : this.sources) {
      // advance our source to its first possible value
      src.seek(range, collection, b);
      if (src.hasTop()) {
        minHeap.add(src);
      }
    }
    next();
  }

  @Override
  public Key getTopKey() {
    return topKey;
  }

  @Override
  public Value getTopValue() {
    return Common.EMPTY;
  }

  @Override
  public SortedKeyValueIterator<Key, Value> deepCopy(IteratorEnvironment iteratorEnvironment) {
    return null;
  }

  @Override
  public void moveToDocument(Text id) throws IOException {
    topKey = null;
    minHeap.clear();
    for (IndexIterator source : this.sources) {
      source.moveToDocument(id);
      if (source.hasTop()) {
        minHeap.add(source);
      }
    }
    next();
  }

  public Multimap<String, String> indexValue() {
    return this.indexValue;
  }
}
