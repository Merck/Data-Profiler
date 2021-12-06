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
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import com.typesafe.config.Config;
import helpers.AccumuloHelper;
import helpers.RulesOfUseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.Iterator;
import java.util.Map.Entry;

import static helpers.AccumuloHelper.decodeForwardSlash;

@Authenticated
@AccumuloUserContext
public class AccumuloControllerDatasetProp extends Controller {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Config config;
  private final AccumuloHelper accumulo;

  @Inject
  public AccumuloControllerDatasetProp(Config config) throws IOException {
    this.config = config;
    this.accumulo = new AccumuloHelper();
  }

  private void addPropertiesToMetadata(Context context, VersionedMetadataObject m, JsonNode json)
    throws IOException, BasicAccumuloException {
    Map<String, String> properties = m.getProperties();
    Iterator<Entry<String, JsonNode>> nodes = json.fields();
    while (nodes.hasNext()) {
      Entry<String, JsonNode> j = nodes.next();
      properties.put(j.getKey(), j.getValue().asText());
    }

    m.setProperties(properties);
    m.put(context);
  }

  private VersionedMetadataObject getDatasetMetadata(Context context, String dataset) {
    return new VersionedMetadataObject()
      .fetchDataset(context, context.getCurrentMetadataVersion(), dataset);
  }

  private VersionedMetadataObject getTableMetadata(Context context, String dataset, String table) {
    return new VersionedMetadataObject()
        .fetchTable(context, context.getCurrentMetadataVersion(), dataset, table);
  }

  public Result getDatasetProperties(Request req, String dataset) {
    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    dataset = decodeForwardSlash(dataset);
    
    VersionedMetadataObject m = getDatasetMetadata(context, dataset);
    
    if (m == null) {
      return badRequest("Could not find dataset");
    }
    return ok(Json.toJson(m.getProperties()));
  }

  private VersionedMetadataObject getColumnMetadata(
      Context context, String dataset, String table, String column) {
    return new VersionedMetadataObject()
      .fetchColumn(context, context.getCurrentMetadataVersion(), dataset, table, column);
  }
    
  @BodyParser.Of(play.mvc.BodyParser.Json.class)
  public Result setDatasetProperties(Request req, String dataset)
      throws IOException, BasicAccumuloException {
    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    dataset = decodeForwardSlash(dataset);
    
    JsonNode json = req.body().asJson();
    if (json == null) {
      return badRequest("Body is required.");
    }
    
    VersionedMetadataObject m = getDatasetMetadata(context, dataset);
    
    if (m == null) {
      return noContent();
    }
    
    addPropertiesToMetadata(context, m, json);
    return ok();
  }

  public Result getTableProperties(Request req, String dataset, String table) {
    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    dataset = decodeForwardSlash(dataset);
    table = decodeForwardSlash(table);
    
    VersionedMetadataObject m = getTableMetadata(context, dataset, table);
    
    if (m == null) {
      return badRequest("Could not find table");
    }
    return ok(Json.toJson(m.getProperties()));
  }
    
  @BodyParser.Of(play.mvc.BodyParser.Json.class)
  public Result setTableProperties(Request req, String dataset, String table)
      throws IOException, BasicAccumuloException {
    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    dataset = decodeForwardSlash(dataset);
    
    JsonNode json = req.body().asJson();
    if (json == null) {
      return badRequest("Body is required.");
    }
    
    VersionedMetadataObject m = getTableMetadata(context, dataset, table);
    
    if (m == null) {
      return noContent();
    }
    
    addPropertiesToMetadata(context, m, json);
    return ok();
  }

  public Result getColumnProperties(Request req, String dataset, String table, String column) {
    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    dataset = decodeForwardSlash(dataset);
    table = decodeForwardSlash(table);
    column = decodeForwardSlash(column);
    
    VersionedMetadataObject m = getColumnMetadata(context, dataset, table, column);
    
    if (m == null) {
      return badRequest("Could not find table");
    }
    return ok(Json.toJson(m.getProperties()));
  }
    
  @BodyParser.Of(play.mvc.BodyParser.Json.class)
  public Result setColumnProperties(Request req, String dataset, String table, String column)
      throws IOException, BasicAccumuloException {
    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);

    JsonNode json = req.body().asJson();
    if (json == null) {
      return badRequest("Body is required.");
    }
    
    VersionedMetadataObject m = getColumnMetadata(context, dataset, table, column);
    
    if (m == null) {
      return noContent();
    }
    
    addPropertiesToMetadata(context, m, json);
    return ok();
  }
}