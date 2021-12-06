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
package objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.apache.commons.codec.binary.Base64;

import javax.inject.Inject;

public class OAuthConfig {
  public static final ObjectMapper mapper = new ObjectMapper();
  private static final String AUTH_CALLBACK_ROUTE = "/authcallback";
  public String baseOAuthRequestUrl;
  public String oAuthCallbackUrl;
  public String consumerId;
  private String consumerSecret;

  @Inject
  public OAuthConfig(
      String baseOAuthRequestUrl,
      String consumerId,
      String consumerSecret,
      String oAuthCallbackUrl) {
    this.baseOAuthRequestUrl = baseOAuthRequestUrl;
    this.consumerId = consumerId;
    this.consumerSecret = consumerSecret;
    this.oAuthCallbackUrl = oAuthCallbackUrl + AUTH_CALLBACK_ROUTE;
  }

  private String bearerHeader() {
    String authHeader = this.consumerId + ":" + this.consumerSecret;
    String encodedAuthHeader = new String(Base64.encodeBase64(authHeader.getBytes()));
    return "Basic " + encodedAuthHeader;
  }

  public JsonNode tokenRequest(String code) {
    String reqUrl = this.baseOAuthRequestUrl + "/token";
    String authString = bearerHeader();
    try {
      HttpResponse<com.mashape.unirest.http.JsonNode> jsonResponse =
          Unirest.post(reqUrl)
              .header("Accept", "application/json")
              .header("Content-Type", "application/x-www-form-urlencoded")
              .header("Authorization", authString)
              .field("grant_type", "authorization_code")
              .field("code", code)
              .field("redirect_uri", this.oAuthCallbackUrl)
              .asJson();
      String responseJSONString = jsonResponse.getBody().toString();
      JsonNode node = mapper.readValue(responseJSONString, JsonNode.class);
      return node;
    } catch (Exception e) {
      return NullNode.getInstance();
    }
  }
}
