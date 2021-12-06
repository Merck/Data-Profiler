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

import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Const;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.iterators.ClosableIterator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.accumulo.core.client.BatchScanner;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.IteratorSetting.Column;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.data.Range;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;

public class ObjectScannerIterable<T extends AccumuloObject> implements Iterable<T> {
  private final Context context;
  private String table;
  private final T builder;
  private Boolean batch = false;
  private Boolean multiRangeScanner = false;
  private final ArrayList<Range> ranges = new ArrayList<>();
  private final ArrayList<Text> colFams = new ArrayList<>();
  private final ArrayList<Column> columns = new ArrayList<>();
  private final ArrayList<IteratorSetting> iterators = new ArrayList<>();

  public ObjectScannerIterable(Context context, String table, T builder) {
    this.table = table;
    this.context = context;
    this.builder = builder;
  }

  public ObjectScannerIterable<T> setBatch(Boolean batch) {
    this.batch = batch;

    return this;
  }

  public ObjectScannerIterable<T> setMultiRangeScanner() {
    this.multiRangeScanner = true;
    return this;
  }

  public ObjectScannerIterable<T> addRange(Range range) {
    ranges.add(range);

    return this;
  }

  public ObjectScannerIterable<T> clearColumn() {
    columns.clear();

    return this;
  }

  public ObjectScannerIterable<T> fetchColumn(String colFam, String colQual) {
    clearColumn();
    clearColumnFamily();

    return addColumn(colFam, colQual);
  }

  public ObjectScannerIterable<T> addColumn(String colFam, String colQual) {
    assert colFam != null && colQual != null;
    columns.add(new Column(colFam, colQual));

    return this;
  }

  public ObjectScannerIterable<T> clearColumnFamily() {
    colFams.clear();

    return this;
  }

  public ObjectScannerIterable<T> fetchColumnFamily(String colFam) {
    clearColumn();
    clearColumnFamily();
    return addColumnFamily(colFam);
  }

  public ObjectScannerIterable<T> addColumnFamily(String colFam) {
    colFams.add(new Text(colFam));

    return this;
  }

  public ObjectScannerIterable<T> addScanIterator(IteratorSetting iterator) {
    iterators.add(iterator);

    return this;
  }

  public ObjectScannerIterable<T> setTable(String table) {
    this.table = table;

    return this;
  }

  public ScannerBase createScanner() {
    return createScanner(true);
  }

  public ScannerBase createScanner(boolean setRanges) {
    ScannerBase scanner;
    if (batch) {
      BatchScanner s;
      try {
        s = context.createBatchScanner(table);
      } catch (BasicAccumuloException e) {
        throw new RuntimeException(e.toString());
      }

      if (setRanges) {
        if (ranges.size() > 0) {
          s.setRanges(ranges);
        } else {
          s.setRanges(Collections.singleton(new Range(Const.LOW_BYTE, Const.HIGH_BYTE)));
        }
      }

      scanner = s;
    } else {
      if (setRanges && ranges.size() > 1) {
        throw new RuntimeException("Cannot add more than one range to a non-batch scanner");
      }

      Scanner s;
      try {
        s = context.createScanner(table);
      } catch (BasicAccumuloException e) {
        throw new RuntimeException(e.toString());
      }

      if (setRanges && ranges.size() > 0) {
        s.setRange(ranges.get(0));
      }

      scanner = s;
    }

    for (Text c : colFams) {
      scanner.fetchColumnFamily(c);
    }

    for (Column c : columns) {
      scanner.fetchColumn(c);
    }

    if (iterators.size() > 0) {
      for (IteratorSetting i : iterators) {
        scanner.addScanIterator(i);
      }
    }

    return scanner;
  }

  @Override
  public ClosableIterator<T> iterator() {
    if (this.multiRangeScanner) {
      return new MultiRangeScannerIterator<>(
          getBuilder(), (Scanner) createScanner(false), this.ranges);
    } else {
      return new ObjectScannerIterator<T>(context, createScanner(), builder);
    }
  }

  public T fetch() {
    return (T) builder.fetch(this);
  }

  public ClosableIterator<T> closeableIterator() {
    return iterator();
  }

  public Job createInputJob() throws IOException, BasicAccumuloException {
    return context.createInputJob(table, colFams, columns, ranges, iterators);
  }

  public void applyInputConfiguration(Configuration configuration) throws BasicAccumuloException {
    context.applyInputConfiguration(configuration, table, colFams, columns, ranges, iterators);
  }

  public Job createOutputJob() throws IOException, BasicAccumuloException {
    return context.createOutputJob(table);
  }

  public Context getContext() {
    return context;
  }

  public String getTable() {
    return table;
  }

  public T getBuilder() {
    return builder;
  }

  public Boolean getBatch() {
    return batch;
  }

  public ArrayList<Range> getRanges() {
    return ranges;
  }

  public ArrayList<IteratorSetting> getIterators() {
    return iterators;
  }

  @Override
  public String toString() {
    return "ObjectScannerIterable{"
        + "table='"
        + table
        + '\''
        + ", batch="
        + batch
        + ", ranges="
        + ranges
        + '\''
        + ", iterators="
        + iterators
        + '}';
  }
}
