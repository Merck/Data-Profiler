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
package actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.objects.APIToken;
import com.typesafe.config.Config;
import helpers.DataprofilerConfigAdaptor;
import helpers.OAuthHelper;
import helpers.RulesOfUseHelper;
import helpers.RulesOfUseHelper.UserAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AuthenticationAction extends Action<Authenticated> {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final String API_TOKEN_HEADER = "X-Api-Key";
  private final String AUTH_TOKEN_HEADER = "X-Authorization-Token";
  private final String USERNAME_HEADER = "X-Username";
  private final String ASSIGNED_ATTRIBUTES_HEADER = "X-Assigned-Attributes";
  private final String CURRENT_ATTRIBUTES_HEADER = "X-Current-Attributes";
  private final String ATTRIBUTES_TO_REJECT_HEADER = "X-Attributes-To-Reject";
  private final String AUTHENTICATED_VIA_HEADER = "X-Authenticated-Via";
  private final String SPECIAL_ATTRIBUTES_HEADER = "X-Special-Attributes-To-Apply";
  private final String AUTHENTICATED_VIA_STATIC_API_TOKEN = "static-api-token";
  private final String MSG_NOT_ON_WHITELIST = "";
  private final String MSG_UNAUTHORIZED = "Unauthorized";
  private final String LOCAL_DEVELOPER = "local-developer";

  private static final ObjectMapper mapper = new ObjectMapper();

  private Config config;
  private String authMethod;
  private Boolean requireLoginAttributeForAccess;
  private com.dataprofiler.util.Context dpContext;
  private RulesOfUseHelper rulesOfUseHelper;

  private OAuthHelper oAuthHelper;
  private Boolean isUsingApiKey = false;
  private Http.Request req;
  private String username = null;
  private HashMap<String, String> requestHeaders = new HashMap<String, String>();
  private HashMap<String, String> responseHeaders = new HashMap<String, String>();
  private UserAttributes rouAttributes;

  @Inject
  public AuthenticationAction(Config config) {
    this.config = config;
    logger.debug("AuthenticationAction ctor");
  }

  /*
   * This is the main call for every request
   */
  public CompletionStage<Result> call(Http.Request req) {
    return prepareCall(req);
  }

  public CompletionStage<Result> prepareCall(Http.Request req) {
    logger.debug("prepareCall for authenticated action");
    initialize();
    setRequest(req);
    setRequestHeaders();
    doAuthentication();
    return finishAuthentication();
  }

  public void initialize() {
    try {
      dpContext = new com.dataprofiler.util.Context(
          DataprofilerConfigAdaptor.fromPlayConfigruation(config));
    } catch (BasicAccumuloException e) {
      logger.error(e.toString());
    }
    isUsingApiKey = false;
    authMethod = config.getString("auth.method");
    requireLoginAttributeForAccess =
        Boolean.valueOf(config.getString("auth.requireLoginAttributeForAccess"));
    rulesOfUseHelper = new RulesOfUseHelper(config.getString("rulesOfUse.baseApi"),
        config.getString("rulesOfUse.apiKey"));
    String authorizationEndpoint;
    String tokenEndpoint;
    String userInfoEndpoint;
    String oAuthClientId;
    String oAuthClientSecret;
    String callbackUrlBase;
    String oAuthScope;
    switch (authMethod) {
      case "oauth":
        authorizationEndpoint = config.getString("oAuthAuthorizationEndpoint");
        tokenEndpoint = config.getString("oAuthTokenEndpoint");
        userInfoEndpoint = config.getString("oAuthUserInfoEndpoint");
        oAuthClientId = config.getString("oAuthClientId");
        oAuthClientSecret = config.getString("oAuthClientSecret");
        oAuthScope = config.getString("oAuthScope");
        oAuthHelper = new OAuthHelper(authorizationEndpoint,
                                      tokenEndpoint,
                                      userInfoEndpoint,
                                      oAuthClientId, 
                                      oAuthClientSecret, 
                                      oAuthScope, 
                                      null);
        break;
      case LOCAL_DEVELOPER:
        break;
      default:
        break;
    }
  }

  public void setRequestHeaders() {
    requestHeaders.put(API_TOKEN_HEADER, req.header(API_TOKEN_HEADER).orElse(null));
    requestHeaders.put(AUTH_TOKEN_HEADER, req.header(AUTH_TOKEN_HEADER).orElse(null));
    requestHeaders.put(USERNAME_HEADER, req.header(USERNAME_HEADER).orElse(null));
    requestHeaders.put(SPECIAL_ATTRIBUTES_HEADER,
        req.header(SPECIAL_ATTRIBUTES_HEADER).orElse(null));
    requestHeaders.put(ATTRIBUTES_TO_REJECT_HEADER,
        req.header(ATTRIBUTES_TO_REJECT_HEADER).orElse(null));
    requestHeaders.values().removeIf(Objects::isNull);
    logger.debug("set request headers: " + requestHeaders.keySet());
    if (requestHeaders.get(API_TOKEN_HEADER) != null) {
      isUsingApiKey = true;
    }
  }

  public Result setResponseHeaders(Result res) {
    if (username != null) {
      res = res.withHeader(USERNAME_HEADER, username);
    }
    String via = isUsingApiKey ? AUTHENTICATED_VIA_STATIC_API_TOKEN : authMethod;
    if (via != null) {
      res = res.withHeader(AUTHENTICATED_VIA_HEADER, via);
    }
    if (requestHeaders.get(AUTH_TOKEN_HEADER) != null) {
      res = res.withHeader(AUTH_TOKEN_HEADER, requestHeaders.get(AUTH_TOKEN_HEADER));
    }
    res = res.withHeader(ASSIGNED_ATTRIBUTES_HEADER, rouAttributes.allAsJSONString())
        .withHeader(CURRENT_ATTRIBUTES_HEADER, rouAttributes.currentAsJSONString());

    return res;
  }

  public void doAuthentication() {
    String authedUsername;
    logger.debug("doAuthentication using api key: " + isUsingApiKey);
    if (isUsingApiKey == true) {
      logger.debug("using api key for authentication");
      authedUsername = authenticateApiKey();
    } else {
      username = requestHeaders.get(USERNAME_HEADER);
      authedUsername = authenticateWebUser();
      logger.debug("using user name header to authenticate web user: " + authedUsername);
    }
    username = authedUsername;
  }

  public CompletionStage<Result> finishAuthentication() {
    if (username != null) {
      if (username.equals("root")) {
        logger.debug("Getting attributes for root");
        rouAttributes = rulesOfUseHelper.getRootAttibutes(username);
      } else {
        logger.debug("Getting attributes for " + username);
        rouAttributes = rulesOfUseHelper.getUserAttributes(username);
      }
      logger.debug(
          "fetched user: " + username + " attributes: " + rouAttributes.getAllVisibilities());
      rouAttributes.setSpecialVisibilitiesToAdd(requestHeaders.get(SPECIAL_ATTRIBUTES_HEADER));
      rouAttributes.setVisibilitiesToReject(requestHeaders.get(ATTRIBUTES_TO_REJECT_HEADER));
      logger.debug(
          "rouAttributes.setVisibilitiesToReject: " + rouAttributes.getVisibilitiesToReject());
      logger.debug("requireLoginAttributeForAccess: " + requireLoginAttributeForAccess
          + " hasLoginAttribute: " + rouAttributes.hasLoginAttribute());
      if (requireLoginAttributeForAccess && rouAttributes.hasLoginAttribute() == false) {
        logger.debug("user: " + username + " is not on the whitelist, returning forbidden");
        Result forbidden = Results.forbidden(Json.toJson(MSG_NOT_ON_WHITELIST));
        return CompletableFuture.completedFuture(forbidden);
      }

      logger.debug("setting request key: " + RulesOfUseHelper.ROU_ATTRIBUTES_TYPED_KEY
          + " for user: " + username + " attributes: " + rouAttributes.getAllVisibilities());
      req = req.addAttr(RulesOfUseHelper.ROU_ATTRIBUTES_TYPED_KEY, rouAttributes);
      return delegate.call(req).thenApply(result -> setResponseHeaders(result));
    } else {
      logger.debug("finishAuthentication user is null! returning unauthorized");
      Result unauthorized = Results.unauthorized(MSG_UNAUTHORIZED);
      return CompletableFuture.completedFuture(unauthorized);
    }
  }

  private String authenticateApiKey() {
    String token = requestHeaders.get(API_TOKEN_HEADER);
    if (dpContext == null) {
      logger.error("Could not validate API token because there was no connection to Accumulo");
      return null;
    }
    APIToken tok = APIToken.fetchByToken(dpContext, token);
    if (tok == null) {
      logger.error("Invalid API token");
      return null;
    } else {
      return tok.getUsername();
    }
  }

  public void setRequest(Http.Request req) {
    this.req = req;
  }

  private String authenticateWebUser() {
    String token = requestHeaders.get(AUTH_TOKEN_HEADER);
    if (authMethod.equals(LOCAL_DEVELOPER)) {
      logger.debug("auth method is local developer");
      return localDeveloperGetUsername(token);
    } else {
      Boolean positiveTokenResponse =
          rulesOfUseHelper.checkTokenAuthenticatedFromRou(this.username, token);
      logger.debug("authenticateWebUser with user: " + username + " token: " + token
          + " positiveTokenResponse: " + positiveTokenResponse);
      return (positiveTokenResponse ? username : null);
    }
  }

  private String localDeveloperGetUsername(String token) {
    String url = config.getString("thisPlayFramework.url");
    Boolean runningLocally = url.startsWith("http://localhost");
    Boolean tokenIsLocalDeveloper = token.equals(LOCAL_DEVELOPER);
    String username = runningLocally && tokenIsLocalDeveloper ? "developer" : null;
    return username;
  }

}
