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
import com.dataprofiler.util.Context;
import com.typesafe.config.Config;
import helpers.AccumuloHelper;
import helpers.MetadataVersionHistoryHelper;
import org.apache.log4j.Logger;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;

import static helpers.RulesOfUseHelper.USER_CONTEXT_TYPED_KEY;
import static play.libs.Json.toJson;

@Authenticated
@AccumuloUserContext
public class MetadataVersionHistoryController extends Controller {
  private static final Logger logger = Logger.getLogger(MetadataVersionHistoryController.class);
  private final Config config;
  private final MetadataVersionHistoryHelper helper;

  @Inject
  public MetadataVersionHistoryController(Config config) {
    this.config = config;
    this.helper = new MetadataVersionHistoryHelper();
  }

  public CompletableFuture<Result> metadataVersionHistory(Request req, Integer max) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            Context context = getRequestContext(req);
            return ok(toJson(helper.fetchAllMetadataVersionHistory(context, max)));
          } catch (Exception e) {
            e.printStackTrace();
            return internalServerError(toJson(e.getMessage()));
          }
        });
  }

  public CompletableFuture<Result> metadataVersionHistoryByDataset(
      Request req, String dataset, Integer max) {

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            Context context = getRequestContext(req);
            return ok(
                toJson(
                    helper.fetchDatasetMetadataVersionHistory(
                        context, AccumuloHelper.decodeForwardSlash(dataset), max)));
          } catch (Exception e) {
            e.printStackTrace();
            return internalServerError(toJson(e.getMessage()));
          }
        });
  }

  private Context getRequestContext(Request req) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    return context;
  }
}
