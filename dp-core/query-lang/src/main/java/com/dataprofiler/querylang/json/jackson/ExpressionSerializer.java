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
import com.dataprofiler.querylang.expr.ColumnExpression;
import com.dataprofiler.querylang.expr.EqExpression;
import com.dataprofiler.querylang.expr.Expression;
import com.dataprofiler.querylang.expr.GreaterThanEqExpression;
import com.dataprofiler.querylang.expr.GreaterThanExpression;
import com.dataprofiler.querylang.expr.LessThanEqExpression;
import com.dataprofiler.querylang.expr.LessThanExpression;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Map;

public class ExpressionSerializer extends StdSerializer<Expression> {

  public static final Map<Class<? extends ColumnExpression>, String> ID_MAP =
      ImmutableMap.of(LessThanExpression.class, "$lt",
                      LessThanEqExpression.class, "$lte",
                      GreaterThanExpression.class, "$gt",
                      GreaterThanEqExpression.class, "$gte");

  public ExpressionSerializer() {
    super(Expression.class);
  }

  public void serialize(Expression expression, JsonGenerator jsonGenerator,
      SerializerProvider serializerProvider) throws IOException {

      jsonGenerator.writeStartObject();

      if (expression instanceof BooleanExpression) {
        BooleanExpression boolExpr = (BooleanExpression) expression;
        jsonGenerator.writeArrayFieldStart("$" + boolExpr.getOperation().toString().toLowerCase());
        for (Expression subExpr : boolExpr.getSubExpressions()) {
          jsonGenerator.writeObject(subExpr);
        }
        jsonGenerator.writeEndArray();
      } else if (expression instanceof EqExpression) {
        EqExpression eqExpr = (EqExpression) expression;
        jsonGenerator.writeObjectFieldStart("$eq");
        jsonGenerator.writeStringField("column", eqExpr.getColumn());
        jsonGenerator.writeStringField("value", eqExpr.getValue());
        jsonGenerator.writeStringField("type", eqExpr.getType());
        jsonGenerator.writeEndObject();
      } else if (expression instanceof ColumnExpression && ID_MAP.containsKey(expression.getClass())) {
        ColumnExpression colExpression = (ColumnExpression) expression;
        jsonGenerator.writeObjectFieldStart(ID_MAP.get(colExpression.getClass()));
        jsonGenerator.writeStringField("column", colExpression.getColumn());
        jsonGenerator.writeStringField("value", colExpression.getValue());
        jsonGenerator.writeEndObject();
      }

      jsonGenerator.writeEndObject();
  }
}
