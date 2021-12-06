package com.dataprofiler.querylang.json;

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
import com.dataprofiler.querylang.expr.Expression;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class BooleanExpressionSerDe implements JsonSerializer<BooleanExpression>,
        JsonDeserializer<BooleanExpression> {

  @Override
  public JsonElement serialize(BooleanExpression booleanExpression, Type type,
                               JsonSerializationContext jsonSerializationContext) {
    JsonObject root = new JsonObject();
    JsonArray subExpressions = new JsonArray();
    booleanExpression.getSubExpressions().forEach(expr -> {
      subExpressions.add(jsonSerializationContext.serialize(expr));
    });
    root.add("$" + booleanExpression.getOperation().name().toLowerCase(), subExpressions);
    return root;
  }

  @Override
  public BooleanExpression deserialize(JsonElement jsonElement, Type type,
      JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
    BooleanExpression boolExpr = new BooleanExpression();
    JsonArray jsonSubExprs;
    JsonObject rootExpr = jsonElement.getAsJsonObject();
    if (rootExpr.has("$and")) {
      boolExpr.setOperation(BooleanExpression.Operation.AND);
      jsonSubExprs = rootExpr.getAsJsonArray("$and");
    } else if (rootExpr.has("$or")) {
      boolExpr.setOperation(BooleanExpression.Operation.OR);
      jsonSubExprs = rootExpr.getAsJsonArray("$or");
    } else {
      return null;
    }

    List<Expression> subExprs = new ArrayList<>(jsonSubExprs.size());
    for (JsonElement element : jsonSubExprs) {
      Expression expr = jsonDeserializationContext.deserialize(element, Expression.class);
      if (expr != null) {
        subExprs.add(expr);
      }
    }

    boolExpr.setSubExpressions(subExprs);
    return boolExpr;
  }
}
