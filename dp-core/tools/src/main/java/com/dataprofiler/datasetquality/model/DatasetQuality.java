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

public class DatasetQuality implements Comparable<DatasetQuality> {
  protected long updatedOnMillis;
  protected long datasetUpdatedOnMillis;
  protected String dataset;
  protected String table;
  protected String column;
  protected long count;
  protected long totalNumValues;
  protected Set<ColumnTypeCount> columnTypeCounts;
  protected QualityEnum qualityEnum;
  protected QualityWarningEnum qualityWarningEnum;

  public DatasetQuality() {
    super();
    ZoneOffset zone = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    this.updatedOnMillis = LocalDateTime.now().toEpochSecond(zone) * 1000;
    this.qualityEnum = QualityEnum.UNKNOWN;
    this.qualityWarningEnum = QualityWarningEnum.IGNORE;
    this.datasetUpdatedOnMillis = -1;
  }

  public DatasetQuality(final DatasetQuality datasetQuality) {
    this();
    if (isNull(datasetQuality)) {
      return;
    }
    this.updatedOnMillis = datasetQuality.getUpdatedOnMillis();
    this.datasetUpdatedOnMillis = datasetQuality.getDatasetUpdatedOnMillis();
    this.dataset = datasetQuality.getDataset();
    this.table = datasetQuality.getTable();
    this.column = datasetQuality.getColumn();
    this.count = datasetQuality.getCount();
    this.totalNumValues = datasetQuality.getTotalNumValues();
    this.qualityEnum = datasetQuality.getQualityEnum();
    this.qualityWarningEnum = datasetQuality.getQualityWarningEnum();
    this.columnTypeCounts = datasetQuality.getColumnTypeCounts();
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

  @Override
  public int compareTo(DatasetQuality o) {
    if (isNull(o)) {
      return Integer.MAX_VALUE;
    }
    return Long.compare(this.getDatasetUpdatedOnMillis(), o.getDatasetUpdatedOnMillis());
  }

  public long getUpdatedOnMillis() {
    return updatedOnMillis;
  }

  public void setUpdatedOnMillis(long updatedOnMillis) {
    this.updatedOnMillis = updatedOnMillis;
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

  public long getCount() {
    return count;
  }

  public void setCount(long count) {
    this.count = count;
  }

  public long getTotalNumValues() {
    return totalNumValues;
  }

  public void setTotalNumValues(long totalNumValues) {
    this.totalNumValues = totalNumValues;
  }

  public QualityEnum getQualityEnum() {
    return qualityEnum;
  }

  public void setQualityEnum(QualityEnum qualityEnum) {
    this.qualityEnum = qualityEnum;
  }

  public QualityWarningEnum getQualityWarningEnum() {
    return qualityWarningEnum;
  }

  public void setQualityWarningEnum(QualityWarningEnum qualityWarningEnum) {
    this.qualityWarningEnum = qualityWarningEnum;
  }

  public long getDatasetUpdatedOnMillis() {
    return datasetUpdatedOnMillis;
  }

  public void setDatasetUpdatedOnMillis(long datasetUpdatedOnMillis) {
    this.datasetUpdatedOnMillis = datasetUpdatedOnMillis;
  }

  public Set<ColumnTypeCount> getColumnTypeCounts() {
    if (isNull(columnTypeCounts)) {
      return null;
    }

    return columnTypeCounts.stream().map(ColumnTypeCount::new).collect(toSet());
  }

  public void setColumnTypeCounts(Collection<ColumnTypeCount> columnTypeCounts) {
    if (isNull(columnTypeCounts)) {
      this.columnTypeCounts = null;
      return;
    }

    this.columnTypeCounts = columnTypeCounts.stream().map(ColumnTypeCount::new).collect(toSet());
  }
}
