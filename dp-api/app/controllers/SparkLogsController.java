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
import akka.util.ByteString;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.typesafe.config.Config;
import kong.unirest.GetRequest;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.http.HttpEntity;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import scala.Tuple2;

import javax.inject.Inject;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;


@Authenticated
public class SparkLogsController extends Controller {
  private static final Logger LOG = LoggerFactory.getLogger(SparkLogsController.class);

  public static final String allProperties = "id environment type status creatingUser details createdAt updatedAt";
  public static final String workURLFormat = "http://%s:8081/logPage/?driverId=%s&logType=stderr";
  public static final String sparkMasterAppURL = "http://spark-master-production.dataprofiler.com:8080/json/v1/applications";
  public static final String jobsGraphQLFormat = "https://%s-internal.dataprofiler.com/jobs/graphql";

  /**
   * Returns the first JSONObject that whose `id` field mataches
   * the supplied `driverId`.
   *
   * @param driverId
   * @param arrays
   * @return
   */
  public static JSONObject findDriverById(String driverId, JSONArray... arrays) {
    for (JSONArray array : arrays) {
      for (Object thing : array) {
        if (thing instanceof JSONObject) {
          JSONObject obj = (JSONObject) thing;
          if (driverId.equals(obj.getString("id"))) {
            return obj;
          }
        }
      }
    }
    return null;
  }

  public static <T> Optional<T> tryOptional(Callable<T> fn) {
    try {
      return Optional.of(fn.call());
    } catch (Exception e) {
      LOG.warn("Error raised attempting to fetch a value!", e);
      return Optional.empty();
    }
  }

  private final String environment;

  public SparkLogsController(String environment) {
    List<String> envParts = Splitter.on('-').splitToList(environment);
    if (envParts.size() == 1) {
      this.environment = envParts.get(0);
    } else if (envParts.isEmpty()) {
      this.environment = "development"; // yolo
    } else {
      this.environment = envParts.get(envParts.size() - 1);
    }
  }

  @Inject
  public SparkLogsController(Config config) {
    this(config.getString("cluster.name"));
  }

  /*
   * Avoid mixing up com.mashape.unirest and kong by just querying
   * the jobs API directly here w/ kong. This will break for someone once
   * the jobs API changes.
   */
  public JSONObject fetchJobDetails(int jobId) {
    String queryString = format("{job(id: %s) {%s}}", jobId, allProperties);
    HashMap<String, String> query = new HashMap();
    query.put("query", queryString);
    String body = Unirest.primaryInstance().config().getObjectMapper().writeValue(query);
    HttpResponse<JsonNode> jsonResponse = Unirest.post(format(jobsGraphQLFormat, this.environment))
    .header("Content-Type", "application/json")
    .body(body)
    .asJson();

    LOG.error("Job details fetch returned {}: {}", jsonResponse.getStatus(), jsonResponse.getStatusText());
    JsonNode ret = jsonResponse.getBody();
    if (ret == null || ret.getObject().has("errors")) {
      return null;
    } else {
      return ret.getObject().getJSONObject("data").getJSONObject("job");
    }
  }

  public Result fetchLogsFromCluster(Request req, String jobId) throws Exception {
    return Optional.ofNullable(fetchJobDetails(Integer.parseInt(jobId)))

    // Extract the driver id from the job's details
    .map(job -> job.getJSONObject("details").getString("driverId"))

    // Search the active and completed drivers from the spark master for a driver
    // object with a matching ID
    .map(driverId -> {
      HttpResponse < JsonNode > response = Unirest.get(sparkMasterAppURL).asJson();
      JSONObject sparkMasterResponse = response.getBody().getObject();
      JSONArray activedrivers = sparkMasterResponse.getJSONArray("activedrivers");
      JSONArray completedDrivers = sparkMasterResponse.getJSONArray("completeddrivers");
      return findDriverById(driverId, activedrivers, completedDrivers);})

    // Extract the worker IP address from the driver entry and curry along the driver object
    .map(driver -> {
      Pattern ipMatcher = Pattern.compile(".*-(\\d+\\.\\d+\\.\\d+\\.\\d+)-.*");
      return new Tuple2<>(driver, ipMatcher.matcher(driver.getString("worker")));
    })

    // make sure we found an IP address in the worker string
    .filter(tuple -> tuple._2.matches())

    // reverse DNS the IP we found and fetch the stderr log of the driver from it
    .map(tuple -> {
      JSONObject driver = tuple._1;
      Matcher matcher = tuple._2;
      String ipAddress = matcher.group(1);
      String hostName = tryOptional(() -> InetAddress.getByName(ipAddress).getCanonicalHostName()).orElse("");
      String urlPrefix = format(workURLFormat, hostName, driver.getString("id"));
      StringBuilder urlBuilder = new StringBuilder(urlPrefix);
      req.queryString().forEach((k, vs) -> urlBuilder.append("&" + k + "=" + vs[0]));
      GetRequest logFetch = Unirest.get(urlBuilder.toString());
      return logFetch.asString();
    })

    // parse out the log body. we got back HTML and need to pull out what's
    // in between the <pre></pre> tags
    .map(logsResponse -> {
      String logsHTML = logsResponse.getBody();
      Document d = Jsoup.parse(logsHTML);
      Element element = d.selectFirst("pre");
      String logz = element.wholeText();
      return new Result(200,
          new HttpEntity.Strict(ByteString.fromString(logz, Charsets.UTF_8),
      Optional.of("text/plain")));
    })
    .orElseGet(() -> new Result(404));
  }

  public static void main(String[] args) throws Throwable {
    Unirest.config().proxy("localhost", 8080);
    SparkLogsController slc = new SparkLogsController("kube-development");
    LOG.info("{}", slc.fetchJobDetails(29640));
  }
}
