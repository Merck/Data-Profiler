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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class TableVersion {
  protected Map<String, String> columns;
  protected String table;
  protected String version;

  public TableVersion() {
    super();
    columns = new HashMap<>();
  }

  public TableVersion(TableVersion tableVersion) {
    this();
    this.table = tableVersion.getTable();
    this.version = tableVersion.getVersion();
    Map<String, String> map = tableVersion.getColumns();
    if (nonNull(map)) {
      this.columns = map.entrySet().stream().collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
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

  public Map<String, String> getColumns() {
    if (isNull(columns)) {
      return null;
    }

    return columns.entrySet().stream().collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public void setColumns(Map<String, String> columns) {
    if (isNull(columns)) {
      this.columns = null;
      return;
    }

    this.columns =
        columns.entrySet().stream().collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
