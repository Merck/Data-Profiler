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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import helpers.EmailNotifier;
import helpers.RulesOfUseHelper;
import helpers.RulesOfUseHelper.UserAttributes;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;

import javax.inject.Inject;

@Authenticated
public class JIRAMailController extends Controller {

  private static ObjectMapper mapper = new ObjectMapper();
  private static final String[] mailTo = new String[] {""};
  private String environment;

  @Inject
  public JIRAMailController(Config config) {
    this.environment = config.getString("cluster.name");
  }

  @BodyParser.Of(BodyParser.Json.class)
  public Result send(Request req) {
    UserAttributes currentUserAttributes =
        (UserAttributes) req.attrs().get(RulesOfUseHelper.ROU_ATTRIBUTES_TYPED_KEY);
    String username = currentUserAttributes.getUsername();
    JsonNode reqBody = req.body().asJson();
    String subject = reqBody.get("subject").asText();
    JsonNode contents =
        (JsonNode)
            ((ObjectNode) reqBody.get("contents"))
                .put("submitterID", username)
                .put("environment", environment);
    try {
      Object json = mapper.readValue(contents.toString(), Object.class);
      String mailbody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
      EmailNotifier.sendEmailFromArbitrary(
          mailTo, "", subject, mailbody);
      return ok(Json.toJson("sent"));
    } catch (Exception e) {
      e.printStackTrace();
      return internalServerError();
    }
  }
}
