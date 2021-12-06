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
import com.typesafe.config.Config;
import helpers.RulesOfUseHelper;
import helpers.RulesOfUseHelper.UserAttributes;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;

import javax.inject.Inject;

import static java.lang.String.format;

@Authenticated
public class LandingPageController extends Controller {
  private final Config config;
  private final String rulesOfUseApiPath;
  private final String rulesOfUseApiKey;

  @Inject
  public LandingPageController(Config config) {
    this.config = config;
    this.rulesOfUseApiPath = config.getString("rulesOfUseApi.url");
    this.rulesOfUseApiKey = config.getString("rulesOfUseApi.key");
  }

  @BodyParser.Of(BodyParser.Json.class)
  public Result applyFirstLoginBadge(Request req) {
    UserAttributes curUserAttrs =
        (UserAttributes) req.attrs().get(RulesOfUseHelper.ROU_ATTRIBUTES_TYPED_KEY);
    String currentUser = curUserAttrs.getUsername();
    RulesOfUseHelper rouh = new RulesOfUseHelper(rulesOfUseApiPath, rulesOfUseApiKey);
    boolean success = rouh.addFirstLoginBadge(currentUser);
    return ok(statusJson(success));
  }

  public Result resetFirstLoginBadge(Request req) {
    UserAttributes curUserAttrs =
        (UserAttributes) req.attrs().get(RulesOfUseHelper.ROU_ATTRIBUTES_TYPED_KEY);
    String currentUser = curUserAttrs.getUsername();
    RulesOfUseHelper rouh = new RulesOfUseHelper(rulesOfUseApiPath, rulesOfUseApiKey);
    boolean success = rouh.removeFirstLoginBadge(currentUser);
    return ok(statusJson(success));
  }

  public Result currentUser(Request req) throws Exception {
    UserAttributes curUserAttrs =
        (UserAttributes) req.attrs().get(RulesOfUseHelper.ROU_ATTRIBUTES_TYPED_KEY);
    RulesOfUseHelper rouh = new RulesOfUseHelper(rulesOfUseApiPath, rulesOfUseApiKey);
    RulesOfUseHelper.UserAttributes ua = rouh.getUserAttributes(curUserAttrs.getUsername());
    return ok(ua.asJson());
  }

  public String statusJson(boolean status) {
    return format("{ status: %s }", status);
  }
}
