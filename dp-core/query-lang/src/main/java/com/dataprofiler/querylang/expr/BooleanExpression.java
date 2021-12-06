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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.dataprofiler.querylang.json.BooleanExpressionSerDe;
import com.dataprofiler.querylang.json.EqExpressionSerDe;
import com.dataprofiler.querylang.json.ExpressionSerDe;
import java.util.List;

public class BooleanExpression implements Expression {
  public enum Operation {
    AND,
    OR
  }

  private Operation operation;
  private List<Expression> subExpressions;

  public BooleanExpression() {}

  public BooleanExpression(Operation operation, List<Expression> subExpressions) {
    this.operation = operation;
    this.subExpressions = ImmutableList.copyOf(subExpressions);
  }

  public Operation getOperation() {
    return operation;
  }

  public void setOperation(Operation operation) {
    this.operation = operation;
  }

  public List<Expression> getSubExpressions() {
    return subExpressions;
  }

  public void setSubExpressions(List<Expression> subExpressions) {
    this.subExpressions = ImmutableList.copyOf(subExpressions);
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.accept(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    BooleanExpression that = (BooleanExpression) o;
    return getOperation() == that.getOperation() &&
        Objects.equal(getSubExpressions(), that.getSubExpressions());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getOperation(), getSubExpressions());
  }

  @Override
  public String toString() {
    return "BooleanExpression{" +
        "operation=" + operation +
        ", subExpressions=" + subExpressions +
        '}';
  }

  public static void main(String[] args) throws Exception {
    Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(BooleanExpression.class, new BooleanExpressionSerDe())
        .registerTypeAdapter(EqExpression.class, new EqExpressionSerDe())
        .registerTypeAdapter(Expression.class, new ExpressionSerDe())
        .create();

    EqExpression a = new EqExpression("name", "bill", "string");
    EqExpression b = new EqExpression("name", "bob", "string");
    BooleanExpression or = new BooleanExpression(Operation.OR, ImmutableList.of(a, b));

    JsonElement serialized = gson.toJsonTree(or, BooleanExpression.class);
    System.out.println(serialized);

    BooleanExpression deserialized = gson.fromJson(serialized.toString(), BooleanExpression.class);
    System.out.println(deserialized);


    String eqA = gson.toJson(a);
    System.out.println(eqA);
    Expression eqAExpr = gson.fromJson(eqA, Expression.class);
    System.out.println(eqAExpr);

    String query= "{'$or': [{'$eq': {'type': 'string', 'column': 'State Name', 'value': 'Wyoming,County/County'}}, { '$eq': {'type': 'string', 'column': 'Equivalent', 'value': 'Anchorage Municipality'}}]}"
        .replace('\'', '"');

    Expression expr = gson.fromJson(query, Expression.class);
    System.out.println(expr);
  }
}
