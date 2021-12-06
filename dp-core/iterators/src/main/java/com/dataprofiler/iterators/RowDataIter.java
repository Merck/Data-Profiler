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

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.OptionDescriber;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.WrappingIterator;

public class RowDataIter extends WrappingIterator implements OptionDescriber {

  public static final String BEGIN_INDEX_OPTION = "0";
  public static final String END_INDEX_OPTION = "-1";
  public static final String ROW_DATA_ITER_NAME = "RowDataIter";

  private Long beginIndex = 0L;
  private Long endIndex = -1L;

  @Override
  public IteratorOptions describeOptions() {
    IteratorOptions iterOpts = new IteratorOptions(ROW_DATA_ITER_NAME, "", null, null);
    iterOpts.addNamedOption(
        BEGIN_INDEX_OPTION, "The index of the first row of the range (default = 0)");
    iterOpts.addNamedOption(
        END_INDEX_OPTION, "The index of the last row of the range (default = -1)");
    return iterOpts;
  }

  @Override
  public boolean validateOptions(Map<String, String> options) {
    if (options.containsKey(BEGIN_INDEX_OPTION)) {
      try {
        Long.parseLong(options.get(BEGIN_INDEX_OPTION));
      } catch (NumberFormatException e) {
        return false;
      }
    }

    if (options.containsKey(END_INDEX_OPTION)) {
      try {
        Long.parseLong(options.get(END_INDEX_OPTION));
      } catch (NumberFormatException e) {
        return false;
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

    if (options.containsKey(BEGIN_INDEX_OPTION)) {
      beginIndex = Long.parseLong(options.get(BEGIN_INDEX_OPTION));
    }

    if (options.containsKey(END_INDEX_OPTION)) {
      endIndex = Long.parseLong(options.get(END_INDEX_OPTION));
    }
  }

  @Override
  public void seek(Range range, Collection<ByteSequence> columnFamilies, boolean inclusive)
      throws IOException {
    super.seek(range, columnFamilies, inclusive);
    skipToRange();
  }

  @Override
  public void next() throws IOException {
    getSource().next();
    skipToRange();
  }

  private void skipToRange() throws IOException {

    if (getSource().hasTop() && !getSource().getTopKey().isDeleted()) {
      Long currIndex = Long.parseLong(getSource().getTopKey().getColumnQualifier().toString());

      // Skip indices out of the specified range
      while (currIndex < beginIndex || (endIndex != -1L && currIndex > endIndex)) {
        getSource().next();
        if (getSource().hasTop() && !getSource().getTopKey().isDeleted()) {
          currIndex = Long.parseLong(getSource().getTopKey().getColumnQualifier().toString());
        } else {
          break;
        }
      }
    }
  }
}
