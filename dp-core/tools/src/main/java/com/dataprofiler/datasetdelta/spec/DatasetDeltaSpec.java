package com.dataprofiler.datasetdelta.spec;

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
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class DatasetDeltaSpec {

  @JsonIgnore private ObjectMapper mapper;

  // list of datasets to run performance numbers
  Set<String> datasets = new HashSet<>();

  // should run on all datasets
  Boolean allDatasets = false;

  public ObjectMapper getMapper() {
    return mapper;
  }

  public void setMapper(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public Set<String> getDatasets() {
    return unmodifiableSet(new HashSet<>(datasets));
  }

  public void setDatasets(Set<String> datasets) {
    this.datasets = datasets;
  }

  public Boolean isAllDatasets() {
    return allDatasets;
  }

  public void setAllDatasets(Boolean allDatasets) {
    this.allDatasets = allDatasets;
  }

  public DatasetDeltaSpec() {
    super();
    mapper = ObjectMapperFactory.initObjectMapper();
  }

  public String toJson() throws JsonProcessingException {
    return mapper.writeValueAsString(this);
  }

  public static DatasetDeltaSpec fromJson(String json) throws IOException {
    ObjectMapper mapper = ObjectMapperFactory.initObjectMapper();
    return mapper.readValue(json, new TypeReference<DatasetDeltaSpec>() {});
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
