package com.dataprofiler.rulesofuse;

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
import static java.lang.String.join;
import static java.util.stream.Collectors.toSet;

import com.dataprofiler.UnirestConfig;
import com.dataprofiler.rulesofuse.response.RouResponse;
import com.dataprofiler.rulesofuse.response.RouUserData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.log4j.Logger;

public class RulesOfUseHelper {
  private static final Logger logger = Logger.getLogger(RulesOfUseHelper.class);

  private static final int HTTP_CONCURRENCY = 128;
  public static final String SYSTEM_CAPABILITY_PREFIX = "system.";
  public static final String SPECIAL_DATA_PREFIX = "special.";
  public static final String ROU_ATTRIBUTES_KEY = "rouAttributes";
  public static final String FIRST_LOGIN_BADGE = "system.seen_joyride";

  public static final String LOGIN_CAPABILITY = "login";
  public static final String ADMIN_CAPABILITY = "admin";
  public static final String DOWNLOAD_CAPABILITY = "download";
  public static final String COMMAND_JOB_CAPABILITY = "command_job";
  public static final String MAKE_CAPABILITY = "make";

  private static final ObjectMapper mapper = new ObjectMapper();
  private final String apiUrl;
  private final String apiKey;

  static {
    UnirestConfig.config(HTTP_CONCURRENCY);
  }

  public RulesOfUseHelper(String apiUrl, String apiKey) {
    this.apiUrl = apiUrl;
    this.apiKey = apiKey;
  }

  private JsonNode rouCall(String query) throws RulesOfUseException {
    String requestPath = this.apiUrl + "/graphql";
    ObjectNode payload = mapper.createObjectNode();
    payload.put("query", query);
    try {
      HttpResponse<kong.unirest.JsonNode> jsonResponse =
          Unirest.post(requestPath)
              .header("Accept", "application/json")
              .header("Content-Type", "application/json")
              .header("Authorization", this.apiKey)
              .body(mapper.writeValueAsString(payload))
              .asJson();
      return unirestNodeToJacksonNode(jsonResponse);
    } catch (Exception e) {
      logger.error("Failed to query ROU for " + query);
      logger.error(e.getMessage());
      throw new RulesOfUseException(e);
    }
  }

  public Boolean storeLoginInRou(String username, String token) throws RulesOfUseException {
    String query =
        "mutation{tokenLogin(username:\""
            + username
            + "\", token:\""
            + token
            + "\"){username expires_at inactive_at }}";
    JsonNode result = rouCall(query);
    return result != null && result.get("errors") == null;
  }

  public Boolean checkTokenAuthenticatedFromRou(String username, String token)
      throws RulesOfUseException {
    String query =
        "{checkToken(username:\""
            + username
            + "\", token:\""
            + token
            + "\"){username expires_at inactive_at}}";
    JsonNode result = rouCall(query);
    boolean hasErrors = (result == null || result.get("errors") != null);
    if (hasErrors) {
      return false;
    }
    JsonNode usernameNode = result.path("data").path("checkToken").path("username");
    if (usernameNode.isMissingNode()) {
      return false;
    }
    return usernameNode.asText().equals(username);
  }

  public UserAttributes getUserAttributes(String username) throws RulesOfUseException {
    String query = "{user(username:\"" + username + "\"){attributes{value,is_active}}}";
    JsonNode node = rouCall(query);

    Set<String> visibilities = new HashSet<>();
    UserAttributes ats = new UserAttributes(username);
    JsonNode array = node.path("data").path("user").path("attributes");
    if (array.isMissingNode()) {
      ats.setAllVisibilities(visibilities);
      return ats;
    }

    for (JsonNode element : array) {
      String visibilityString = element.get("value").asText();
      if (!visibilityString.startsWith(SYSTEM_CAPABILITY_PREFIX)
          && !element.get("is_active").asBoolean()) {
        continue;
      }
      if (visibilityString.startsWith(SPECIAL_DATA_PREFIX)) {
        continue;
      }
      visibilities.add(visibilityString);
    }
    ats.setAllVisibilities(visibilities);
    return ats;
  }

