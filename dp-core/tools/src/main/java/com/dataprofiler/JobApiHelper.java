package com.dataprofiler;

/*-
 * 
 * dataprofiler-tools
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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.IOException;
import java.util.HashMap;

public class JobApiHelper {
  private final String url;
  private static final ObjectMapper mapper = new ObjectMapper();

  public JobApiHelper(String url) {
    this.url = url;
  }

  public static class Result {
    public JsonNode json;
    public int statusCode;

    public Result(JsonNode json, int statusCode) {
      this.json = json;
      this.statusCode = statusCode;
    }
  }

  public Result execQuery(String queryString, String resultPath)
      throws IOException, UnirestException {
    HashMap<String, String> query = new HashMap<>();
    query.put("query", queryString);
    String body = mapper.writeValueAsString(query);
    HttpResponse<String> jsonResponse =
        Unirest.post(this.url).header("Content-Type", "application/json").body(body).asString();

    JsonNode ret = null;
    String rs = jsonResponse.getBody();
    if (rs != null) {
      ret = mapper.readValue(rs, JsonNode.class);
    }

    JsonNode out;
    if (ret.get("errors") != null) {
      out = ret.get("errors");
    } else {
      out = ret.get("data").get(resultPath);
    }

    return new Result(out, jsonResponse.getStatus());
  }
}
