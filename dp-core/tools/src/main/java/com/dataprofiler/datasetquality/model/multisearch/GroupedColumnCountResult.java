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

import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class GroupedColumnCountResult implements Comparable<GroupedColumnCountResult> {
  protected List<ColumnCountResult> elements;
  protected List<String> values;
  protected long count;

  public GroupedColumnCountResult() {
    super();
  }

  public GroupedColumnCountResult(GroupedColumnCountResult result) {
    this();
    if (isNull(result)) {
      return;
    }
    this.count = result.count;
    if (nonNull(result.getValues())) {
      this.values = result.getValues().stream().collect(toList());
    }
    if (nonNull(result.getElements())) {
      this.elements = result.getElements().stream().collect(toList());
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
  public int compareTo(GroupedColumnCountResult o) {
    if (isNull(o)) {
      return Integer.MAX_VALUE;
    }
    return Long.compare(this.getCount(), o.getCount());
  }

  public List<ColumnCountResult> getElements() {
    return elements;
  }

  public void setElements(List<ColumnCountResult> elements) {
    this.elements = elements;
  }

  public List<String> getValues() {
    return values;
  }

  public void setValues(List<String> values) {
    this.values = values;
  }

  public long getCount() {
    return count;
  }

  public void setCount(long count) {
    this.count = count;
  }
}
