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
import play.Logger;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Date;

public class LoggingFilter extends EssentialFilter {

  private final Executor executor;
  private final String AUTHENTICATED_VIA_HEADER = "X-Authenticated-Via";
  private final String USERNAME_HEADER = "X-Username";
  private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");


  @Inject
  public LoggingFilter(Executor executor) {
    super();
    this.executor = executor;
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        request -> {
          long startTime = System.currentTimeMillis();
          Accumulator<ByteString, Result> accumulator = next.apply(request);
          return accumulator.map(
              result -> {
                String uri = request.uri();
                if (uri.startsWith("/jobs") || uri.startsWith("/accumulo_syntax")) {
                  return result;
                }
                long endTime = System.currentTimeMillis();
                long requestTime = endTime - startTime;
                String timestamp = dateFormat.format(new Date());
                Optional<String> usernameHeaderVal = result.header(USERNAME_HEADER);
                if (usernameHeaderVal.isPresent()) {
                  String username = result.header(USERNAME_HEADER).get();
                  String authenticatedVia = result.header(AUTHENTICATED_VIA_HEADER).get();
                  Logger.info(
                      "{} {} {} {} ({} via {} - {}ms)",
                      timestamp,
                      result.status(),
                      request.method(),
                      uri,
                      username,
                      authenticatedVia,
                      requestTime);
                } else {
                  Logger.info(
                      "{} {} {} {} ({}ms)",
                      timestamp,
                      result.status(),
                      request.method(),
                      uri,
                      requestTime);
                }
                return result;
              },
              executor);
        });
  }
}
