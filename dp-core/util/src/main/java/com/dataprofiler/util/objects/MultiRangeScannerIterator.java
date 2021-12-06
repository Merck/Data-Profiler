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

import com.dataprofiler.util.iterators.ClosableIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;

public class MultiRangeScannerIterator<T extends AccumuloObject<T>> implements ClosableIterator<T> {

  private final T builder;
  private final Scanner scanner;
  private Iterator<Entry<Key, Value>> scannerIterator;
  private final ArrayList<Range> ranges;
  private T next;

  public MultiRangeScannerIterator(T builder, Scanner scanner, Collection<Range> ranges) {
    this.builder = builder;
    this.scanner = scanner;
    this.ranges = new ArrayList<>(ranges);
    Collections.sort(this.ranges);
    init();
  }

  public void init() {
    while (!hasNext() && !ranges.isEmpty()) {
      scanner.setRange(ranges.remove(0));
      this.scannerIterator = scanner.iterator();
      if (this.scannerIterator.hasNext()) {
        this.next = builder.fromEntry(this.scannerIterator.next());
      }
    }
  }

  @Override
  public boolean hasNext() {
    return next != null;
  }

  @Override
  public T next() {
    T toReturn = next;
    next = null;

    if (this.scannerIterator.hasNext()) {
      this.next = builder.fromEntry(this.scannerIterator.next());
    } else {
      init();
    }

    return toReturn;
  }

  @Override
  public void close() throws Exception {}
}
