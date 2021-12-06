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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dataprofiler.JobApiHelper;
import com.dataprofiler.util.Context;
import com.typesafe.config.Config;
import helpers.RulesOfUseHelper;
import helpers.RulesOfUseHelper.UserAttributes;
import play.Logger;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

@Authenticated
@AccumuloUserContext
public class JobController extends Controller {
  public static final String allProperties =
      "id environment type status creatingUser details createdAt updatedAt";
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final int MAX_JOBS = 100000;
  private final String dataLoadingDaemonUrl;
  private final String environment;
  private final JobApiHelper apiHelper;

  @Inject
  public JobController(Config config) {
    this.dataLoadingDaemonUrl = config.getString("jobsApi.url");
    this.environment = config.getString("cluster.name");
    this.apiHelper = new JobApiHelper(this.dataLoadingDaemonUrl);
  }

  /**
   * Performs the equivalent of the JavaScript encodeURI function Java URLEncoder unescaped
   * characters are: A-Z a-z 0-9 . - * _
   *
   * <p>JavaScript encodeURI unescaped characters are: A-Z a-z 0-9 ; , / ? : @ & = + $ - _ . ! ~ * '
   * ( ) #
   *
   * @param s The string to encode
   * @return The encoded string
   * @throws UnsupportedEncodingException
   */
  public static String encodeURI(String s) throws UnsupportedEncodingException {

    return URLEncoder.encode(s, "UTF-8")
        .replaceAll("\\+", "%20")
        .replaceAll("%3B", ";")
        .replaceAll("%2C", ",")
        .replaceAll("%2F", "/")
        .replaceAll("%3F", "?")
        .replaceAll("%3A", ":")
        .replaceAll("%40", "@")
        .replaceAll("%26", "&")
        .replaceAll("%3D", "=")
        .replaceAll("%2B", "+")
        .replaceAll("%24", "\\$")
        .replaceAll("%21", "!")
        .replaceAll("%7E", "~")
        .replaceAll("%27", "'")
        .replaceAll("%28", "(")
        .replaceAll("%29", ")")
        .replaceAll("%23", "#");
  }

  private Result execQuery(String queryString, String resultPath) {
    try {
      JobApiHelper.Result res = apiHelper.execQuery(queryString, resultPath);

      return status(res.statusCode, res.json);
    } catch (Exception e) {
      e.printStackTrace();
      Logger.error("Failed to query jobs api: " + e);
      return internalServerError();
    }
  }

  private Result createJob(Request req, String jobType)
      throws JsonProcessingException, UnsupportedEncodingException {
    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);

    JsonNode json = req.body().asJson();
    if (json == null) {
      return badRequest("Body is required.");
    }

    UserAttributes currentUserAttributes =
        (UserAttributes) req.attrs().get(RulesOfUseHelper.ROU_ATTRIBUTES_TYPED_KEY);

    String details = mapper.writeValueAsString(json);
    details = encodeURI(details);

    String mutation =
        String.format(
            "mutation { createJob( environment: \"%s\", type: \"%s\", creatingUser: \"%s\", details: \"%s\"){ %s }}",
            environment, jobType, currentUserAttributes.username, details, allProperties);

    return execQuery(mutation, "createJob");
  }

  @BodyParser.Of(play.mvc.BodyParser.Json.class)
  public Result createTableLoad(Request req)
      throws JsonProcessingException, UnsupportedEncodingException {
    return createJob(req, "tableLoad");
  }

  @BodyParser.Of(play.mvc.BodyParser.Json.class)
  public Result createCommit(Request req)
      throws JsonProcessingException, UnsupportedEncodingException {
    return createJob(req, "commit");
  }

  @BodyParser.Of(play.mvc.BodyParser.Json.class)
  public Result createDownload(Request req)
      throws JsonProcessingException, UnsupportedEncodingException {
    return createJob(req, "download");
  }

  @BodyParser.Of(play.mvc.BodyParser.Json.class)
  public Result createCancelJob(Request req)
      throws JsonProcessingException, UnsupportedEncodingException {
    return createJob(req, "canceller");
  }

  @BodyParser.Of(play.mvc.BodyParser.Json.class)
  public Result createConnectionEngineJob(Request req) throws JsonProcessingException, UnsupportedEncodingException {
    return createJob(req, "connectionengine");
  }

  public Result getDetails(Request req, String jobId) {
    String queryString = String.format("{job(id: %s) {%s}}", jobId, allProperties);
    return execQuery(queryString, "job");
  }

  public Result getAll(
      Request request, String jobType, String status, String creatinguser, int limit) {
    ArrayList<String> params = new ArrayList<>();

    if (!status.equals("all")) {
      params.add(String.format("status: \"%s\"", status));
    }

    if (!jobType.equals("all")) {
      params.add(String.format("type: \"%s\"", jobType));
    }

    if (!creatinguser.equals("all")) {
      params.add(String.format("creatingUser: \"%s\"", creatinguser));
    }

    params.add(String.format("limit: %d", limit));

    String paramString = String.join(",", params);
    String queryString = String.format("{jobs(%s) { %s }}", paramString, allProperties);
    System.out.println(queryString);
    return execQuery(queryString, "jobs");
  }

  public Result jobsForUser(Request request, String creatingUser, int limit) {
    String queryString =
        String.format(
            "{jobsForUser(user: \"%s\", limit: %d) { %s }}", creatingUser, limit, allProperties);
    return execQuery(queryString, "jobsForUser");
  }
}
