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
package helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import objects.OAuthConfig;

import javax.inject.Inject;

public class OAuthHelper extends OAuthConfig {

  @Inject
  public OAuthHelper(
      String baseOAuthRequestUrl,
      String consumerId,
      String consumerSecret,
      String oAuthCallbackUrl) {
    super(baseOAuthRequestUrl, consumerId, consumerSecret, oAuthCallbackUrl);
  }

  public String getUsername(String authToken) {
    try {
      HttpResponse<com.mashape.unirest.http.JsonNode> jsonResponse =
          Unirest.get(this.baseOAuthRequestUrl + "/userinfo")
              .header("Accept", "application/json")
              .header("Authorization", "Bearer " + authToken)
              .header("Content-Type", "application/json")
              .asJson();
      String responseJSONString = jsonResponse.getBody().toString();
      JsonNode node = mapper.readValue(responseJSONString, JsonNode.class);
      if (node.has("id")) {
        return node.get("id").asText();
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }
}
