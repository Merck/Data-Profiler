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
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorUtil;
import org.apache.hadoop.io.Text;

public class ColumnSampleObject extends VersionedPurgableDatasetObject<ColumnSampleObject> {

  private static final String accumuloTable = Const.ACCUMULO_SAMPLES_TABLE_ENV_KEY;
  public String dataset;
  public String table; // this is table id
  public String column;
  public ColumnCountObject.ValueCountObject[] samples;

  public ColumnSampleObject() {
    super(accumuloTable);
  }

  public ColumnSampleObject(ColumnReservoirSampleObject reservoirSample) {
    super(accumuloTable);

    this.dataset = reservoirSample.dataset;
    this.table = reservoirSample.table;
    this.column = reservoirSample.column;
    this.visibility = reservoirSample.visibility;
    this.samples =
        Arrays.stream(reservoirSample.getSamples())
            .map(
                r ->
                    new ColumnCountObject.ValueCountObject(
                        r.getValue(), Long.valueOf(r.getCount())))
            .toArray(ColumnCountObject.ValueCountObject[]::new);
  }

  public ColumnCountObject.ValueCountObject[] getSamples() {
    return samples;
  }

  public void setSamples(ColumnCountObject.ValueCountObject[] samples) {
    this.samples = samples;
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

  public ObjectScannerIterable<ColumnSampleObject> findByDataset(Context context, String dataset) {
    return scan(context)
        .addRange(createInclusiveRange(dataset))
        .fetchColumnFamily(Const.COL_FAM_SAMPLE);
  }

  public ObjectScannerIterable<ColumnSampleObject> findByTable(
      Context context, String dataset, String table) {
    return scan(context)
        .addRange(createInclusiveRange(dataset, table))
        .fetchColumnFamily(Const.COL_FAM_SAMPLE);
  }

  public ObjectScannerIterable<ColumnSampleObject> findByColumn(
      Context context, String dataset, String table, String column) {
    return scan(context)
        .addRange(createInclusiveRange(dataset, table, column))
        .fetchColumnFamily(Const.COL_FAM_SAMPLE);
  }

  @Override
  public ColumnSampleObject fromEntry(Entry<Key, Value> entry) {
    ColumnSampleObject obj = new ColumnSampleObject();

    Key key = entry.getKey();

    String[] parts = splitString(key.getRow().toString(), 3);
    obj.dataset = parts[0];
    obj.table = parts[1];
    obj.column = parts[2];

    try {
      obj.samples =
          mapper.readValue(entry.getValue().toString(), ColumnCountObject.ValueCountObject[].class);
    } catch (IOException e) {
      throw new InvalidDataFormat(e.toString());
    }

    obj.updatePropertiesFromEntry(entry);

    return obj;
  }

  @Override
  public Key createAccumuloKey() throws InvalidDataFormat {
    return new Key(joinKeyComponents(dataset, table, column), Const.COL_FAM_SAMPLE, "", visibility);
  }

  @Override
  public Value createAccumuloValue() throws InvalidDataFormat {
    try {
      return new Value(mapper.writeValueAsBytes(samples));
    } catch (JsonProcessingException e) {
      throw new InvalidDataFormat("Unable to convert samples as JSON object");
    }
  }

  @Override
  public void bulkPurgeTable(Context context, String datasetName, String tableName, String tableId)
      throws BasicAccumuloException {
    Text start = new Text(joinKeyComponentsEndDelimited(datasetName, tableId));
    Text end = new Text(joinKeyComponentsEndDelimited(datasetName, tableId) + Const.HIGH_BYTE);
    bulkPurgeRange(context, start, end);
  }

  public void bulkDeleteContainingTable(Context context) throws BasicAccumuloException {
    super.bulkDeleteContainingTable(context);
    String currTable = getTable(context);

    try {
      TableOperations tops = context.getConnector().tableOperations();

      // Delete all iterators applied to this table
      Map<String, EnumSet<IteratorUtil.IteratorScope>> iterators = tops.listIterators(currTable);

      for (Map.Entry<String, EnumSet<IteratorUtil.IteratorScope>> iterator : iterators.entrySet()) {
        tops.removeIterator(currTable, iterator.getKey(), iterator.getValue());
      }
    } catch (Exception e) {
      throw new BasicAccumuloException(e.getMessage());
    }
  }
}
