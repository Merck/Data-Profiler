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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import org.apache.commons.lang.RandomStringUtils;

import javax.inject.Inject;

public class InfoController extends Controller {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String AUTH_CALLBACK_ROUTE = "/authcallback";
  private Config config;

  @Inject
  public InfoController(Config config) {
    this.config = config;
  }

  public Result notFound(Request req, String ignored) {
    return notFound();
  }

  public Result info(Request req) {
    ObjectNode ret = mapper.createObjectNode();
    Boolean isAlt = false;
    String altParam = req.getQueryString("alt");
    if (altParam != null && altParam.equals("true")) {
      isAlt = true;
      ret.put("altAuth", true);
    }

    ret.put("authenticationType", this.config.getString("auth.method"));
    ret.put("clusterName", this.config.getString("cluster.name"));
    ret.put("oAuthCallbackUrl", req.getQueryString("uiPath") + AUTH_CALLBACK_ROUTE);

    ret.put(
        "requireLoginAttributeForAccess",
        Boolean.valueOf(config.getString("auth.requireLoginAttributeForAccess")));

    String oAuthState = this.config.getString("auth.serverState");
    if (oAuthState != null && !oAuthState.isEmpty()) {
      ret.put("oAuthState", oAuthState);
    }

    String oAuthServer = this.config.getString("oAuthServer");
    String altOAuthServer = this.config.getString("oAuthServerAlt");
    if (oAuthServer != null && !oAuthServer.isEmpty()) {
      ret.put("oAuthServer", isAlt ? altOAuthServer : oAuthServer);
    }

    String oAuthConsumerId = this.config.getString("oAuthConsumerId");
    String altOAuthConsumerId = this.config.getString("oAuthConsumerIdAlt");
    if (oAuthConsumerId != null && !oAuthConsumerId.isEmpty()) {
      ret.put("oAuthConsumerId", isAlt ? altOAuthConsumerId : oAuthConsumerId);
    }
    return ok(Json.toJson(ret));
  }

  public Result random(Request req) {
    ObjectNode ret = mapper.createObjectNode();
    ret.put("random", RandomStringUtils.randomAlphanumeric(20).toLowerCase());
    return ok(Json.toJson(ret));
  }

}
