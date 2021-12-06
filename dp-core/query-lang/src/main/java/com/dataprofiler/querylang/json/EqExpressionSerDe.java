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

import com.dataprofiler.querylang.expr.EqExpression;
import com.google.gson.*;

import java.lang.reflect.Type;

public class EqExpressionSerDe implements JsonSerializer<EqExpression>, JsonDeserializer<EqExpression> {

  @Override
  public JsonElement serialize(EqExpression eqExpression, Type type,
                               JsonSerializationContext jsonSerializationContext) {
    JsonObject expr = new JsonObject();
    expr.addProperty("column", eqExpression.getColumn());
    expr.addProperty("value", eqExpression.getValue());
    expr.addProperty("type", eqExpression.getType());
    JsonObject wrapper = new JsonObject();
    wrapper.add("$eq", expr);
    return wrapper;
  }

  @Override
  public EqExpression deserialize(JsonElement jsonElement, Type type,
      JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
    JsonObject inner = jsonElement.getAsJsonObject().getAsJsonObject("$eq");
    return new EqExpression(inner.get("column").getAsString(),
        inner.get("value").getAsString(),
        inner.get("type") == null ? null : inner.get("type").getAsString());
  }
}
