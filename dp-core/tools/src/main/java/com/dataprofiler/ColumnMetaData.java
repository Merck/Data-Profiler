package com.dataprofiler;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;

public class ColumnMetaData {
  public String name;
  public DataType dataType;
  public Boolean nullable;
  public int columnNumber = -1;

  public ColumnMetaData(String name) {
    this.name = name;
    this.dataType = DataTypes.StringType;
    this.nullable = true;
  }

  public ColumnMetaData(String name, DataType type) {
    this(name);
    this.dataType = type;
  }

  public ColumnMetaData(String name, DataType type, Boolean nullable, int columnNumber) {
    this(name, type);
    this.nullable = nullable;
    this.columnNumber = columnNumber;
  }

  public ColumnMetaData(String name, String type, Boolean nullable, int columnNumber) {
    this(name);
    setDataType(type);
    this.nullable = nullable;
    this.columnNumber = columnNumber;
  }

  public void update(ColumnMetaData other) {
    this.name = other.name;
    this.dataType = other.dataType;
    this.nullable = other.nullable;
    // Only override things that are set
    if (other.columnNumber != -1) {
      this.columnNumber = other.columnNumber;
    }
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DataType getDataType() {
    return dataType;
  }

  public String getTypeString() {
    String typeString = dataType.json();
    // remove quoutes
    return typeString.substring(1, typeString.length() - 1);
  }

  @JsonIgnore
  public void setDataType(DataType dataType) {
    this.dataType = dataType;
  }

  public void setDataType(String dataType) {
    this.dataType = DataType.fromJson(String.format("\"%s\"", dataType));
  }

  public Boolean getNullable() {
    return nullable;
  }

  public void setNullable(Boolean nullable) {
    this.nullable = nullable;
  }

  public int getColumnNumber() {
    return columnNumber;
  }

  public void setColumnNumber(int columnNumber) {
    this.columnNumber = columnNumber;
  }

  @Override
  public String toString() {
    return "ColumnMetaData{"
        + "name='"
        + name
        + '\''
        + ", dataType="
        + dataType.json()
        + ", nullable="
        + nullable
        + ", columnNumber="
        + columnNumber
        + '}';
  }
}
