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
package filters;

import akka.util.ByteString;
import com.typesafe.config.Config;
import helpers.URLEncodingHelper;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Result;
import kong.unirest.Unirest;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.Executor;

public class StatsFilter extends EssentialFilter {

  private final Executor executor;
  private String siteId;
  private String thisUrl;
  private final String AUTHENTICATED_VIA_HEADER = "X-Authenticated-Via";
  private final String USERNAME_HEADER = "X-Username";
  private final String ANALYTICS = "https://admin.dataprofiler.com/analytics/matomo.php";

  @Inject
  public StatsFilter(Executor executor, Config config) {
    super();
    this.executor = executor;
    this.siteId = config.getString("analytics.siteId");
    this.thisUrl = config.getString("thisPlayFramework.url");
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        request -> {
          long startTime = System.currentTimeMillis();
          Accumulator<ByteString, Result> accumulator = next.apply(request);
          return accumulator.map(
              result -> {
                long endTime = System.currentTimeMillis();
                long requestTime = endTime - startTime;
                String cleanPath =
                    (request.uri().startsWith("/") ? request.uri().substring(1) : request.uri())
                        .toLowerCase()
                        .replace("/", "~");
                if (!cleanPath.startsWith("health") && !cleanPath.contains("accumulo_syntax")) {
                  try {
                    Unirest.post(ANALYTICS)
                      .queryString("idsite", this.siteId)
                      .queryString("url", thisUrl + request.uri())
                      .queryString("rec", 1)
                      .queryString("apiv",1)
                      .queryString("_id",result.header(USERNAME_HEADER).isPresent() ? result.header(USERNAME_HEADER).get() : "unauthenticated")
                      .queryString("uid",result.header(USERNAME_HEADER).isPresent() ? result.header(USERNAME_HEADER).get() : "unauthenticated")
                      .queryString("gt_ms", requestTime) // deprecated in matomo v4
                      .queryString("pf_srv", requestTime)
                      .queryString("action_name", "api_hit")
                      .asEmpty();
                  } catch (Exception e) {
                    System.err.println(e);
                  }
                }
                return result;
              },
              executor);
        });
  }
}
