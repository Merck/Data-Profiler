package com.dataprofiler.tablemapper.request;

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
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class TablesLikeQueryParams {

  protected String environment;

  protected String datasetName;

  protected String tableName;

  protected String externalName;

  protected String visibility;

  public TablesLikeQueryParams() {
    super();
  }

  public String getEnvironment() {
    return environment;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getExternalName() {
    return externalName;
  }

  public void setExternalName(String externalName) {
    this.externalName = externalName;
  }

  public String getVisibility() {
    return visibility;
  }

  public void setVisibility(String visibility) {
    this.visibility = visibility;
  }

  public Map<String, Object> paramsToGraphQlVars() {
    Map<String, Object> queryFields = new HashMap<>();
    if (hasValue(environment)) {
      queryFields.put("environment", environment);
    }
    if (hasValue(datasetName)) {
      queryFields.put("datasetname", datasetName);
    }
    if (hasValue(tableName)) {
      queryFields.put("tablename", tableName);
    }
    if (hasValue(visibility)) {
      queryFields.put("visibility", visibility);
    }
    if (hasValue(externalName)) {
      queryFields.put("external_name", externalName);
    }
    return queryFields;
  }

  public Map<String, String> graphqlQueryParams() {
    Map<String, String> queryFields = new HashMap<>();
    if (hasValue(environment)) {
      queryFields.put("$environment", "String");
    }
    if (hasValue(datasetName)) {
      queryFields.put("$datasetname", "String");
    }
    if (hasValue(tableName)) {
      queryFields.put("$tablename", "String");
    }
    if (hasValue(visibility)) {
      queryFields.put("$visibility", "String");
    }
    if (hasValue(externalName)) {
      queryFields.put("$external_name", "String");
    }
    return queryFields;
  }

  /**
   * the graphql query header e.g. query ($environment: String, etc.)
   *
   * @return
   */
  public String generateGraphqlQueryHeader() {
    Map<String, String> queryFields = graphqlQueryParams();
    return queryFields.entrySet().stream()
        .map(entry -> format("%s: %s", entry.getKey(), entry.getValue()))
        .collect(joining(", "));
  }

  /**
   * the graphql query header eg query (environment: $environment, etc)
   *
   * @return
   */
  public String generateGraphqlQueryParams() {
    Map<String, String> queryFields = graphqlQueryParams();
    // remove the leading $ in the string and return a string in the form of
    // external_name: $external_name
    return queryFields.entrySet().stream()
        .map(entry -> format("%s: %s", entry.getKey().substring(1), entry.getKey()))
        .collect(joining(", "));
  }

  private boolean hasValue(String val) {
    return nonNull(val) && !val.isEmpty();
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  @Override
  public boolean equals(Object rhs) {
    return EqualsBuilder.reflectionEquals(this, rhs);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this, false);
  }
}
