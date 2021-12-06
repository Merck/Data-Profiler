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
import java.util.ArrayDeque;
import java.util.Deque;

public class RangeCheckingVisitor implements Visitor {
  private final ArrayDeque<Boolean> stack = new ArrayDeque<>();

  public void reset() {
    stack.clear();
  }

  public Deque<Boolean> getStack() {
    return new ArrayDeque<Boolean>(stack);
  }

  public Boolean getLastStatus() {
    return stack.peekLast();
  }

  @Override
  public void accept(BooleanExpression expr) {
    if (expr.getOperation().equals(Operation.OR)) {
      boolean result = true;
      for (Expression child : expr.getSubExpressions()) {
        child.accept(this);
        result &= stack.pop();
      }
      stack.push(result);
    } else if (expr.getOperation().equals(Operation.AND)) {
      boolean result = false;
      for (Expression child : expr.getSubExpressions()) {
        child.accept(this);
        result |= stack.pop();
      }
      stack.push(result);
    }
  }

  @Override
  public void accept(EqExpression expr) {
    stack.push(true);
  }

  @Override
  public void accept(GreaterThanExpression expr) {
    stack.push(false);
  }

  @Override
  public void accept(GreaterThanEqExpression expr) {
    stack.push(false);
  }

  @Override
  public void accept(LessThanExpression expr) {
    stack.push(false);
  }

  @Override
  public void accept(LessThanEqExpression expr) {
    stack.push(false);
  }

  public static void main(String[] args) {
    RangeCheckingVisitor rcv = new RangeCheckingVisitor();

    {
      rcv.reset();
      Expression expr = Expressions.parse("{'$eq': {'column': 'c', 'value': 'v'}}");
      expr.accept(rcv);
      System.out.println(rcv.getLastStatus());
    }

    {
      rcv.reset();
      Expression expr =
          Expressions.parse(
              "{'$or': [{'$eq': {'column': 'c', 'value': 'v'}},"
                  + "{'$eq': {'column': 'c', 'value': 'v'}}]}");
      expr.accept(rcv);
      System.out.println(rcv.getLastStatus());
    }

    {
      rcv.reset();
      Expression expr =
          Expressions.parse(
              "{'$or': [{'$eq': {'column': 'c', 'value': 'v'}},"
                  + "{'$lt': {'column': 'c', 'value': 'v'}}]}");
      expr.accept(rcv);
      System.out.println(rcv.getLastStatus());
    }

    {
      rcv.reset();
      Expression expr =
          Expressions.parse(
              "{'$and': [{'$eq': {'column': 'c', 'value': 'v'}},"
                  + "{'$lt': {'column': 'c', 'value': 'v'}}]}");
      expr.accept(rcv);
      System.out.println(rcv.getLastStatus());
    }

    {
      rcv.reset();
      Expression expr =
          Expressions.parse(
              "{'$and': ["
                  + "{'$or': [{'$eq': {'column': 'c', 'value': 'v'}}, {'$eq': {'column': 'c', 'value': 'v'}}]},"
                  + "{'$lt': {'column': 'c', 'value': 'v'}}]}");
      expr.accept(rcv);
      System.out.println(rcv.getLastStatus());
    }
  }
}
