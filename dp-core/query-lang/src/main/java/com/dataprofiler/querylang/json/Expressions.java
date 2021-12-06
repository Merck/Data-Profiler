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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Expressions {
  public static Gson GSON = new GsonBuilder()
      .setPrettyPrinting()
      .registerTypeAdapter(BooleanExpression.class, new BooleanExpressionSerDe())
      .registerTypeAdapter(EqExpression.class, new EqExpressionSerDe())
      .registerTypeAdapter(Expression.class, new ExpressionSerDe())
      .registerTypeAdapter(GreaterThanExpression.class, new GreaterThanSerDe())
      .registerTypeAdapter(LessThanExpression.class, new LessThanSerDe())
      .registerTypeAdapter(GreaterThanEqExpression.class, new GreaterThanEqSerDe())
      .registerTypeAdapter(LessThanEqExpression.class, new LessThanEqSerDe())
      .create();

  public static Expression parse(String json) {
    return GSON.fromJson(json, Expression.class);
  }

  public static String json(Expression expr) {
    return GSON.toJson(expr, Expression.class);
  }
}