  /**
   * Get UserAttribtues for a root like user. That means that they have all capabilities and all
   * active visibilities.
   *
   * @return
   */
  public UserAttributes getRootAttibutes(String username) throws RulesOfUseException {
    JsonNode node = rouCall("{attributesActive}");
    UserAttributes ats = new UserAttributes(username);

    if (node != null
        && node.get("data") != null
        && node.get("data").get("attributesActive") != null) {
      ArrayNode array = (ArrayNode) node.get("data").get("attributesActive");

      for (JsonNode element : array) {
        String visibilityString = element.asText();
        ats.allVisibilities.add(visibilityString);
      }
      ats.calculateCurrentVisibilities();
    }

    ats.systemCapabilities.add("system.admin");
    ats.systemCapabilities.add("system.download");
    ats.systemCapabilities.add("system.login");

    return ats;
  }

  /**
   * query rou for numUsersWithAttribute with a given dataset attribute for example:
   * LIST.PUBLIC_DATA, returns a count
   *
   * @param datasetAttribute
   * @return
   */
  public JsonNode numUsersWithAttribute(String datasetAttribute) throws RulesOfUseException {
    String query = format("{ numUsersWithAttribute(value: \"%s\")}", datasetAttribute);
    return rouCall(query);
  }

  public RouResponse queryNumUsersWithAttribute(String datasetAttribute)
      throws RulesOfUseException {

    JsonNode node = numUsersWithAttribute(datasetAttribute);
    if (logger.isDebugEnabled()) {
      logger.debug(node.toString());
    }
    try {
      return mapper.readValue(node.toString(), RouResponse.class);
    } catch (IOException ioe) {
      throw new RulesOfUseException(ioe);
    }
  }

  /**
   * query rou for a list of usersWithAttribute with a given dataset attribute for example:
   * LIST.PUBLIC_DATA, returns a count
   *
   * @param datasetAttribute
   * @return
   */
  public JsonNode usersWithAttribute(String datasetAttribute) throws RulesOfUseException {
    String query =
        format(
            "{\n"
                + "  usersWithAttribute(value: \"%s\") {\n"
                + "    id\n"
                + "    username\n"
                + "  }\n"
                + "}",
            datasetAttribute);
    return rouCall(query);
  }

  /**
   * query rou for a list of usersWithAttribute with a given dataset attribute for example:
   * LIST.PUBLIC_DATA, returns a count
   *
   * @return
   */
  public JsonNode usersWithContractorAttribute() throws RulesOfUseException {
    String contractorAttribute = "hr.prmry_job_typ_desc.contractor";
    return usersWithAttribute(contractorAttribute);
  }

  public RouUserData queryUsersWithAttribute(String datasetAttribute) throws RulesOfUseException {
    JsonNode node = usersWithAttribute(datasetAttribute);
    if (logger.isTraceEnabled()) {
      logger.trace(node.toString());
    }
    try {
      return mapper.readValue(node.toString(), RouUserData.class);
    } catch (IOException ioe) {
      throw new RulesOfUseException(ioe);
    }
  }

  public RouUserData queryUsersWithContractorAttribute() throws RulesOfUseException {
    JsonNode node = usersWithContractorAttribute();
    if (logger.isTraceEnabled()) {
      logger.trace(node.toString());
    }
    try {
      return mapper.readValue(node.toString(), RouUserData.class);
    } catch (IOException ioe) {
      throw new RulesOfUseException(ioe);
    }
  }

