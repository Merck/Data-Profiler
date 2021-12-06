package com.dataprofiler.datasetquality.model.multisearch;

/*-
 * 
 * dataprofiler-tools
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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import com.dataprofiler.util.objects.ColumnCountIndexObject;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class ColumnCountResult implements Comparable<ColumnCountResult> {
  protected String dataset;
  protected String table;
  protected String tableId;
  protected String column;
  protected List<String> values;
  protected List<Long> counts;

  public ColumnCountResult() {
    super();
  }

  public ColumnCountResult(ColumnCountIndexObject indexObject) {
    this();
    if (isNull(indexObject)) {
      return;
    }
    this.dataset = indexObject.getDataset();
    this.table = indexObject.getTable();
    this.tableId = indexObject.getTableId();
    this.column = indexObject.getColumn();
    this.values = new ArrayList<>();
    this.values.add(indexObject.getValue());
    this.counts = new ArrayList<>();
    this.counts.add(indexObject.getCount());
  }

  public ColumnCountResult(ColumnCountResult result) {
    this();
    if (isNull(result)) {
      return;
    }
    this.dataset = result.dataset;
    this.table = result.table;
    this.tableId = result.tableId;
    this.column = result.column;
    if (nonNull(result.getValues())) {
      this.values = result.getValues().stream().collect(toList());
    }
    if (nonNull(result.getCounts())) {
      this.counts = result.getCounts().stream().collect(toList());
    }
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  @Override
  public boolean equals(Object rhs) {
    return EqualsBuilder.reflectionEquals(this, rhs);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this, false);
  }

  @Override
  public int compareTo(ColumnCountResult o) {
    if (isNull(o) || isNull(o.getDataset())) {
      return Integer.MAX_VALUE;
    }

    return o.getDataset().compareTo(this.getDataset());
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

  public String getTableId() {
    return tableId;
  }

  public void setTableId(String tableId) {
    this.tableId = tableId;
  }

  public String getColumn() {
    return column;
  }

  public void setColumn(String column) {
    this.column = column;
  }

  public List<String> getValues() {
    return values;
  }

  public void setValues(List<String> values) {
    this.values = values;
  }

  public List<Long> getCounts() {
    return counts;
  }

  public void setCounts(List<Long> counts) {
    this.counts = counts;
  }

  public static ColumnCountResult merge(ColumnCountResult original, ColumnCountResult update) {
    ColumnCountResult result = new ColumnCountResult(original);
    List<Long> counts = new ArrayList<>(result.getCounts());
    counts.addAll(update.getCounts());
    result.setCounts(counts);
    List<String> values = new ArrayList<>(result.getValues());
    values.addAll(update.getValues());
    result.setValues(values);
    return result;
  }
}
