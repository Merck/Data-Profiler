package com.dataprofiler.sqlsync.spec;

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

import static java.util.Collections.unmodifiableSet;

import com.dataprofiler.factory.ObjectMapperFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class MultiSqlSyncSpec {

  @JsonIgnore private final ObjectMapper mapper;

  // list of tables to export
  List<SqlSyncSpec> downloads = new ArrayList<>();
  // jdbc connection for the target output database
  JdbcConnection jdbcConnection;
  // visibilities used during read and applied on the output via the table mapper subsystem
  Set<String> visibilities;
  // users with access to the output external table, mapped via the table mapper subsystem
  Set<String> externalUsers;

  public MultiSqlSyncSpec() {
    super();
    mapper = ObjectMapperFactory.initObjectMapper();
  }

  public Set<String> getVisibilities() {
    return unmodifiableSet(visibilities);
  }

  public void setVisibilities(Set<String> visibilities) {
    this.visibilities = visibilities;
  }

  public Set<String> getExternalUsers() {
    return unmodifiableSet(externalUsers);
  }

  public void setExternalUsers(Set<String> externalUsers) {
    this.externalUsers = externalUsers;
  }

  public String toJson() throws JsonProcessingException {
    return mapper.writeValueAsString(this);
  }

  public static MultiSqlSyncSpec fromJson(String json) throws IOException {
    ObjectMapper mapper = ObjectMapperFactory.initObjectMapper();
    return mapper.readValue(json, new TypeReference<MultiSqlSyncSpec>() {});
  }

  public JdbcConnection getJdbcConnection() {
    return jdbcConnection;
  }

  public void setJdbcConnection(JdbcConnection jdbcConnection) {
    this.jdbcConnection = jdbcConnection;
  }

  public List<SqlSyncSpec> getDownloads() {
    return this.downloads;
  }

  public void setDownloads(List<SqlSyncSpec> downloads) {
    this.downloads = downloads;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .appendSuper(super.toString())
        .append("downloads", downloads)
        .append("jdbcConnection", jdbcConnection)
        .append("visibilities", visibilities)
        .append("externalUsers", externalUsers)
        .toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }
    MultiSqlSyncSpec rhs = (MultiSqlSyncSpec) obj;
    return new EqualsBuilder()
        .appendSuper(super.equals(obj))
        .append(downloads, rhs.downloads)
        .append(jdbcConnection, rhs.jdbcConnection)
        .append(visibilities, rhs.visibilities)
        .append(externalUsers, rhs.externalUsers)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(2437, 419)
        .appendSuper(super.hashCode())
        .append(downloads)
        .append(jdbcConnection)
        .append(visibilities)
        .append(externalUsers)
        .toHashCode();
  }
}
