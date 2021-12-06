package com.dataprofiler.util.objects;

/*-
 * 
 * dataprofiler-util
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

import com.dataprofiler.util.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.log4j.Logger;

public class MetadataConceptObject {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final Logger logger = Logger.getLogger(MetadataConceptObject.class);
  public Map<String, VersionedMetadataObject> elements;
  public Map<String, Set<Map<String, Object>>> element_to_concept;
  public Map<String, Set<Map<String, Object>>> concept_to_element;
  public Set<ResolutionObject> geography_resolution;
  public Set<ResolutionObject> time_resolution;

  public MetadataConceptObject() {
    elements = new HashMap<>();
    element_to_concept = new HashMap<>();
    concept_to_element = new HashMap<>();
    geography_resolution = new TreeSet<>();
    time_resolution = new TreeSet<>();
  }

  public static MetadataConceptObject fromDatasets(
      Context context, MetadataVersionObject version, int confidence)
      throws JsonProcessingException {
    MetadataConceptObject returnObj = new MetadataConceptObject();
    // Get Datasets
    for (VersionedMetadataObject m : new VersionedMetadataObject().scanDatasets(context, version)) {
      returnObj.elements.put(m.dataset_name, m);
    }

    return returnObj;
  }

  public static MetadataConceptObject fromTables(
      Context context, MetadataVersionObject version, String dataset, Integer confidence)
      throws JsonProcessingException {

    MetadataConceptObject returnObj = new MetadataConceptObject();

    // Get getAllTables
    for (VersionedMetadataObject m :
        new VersionedMetadataObject().scanTables(context, version, dataset)) {
      returnObj.elements.put(m.table_name, m);
    }

    return returnObj;
  }

  public static MetadataConceptObject fromColumns(
      Context context,
      MetadataVersionObject version,
      String dataset,
      String table,
      Integer confidence)
      throws JsonProcessingException {

    MetadataConceptObject returnObj = new MetadataConceptObject();

    // Get columns
    for (VersionedMetadataObject m :
        new VersionedMetadataObject().scanColumns(context, version, dataset, table)) {
      returnObj.elements.put(m.column_name, m);
    }

    return returnObj;
  }
}
