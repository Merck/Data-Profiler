package com.dataprofiler.iterators.indexData;

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

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.accumulo.core.data.ArrayByteSequence;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.Filter;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.hadoop.io.Text;

public class ActiveTableFilter extends Filter {
  public static final String CONF_ACTIVE_TABLES = "active_tables";
  public static final char NUL_CHAR = 0;

  /**
   * Utility method for getting the prefix from the column qualifier value of a GLOBAL index entry.
   * This grabs everything up to, but excluding, the second null byte in a byte string.
   *
   * @param columnQualifier
   * @return a substring up to the second null byte, or null if 2 null bytes are not found.
   */
  public static ByteSequence extractTableId(ByteSequence columnQualifier) {
    // format is DATASET\x00TABLE_ID\x00COLUMN
    int found = 0;
    int i = 0;
    for (; i < columnQualifier.length() && found < 2; ++i) {
      if (columnQualifier.byteAt(i) == NUL_CHAR) {
        ++found;
      }
    }

    if (found == 2) {
      return new ArrayByteSequence(columnQualifier.getBackingArray(), 0, i - 1);
    } else {
      return null;
    }
  }

  private Set<ByteSequence> filters;

  @Override
  public void init(
      SortedKeyValueIterator<Key, Value> source,
      Map<String, String> options,
      IteratorEnvironment env)
      throws IOException {
    super.init(source, options, env);

    if (options.containsKey(CONF_ACTIVE_TABLES)) {
      this.filters =
          ImmutableSet.copyOf(
                  Splitter.on("__&&__").trimResults().split(options.get(CONF_ACTIVE_TABLES)))
              .stream()
              .map(s -> s.getBytes(Charsets.UTF_8))
              .map(ba -> new ArrayByteSequence(ba))
              .collect(Collectors.toSet());
    } else {
      filters = Collections.emptySet();
    }
  }

  @Override
  public boolean accept(Key k, Value v) {
    if (filters == null || filters.isEmpty()) {
      return true;
    }

    ByteSequence tableId = extractTableId(k.getColumnQualifierData());
    if (tableId == null) {
      return false;
    } else {
      return this.filters.contains(tableId);
    }
  }

  @Override
  public SortedKeyValueIterator<Key, Value> deepCopy(IteratorEnvironment env) {
    ActiveTableFilter f = (ActiveTableFilter) super.deepCopy(env);
    f.filters = this.filters;
    return f;
  }

  public static void main(String[] args) throws Exception {
    Text t =
        new Text(
            "BIOASSAY (Avro)\u0000JOBS_BKUP-9e9e2074-2256-4a65-abd9-0eac5a39b883\u0000JOB_NAME");
    ArrayByteSequence bs = new ArrayByteSequence(t.getBytes(), 0, t.getLength());
    ByteSequence tableId = extractTableId(bs);
    System.out.println(new Text(tableId.toArray()));

    Key k =
        new Key(
            "term",
            "GLOBAL",
            "BIOASSAY (Avro)\u0000JOBS_BKUP-9e9e2074-2256-4a65-abd9-0c5a39b883\u0000JOB_NAME");
    ActiveTableFilter f = new ActiveTableFilter();
    f.filters =
        ImmutableSet.of(
            new ArrayByteSequence(
                "BIOASSAY (Avro)\u0000JOBS_BKUP-9e9e2074-2256-4a65-abd9-0eac5a39b883"
                    .getBytes(Charsets.UTF_8)));
    System.out.println(f.accept(k, null));
  }
}
