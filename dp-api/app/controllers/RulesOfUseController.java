/**
*  Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
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
**/
package controllers;

import actions.Authenticated;
import actions.RequireCapability;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.dataprofiler.util.BasicAccumuloException;
import com.typesafe.config.Config;
import helpers.RulesOfUseHelper;
import helpers.RulesOfUseHelper.UserAttributes;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;

import javax.inject.Inject;

import static helpers.RulesOfUseHelper.ADMIN_CAPABILITY;

@Authenticated
@RequireCapability(value = ADMIN_CAPABILITY)
public class RulesOfUseController extends Controller {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String GRAPHQL_PATH = "/graphql";
  private Config config;
  private String rulesOfUseApiPath;
  private String rulesOfUseApiKey;

  @Inject
  public RulesOfUseController(Config config) {
    this.config = config;
    this.rulesOfUseApiPath = config.getString("rulesOfUseApi.url");
    this.rulesOfUseApiKey = config.getString("rulesOfUseApi.key");
  }

  @BodyParser.Of(BodyParser.Json.class)
  public Result proxy(Request req) {
    try {
      JsonNode body = req.body().asJson();
      String bodyText = body.get("query").toString().toLowerCase();
      UserAttributes curUserAttrs =
          (UserAttributes) req.attrs().get(RulesOfUseHelper.ROU_ATTRIBUTES_TYPED_KEY);
      HttpResponse<com.mashape.unirest.http.JsonNode> jsonResponse =
          Unirest.post(this.rulesOfUseApiPath + GRAPHQL_PATH)
              .header("Accept", "application/json")
              .header("Authorization", rulesOfUseApiKey)
              .header("X-Username", curUserAttrs.username)
              .header("Content-Type", "application/json")
              .body(mapper.writeValueAsString(body))
              .asJson();
      Integer proxyResponseStatusCode = jsonResponse.getStatus();
      String responseJSONString = jsonResponse.getBody().toString();
      JsonNode ret = null;
      if (responseJSONString != null) {
        ret = mapper.readValue(responseJSONString, JsonNode.class);
      }
      return status(proxyResponseStatusCode, Json.toJson(ret));
    } catch (Exception e) {
      e.printStackTrace(System.out);
      return internalServerError();
    }
  }

  public Result userAuthorizations(Request req, String username) {
    RulesOfUseHelper rouh = new RulesOfUseHelper(rulesOfUseApiPath, rulesOfUseApiKey);
    RulesOfUseHelper.UserAttributes ua = rouh.getUserAttributes(username);
    return ok(ua.currentAsJSONString());
  }

  public Result user(Request req, String username) throws JsonProcessingException {
    RulesOfUseHelper rouh = new RulesOfUseHelper(rulesOfUseApiPath, rulesOfUseApiKey);
    RulesOfUseHelper.UserAttributes ua = rouh.getUserAttributes(username);
    return ok(ua.asJson());
  }

}
