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

import com.dataprofiler.util.Context;
import com.dataprofiler.util.iterators.ClosableIterator;
import java.util.Iterator;
import java.util.Map.Entry;
import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;

public class ObjectScannerIterator<T extends AccumuloObject> implements ClosableIterator<T> {
  private final Context context;
  private final ScannerBase scanner;
  private final Iterator<Entry<Key, Value>> iterator;
  private final T builder;

  public ObjectScannerIterator(Context context, ScannerBase scanner, T builder) {
    this.context = context;
    this.scanner = scanner;
    this.iterator = this.scanner.iterator();
    this.builder = builder;
  }

  @Override
  public boolean hasNext() {
    if (!iterator.hasNext()) {
      try {
        close();
      } catch (Exception e) {
        // This is so ugly, but there is not much else to do
        throw new RuntimeException(e.toString());
      }
      return false;
    }

    return true;
  }

  @Override
  public T next() {
    Entry<Key, Value> entry = iterator.next();

    return (T) builder.fromEntry(entry);
  }

  @Override
  public void close() throws Exception {
    scanner.close();
  }
}
