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

import com.dataprofiler.util.Const;
import com.dataprofiler.util.objects.samples.ReservoirSample;
import java.util.Arrays;
import java.util.Map;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.hadoop.io.Text;

public class ColumnReservoirSampleObject extends AccumuloObject<ColumnReservoirSampleObject> {

  private static final Text DELIMITER = new Text(Const.DELIMITER);
  private static final int DELIMITER_LEN = DELIMITER.getLength();
  private static final String accumuloTable = Const.ACCUMULO_COLUMN_COUNTS_TABLE_ENV_KEY;

  public String dataset;
  public String table;
  public String column;
  public int numSamples;
  public ReservoirSample[] samples;

  public ColumnReservoirSampleObject(String dataset, String table, String column, int numSamples) {
    this();
    this.dataset = dataset;
    this.table = table;
    this.column = column;
    this.numSamples = numSamples;
  }

  public ColumnReservoirSampleObject() {
    super(accumuloTable);
    this.samples = new ReservoirSample[0];
  }

  public static ColumnReservoirSampleObject combineSamples(
      ColumnReservoirSampleObject s1, ColumnReservoirSampleObject s2) {
    ColumnReservoirSampleObject combined = new ColumnReservoirSampleObject();

    combined.dataset = s1.dataset != null && !s1.dataset.isEmpty() ? s1.dataset : s2.dataset;
    combined.table = s1.table != null && !s1.table.isEmpty() ? s1.table : s2.table;
    combined.column = s1.column != null && !s1.column.isEmpty() ? s1.column : s2.column;
    combined.visibility =
        s1.visibility != null && !s1.visibility.isEmpty() ? s1.visibility : s2.visibility;
    combined.numSamples = s1.numSamples != 0 ? s1.numSamples : s2.numSamples;
    combined.samples = mergeSamples(s1.getSamples(), s2.getSamples(), combined.numSamples);

    return combined;
  }

  public static ReservoirSample[] mergeSamples(ReservoirSample[] a, ReservoirSample[] b, int len) {

    int aLen = a.length;
    int bLen = b.length;

    len = len < aLen + bLen ? len : aLen + bLen;

    ReservoirSample[] answer = new ReservoirSample[len];
    int i = len - 1;
    int j = aLen - 1;
    int k = bLen - 1;

    while (i >= 0 && j >= 0 && k >= 0) {
      answer[i--] = a[j].getRand() > b[k].getRand() ? a[j--] : b[k--];
    }

    while (i >= 0 && j >= 0) {
      answer[i--] = a[j--];
    }

    while (i >= 0 && k >= 0) {
      answer[i--] = b[k--];
    }

    return answer;
  }

  public int getNumSamples() {
    return numSamples;
  }

  public void setNumSamples(int numSamples) {
    this.numSamples = numSamples;
  }

  public String getDataset() {
    return dataset;
  }

  public void setDataset(String dataset) {
    this.dataset = dataset;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public String getColumn() {
    return column;
  }

  public void setColumn(String column) {
    this.column = column;
  }

  public ReservoirSample[] getSamples() {
    return samples;
  }

  public void setSamples(ReservoirSample[] samples) {
    this.samples = samples;
  }

  @Override
  public ColumnReservoirSampleObject fromEntry(Map.Entry<Key, Value> entry) {
    ColumnReservoirSampleObject so = new ColumnReservoirSampleObject();
    Key key = entry.getKey();

    Text rowId = key.getRow();

    // Get the dataset
    int firstDelimIdx = rowId.find(DELIMITER.toString());
    if (firstDelimIdx < 0) {
      throw new InvalidDataFormat("Key with wrong number of fields");
    }
    so.dataset = new String(Arrays.copyOfRange(rowId.getBytes(), 0, firstDelimIdx));

    // Get the table
    int secondDelimIdx = rowId.find(DELIMITER.toString(), firstDelimIdx + 1);
    if (secondDelimIdx < 0) {
      throw new InvalidDataFormat("Key with wrong number of fields");
    }
    so.table =
        new String(
            Arrays.copyOfRange(rowId.getBytes(), firstDelimIdx + DELIMITER_LEN, secondDelimIdx));

    // Get column
    int thirdDelimIdx = rowId.find(DELIMITER.toString(), secondDelimIdx + 1);
    if (thirdDelimIdx < 0) {
      throw new InvalidDataFormat("Key with wrong number of fields");
    }
    so.column =
        new String(
            Arrays.copyOfRange(rowId.getBytes(), secondDelimIdx + DELIMITER_LEN, thirdDelimIdx));

    // Get the samples
    try {
      so.samples = mapper.readValue(entry.getValue().get(), ReservoirSample[].class);
    } catch (Exception ex) {
      throw new InvalidDataFormat(
          "Unable to convert value/cnt as JSON object: "
              + key.getColumnQualifier().toString()
              + " "
              + ex);
    }

    so.updatePropertiesFromEntry(entry);

    return so;
  }

  @Override
  public Key createAccumuloKey() throws InvalidDataFormat {
    return null;
  }
}
