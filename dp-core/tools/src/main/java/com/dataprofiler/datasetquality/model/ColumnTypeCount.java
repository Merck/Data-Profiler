package com.dataprofiler.datasetquality.model;

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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class ColumnTypeCount implements Comparable<ColumnTypeCount> {
  protected long count;
  protected String dataType;

  public ColumnTypeCount() {
    super();
  }

  public ColumnTypeCount(String dataType, long count) {
    this();
    this.dataType = dataType;
    this.count = count;
  }

  public ColumnTypeCount(ColumnTypeCount columnTypeCount) {
    this();
    if (isNull(columnTypeCount)) {
      return;
    }

    this.dataType = columnTypeCount.getDataType();
    this.count = columnTypeCount.getCount();
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
  public int compareTo(ColumnTypeCount o) {
    if (isNull(o)) {
      return Integer.MAX_VALUE;
    }
    return Long.compare(count, o.getCount());
  }

  public void incrementCount() {
    this.count += 1;
  }

  public long getCount() {
    return count;
  }

  public void setCount(long count) {
    this.count = count;
  }

  public String getDataType() {
    return dataType;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
  }
}
