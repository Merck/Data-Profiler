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

import actions.AuthenticationAction;
import actions.RequireCapability;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.typesafe.config.Config;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.With;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import static helpers.RulesOfUseHelper.MAKE_CAPABILITY;
import static play.mvc.Results.internalServerError;
import static play.mvc.Results.ok;

@With(AuthenticationAction.class)
@RequireCapability(value = MAKE_CAPABILITY)
public class MakeController {
  private String dataLoadingDaemonHostname;

  @Inject
  public MakeController(Config config) {
    String dataLoadingDaemonUrl = config.getString("jobsApi.url");
    // Current config has the full path - so parse that out
    List<String> splits = Arrays.asList(dataLoadingDaemonUrl.split("/"));
    assert (splits.size() > 2);
    splits = splits.subList(0, splits.size() - 2);
    dataLoadingDaemonHostname = String.join("/", splits);
  }

  public Result proxy(Request req, String path) {
    HttpResponse<String> response;
    try {
      response = Unirest.get(dataLoadingDaemonHostname + path).asString();
    } catch (UnirestException e) {
      return internalServerError("Could not list plugins");
    }
    return ok(response.getBody()).as("application/json");
  }

  public Result makePlugins(Request req) {
    return proxy(req, "/make/plugins");
  }

  public Result getJsonForm(Request req, String pluginId) {
    return proxy(req, "/make/plugin/" + pluginId + "/json_form");
  }
}
