package com.dataprofiler.datasetperformance.model.status;

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

import java.time.Duration;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class PerformanceTaskStatus {
  public boolean success;
  public String dataset;
  public int numChartDays;
  public long numUsersWithAttribute;
  public long allTimeSearchTrendStat;
  public long allTimeDownloadTrendStat;
  public Duration duration;

  public PerformanceTaskStatus() {
    super();
    numChartDays = 0;
    duration = Duration.ZERO;
  }

  public String getDataset() {
    return dataset;
  }

  public PerformanceTaskStatus setDataset(String dataset) {
    this.dataset = dataset;
    return this;
  }

  public Duration getDuration() {
    return duration;
  }

  public PerformanceTaskStatus setDuration(Duration duration) {
    this.duration = duration;
    return this;
  }

  public boolean isSuccess() {
    return success;
  }

  public PerformanceTaskStatus setSuccess(boolean success) {
    this.success = success;
    return this;
  }

  public int getNumChartDays() {
    return numChartDays;
  }

  public PerformanceTaskStatus setNumChartDays(int numChartDays) {
    this.numChartDays = numChartDays;
    return this;
  }

  public long getNumUsersWithAttribute() {
    return numUsersWithAttribute;
  }

  public PerformanceTaskStatus setNumUsersWithAttribute(long numUsersWithAttribute) {
    this.numUsersWithAttribute = numUsersWithAttribute;
    return this;
  }

  public long getAllTimeSearchTrendStat() {
    return allTimeSearchTrendStat;
  }

  public PerformanceTaskStatus setAllTimeSearchTrendStat(long allTimeSearchTrendStat) {
    this.allTimeSearchTrendStat = allTimeSearchTrendStat;
    return this;
  }

  public long getAllTimeDownloadTrendStat() {
    return allTimeDownloadTrendStat;
  }

  public PerformanceTaskStatus setAllTimeDownloadTrendStat(long allTimeDownloadTrendStat) {
    this.allTimeDownloadTrendStat = allTimeDownloadTrendStat;
    return this;
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
}
