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
import static java.util.stream.Collectors.toSet;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class CompleteQuality {
  protected long updatedOnMillis;
  protected Set<DatasetQuality> qualities;

  public CompleteQuality() {
    super();
    ZoneOffset zone = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    this.updatedOnMillis = LocalDateTime.now().toEpochSecond(zone) * 1000;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  @Override
  public boolean equals(Object rhs) {
    return EqualsBuilder.reflectionEquals(this, rhs, "updatedOnMillis");
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this, false);
  }

  public Set<DatasetQuality> getQualities() {
    if (isNull(qualities)) {
      return null;
    }

    return qualities.stream().map(DatasetQuality::new).collect(toSet());
  }

  public void setQualities(Collection<DatasetQuality> qualities) {
    if (isNull(qualities)) {
      this.qualities = null;
      return;
    }

    this.qualities = qualities.stream().map(DatasetQuality::new).collect(toSet());
  }
}
