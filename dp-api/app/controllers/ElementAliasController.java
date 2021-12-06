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

import actions.AccumuloUserContext;
import actions.Authenticated;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dataprofiler.util.Context;
import com.typesafe.config.Config;
import helpers.AccumuloHelper;
import helpers.RulesOfUseHelper;
import helpers.RulesOfUseHelper.UserAttributes;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;

import javax.inject.Inject;

@Authenticated
@AccumuloUserContext
public class ElementAliasController extends Controller {

  private static ObjectMapper mapper = new ObjectMapper();
  private AccumuloHelper accumulo;

  @Inject
  public ElementAliasController(Config config) {
    this.accumulo = new AccumuloHelper();
  }

  public Result list(Request req) {
    Context rootContext = (Context) req.attrs().get(RulesOfUseHelper.ROOT_CONTEXT_TYPED_KEY);
    return ok(Json.toJson(accumulo.columnAliases(rootContext)));
  }

  public Result delete(Request req, String dataset, String table, String column) {
    Context rootContext = (Context) req.attrs().get(RulesOfUseHelper.ROOT_CONTEXT_TYPED_KEY);
    return ok(Json.toJson(accumulo.deleteColumnAlias(rootContext, dataset, table, column)));
  }

  @BodyParser.Of(play.mvc.BodyParser.Json.class)
  public Result create(Request req) {
    Context rootContext = (Context) req.attrs().get(RulesOfUseHelper.ROOT_CONTEXT_TYPED_KEY);
    UserAttributes curUserAttrs =
        (UserAttributes) req.attrs().get(RulesOfUseHelper.ROU_ATTRIBUTES_TYPED_KEY);
    String createdBy = curUserAttrs.getUsername();
    JsonNode reqBody = req.body().asJson();
    String dataset = reqBody.get("dataset").asText();
    String table = reqBody.get("table").asText();
    String column = reqBody.get("column").asText();
    String alias = reqBody.get("alias").asText();

    if (column.equals(alias) || alias.length() == 0 || alias == null) {
      return delete(req, dataset, table, column);
    }

    try {
      return ok(
          Json.toJson(
              accumulo.newColumnAlias(rootContext, dataset, table, column, alias, createdBy)));
    } catch (Exception e) {
      return internalServerError(Json.toJson("Cannot create custom column name"));
    }
  }
}
