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
package controllers.accumulo;

import actions.AccumuloUserContext;
import actions.Authenticated;
import com.fasterxml.jackson.databind.JsonNode;
import com.dataprofiler.util.Context;
import com.typesafe.config.Config;
import helpers.AccumuloHelper;
import helpers.RulesOfUseHelper;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import objects.ColumnCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.cache.NamedCache;
import play.cache.SyncCacheApi;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;

@Authenticated
@AccumuloUserContext
public class AccumuloControllerPickerColSearch extends Controller {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Config config;
  private final AccumuloHelper accumulo;
  private final SyncCacheApi cache;

  @Inject
  public AccumuloControllerPickerColSearch(
      Config config, @NamedCache("colpicker-cache") SyncCacheApi cache) {
    this.config = config;
    this.accumulo = new AccumuloHelper();
    this.cache = cache;
  }

  @BodyParser.Of(play.mvc.BodyParser.Json.class)
  public Result pickerColumnSearch(Request req) throws Exception {
    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    JsonNode reqBody = req.body().asJson();
    String dataset = reqBody.has("dataset") ? reqBody.get("dataset").asText() : null;
    String sessionId =
        reqBody.has("sessionId") ? reqBody.get("sessionId").asText() : UUID.randomUUID().toString();
    Integer limit = reqBody.get("limit").asInt(100);
    String search =
        reqBody.has("search")
            ? (reqBody.get("search").asText().equals("") ? null : reqBody.get("search").asText())
            : null;
    Boolean sortAsc =
        reqBody.has("sortDirection") && (reqBody.get("sortDirection").asText().equals("asc"));
    Boolean sortName =
        reqBody.has("sortOrder") && (reqBody.get("sortOrder").asText().equals("name"));
    List<ColumnCollection.ColumnInfo> ret =
        accumulo.pickerColumnSearch(
            context,
            context.getCurrentMetadataVersion(),
            sessionId,
            cache,
            dataset,
            limit,
            search,
            sortAsc,
            sortName);
    return ok(Json.toJson(ret));
  }
}
