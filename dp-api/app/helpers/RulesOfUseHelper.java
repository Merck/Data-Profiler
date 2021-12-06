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

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.dataprofiler.util.Context;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import org.apache.accumulo.core.security.Authorizations;
import play.Logger;
import play.libs.typedmap.TypedKey;

public class RulesOfUseHelper {

  public static final String SYSTEM_CAPABILITY_PREFIX = "system.";
  public static final String SPECIAL_DATA_PREFIX = "special.";
  public static final String ROU_ATTRIBUTES_KEY = "rouAttributes";
  public static final String FIRST_LOGIN_BADGE = "system.seen_joyride";

  public static final TypedKey ROU_ATTRIBUTES_TYPED_KEY = TypedKey.create(ROU_ATTRIBUTES_KEY);
  public static final TypedKey<Context> ROOT_CONTEXT_TYPED_KEY = TypedKey.create("rootContext");
  public static final TypedKey<Context> USER_CONTEXT_TYPED_KEY = TypedKey.create("userContext");

  public static final String LOGIN_CAPABILITY = "login";
  public static final String ADMIN_CAPABILITY = "admin";
  public static final String DOWNLOAD_CAPABILITY = "download";
  public static final String COMMAND_JOB_CAPABILITY = "command_job";
  public static final String MAKE_CAPABILITY = "make";

  private static final ObjectMapper mapper = new ObjectMapper();
  private String apiUrl;
  private String apiKey;

  @Inject
  public RulesOfUseHelper(String apiUrl, String apiKey) {
    this.apiUrl = apiUrl;
    this.apiKey = apiKey;
  }

  private JsonNode rouCall(String query) {
    String requestPath = this.apiUrl + "/graphql";
    ObjectNode payload = mapper.createObjectNode();
    payload.put("query", query);
    try {
      HttpResponse<com.mashape.unirest.http.JsonNode> jsonResponse =
          Unirest.post(requestPath)
              .header("Accept", "application/json")
              .header("Content-Type", "application/json")
              .header("Authorization", this.apiKey)
              .body(mapper.writeValueAsString(payload))
              .asJson();
      String responseJSONString = jsonResponse.getBody().toString();
      return mapper.readValue(responseJSONString, JsonNode.class);
    } catch (Exception e) {
      Logger.error("Failed to query ROU for " + query);
      return NullNode.getInstance();
    }
  }

  public Boolean storeLoginInRou(String username, String token) {
    String query =
        "mutation{tokenLogin(username:\""
            + username
            + "\", token:\""
            + token
            + "\"){username expires_at inactive_at }}";
    JsonNode result = rouCall(query);
    return result != null && result.get("errors") == null;
  }

  public Boolean checkTokenAuthenticatedFromRou(String username, String token) {
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

  public UserAttributes getUserAttributes(String username) {
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
  public UserAttributes getRootAttibutes(String username) {
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
   * @param username
   * @return
   */
  public boolean addFirstLoginBadge(String username) {
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
  public boolean removeAttributes(String username, Set<String> visibilities) {
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
  public boolean removeFirstLoginBadge(String username) {
    Set<String> set = new HashSet<String>();
    set.add(FIRST_LOGIN_BADGE);
    return this.removeAttributes(username, set);
  }

  public static class UserAttributes {

    private final TypeReference<ArrayList<String>> stringArrType =
        new TypeReference<ArrayList<String>>() {};
    public String username;
    private Set<String> allVisibilities = new HashSet<>();
    private Set<String> visibilitiesToReject = new HashSet<>();
    private Set<String> currentVisibilities = new HashSet<>();
    private Set<String> systemCapabilities = new HashSet<>();
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

    public Set<String> getCurrentVisibilities() {
      return new HashSet<>(currentVisibilities);
    }

    public Set<String> getAllVisibilities() {
      if (isNull(allVisibilities)) {
        return null;
      }
      return new HashSet<>(allVisibilities);
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
