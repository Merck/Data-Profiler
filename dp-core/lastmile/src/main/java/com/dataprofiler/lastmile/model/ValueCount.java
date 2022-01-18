package com.dataprofiler.lastmile.model;

/*-
 *
 * dataprofiler-lastmile
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

import static java.util.Objects.nonNull;

import java.time.temporal.Temporal;
import java.util.Date;
import java.util.Objects;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class ValueCount {

  protected String value;
  protected long count;

  public ValueCount(String value, long count) {
    super();
    this.value = value;
    this.count = count;
  }

  public ValueCount(Temporal value, long count) {
    super();
    if (nonNull(value)) {
      this.value = value.toString();
    }
    this.count = count;
  }

  public ValueCount(Date value, long count) {
    super();
    if (nonNull(value)) {
      this.value = value.toString();
    }
    this.count = count;
  }

  public ValueCount(ValueCount valueCount) {
    super();
    if (nonNull(valueCount)) {
      this.value = valueCount.getValue();
      this.count = valueCount.getCount();
    }
  }

  public String getValue() {
    return value;
  }

  public long getCount() {
    return count;
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
    return Objects.hash(value, count);
  }
}
