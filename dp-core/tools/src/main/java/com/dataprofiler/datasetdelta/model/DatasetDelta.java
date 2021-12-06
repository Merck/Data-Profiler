package com.dataprofiler.datasetdelta.model;

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

import static com.dataprofiler.datasetdelta.model.DeltaEnum.NO_OP;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class DatasetDelta implements Comparable<DatasetDelta> {

  protected long updatedOnMillis;
  protected long datasetUpdatedOnMillis;
  protected String dataset;
  protected String table;
  protected String column;
  protected DeltaEnum deltaEnum;
  protected String valueFrom;
  protected String valueTo;
  protected String fromVersion;
  protected String targetVersion;
  protected CommentUUID comment;

  public DatasetDelta() {
    super();
    deltaEnum = NO_OP;
    ZoneOffset zone = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    updatedOnMillis = LocalDateTime.now().toEpochSecond(zone) * 1000;
  }

  public DatasetDelta(DatasetDelta datasetDelta) {
    super();
    this.updatedOnMillis = datasetDelta.getUpdatedOnMillis();
    this.dataset = datasetDelta.getDataset();
    this.table = datasetDelta.getTable();
    this.column = datasetDelta.getColumn();
    this.deltaEnum =
        !isNull(datasetDelta.getDeltaEnum())
            ? DeltaEnum.valueOf(datasetDelta.getDeltaEnum().toString())
            : null;
    this.valueFrom = datasetDelta.getValueFrom();
    this.valueTo = datasetDelta.getValueTo();
    this.datasetUpdatedOnMillis = datasetDelta.getDatasetUpdatedOnMillis();
    this.targetVersion = datasetDelta.getTargetVersion();
    this.fromVersion = datasetDelta.getFromVersion();
    if (nonNull(datasetDelta.getComment())) {
      this.comment = new CommentUUID(datasetDelta.getComment());
    }
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  @Override
  public boolean equals(Object rhs) {
    return EqualsBuilder.reflectionEquals(this, rhs, "comment");
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this, "comment");
  }

  @Override
  public int compareTo(DatasetDelta o) {
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

  public DeltaEnum getDeltaEnum() {
    return deltaEnum;
  }

  public void setDeltaEnum(DeltaEnum deltaEnum) {
    this.deltaEnum = deltaEnum;
  }

  public String getValueFrom() {
    return valueFrom;
  }

  public void setValueFrom(String valueFrom) {
    this.valueFrom = valueFrom;
  }

  public String getValueTo() {
    return valueTo;
  }

  public void setValueTo(String valueTo) {
    this.valueTo = valueTo;
  }

  public String getFromVersion() {
    return fromVersion;
  }

  public void setFromVersion(String fromVersion) {
    this.fromVersion = fromVersion;
  }

  public String getTargetVersion() {
    return targetVersion;
  }

  public void setTargetVersion(String targetVersion) {
    this.targetVersion = targetVersion;
  }

  public long getDatasetUpdatedOnMillis() {
    return datasetUpdatedOnMillis;
  }

  public void setDatasetUpdatedOnMillis(long datasetUpdatedOnMillis) {
    this.datasetUpdatedOnMillis = datasetUpdatedOnMillis;
  }

  public CommentUUID getComment() {
    return comment;
  }

  public void setComment(CommentUUID comment) {
    this.comment = comment;
  }

  public boolean hasLinkedComment() {
    return nonNull(getComment()) && nonNull(getComment().getUuid());
  }
}
