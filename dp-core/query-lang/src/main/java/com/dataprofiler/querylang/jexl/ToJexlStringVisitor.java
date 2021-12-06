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

import com.dataprofiler.querylang.expr.BooleanExpression;
import com.dataprofiler.querylang.expr.EqExpression;
import com.dataprofiler.querylang.expr.Expression;
import com.dataprofiler.querylang.expr.GreaterThanEqExpression;
import com.dataprofiler.querylang.expr.GreaterThanExpression;
import com.dataprofiler.querylang.expr.LessThanEqExpression;
import com.dataprofiler.querylang.expr.LessThanExpression;
import com.dataprofiler.querylang.expr.Visitor;
import com.dataprofiler.querylang.json.Expressions;

import java.util.List;

public class ToJexlStringVisitor implements Visitor {

  private final StringBuilder buffer;

  public ToJexlStringVisitor(StringBuilder buffer) {
    this.buffer = buffer;
  }

  public ToJexlStringVisitor() {
    this(new StringBuilder());
  }

  @Override
  public String toString() {
    return buffer.toString().trim();
  }

  public void reset() {
    buffer.setLength(0);
  }

  @Override
  public void accept(BooleanExpression expr) {
    buffer.append(" (");
    String joiner = "";
    switch (expr.getOperation()) {
      case AND:
        joiner = " && ";
        break;
      case OR:
        joiner = " || ";
        break;
    }
    List<Expression> subExprs = expr.getSubExpressions();
    for (int i = 0; i < subExprs.size(); ++i) {
      subExprs.get(i).accept(this);
      if (i != subExprs.size() - 1) {
        buffer.append(joiner);
      }
    }
    buffer.append(")");
  }

  public static void substituteOperator(StringBuilder buffer, String column, String type, String value, String opStr) {
    buffer.append(column.toUpperCase())
          .append(" ").append(opStr).append(" ");

     if (type == null || "string".equalsIgnoreCase(type)) {
      buffer.append('"').append(value).append('"');
     } else {
       buffer.append(value);
     }
  }

  @Override
  public void accept(EqExpression expr) {
    substituteOperator(buffer, expr.getColumn(), expr.getType(), expr.getValue(), "==");
  }

  @Override
  public void accept(GreaterThanExpression expr) {
    substituteOperator(buffer, expr.getColumn(), null, expr.getValue(), ">");
  }

  @Override
  public void accept(GreaterThanEqExpression expr) {
    substituteOperator(buffer, expr.getColumn(), null, expr.getValue(), ">=");
  }

  @Override
  public void accept(LessThanExpression expr) {
    substituteOperator(buffer, expr.getColumn(), null, expr.getValue(), "<");
  }

  @Override
  public void accept(LessThanEqExpression expr) {
    substituteOperator(buffer, expr.getColumn(), null, expr.getValue(), "<=");
  }

  public static void main(String[] args) {
    ToJexlStringVisitor vis = new ToJexlStringVisitor();
    Expression expr = Expressions.parse("{'$lt':{'column': 'name', 'value': 'bill', type: 'string'}}");
    expr.accept(vis);
    System.out.println(vis.buffer.toString().trim());
  }
}