  /**
   * @param username
   * @return
   */
  public boolean addFirstLoginBadge(String username) throws RulesOfUseException {
    String newline = "\n";
    String mutation =
        "mutation {"
            + newline
            + "createUpdateUser("
            + newline
            + format("username: \"%s\"", username)
            + newline
            + format("attributes: [\"%s\"]", FIRST_LOGIN_BADGE)
            + ") {"
            + newline
            + "id attributes { value } }"
            + newline
            + "}";
    JsonNode result = rouCall(mutation);
    boolean hasErrors = result == null || !result.path("errors").isMissingNode();
    if (hasErrors) {
      return false;
    }
    UserAttributes userAttributes = new UserAttributes(username);
    JsonNode attributes = result.path("data").path("createUpdateUser").path("attributes");
    if (attributes.isMissingNode()) {
      return false;
    }

    Set<String> respVizSet = new HashSet<>();
    for (JsonNode element : attributes) {
      String visibilityString = element.get("value").asText();
      respVizSet.add(visibilityString);
    }
    userAttributes.setAllVisibilities(respVizSet);
    return userAttributes.hasFirstLoginBadge();
  }

  /**
   * @param username
   * @param visibilities
   * @return
   */
  public boolean removeAttributes(String username, Set<String> visibilities)
      throws RulesOfUseException {
    Set<String> quotedVisibilities =
        visibilities.stream().map(el -> format("\"%s\"", el)).collect(toSet());
    String newline = "\n";
    String mutation =
        "mutation {"
            + newline
            + "removeAttributesFromUser("
            + newline
            + format("username: \"%s\"", username)
            + newline
            + format("attributes: [%s]", join(",", quotedVisibilities))
            + ") {"
            + newline
            + "id attributes { value } }"
            + newline
            + "}";
    JsonNode result = rouCall(mutation);
    boolean hasErrors = result == null || !result.path("errors").isMissingNode();
    if (hasErrors) {
      return false;
    }
    JsonNode attributes = result.path("data").path("removeAttributesFromUser").path("attributes");
    if (attributes.isMissingNode()) {
      return false;
    }

    Set<String> respVizSet = new HashSet<>();
    for (JsonNode element : attributes) {
      String visibilityString = element.get("value").asText();
      respVizSet.add(visibilityString);
    }

    boolean removedAllVisibilities = true;
    for (String requestedViz : visibilities) {
      if (respVizSet.contains(requestedViz)) {
        removedAllVisibilities = false;
        break;
      }
    }
    return removedAllVisibilities;
  }

  /**
   * @param username
   * @return
   */
  public boolean removeFirstLoginBadge(String username) throws RulesOfUseException {
    Set<String> set = new HashSet<>();
    set.add(FIRST_LOGIN_BADGE);
    return this.removeAttributes(username, set);
  }

  protected JsonNode unirestNodeToJacksonNode(HttpResponse<kong.unirest.JsonNode> jsonResponse)
      throws IOException {
    if (jsonResponse == null || !jsonResponse.isSuccess()) {
      return NullNode.getInstance();
    }
    String responseJSONString = jsonResponse.getBody().toString();
    return mapper.readValue(responseJSONString, JsonNode.class);
  }

  public static class UserAttributes {

    private final TypeReference<ArrayList<String>> stringArrType =
        new TypeReference<ArrayList<String>>() {};
    public String username;
    private Set<String> allVisibilities = new HashSet<>();
    private Set<String> visibilitiesToReject = new HashSet<>();
    private Set<String> currentVisibilities = new HashSet<>();
    private final Set<String> systemCapabilities = new HashSet<>();
    private Set<String> specialVisibilitiesToAdd = new HashSet<>();

    public UserAttributes() {}

    public UserAttributes(String username) {
      this.username = username;
    }

    private void calculateCurrentVisibilities() {
      currentVisibilities = new HashSet<>();
      currentVisibilities.addAll(allVisibilities);
      currentVisibilities.addAll(specialVisibilitiesToAdd);
      currentVisibilities.removeAll(visibilitiesToReject);
      currentVisibilities.removeAll(systemCapabilities);
    }

    private String setToJSON(Set<String> s) {
      try {
        return mapper.writeValueAsString(s);
      } catch (JsonProcessingException e) {
        return "";
      }
    }

