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

import com.dataprofiler.util.objects.CustomAnnotation.AnnotationType;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.log4j.Logger;

public class CustomAnnotationCount implements Comparable<CustomAnnotationCount> {

  private static final Logger logger = Logger.getLogger(CustomAnnotationCount.class);

  private AnnotationType annotationType;
  private String dataset;
  private String table;
  private String column;
  private final AtomicLong count;

  public CustomAnnotationCount(AnnotationType annotationType) {
    this(annotationType, "", "", "", 0);
  }

  public CustomAnnotationCount(
      AnnotationType annotationType, String dataset, String table, String column, long count) {
    super();
    this.annotationType = annotationType;
    this.dataset = dataset;
    this.table = table;
    this.column = column;
    this.count = new AtomicLong(count);
  }

  public AnnotationType getAnnotationType() {
    return annotationType;
  }

  public void setAnnotationType(AnnotationType annotationType) {
    this.annotationType = annotationType;
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
    return this.count.get();
  }

  public CustomAnnotationCount incrementCount() {
    this.count.incrementAndGet();
    return this;
  }

  public static CustomAnnotationCount emptyDatasetCount(String dataset) {
    CustomAnnotationCount count =
        new CustomAnnotationCount(CustomAnnotation.AnnotationType.DATASET);
    count.setDataset(dataset);
    return count;
  }

  public static CustomAnnotationCount emptyTableCount(String dataset, String table) {
    CustomAnnotationCount count = new CustomAnnotationCount(AnnotationType.TABLE);
    count.setDataset(dataset);
    count.setTable(table);
    return count;
  }

  public static CustomAnnotationCount emptyColumnCount(
      String dataset, String table, String column) {
    CustomAnnotationCount count = new CustomAnnotationCount(AnnotationType.COLUMN);
    count.setDataset(dataset);
    count.setTable(table);
    count.setColumn(column);
    return count;
  }

  @Override
  public int compareTo(CustomAnnotationCount o) {
    Function<CustomAnnotationCount, String> genCompareKey =
        (obj) ->
            String.format(
                "%s-%s-%s-%s-%d",
                obj.annotationType, obj.dataset, obj.table, obj.column, obj.count.get());
    String thisKey = genCompareKey.apply(this);
    String compareKey = genCompareKey.apply(o);
    return thisKey.compareTo(compareKey);
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
