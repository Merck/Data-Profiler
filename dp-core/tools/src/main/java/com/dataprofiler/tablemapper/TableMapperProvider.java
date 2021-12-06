package com.dataprofiler.tablemapper;

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

import static java.util.Objects.nonNull;

import com.dataprofiler.tablemapper.request.TablesLikeQueryParams;
import com.dataprofiler.tablemapper.response.Table;
import com.dataprofiler.tablemapper.response.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.log4j.Logger;

public class TableMapperProvider {
  private static final Logger logger = Logger.getLogger(TableMapperProvider.class);

  private static final ObjectMapper mapper = new ObjectMapper();
  private final String apiUrl;

  //  static {
  //    // TODO: create a new unirest object here? make sure to close the new instance?
  //    UnirestConfig.config();
  //  }

  public TableMapperProvider(String apiUrl) {
    super();
    this.apiUrl = apiUrl;
  }

  public List<User> allUsers() throws TableMapperException {
    JsonNode node = queryAllUsers().path("data").path("allUsers");
    if (logger.isTraceEnabled()) {
      logger.trace(node.toString());
    }
    try {
      return mapper.readValue(node.toString(), new TypeReference<List<User>>() {});
    } catch (IOException ioe) {
      throw new TableMapperException(ioe);
    }
  }

  public List<Table> allTables() throws TableMapperException {
    JsonNode node = queryAllTables().path("data").path("allTables");
    if (logger.isTraceEnabled()) {
      logger.trace(node.toString());
    }
    try {
      return mapper.readValue(node.toString(), new TypeReference<List<Table>>() {});
    } catch (IOException ioe) {
      throw new TableMapperException(ioe);
    }
  }

  public List<Table> tablesLike(TablesLikeQueryParams params) throws TableMapperException {
    JsonNode node = queryTablesLike(params).path("data").path("tablesLike");
    if (logger.isTraceEnabled()) {
      logger.trace(node.toString());
    }
    try {
      return mapper.readValue(node.toString(), new TypeReference<List<Table>>() {});
    } catch (IOException ioe) {
      throw new TableMapperException(ioe);
    }
  }

  public List<User> usersLike(String dataset) throws TableMapperException {
    JsonNode node = queryUsersLike(dataset).path("data").path("usersLike");
    if (logger.isTraceEnabled()) {
      logger.trace(node.toString());
    }
    try {
      return mapper.readValue(node.toString(), new TypeReference<List<User>>() {});
    } catch (IOException ioe) {
      throw new TableMapperException(ioe);
    }
  }

  public Float deleteTableById(Integer id) throws TableMapperException {
    JsonNode node = mutateDeleteTableById(id).path("data").path("deleteTables");
    if (logger.isTraceEnabled()) {
      logger.trace(node.toString());
    }
    try {
      JsonNode value = node.path("deleteTables");
      if (value.isMissingNode() || value.isEmpty()) {
        return -1f;
      }
      return mapper.readValue(value.toString(), Float.class);
    } catch (IOException ioe) {
      throw new TableMapperException(ioe);
    }
  }

  protected JsonNode queryAllUsers() throws TableMapperException {
    String query =
        "query {\n"
            + "  allUsers {\n"
            + "    id\n"
            + "    username\n"
            + "    tables {\n"
            + "      id\n"
            + "      environment\n"
            + "      datasetname\n"
            + "      external_name\n"
            + "      tablename\n"
            + "      visibility\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    return graphqlCall(query);
  }

  protected JsonNode queryAllTables() throws TableMapperException {
    String query =
        "{\n"
            + "  allTables {\n"
            + "    id\n"
            + "    environment\n"
            + "    datasetname\n"
            + "    tablename\n"
            + "    visibility\n"
            + "    users {\n"
            + "      id\n"
            + "      username\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    return graphqlCall(query);
  }

  protected JsonNode queryTablesLike(TablesLikeQueryParams params) throws TableMapperException {
    String query =
        "query ("
            + params.generateGraphqlQueryHeader()
            + ") {\n"
            + "  tablesLike("
            + params.generateGraphqlQueryParams()
            + ") {\n"
            + "    id\n"
            + "    environment\n"
            + "    datasetname\n"
            + "    tablename\n"
            + "    visibility\n"
            + "    users {\n"
            + "      id\n"
            + "      username\n"
            + "      tables {\n"
            + "        id\n"
            + "        tablename\n"
            + "        users {\n"
            + "          id\n"
            + "          username\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    Map<String, Object> variables = params.paramsToGraphQlVars();
    return graphqlCall(query, variables);
  }

  protected JsonNode queryUsersLike(String userName) throws TableMapperException {
    String query =
        "query($username: String!){\n"
            + "  usersLike(username: $username) {\n"
            + "    id\n"
            + "    username\n"
            + "    tables {\n"
            + "      id\n"
            + "      environment\n"
            + "      datasetname\n"
            + "      tablename\n"
            + "      external_name\n"
            + "      visibility\n"
            + "    }\n"
            + "  }\n"
            + "}";
    Map<String, Object> variables = new HashMap<>();
    variables.put("username", userName);
    return graphqlCall(query, variables);
  }

  protected JsonNode mutateDeleteTableById(Integer id) throws TableMapperException {
    String mutation = "mutation ($id: Int) {\n" + "    deleteTables(id: $id)\n" + "}";
    Map<String, Object> variables = new HashMap<>();
    variables.put("id", id);
    return graphqlCall(mutation, variables);
  }

  private JsonNode graphqlCall(String query) throws TableMapperException {
    return graphqlCall(query, null);
  }

  private JsonNode graphqlCall(String query, Map<String, Object> variables)
      throws TableMapperException {
    ObjectNode payload = mapper.createObjectNode();
    payload.put("query", query);
    if (nonNull(variables) && !variables.isEmpty()) {
      JsonNode variablesNode = mapper.valueToTree(variables);
      payload.set("variables", variablesNode);
    }

    try {
      String graphQlPayload = mapper.writeValueAsString(payload);
      HttpResponse<kong.unirest.JsonNode> jsonResponse =
          Unirest.post(apiUrl)
              .header("Accept", "application/json")
              .header("Content-Type", "application/json")
              //              .header("Authorization", this.apiKey)
              .body(graphQlPayload)
              .asJson();
      return unirestNodeToJacksonNode(jsonResponse);

    } catch (Exception e) {
      logger.error("Failed to query tablemapper API query: " + query);
      logger.error(e.getMessage());
      throw new TableMapperException(e);
    }
  }

  private JsonNode unirestNodeToJacksonNode(HttpResponse<kong.unirest.JsonNode> jsonResponse)
      throws IOException {
    if (jsonResponse == null || !jsonResponse.isSuccess()) {
      return NullNode.getInstance();
    }
    return mapper.readValue(jsonResponse.getBody().toString(), JsonNode.class);
  }
}
