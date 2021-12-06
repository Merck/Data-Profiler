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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.OptionDescriber;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.WrappingIterator;

public class KeyValueFilter extends WrappingIterator implements OptionDescriber {

  public static final String KEY = "key";
  public static final String VALUE = "value";
  public static final String INVERT = "invert";

  private String key;
  private String value;
  private Boolean invert = false;
  private ObjectMapper mapper;
  private TypeReference<Map<String, String>> typeRef;

  public KeyValueFilter() {}

  public KeyValueFilter(KeyValueFilter iter) {
    this.key = iter.key;
    this.value = iter.value;
    this.invert = iter.invert;
  }

  @Override
  public IteratorOptions describeOptions() {
    Map<String, String> namedOptions = new HashMap<>();
    namedOptions.put(KEY, "key to apply filter to");
    namedOptions.put(VALUE, "value to filter on");
    namedOptions.put(INVERT, "invert the filter");

    return new IteratorOptions("key_value_filter", "Filters json", namedOptions, null);
  }

  public boolean validateOptions(Map<String, String> options) {
    if (options.get(KEY) == null) {
      throw new IllegalArgumentException("Option '" + KEY + "' must be specified");
    }

    if (options.get(VALUE) == null) {
      throw new IllegalArgumentException("Option '" + VALUE + "' must be specified");
    }

    if (options.get(INVERT) != null) {
      try {
        Boolean.parseBoolean(options.get(INVERT));
      } catch (Exception ex) {
        throw new IllegalArgumentException("bad boolean " + INVERT + ":" + options.get(INVERT));
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

    this.mapper = new ObjectMapper();
    this.typeRef = new TypeReference<Map<String, String>>() {};
    this.invert = false;

    if (options.get(INVERT) != null) {
      this.invert = Boolean.parseBoolean(options.get(INVERT));
    }

    if (options.get(KEY) != null) {
      this.key = options.get(KEY);
    }

    if (options.get(VALUE) != null) {
      this.value = options.get(VALUE);
    }
  }

  @Override
  public void seek(Range range, Collection<ByteSequence> columnFamilies, boolean inclusive)
      throws IOException {
    super.seek(range, columnFamilies, inclusive);
    this.calculateNext();
  }

  @Override
  public void next() throws IOException {
    getSource().next();
    this.calculateNext();
  }

  @Override
  public SortedKeyValueIterator<Key, Value> deepCopy(IteratorEnvironment env) {
    return new KeyValueFilter(this);
  }

  private void calculateNext() throws IOException {
    while (getSource().hasTop() && !getSource().getTopKey().isDeleted()) {

      Map<String, String> data = mapper.readValue(getSource().getTopValue().toString(), typeRef);

      if (data.containsKey(key)) {
        if ((data.get(key).equals(value) || invert) && !(data.get(key).equals(value) && invert)) {
          return;
        }
      }

      this.getSource().next();
    }
  }
}
