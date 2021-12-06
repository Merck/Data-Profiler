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

import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import kong.unirest.UnirestInstance;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.admin.SecurityOperations;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.ColumnVisibility;
import org.apache.accumulo.core.security.ColumnVisibility.Node;
import org.apache.accumulo.core.security.ColumnVisibility.NodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveVisibilityManager implements Closeable {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final Logger log = LoggerFactory.getLogger(ActiveVisibilityManager.class);
  private final Context context;
  private UnirestInstance unirest;

  public ActiveVisibilityManager() throws IOException, BasicAccumuloException {
    context = new Context();
    init();
  }

  public ActiveVisibilityManager(String[] argv) throws IOException, BasicAccumuloException {
    context = new Context(argv);
    init();
  }

  public ActiveVisibilityManager(Context context) {
    this.context = context;
    init();
  }

  void init() {
    this.unirest = Unirest.spawnInstance();
    this.unirest
        .config()
        .automaticRetries(true)
        .addDefaultHeader("Accept", "application/json")
        .addDefaultHeader("Content-Type", "application/json")
        .addDefaultHeader("Authorization", context.getConfig().rulesOfUseApiKey);
  }

  public String rouCall(String query) throws JsonProcessingException, UnirestException {
    log.info("Executing query: " + query);
    String requestPath = context.getConfig().rulesOfUseApiPath + "/graphql";

    ObjectNode payload = mapper.createObjectNode();
    payload.put("query", query);

    HttpResponse<String> response =
        this.unirest.post(requestPath).body(mapper.writeValueAsString(payload)).asString();

    if (response.getStatus() != 200) {
      log.warn(
          "Received a {} response from the ROU. Body: {}",
          response.getStatus(),
          response.getBody());
    } else {
      return response.getBody();
    }

    throw new IllegalStateException(
        "Max retries reached while attempting to activate visibility " + query);
  }

  public void setVisibilityActive(String visibility)
      throws JsonProcessingException, UnirestException {
    String query =
        "mutation{markAttributeActive(value:\"" + visibility + "\"){id value is_active}}";
    log.debug(rouCall(query));
  }

  public Set<String> extractVisibilitiesFromExpression(String e) {
    ColumnVisibility ve = new ColumnVisibility(e);
    byte[] expression = ve.getExpression();

    Stack<Node> stack = new Stack<>();
    Set<String> visibilities = new HashSet<>();

    stack.push(ve.getParseTree());
    while (stack.size() > 0) {
      Node cur = stack.pop();
      if (cur.getType() == NodeType.TERM) {
        visibilities.add(cur.getTerm(expression).toString());
      }

      for (Node n : cur.getChildren()) {
        stack.push(n);
      }
    }

    return visibilities;
  }

  public void setRootAuthorizations()
      throws IOException, UnirestException, AccumuloSecurityException, AccumuloException,
          BasicAccumuloException {
    String query = "{attributesActive}";

    String responseJSONString = rouCall(query);
    com.fasterxml.jackson.databind.JsonNode node =
        mapper.readValue(responseJSONString, com.fasterxml.jackson.databind.JsonNode.class);
    HashSet<String> visibilities = new HashSet<>();
    try {
      ArrayNode array = (ArrayNode) node.get("data").get("attributesActive");
      for (Iterator<com.fasterxml.jackson.databind.JsonNode> it = array.iterator();
          it.hasNext(); ) {
        com.fasterxml.jackson.databind.JsonNode element = it.next();
        String visibilityString = element.asText();
        visibilities.add(visibilityString);
      }
    } catch (NullPointerException e) {
      log.error("Failed to parse root attributes");
      return;
    }

    log.debug("Setting root auths: " + visibilities);

    Authorizations authorizations =
        new Authorizations(visibilities.toArray(new String[visibilities.size()]));

    context.getConnector().securityOperations().changeUserAuthorizations("root", authorizations);
  }

  public void setVisibilitiesFromExpressionAsActive(String expression)
      throws IOException, UnirestException, AccumuloSecurityException, AccumuloException,
          BasicAccumuloException {
    Set<String> visibilities = extractVisibilitiesFromExpression(expression);

    SecurityOperations securityOps = this.context.getConnector().securityOperations();
    Set<String> auths =
        securityOps
            .getUserAuthorizations(this.context.getConfig().accumuloUser)
            .getAuthorizations()
            .stream()
            .map(ba -> new String(ba, Charsets.UTF_8))
            .map(String::toUpperCase)
            .collect(Collectors.toSet());
    boolean rootsAuthNeedSetting = false;
    for (String viz : visibilities) {
      if (auths.contains(viz)) {
        log.debug("Not activating auth {} as it's already set on the Accumulo user", viz);
      } else {
        log.info("Activating " + viz);
        setVisibilityActive(viz);
        rootsAuthNeedSetting = true;
      }
    }

    if (rootsAuthNeedSetting) {
      setRootAuthorizations();
    }
  }

  @Override
  public void close() {
    this.unirest.close();
  }

  public static void main(String[] argv)
      throws IOException, UnirestException, AccumuloSecurityException,
          AccumuloException, BasicAccumuloException {
    ActiveVisibilityManager m = new ActiveVisibilityManager(argv);

    if (m.context.getConfig().parameters.size() != 1) {
      System.out.println("VisibilityExtractor visibility_string");
      System.exit(1);
    }

    m.setVisibilitiesFromExpressionAsActive(m.context.getConfig().parameters.get(0));
  }
}
