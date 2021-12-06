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

import com.dataprofiler.querylang.expr.LessThanEqExpression;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

public class LessThanEqSerDe implements JsonSerializer<LessThanEqExpression>,
    JsonDeserializer<LessThanEqExpression> {

  @Override
  public LessThanEqExpression deserialize(JsonElement jsonElement, Type type,
      JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
    JsonObject root = jsonElement.getAsJsonObject();
    JsonObject inner = root.getAsJsonObject("$lte");
    return new LessThanEqExpression(inner.get("column").getAsString(),
        inner.get("value").getAsString());
  }

  @Override
  public JsonElement serialize(LessThanEqExpression rangeExpression, Type type,
      JsonSerializationContext jsonSerializationContext) {
    JsonObject inner = new JsonObject();
    inner.addProperty("column", rangeExpression.getColumn());
    inner.addProperty("value", rangeExpression.getValue());
    JsonObject wrapper = new JsonObject();
    wrapper.add("$lte", inner);
    return wrapper;
  }

}
