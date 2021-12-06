package com.dataprofiler.util;

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

import com.dataprofiler.querylang.expr.Expression;
import com.dataprofiler.querylang.json.Expressions;
import com.dataprofiler.test.IntegrationTest;
import org.junit.*;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category(IntegrationTest.class)
public class RangeCheckingVisitorTest {

  RangeCheckingVisitor rcv = new RangeCheckingVisitor();

  @Before
  public void setup() {
    rcv.reset();
  }

  @Test
  public void testEq() {
    Expression expr = Expressions.parse("{'$eq': {'column': 'c', 'value': 'v'}}");
    expr.accept(rcv);
    assertTrue(rcv.getLastStatus());
  }

  @Test
  public void testUnionOfEq() {
    Expression expr = Expressions.parse("{'$or': [{'$eq': {'column': 'c', 'value': 'v'}},"
        + "{'$eq': {'column': 'c', 'value': 'v'}}]}");
    expr.accept(rcv);
    assertTrue(rcv.getLastStatus());
  }

  @Test
  public void testUnionWithRange() {
    Expression expr = Expressions.parse("{'$or': [{'$eq': {'column': 'c', 'value': 'v'}},"
        + "{'$lt': {'column': 'c', 'value': 'v'}}]}");
    expr.accept(rcv);
    assertFalse(rcv.getLastStatus());
  }

  @Test
  public void testIntersectionWithRange() {
    Expression expr = Expressions.parse("{'$and': [{'$eq': {'column': 'c', 'value': 'v'}},"
        + "{'$lt': {'column': 'c', 'value': 'v'}}]}");
    expr.accept(rcv);
    assertTrue(rcv.getLastStatus());
  }

  @Test
  public void testIntersectionWithNestedEq() {
    Expression expr = Expressions.parse("{'$and': ["
        + "{'$or': [{'$eq': {'column': 'c', 'value': 'v'}}, {'$eq': {'column': 'c', 'value': 'v'}}]},"
        + "{'$lt': {'column': 'c', 'value': 'v'}}]}");
    expr.accept(rcv);
    assertTrue(rcv.getLastStatus());
  }
}
