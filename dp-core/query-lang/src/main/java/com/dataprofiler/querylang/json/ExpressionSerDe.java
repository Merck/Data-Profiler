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
import com.dataprofiler.querylang.expr.EqExpression;
import com.dataprofiler.querylang.expr.Expression;
import com.dataprofiler.querylang.expr.GreaterThanEqExpression;
import com.dataprofiler.querylang.expr.GreaterThanExpression;
import com.dataprofiler.querylang.expr.LessThanEqExpression;
import com.dataprofiler.querylang.expr.LessThanExpression;
import com.google.gson.*;

import java.lang.reflect.Type;

public class ExpressionSerDe implements JsonSerializer<Expression>, JsonDeserializer<Expression> {

  @Override
  public Expression deserialize(JsonElement jsonElement, Type type,
                                JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
    JsonObject expr = jsonElement.getAsJsonObject();
    if (expr.has("$and") || expr.has("$or")) {
      return jsonDeserializationContext.deserialize(expr, BooleanExpression.class);
    } else if (expr.has("$eq")) {
      return jsonDeserializationContext.deserialize(expr, EqExpression.class);
    } else if (expr.has("$gt")) {
      return jsonDeserializationContext.deserialize(expr, GreaterThanExpression.class);
    } else if (expr.has("$gte")) {
      return jsonDeserializationContext.deserialize(expr, GreaterThanEqExpression.class);
    } else if (expr.has("$lt")) {
      return jsonDeserializationContext.deserialize(expr, LessThanExpression.class);
    } else if (expr.has("$lte")) {
      return jsonDeserializationContext.deserialize(expr, LessThanEqExpression.class);
    } else {
      return null;
    }
  }

  @Override
  public JsonElement serialize(Expression expression, Type type,
      JsonSerializationContext jsonSerializationContext) {
    if (expression instanceof BooleanExpression) {
      return jsonSerializationContext.serialize(expression, BooleanExpression.class);
    } else if (expression instanceof EqExpression) {
      return jsonSerializationContext.serialize(expression, EqExpression.class);
    } else if (expression instanceof GreaterThanExpression) {
      return jsonSerializationContext.serialize(expression, GreaterThanExpression.class);
    } else if (expression instanceof GreaterThanEqExpression) {
      return jsonSerializationContext.serialize(expression, GreaterThanEqExpression.class);
    } else if (expression instanceof LessThanExpression) {
      return jsonSerializationContext.serialize(expression, LessThanExpression.class);
    } else if (expression instanceof LessThanEqExpression) {
      return jsonSerializationContext.serialize(expression, LessThanEqExpression.class);
    } else {
      return null;
    }
  }
}
