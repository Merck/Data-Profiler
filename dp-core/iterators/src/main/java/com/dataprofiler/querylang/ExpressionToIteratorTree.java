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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.dataprofiler.iterators.rowData.v2.AndIterator;
import com.dataprofiler.iterators.rowData.v2.IndexIterator;
import com.dataprofiler.iterators.rowData.v2.OrIterator;
import com.dataprofiler.iterators.rowData.v2.TermIterator;
import com.dataprofiler.querylang.expr.BooleanExpression;
import com.dataprofiler.querylang.expr.EqExpression;
import com.dataprofiler.querylang.expr.Expression;
import java.util.ArrayList;
import java.util.List;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;

public class ExpressionToIteratorTree {

  private final SortedKeyValueIterator<Key, Value> readerToClone;
  private final IteratorEnvironment iteratorEnvironment;

  public ExpressionToIteratorTree(
      SortedKeyValueIterator<Key, Value> readerToClone, IteratorEnvironment iteratorEnvironment) {
    this.readerToClone = readerToClone;
    this.iteratorEnvironment = iteratorEnvironment;
  }

  public IndexIterator handle(Expression expr) {
    if (expr instanceof BooleanExpression) {
      BooleanExpression boolExpr = (BooleanExpression) expr;
      switch (boolExpr.getOperation()) {
        case AND:
          return handleAnd(boolExpr);
        case OR:
          return handleOr(boolExpr);
      }
    } else if (expr instanceof EqExpression) {
      return handleEqExpr((EqExpression) expr);
    }
    return null;
  }

  public OrIterator handleOr(BooleanExpression orExpr) {
    List<IndexIterator> children = new ArrayList<>();
    for (Expression subExpr : orExpr.getSubExpressions()) {
      IndexIterator subReader = handle(subExpr);
      if (subReader != null) {
        children.add(subReader);
      }
    }
    return new OrIterator(children);
  }

  public AndIterator handleAnd(BooleanExpression andExpr) {
    List<IndexIterator> children = new ArrayList<>();
    for (Expression subExpr : andExpr.getSubExpressions()) {
      IndexIterator subReader = handle(subExpr);
      if (subReader != null) {
        children.add(subReader);
      }
    }
    return new AndIterator(children);
  }

  public TermIterator handleEqExpr(EqExpression eqExpr) {
    return new TermIterator(
        eqExpr.getColumn().getBytes(UTF_8),
        eqExpr.getValue().getBytes(UTF_8),
        eqExpr.getType() == null ? null : eqExpr.getType().getBytes(UTF_8),
        readerToClone.deepCopy(iteratorEnvironment));
  }
}
