package com.dataprofiler.datasetperformance.model;

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
import static java.util.stream.Collectors.toList;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class DatasetPerformance {

  protected long updatedOnMillis;
  protected long numUsersWithAttribute;
  protected long numUserWithAttributeAndContractor;
  protected long topViewCountAcrossAllDatasets;
  protected List<DailyStat> chartData;
  protected TrendStat searchAppearanceData;
  protected TrendStat downloadData;
  protected String dataset;

  public DatasetPerformance() {
    super();
    ZoneOffset zone = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    this.updatedOnMillis = LocalDateTime.now().toEpochSecond(zone) * 1000;
    this.numUsersWithAttribute = 0;
    this.chartData = new ArrayList<>();
    this.searchAppearanceData = new TrendStat();
    this.downloadData = new TrendStat();
    this.numUserWithAttributeAndContractor = 0;
    this.topViewCountAcrossAllDatasets = 0;
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

  public long getUpdatedOnMillis() {
    return updatedOnMillis;
  }

  public void setUpdatedOnMillis(long updatedOnMillis) {
    this.updatedOnMillis = updatedOnMillis;
  }

  public long getNumUsersWithAttribute() {
    return numUsersWithAttribute;
  }

  public void setNumUsersWithAttribute(long numUsersWithAttribute) {
    this.numUsersWithAttribute = numUsersWithAttribute;
  }

  public List<DailyStat> getChartData() {
    if (isNull(chartData)) {
      return null;
    }

    return chartData.stream().map(DailyStat::new).collect(toList());
  }

  public void setChartData(List<DailyStat> stats) {
    if (isNull(stats)) {
      chartData = null;
      return;
    }

    chartData = stats.stream().map(DailyStat::new).collect(toList());
  }

  public TrendStat getSearchAppearanceData() {
    if (isNull(searchAppearanceData)) {
      return null;
    }

    return searchAppearanceData;
  }

  public void setSearchAppearanceData(TrendStat stats) {
    if (isNull(stats)) {
      searchAppearanceData = null;
      return;
    }

    this.searchAppearanceData = stats;
  }

  public TrendStat getDownloadData() {
    if (isNull(searchAppearanceData)) {
      return null;
    }

    return downloadData;
  }

  public void setDownloadData(TrendStat stats) {
    if (isNull(stats)) {
      downloadData = null;
      return;
    }

    this.downloadData = stats;
  }

  public long getNumUserWithAttributeAndContractor() {
    return numUserWithAttributeAndContractor;
  }

  public void setNumUserWithAttributeAndContractor(long numUserWithAttributeAndContractor) {
    this.numUserWithAttributeAndContractor = numUserWithAttributeAndContractor;
  }

  public String getDataset() {
    return dataset;
  }

  public void setDataset(String dataset) {
    this.dataset = dataset;
  }

  public long getTopViewCountAcrossAllDatasets() {
    return topViewCountAcrossAllDatasets;
  }

  public void setTopViewCountAcrossAllDatasets(long topViewCountAcrossAllDatasets) {
    this.topViewCountAcrossAllDatasets = topViewCountAcrossAllDatasets;
  }
}
