package com.dataprofiler.slack.model;

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

import static com.dataprofiler.factory.ObjectMapperFactory.initPrettyPrintObjectMapper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlackMessageTest {
  private static final Logger logger = LoggerFactory.getLogger(SlackMessageTest.class);

  SlackMessage slackMessage;

  @Before
  public void setup() {
    slackMessage = SlackMessageStub.buildMessage();
  }

  @After
  public void teardown() {
    slackMessage = null;
  }

  @Test
  public void canary() {
    assertNotNull(slackMessage);
  }

  @Test
  public void testMarshalToJson() throws IOException {
    ObjectMapper objectMapper = initPrettyPrintObjectMapper();
    String json = objectMapper.writeValueAsString(slackMessage);
    if (logger.isDebugEnabled()) {
      logger.debug(slackMessage.toString());
      logger.debug(json);
    }
    assertSlackMessageJson(objectMapper.readTree(json));
  }

  protected void assertSlackMessageJson(JsonNode json) {
    assertNotNull(json);
    assertFalse(json.isNull());
    JsonNode userName = json.path("username");
    assertEquals("slack user name", userName.asText());
    JsonNode icon = json.path("icon_emoji");
    assertEquals(":cup_with_straw:", icon.asText());
    JsonNode channel = json.path("channel");
    assertEquals("dataprofiler channel", channel.asText());
    JsonNode attachments = json.path("attachments");
    assertTrue(attachments.isArray());
    JsonNode attachment0 = attachments.path(0);
    assertEquals("attachment text", attachment0.path("text").asText());
    assertEquals("#00FF00", attachment0.path("color").asText());
    JsonNode fields = attachment0.path("fields");
    assertTrue(fields.isArray());
    JsonNode field0 = fields.get(0);
    assertEquals("Environment", field0.path("title").asText());
    assertEquals("development", field0.path("value").asText());
    assertTrue(field0.path("short").asBoolean());
  }
}