    public String getUsername() {
      return this.username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String allAsJSONString() {
      return setToJSON(allVisibilities);
    }

    public String currentAsJSONString() {
      return setToJSON(currentVisibilities);
    }

    public String asJson() throws JsonProcessingException {
      return mapper.writeValueAsString(this);
    }

    public String systemCapabilitiesAsJSONString() {
      return setToJSON(systemCapabilities);
    }

    public Boolean hasLoginAttribute() {
      return hasSystemAttribute(RulesOfUseHelper.SYSTEM_CAPABILITY_PREFIX + LOGIN_CAPABILITY);
    }

    public Boolean hasFirstLoginBadge() {
      return hasSystemAttribute(RulesOfUseHelper.FIRST_LOGIN_BADGE);
    }

    public Boolean hasAdminCapability() {
      return hasCapability(ADMIN_CAPABILITY);
    }

    public Boolean hasCapability(String capability) {
      return hasSystemAttribute(RulesOfUseHelper.SYSTEM_CAPABILITY_PREFIX + capability);
    }

    public Boolean hasDownloadAttribute() {
      return hasSystemAttribute(RulesOfUseHelper.SYSTEM_CAPABILITY_PREFIX + DOWNLOAD_CAPABILITY);
    }

    public Boolean hasCommandJobAttribute() {
      return hasSystemAttribute(SYSTEM_CAPABILITY_PREFIX + COMMAND_JOB_CAPABILITY);
    }

    public Boolean hasSystemAttribute(String attributeName) {
      return systemCapabilities.contains(attributeName);
    }

    public Authorizations currentAsAuthorizations() {
      return new Authorizations(
          currentVisibilities.toArray(new String[currentVisibilities.size()]));
    }

    public String currentAsCommaDelimitedString() {
      return join(",", currentVisibilities);
    }

    public Set<String> getAllVisibilities() {
      return allVisibilities;
    }

    public void setAllVisibilities(Set<String> allVisibilities) {
      // Remove system attributes and add them to the system list
      for (String currentVisibility : allVisibilities) {
        if (currentVisibility.startsWith(RulesOfUseHelper.SYSTEM_CAPABILITY_PREFIX)) {
          systemCapabilities.add(currentVisibility);
        }
      }
      this.allVisibilities = allVisibilities;
      calculateCurrentVisibilities();
    }

    public Set<String> getVisibilitiesToReject() {
      return visibilitiesToReject;
    }

    public void setVisibilitiesToReject(Set<String> visibilitiesToReject) {
      this.visibilitiesToReject = visibilitiesToReject;
      calculateCurrentVisibilities();
    }

    public boolean setVisibilitiesToReject(String visibilitiesToRejectJSONString) {
      if (visibilitiesToRejectJSONString == null) {
        return false;
      }

      try {
        ArrayList<String> reject = mapper.readValue(visibilitiesToRejectJSONString, stringArrType);

        setVisibilitiesToReject(new HashSet<>(reject));

        return true;
      } catch (IOException e) {
        return false;
      }
    }

    public void setSpecialVisibilitiesToAdd(Set<String> specialVisibilitiesToAdd) {
      this.specialVisibilitiesToAdd = specialVisibilitiesToAdd;
      calculateCurrentVisibilities();
    }

    public boolean setSpecialVisibilitiesToAdd(String specialVisibilitiesToAddJsonString) {
      if (specialVisibilitiesToAddJsonString == null) {
        return false;
      }

      try {
        ArrayList<String> specials =
            mapper.readValue(specialVisibilitiesToAddJsonString, stringArrType);

        ArrayList<String> filteredSpecials = new ArrayList<>();

        for (String element : specials) {
          if (element.toLowerCase().startsWith("special.")) {
            filteredSpecials.add(element);
          }
        }

        setSpecialVisibilitiesToAdd(new HashSet<>(filteredSpecials));
        return true;
      } catch (IOException e) {
        return false;
      }
    }
  }
}
