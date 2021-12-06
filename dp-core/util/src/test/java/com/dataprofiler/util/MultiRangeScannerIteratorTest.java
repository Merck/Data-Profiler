package com.dataprofiler.util;

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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.dataprofiler.util.objects.AccumuloObject;
import com.dataprofiler.util.objects.InvalidDataFormat;
import com.dataprofiler.util.objects.MultiRangeScannerIterator;

import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

public class MultiRangeScannerIteratorTest {
  
  private static final Value BLANK = new Value();

  // Simple AccumuloObject that echoes back the original
  // key and value. This is what the MultiRangeScannerIterator
  // API provides, and we can go to/from entries with it.
  static class MyAO extends AccumuloObject<MyAO> {
    Key k;
    Value v;
    @Override
    public MyAO fromEntry(Entry<Key, Value> entry) {
      MyAO ao = new MyAO();
      ao.k = entry.getKey();
      ao.v = entry.getValue();
      return ao;
    }

    @Override
    public Key createAccumuloKey() throws InvalidDataFormat {
      return k;
    }
  }

  // Returns a Mockito'd Scanner that can loop over the chunks we provide.
  // Some caveats:
  //  1. Sorting isn't considered here
  //  2. This maps the user values to Keys by setting the Key's row value to be
  //       `object#toString`
  //  3. Don't modify the collections underneath the iterator. bad juju happens there
  //  4. The scanner ignores any setting of the Range
  public static Scanner scannerOver(final List<? extends Collection<?>> chunks) {
    Scanner mockScanner = Mockito.mock(Scanner.class);

    AtomicInteger loopCount = new AtomicInteger(0);

    when(mockScanner.iterator()).thenAnswer(
        (Answer<Iterator<Entry<Key, Value>>>) invocationOnMock -> {
          if (loopCount.get() >= chunks.size()) {
            return Collections.emptyIterator();
          } else {
            return chunks.get(loopCount.getAndIncrement())
                .stream()
                .map(i -> Maps.immutableEntry(new Key(i.toString()), BLANK))
                .collect(Collectors.toList())
                .iterator();
          }
        });
    
    doNothing().when(mockScanner).setRange(any());

    return mockScanner;
  }

  /*
   * Mega simple test that verifies if we have 3 blocks
   * of data, we can simulate flattening the blocks into
   * a single list/stream. This simulates taking multiple
   * Range objects, and running them serially into a Scanner
   * in a single Iterator.
   */
  @Test
  public void sunnyDay() {
    List<List<Integer>> blocks = ImmutableList.of(
        ImmutableList.of(1, 2, 3),
        ImmutableList.of(10, 20, 30),
        ImmutableList.of(100, 200, 300)
    );

    Scanner mockScanner = scannerOver(blocks);

    MultiRangeScannerIterator<MyAO> mrsi = new MultiRangeScannerIterator<MyAO>(new MyAO(), mockScanner,
        ImmutableList.of(new Range(), new Range(), new Range()));

    List<String> check = blocks.stream()
        .flatMap(List::stream)
        .map(i -> i.toString())
        .collect(Collectors.toList());

    List<String> sink = new ArrayList<>();
    while (mrsi.hasNext()) {
      sink.add(mrsi.next().k.getRow().toString());
    }

    assertEquals(check, sink);

    verify(mockScanner, times(3)).iterator();
  }
}
