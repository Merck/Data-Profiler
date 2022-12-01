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
package objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import javax.inject.Inject;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

public class OAuthConfig {

  private final Logger logger = Logger.getLogger(OAuthConfig.class);

  public static final ObjectMapper mapper = new ObjectMapper();
  private static final String AUTH_CALLBACK_ROUTE = "/authcallback";
  public String authCallbackUrl;
  public String clientId;
  public String scope;
  private String clientSecret;
  public String authorizationEndpoint;
  public String tokenEndpoint;
  public String userInfoEndpoint;

  @Inject
  public OAuthConfig(
    String authorizationEndpoint,
    String tokenEndpoint,
    String userInfoEndpoint,
    String consumerId,
    String consumerSecret,
    String scope,
    String redirectUri
  ) {
    this.authorizationEndpoint = authorizationEndpoint;
    this.tokenEndpoint = tokenEndpoint;
    this.userInfoEndpoint = userInfoEndpoint;
    this.clientId = consumerId;
    this.clientSecret = consumerSecret;
    this.scope = scope;
    this.authCallbackUrl = redirectUri + AUTH_CALLBACK_ROUTE;
  }

  private String bearerHeader() {
    String authHeader = this.clientId + ":" + this.clientSecret;
    String encodedAuthHeader = new String(
      Base64.encodeBase64(authHeader.getBytes())
    );
    return "Basic " + encodedAuthHeader;
  }

  public JsonNode tokenRequest(String code) {
    String authString = bearerHeader();
    try {
      HttpResponse<com.mashape.unirest.http.JsonNode> jsonResponse = Unirest
        .post(this.tokenEndpoint)
        .header("Accept", "application/json")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header("Authorization", authString)
        .field("grant_type", "authorization_code")
        .field("code", code)
        .field("redirect_uri", this.authCallbackUrl)
        .asJson();
      String responseJSONString = jsonResponse.getBody().toString();
      JsonNode node = mapper.readValue(responseJSONString, JsonNode.class);
      return node;
    } catch (Exception e) {
      return NullNode.getInstance();
    }
  }
}
