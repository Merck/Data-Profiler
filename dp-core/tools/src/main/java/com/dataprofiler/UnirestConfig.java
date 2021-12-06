package com.dataprofiler;

/*-
 * 
 * dataprofiler-tools
 *
 * Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
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
 * 
 */

import static java.lang.String.format;
import static java.time.Duration.between;
import static java.time.Instant.now;

import java.time.Duration;
import java.time.Instant;
import kong.unirest.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnirestConfig {
  private static final Logger logger = LoggerFactory.getLogger(UnirestConfig.class);

  private static final int DEFAULT_HTTP_CONCURRENCY = 128;

  public static void config() {
    config(DEFAULT_HTTP_CONCURRENCY);
  }

  public static void config(int httpConcurrency) {
    httpConcurrency = Math.max(1, httpConcurrency);
    Unirest.config()
        .followRedirects(true)
        .cacheResponses(false)
        .automaticRetries(true)
        .concurrency(httpConcurrency * 2, httpConcurrency)
        .instrumentWith(
            requestSummary -> {
              Instant start = now();
              return (responseSummary, exception) -> {
                Instant end = now();
                Duration duration = between(start, end);
                String msg =
                    format(
                        "unirest path: %s status: %s time: %s",
                        requestSummary.getRawPath(), responseSummary.getStatus(), duration);
                if (logger.isInfoEnabled()) {
                  logger.info(msg);
                }
              };
            });
  }
}
