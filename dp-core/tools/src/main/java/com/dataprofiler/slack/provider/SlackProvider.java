package com.dataprofiler.slack.provider;

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
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;

import com.dataprofiler.UnirestConfig;
import com.dataprofiler.slack.SlackApiException;
import com.dataprofiler.slack.model.SlackMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * send slack messages. requires a webhook to send too
 *
 * @see "https://api.slack.com/methods/chat.postMessage"
 */
public class SlackProvider {
  private static final Logger logger = LoggerFactory.getLogger(SlackProvider.class);
  private static final String SLACK_SYMBOL = "⩩";

  private final String slackWebhook;
  private ObjectMapper objectMapper;

  static {
    UnirestConfig.config();
  }

  public SlackProvider(String slackWebhook) {
    super();
    this.slackWebhook = slackWebhook;
  }

  public boolean sendMessage(final SlackMessage slackMessage) throws SlackApiException {
    if (isNull(slackMessage)) {
      return false;
    }

    if (logger.isTraceEnabled()) {
      logger.trace(
          format("%s sending slack message: %s %s", SLACK_SYMBOL, slackWebhook, slackMessage));
    }

    try (UnirestInstance unirestInstance = Unirest.spawnInstance()) {
      HttpResponse<byte[]> jsonResponse =
          unirestInstance
              .post(slackWebhook)
              .header("Accept", "application/json")
              .header("Content-Type", "application/json")
              .body(objectMapper.writeValueAsString(slackMessage))
              .asBytes();
      String resp = new String(jsonResponse.getBody(), UTF_8);

      if (logger.isTraceEnabled()) {
        logger.trace(format("%s received %s", SLACK_SYMBOL, resp));
      }
      return "ok".equalsIgnoreCase(resp.trim());
    } catch (Exception e) {
      logger.error("Failed to send slack message");
      logger.error(e.getMessage());
      throw new SlackApiException(e);
    }
  }

  public void setObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public String getSlackWebhook() {
    return slackWebhook;
  }
}
