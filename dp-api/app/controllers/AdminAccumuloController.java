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

import static helpers.RulesOfUseHelper.ADMIN_CAPABILITY;

import actions.AccumuloUserContext;
import actions.Authenticated;
import actions.RequireCapability;
import com.dataprofiler.util.Context;
import com.typesafe.config.Config;
import helpers.AccumuloHelper;
import helpers.RulesOfUseHelper;
import helpers.RulesOfUseHelper.UserAttributes;
import javax.inject.Inject;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;

@Authenticated
@AccumuloUserContext
@RequireCapability(value = ADMIN_CAPABILITY)
public class AdminAccumuloController extends Controller {
  private AccumuloHelper accumulo;
  private Config config;

  @Inject
  public AdminAccumuloController(Config config) {
    this.accumulo = new AccumuloHelper();
  }

  public Result apiKeys(Request req) {
    UserAttributes curUserAttrs =
        (UserAttributes) req.attrs().get(RulesOfUseHelper.ROU_ATTRIBUTES_TYPED_KEY);
    Boolean isAdmin = curUserAttrs.hasAdminCapability();
    if (!isAdmin) {
      return forbidden(Json.toJson("You are not an administrator, and cannot manage API Keys"));
    }
    Context rootContext = (Context) req.attrs().get(RulesOfUseHelper.ROOT_CONTEXT_TYPED_KEY);

    return ok(Json.toJson(accumulo.apiKeys(rootContext)));
  }

  public Result newApiKey(Request req, String username) {
    UserAttributes curUserAttrs =
        (UserAttributes) req.attrs().get(RulesOfUseHelper.ROU_ATTRIBUTES_TYPED_KEY);
    Boolean isAdmin = curUserAttrs.hasAdminCapability();
    if (!isAdmin) {
      return forbidden(Json.toJson("You are not an administrator, and cannot manage API Keys"));
    }
    Context rootContext = (Context) req.attrs().get(RulesOfUseHelper.ROOT_CONTEXT_TYPED_KEY);
    return ok(Json.toJson(accumulo.newApiKey(rootContext, username)));
  }

  public Result deleteApiKey(Request req, String token) {
    UserAttributes curUserAttrs =
        (UserAttributes) req.attrs().get(RulesOfUseHelper.ROU_ATTRIBUTES_TYPED_KEY);
    Boolean isAdmin = curUserAttrs.hasAdminCapability();
    if (!isAdmin) {
      return forbidden(Json.toJson("You are not an administrator, and cannot manage API Keys"));
    }
    Context rootContext = (Context) req.attrs().get(RulesOfUseHelper.ROOT_CONTEXT_TYPED_KEY);
    return ok(Json.toJson(accumulo.deleteApiKey(rootContext, token)));
  }

  public Result deleteColumnEntity(Request req, String id) {
    Context rootContext = (Context) req.attrs().get(RulesOfUseHelper.ROOT_CONTEXT_TYPED_KEY);

    return ok(Json.toJson(accumulo.deleteColumnEntity(rootContext, id)));
  }
}
