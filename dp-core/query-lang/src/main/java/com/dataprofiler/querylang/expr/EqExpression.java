package com.dataprofiler.querylang.expr;

/*-
 * 
 * dataprofiler-query-lang
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

import java.util.Objects;

public class EqExpression implements ColumnExpression {
  private String column;
  private String value;
  private String type;

  public EqExpression() {}

  public EqExpression(String column, String value, String type) {
    this.column = column;
    this.value = value;
    this.type = type;
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.accept(this);
  }

  public String getColumn() {
    return column;
  }

  public String getValue() {
    return value;
  }

  public String getType() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    EqExpression that = (EqExpression) o;
    return Objects.equals(column, that.column) &&
        Objects.equals(value, that.value) &&
        Objects.equals(type, that.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(column, value, type);
  }

  @Override
  public String toString() {
    return "EqExpression{" +
        "column='" + column + '\'' +
        ", value='" + value + '\'' +
        ", type='" + type + '\'' +
        '}';
  }
}
