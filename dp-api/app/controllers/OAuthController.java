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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import helpers.EmailNotifier;
import helpers.OAuthHelper;
import helpers.RulesOfUseHelper;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;

import javax.inject.Inject;

public class OAuthController extends Controller {

  private Config config;
  private RulesOfUseHelper rulesOfUseHelper;
  private String clusterName;

  @Inject
  public OAuthController(Config config) {
    this.config = config;
    this.clusterName = config.getString("cluster.name");
    this.rulesOfUseHelper =
        new RulesOfUseHelper(
            config.getString("rulesOfUse.baseApi"), config.getString("rulesOfUse.apiKey"));
  }

  @BodyParser.Of(BodyParser.Json.class)
  public Result auth(Request req) {
    JsonNode json = req.body().asJson();
    String code = json.findPath("code").textValue();
    String state = json.findPath("state").textValue();
    String uiPath = json.findPath("uiPath").textValue();
    String altString = json.findPath("alt").textValue();
    Boolean alt = altString != null && altString.equals("true");

    // Check to ensure we're not getting MITM'ed
    if (!state.equals(this.config.getString("auth.serverState"))) {
      return forbidden();
    }

    OAuthHelper oAuthHelper;
    if (alt == true) {
      oAuthHelper = new OAuthHelper(this.config.getString("oAuthServerAlt"), this.config.getString("oAuthConsumerIdAlt"), this.config.getString("oAuthConsumerSecretAlt"), uiPath);
    } else {
      oAuthHelper = new OAuthHelper(this.config.getString("oAuthServer"), this.config.getString("oAuthConsumerId"), this.config.getString("oAuthConsumerSecret"), uiPath);
    }

    ObjectNode ret = (ObjectNode) oAuthHelper.tokenRequest(code);
    if (ret != null && ret.has("access_token")) {
      String token = ret.get("access_token").asText();
      String username = oAuthHelper.getUsername(token);
      if (rulesOfUseHelper.storeLoginInRou(username, token) == true) {
        ret.put("username", username);
        EmailNotifier.sendLoginNotification(username, this.clusterName);
        return ok(Json.toJson(ret));
      } else {
        return forbidden("Cannot store positive authentication in Rules of Use");
      }
    } else {
      return unauthorized();
    }
  }
}
