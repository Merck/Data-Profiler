package com.dataprofiler.querylang;

/*-
 * 
 * dataprofiler-iterators
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

import com.dataprofiler.querylang.expr.BooleanExpression;
import com.dataprofiler.querylang.expr.BooleanExpression.Operation;
import com.dataprofiler.querylang.expr.EqExpression;
import com.dataprofiler.querylang.expr.Expression;
import com.dataprofiler.querylang.expr.GreaterThanEqExpression;
import com.dataprofiler.querylang.expr.GreaterThanExpression;
import com.dataprofiler.querylang.expr.LessThanEqExpression;
import com.dataprofiler.querylang.expr.LessThanExpression;
import com.dataprofiler.querylang.expr.Visitor;
import com.dataprofiler.querylang.json.Expressions;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvaluatingVisitor implements Visitor {

  private Multimap<String, String> record;
  private boolean lastResult;

  public static <K, V> Collection<V> checkMultimap(Multimap<K, V> mm, K key) {
    Collection<V> values;
    return (values = mm.get(key)) == null ? Collections.emptyList() : values;
  }

  @Override
  public void accept(BooleanExpression expr) {
    if (Operation.AND.equals(expr.getOperation())) {
      boolean result = true;
      List<Expression> children = expr.getSubExpressions();
      for (int i = 0; i < children.size() && result; i++) {
        children.get(i).accept(this);
        result &= this.lastResult;
      }
      this.lastResult = result;
    } else {
      boolean result = false;
      List<Expression> children = expr.getSubExpressions();
      for (int i = 0; i < children.size(); i++) {
        children.get(i).accept(this);
        result |= this.lastResult;
      }
      this.lastResult = result;
    }
  }

  @Override
  public void accept(EqExpression expr) {
    this.lastResult =
        checkMultimap(record, expr.getColumn()).stream()
            .anyMatch(v -> v.compareTo(expr.getValue()) == 0);
  }

  @Override
  public void accept(GreaterThanExpression expr) {
    this.lastResult =
        checkMultimap(record, expr.getColumn()).stream()
            .anyMatch(v -> v.compareTo(expr.getValue()) > 0);
  }

  @Override
  public void accept(GreaterThanEqExpression expr) {
    this.lastResult =
        checkMultimap(record, expr.getColumn()).stream()
            .anyMatch(v -> v.compareTo(expr.getValue()) >= 0);
  }

  @Override
  public void accept(LessThanExpression expr) {
    this.lastResult =
        checkMultimap(record, expr.getColumn()).stream()
            .anyMatch(v -> v.compareTo(expr.getValue()) < 0);
  }

  @Override
  public void accept(LessThanEqExpression expr) {
    this.lastResult =
        checkMultimap(record, expr.getColumn()).stream()
            .anyMatch(v -> v.compareTo(expr.getValue()) <= 0);
  }

  public boolean getLastResult() {
    return lastResult;
  }

  public Multimap<String, String> getRecord() {
    return record;
  }

  public void setRecord(Multimap<String, String> record) {
    this.record = record;
  }

  public void setRecord(Map<String, String> record) {
    setRecord(Multimaps.forMap(record));
  }

  public void reset() {
    this.lastResult = false;
    this.record = null;
  }

  public static void main(String[] args) throws Throwable {
    Expression eq = Expressions.parse("{'$eq': {'value': 'steve', 'column': 'name'}}");
    Map<String, String> record = new HashMap<>();
    record.put("name", "steve");
    EvaluatingVisitor vis = new EvaluatingVisitor();
    vis.setRecord(record);
    System.out.println(!vis.lastResult);
    eq.accept(vis);
    System.out.println(vis.lastResult);

    vis.reset();

    record = new HashMap<>(record);
    record.put("age", "32");
    Expression and =
        Expressions.parse(
            "{'$and': ["
                + "{'$eq': {'value': 'steve', 'column': 'name'}},"
                + "{'$eq': {'value': '32', 'column': 'age'}}"
                + "]}");
    vis.setRecord(record);
    System.out.println(!vis.lastResult);
    and.accept(vis);
    System.out.println(vis.lastResult);

    vis.reset();
    and =
        Expressions.parse(
            "{'$and': ["
                + "{'$eq': {'value': 'steve', 'column': 'name'}},"
                + "{'$eq': {'value': '33', 'column': 'age'}}"
                + "]}");
    vis.setRecord(record);
    System.out.println(!vis.lastResult);
    and.accept(vis);
    System.out.println(!vis.lastResult);

    vis.reset();
    Expression expression =
        Expressions.parse(
            "{'$or': ["
                + "{'$eq': {'value': 'steve', 'column': 'name'}},"
                + "{'$eq': {'value': '33', 'column': 'age'}}"
                + "]}");
    vis.setRecord(record);
    System.out.println(!vis.lastResult);
    expression.accept(vis);
    System.out.println(vis.lastResult);

    vis.reset();
    expression =
        Expressions.parse(
            "{'$or': ["
                + "{'$eq': {'value': 'bill', 'column': 'name'}},"
                + "{'$eq': {'value': '33', 'column': 'age'}}"
                + "]}");
    vis.setRecord(record);
    System.out.println(!vis.lastResult);
    expression.accept(vis);
    System.out.println(!vis.lastResult);

    vis.reset();
    expression = Expressions.parse("{'$gt': {'column': 'age', 'value': 30}}");
    vis.setRecord(record);
    System.out.println(!vis.lastResult);
    expression.accept(vis);
    System.out.println(vis.lastResult);

    vis.reset();
    expression = Expressions.parse("{'$lt': {'column': 'age', 'value': 40}}");
    vis.setRecord(record);
    System.out.println(!vis.lastResult);
    expression.accept(vis);
    System.out.println(vis.lastResult);

    vis.reset();
    expression = Expressions.parse("{'$lte': {'column': 'age', 'value': 33}}");
    vis.setRecord(record);
    System.out.println(!vis.lastResult);
    expression.accept(vis);
    System.out.println(vis.lastResult);

    vis.reset();
    expression = Expressions.parse("{'$lte': {'column': 'age', 'value': 32}}");
    vis.setRecord(record);
    System.out.println(!vis.lastResult);
    expression.accept(vis);
    System.out.println(vis.lastResult);

    vis.reset();
    expression = Expressions.parse("{'$gte': {'column': 'age', 'value': 32}}");
    vis.setRecord(record);
    System.out.println(!vis.lastResult);
    expression.accept(vis);
    System.out.println(vis.lastResult);
  }
}
