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
import com.dataprofiler.util.objects.VersionedMetadataObject;
import com.dataprofiler.util.objects.ColumnCountObject;
import com.typesafe.config.Config;
import helpers.AccumuloHelper;
import helpers.RulesOfUseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import static java.util.stream.StreamSupport.stream;

import static java.util.stream.Collectors.toList;

@Authenticated
@AccumuloUserContext
public class AccumuloControllerColValues extends Controller {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private static final String QUERY_PARAM_DATA_SET = "dataset";
  private static final String QUERY_PARAM_TABLE = "table";
  private static final String QUERY_PARAM_COLUMN = "column";
  private static final String QUERY_PARAM_VALUE = "value";
  private final Config config;
  private final AccumuloHelper accumulo;

  @Inject
  public AccumuloControllerColValues(Config config) throws IOException {
    this.config = config;
    this.accumulo = new AccumuloHelper();
  }

  public Result colValueSearch(Request req) {
    JsonNode json = req.body().asJson();
    String dataset = json.findPath(QUERY_PARAM_DATA_SET).textValue();
    String table = json.findPath(QUERY_PARAM_TABLE).textValue();
    String column = json.findPath(QUERY_PARAM_COLUMN).textValue();
    String value = json.findPath(QUERY_PARAM_VALUE).textValue();

    if (dataset == null || table == null || column == null || value == null) {
      return badRequest("Missing required parameters");
    }

    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    VersionedMetadataObject colMetadata =
      new VersionedMetadataObject()
        .fetchColumn(context, context.getCurrentMetadataVersion(), dataset, table, column);

    List<String> result =
      stream(
        new ColumnCountObject()
          .findMatchingValues(context, colMetadata, value)
            .spliterator(),
          false)
        .map(x -> x.value)
        .collect(toList());

    return ok(Json.toJson(result));
  }
}