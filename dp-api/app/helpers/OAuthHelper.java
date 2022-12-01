/**
 * Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 **/
package helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import javax.inject.Inject;
import objects.OAuthConfig;
import org.apache.log4j.Logger;

public class OAuthHelper extends OAuthConfig {

  private final Logger logger = Logger.getLogger(OAuthHelper.class);

  @Inject
  public OAuthHelper(
    String authorizationEndpoint,
    String tokenEndpoint,
    String userInfoEndpoint,
    String consumerId,
    String consumerSecret,
    String scope,
    String redirectUri
  ) {
    super(
      authorizationEndpoint,
      tokenEndpoint,
      userInfoEndpoint,
      consumerId,
      consumerSecret,
      scope,
      redirectUri
    );
  }

  public String getUsername(String authToken) {
    try {
      logger.info("Querying for user info");
      logger.info(this.userInfoEndpoint);
      logger.info("Auth Token: " + authToken);
      HttpResponse<com.mashape.unirest.http.JsonNode> jsonResponse = Unirest
        .get(this.userInfoEndpoint)
        .header("Accept", "application/json")
        .header("Authorization", "Bearer " + authToken)
        .header("Content-Type", "application/json")
        .asJson();
      String responseJSONString = jsonResponse.getBody().toString();
      logger.info(responseJSONString);

      JsonNode node = mapper.readValue(responseJSONString, JsonNode.class);
      if (node.has("email")) {
        return node.get("email").asText();
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }
}
