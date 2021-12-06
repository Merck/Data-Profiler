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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.OptionDescriber;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.WrappingIterator;

public class NthIndexIter extends WrappingIterator implements OptionDescriber {

  public static final String INTERVAL = "INTERVAL";
  private Long n = 100L;
  private Long index = 0L;

  public NthIndexIter() {}

  public NthIndexIter(NthIndexIter nthRowIterator) {
    this.index = nthRowIterator.index;
    this.n = nthRowIterator.n;
  }

  @Override
  public IteratorOptions describeOptions() {
    return new IteratorOptions(
        "nthRowIterator",
        "Return every nth record",
        Collections.singletonMap(INTERVAL, String.format("Default %d", n)),
        null);
  }

  @Override
  public boolean validateOptions(Map<String, String> map) {
    if (map.get(INTERVAL) != null) {
      try {
        Long.valueOf(map.get(INTERVAL));
        return true;
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            String.format("Option '%s' is not a Long", map.get(INTERVAL)));
      }
    }
    return true;
  }

  @Override
  public void init(
      SortedKeyValueIterator<Key, Value> source,
      Map<String, String> options,
      IteratorEnvironment env)
      throws IOException {
    super.init(source, options, env);

    if (options.get(INTERVAL) != null) {
      this.n = Long.valueOf(options.get(INTERVAL));
    }
    this.index = 0L;
  }

  @Override
  public SortedKeyValueIterator<Key, Value> deepCopy(IteratorEnvironment env) {
    return new NthIndexIter(this);
  }

  @Override
  public void seek(Range range, Collection<ByteSequence> columnFamilies, boolean inclusive)
      throws IOException {
    getSource().seek(range, columnFamilies, inclusive);
    calculateNext();
  }

  @Override
  public void next() throws IOException {
    getSource().next();
    calculateNext();
  }

  private void calculateNext() throws IOException {
    while (getSource().hasTop() && !getSource().getTopKey().isDeleted() && index % n != 0) {
      getSource().next();
      index++;
    }
    index++;
  }

  @Override
  public boolean hasTop() {
    return getSource().hasTop();
  }

  @Override
  public Key getTopKey() {
    return getSource().getTopKey();
  }

  @Override
  public Value getTopValue() {
    return new Value(Long.toString(index - 1L).getBytes());
  }
}
