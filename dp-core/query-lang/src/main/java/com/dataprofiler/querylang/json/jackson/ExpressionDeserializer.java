package com.dataprofiler.querylang.json.jackson;

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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExpressionDeserializer extends StdDeserializer<Expression> {
  private static final Logger LOG = LoggerFactory.getLogger(ExpressionDeserializer.class);

  public static final Set<String> LEAF_NODE_OPS = ImmutableSet.of(
      "$eq",
      "$lt",
      "$lte",
      "$gt",
      "$gte"
  );
  public ExpressionDeserializer() {
    super(Expression.class);
  }

  public static Expression route(JsonNode current) {
    if (current.has("$and")) {
      return handleBoolean(BooleanExpression.Operation.AND, current);
    } else if (current.has("$or")) {
      return handleBoolean(BooleanExpression.Operation.OR, current);
    } else {
      final Set<String> availableFields = new HashSet<>();
      Iterators.addAll(availableFields, current.fieldNames());
      SetView<String> foundExpressions = Sets.intersection(LEAF_NODE_OPS, availableFields);

      if (foundExpressions.isEmpty()) {
        LOG.warn("Query did not contain any valid operators. Presented with [{}]", availableFields);
        return null;
      }

      final String operator = foundExpressions.iterator().next();
      if (foundExpressions.size() > 1) {
        LOG.warn("Query contained multiple operators. Choosing [{}] from [{}].",
            operator,
            foundExpressions);
      }
      return handleLeafNode(current, operator);
    }
  }
  public static BooleanExpression handleBoolean(BooleanExpression.Operation op, JsonNode currentNode) {
    BooleanExpression boolExpr = new BooleanExpression();
    boolExpr.setOperation(op);
    JsonNode children = currentNode.get("$" + op.toString().toLowerCase());
    List<Expression> subExprs = new ArrayList<>();
    for (JsonNode child : children) {
      subExprs.add(route(child));
    }
    boolExpr.setSubExpressions(subExprs);
    return boolExpr;
  }

  public static Expression handleLeafNode(JsonNode currentNode, String operator) {
    JsonNode body = currentNode.get(operator);
    final String column = body.get("column").asText();
    final String value = body.get("value").asText();
    switch (operator) {
      case "$eq":
        return new EqExpression(column,
                                value,
                                body.get("type") == null ? null : body.get("type").asText());
      case "$lt":
        return new LessThanExpression(column, value);
      case "$lte":
        return new LessThanEqExpression(column, value);
      case "$gt":
        return new GreaterThanExpression(column, value);
      case "$gte":
        return new GreaterThanEqExpression(column, value);
      default:
        LOG.warn("Unhandled query operator {}", operator);
        return null;
    }
  }

  @Override
  public Expression deserialize(JsonParser jsonParser,
                                DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
    JsonNode root = jsonParser.getCodec().readTree(jsonParser);
    return route(root);
  }
}
