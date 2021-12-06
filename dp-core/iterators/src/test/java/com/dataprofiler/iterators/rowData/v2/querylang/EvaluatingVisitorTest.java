package com.dataprofiler.iterators.rowData.v2.querylang;

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

import static org.junit.Assert.*;

import com.dataprofiler.test.IntegrationTest;
import com.google.common.collect.ImmutableMap;
import com.dataprofiler.querylang.EvaluatingVisitor;
import com.dataprofiler.querylang.expr.Expression;
import com.dataprofiler.querylang.json.Expressions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import java.util.Map;

@Category(IntegrationTest.class)
public class EvaluatingVisitorTest {

  EvaluatingVisitor vis = new EvaluatingVisitor();
  Map<String, String> record = ImmutableMap.of("name", "steve",
      "age", "32");

  @Before
  public void setup() {
    vis.reset();
    vis.setRecord(record);
  }

  @Test
  public void simpleTest() {
    Expression eq = Expressions.parse("{'$eq': {'value': 'steve', 'column': 'name'}}");
    eq.accept(vis);
    assertTrue(vis.getLastResult());
  }


  @Test
  public void testAnd() {
    Expression and = Expressions.parse("{'$and': ["
        + "{'$eq': {'value': 'steve', 'column': 'name'}},"
        + "{'$eq': {'value': '32', 'column': 'age'}}"
        + "]}");
    and.accept(vis);
    assertTrue(vis.getLastResult());
  }

  @Test
  public void testAnd2() {
    Expression and = Expressions.parse("{'$and': ["
        + "{'$eq': {'value': 'steve', 'column': 'name'}},"
        + "{'$eq': {'value': '33', 'column': 'age'}}"
        + "]}");
    and.accept(vis);
    assertFalse(vis.getLastResult());
  }

  @Test
  public void testOr() {
    Expression expression = Expressions.parse("{'$or': ["
        + "{'$eq': {'value': 'steve', 'column': 'name'}},"
        + "{'$eq': {'value': '33', 'column': 'age'}}"
        + "]}");
    expression.accept(vis);
    assertTrue(vis.getLastResult());
  }

  @Test
  public void testOr2() {
    Expression expression = Expressions.parse("{'$or': ["
        + "{'$eq': {'value': 'bill', 'column': 'name'}},"
        + "{'$eq': {'value': '33', 'column': 'age'}}"
        + "]}");
    expression.accept(vis);
    assertFalse(vis.getLastResult());
  }

  @Test
  public void testGT() {
    Expression expression = Expressions.parse("{'$gt': {'column': 'age', 'value': 30}}");
    expression.accept(vis);
    assertTrue(vis.getLastResult());
  }

  @Test
  public void testLT() {
    Expression expression = Expressions.parse("{'$lt': {'column': 'age', 'value': 40}}");
    expression.accept(vis);
    assertTrue(vis.getLastResult());
  }

  @Test
  public void testLTE() {
    Expression expression = Expressions.parse("{'$lte': {'column': 'age', 'value': 33}}");
    expression.accept(vis);
    assertTrue(vis.getLastResult());
  }

  @Test
  public void testLTE2() {
    Expression expression = Expressions.parse("{'$lte': {'column': 'age', 'value': 32}}");
    expression.accept(vis);
    assertTrue(vis.getLastResult());;
  }

  @Test
  public void testGTE() {
    Expression expression = Expressions.parse("{'$gte': {'column': 'age', 'value': 32}}");
    expression.accept(vis);
    assertTrue(vis.getLastResult());;
  }
}
