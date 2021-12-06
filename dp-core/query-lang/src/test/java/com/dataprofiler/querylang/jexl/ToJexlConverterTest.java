package com.dataprofiler.querylang.jexl;

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

import com.dataprofiler.querylang.expr.Expression;
import com.dataprofiler.querylang.json.Expressions;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ToJexlConverterTest {

  @Test
  public void basicOperatorTest() {
    ToJexlStringVisitor vis = new ToJexlStringVisitor();
    Expressions.parse("{'$eq':{'column': 'name', 'value': 'bill', type: 'string'}}")
        .accept(vis);
    assertEquals("NAME == \"bill\"", vis.toString());
    vis.reset();

    Expressions.parse("{'$lt':{'column': 'name', 'value': 'bill', type: 'string'}}")
        .accept(vis);
    assertEquals("NAME < \"bill\"", vis.toString());
    vis.reset();

    Expressions.parse("{'$lte':{'column': 'name', 'value': 'bill', type: 'string'}}")
        .accept(vis);
    assertEquals("NAME <= \"bill\"", vis.toString());
    vis.reset();

    Expressions.parse("{'$gt':{'column': 'name', 'value': 'bill', type: 'string'}}")
        .accept(vis);
    assertEquals("NAME > \"bill\"", vis.toString());
    vis.reset();

    Expressions.parse("{'$gte':{'column': 'name', 'value': 'bill', type: 'string'}}")
        .accept(vis);
    assertEquals("NAME >= \"bill\"", vis.toString());
    vis.reset();
  }

  @Test
  public void basicGroupingTest() {
    ToJexlStringVisitor vis = new ToJexlStringVisitor();
    Expressions.parse("{'$eq':{'column': 'name', 'value': 'bill', type: 'string'}}")
        .accept(vis);
    assertEquals("NAME == \"bill\"", vis.toString());
    vis.reset();

    Expressions.parse("{'$lt':{'column': 'name', 'value': 'bill', type: 'string'}}")
        .accept(vis);
    assertEquals("NAME < \"bill\"", vis.toString());
    vis.reset();

    Expressions.parse("{'$lte':{'column': 'name', 'value': 'bill', type: 'string'}}")
        .accept(vis);
    assertEquals("NAME <= \"bill\"", vis.toString());
    vis.reset();

    Expressions.parse("{'$gt':{'column': 'name', 'value': 'bill', type: 'string'}}")
        .accept(vis);
    assertEquals("NAME > \"bill\"", vis.toString());
    vis.reset();

    Expressions.parse("{'$gte':{'column': 'name', 'value': 'bill', type: 'string'}}")
        .accept(vis);
    assertEquals("NAME >= \"bill\"", vis.toString());
    vis.reset();
  }

  @Test
  public void basicOrTest() {
    Expression expression = Expressions.parse("{'$or': ["
        + "{'$eq': {'value': 'steve', 'column': 'name'}},"
        + "{'$eq': {'value': 33, 'column': 'age', 'type': 'number'}}"
        + "]}");
    ToJexlStringVisitor toJexl = new ToJexlStringVisitor();
    expression.accept(toJexl);
    assertEquals("(NAME == \"steve\" || AGE == 33)", toJexl.toString());
  }

  @Test
  public void basicAndtest() {
    Expression expression = Expressions.parse("{'$and': ["
        + "{'$eq': {'value': 'steve', 'column': 'name'}},"
        + "{'$eq': {'value': 33, 'column': 'age', 'type': 'number'}}"
        + "]}");
    ToJexlStringVisitor toJexl = new ToJexlStringVisitor();
    expression.accept(toJexl);
    assertEquals("(NAME == \"steve\" && AGE == 33)", toJexl.toString());
  }
}

