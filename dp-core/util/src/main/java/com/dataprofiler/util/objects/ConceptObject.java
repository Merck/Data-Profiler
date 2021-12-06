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

import static com.dataprofiler.util.Const.CONCEPT_NAME_PATTERN;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.regex.Matcher;

public class ConceptObject implements Comparable<ConceptObject> {

  private static final java.util.regex.Pattern CONCEPT_NAME_REGEX =
      java.util.regex.Pattern.compile(CONCEPT_NAME_PATTERN);

  /** The column of the matched concept */
  public String concept = "";

  /** The concept friendly name */
  public String friendly_name = "";

  /** The size of the set of matched concepts */
  public Long num_unique_matched_concepts = 0L;

  /** The number of values in the column that match values in a concepts master list */
  public Long num_matched_concepts = 0L;

  /** Number of matched concepts values / number of values in the column */
  public Double column_coverage = 0.0;

  /** Number of possible values for the concepts */
  public Long num_possible_concepts = 0L;

  /** num_matched_concepts / num_possible_concepts */
  public Double concept_coverage = 0.0;

  public ConceptObject() {}

  public ConceptObject(String concept) {
    this.concept = concept;

    Matcher m = CONCEPT_NAME_REGEX.matcher(concept);
    if (m.matches() && m.groupCount() >= 2) {
      friendly_name = m.group(1).replace("_", " ").trim();
    }
  }

  public ConceptObject(String name, Long match_count, Long num_possible_concepts) {
    this.concept = name;
    this.num_matched_concepts = match_count;
    this.num_possible_concepts = num_possible_concepts;
  }

  public static ConceptObject combineConcepts(
      ConceptObject con1, ConceptObject con2, Long numValues) {
    ConceptObject combined = new ConceptObject();

    combined.concept = con1.concept.isEmpty() ? con2.concept : con1.concept;
    combined.friendly_name = con1.friendly_name.isEmpty() ? con2.friendly_name : con1.friendly_name;
    combined.num_matched_concepts = con1.num_matched_concepts + con2.num_matched_concepts;
    combined.column_coverage = combined.num_matched_concepts.doubleValue() / numValues;

    combined.num_unique_matched_concepts =
        con1.num_unique_matched_concepts + con2.num_unique_matched_concepts;
    combined.num_possible_concepts =
        con1.num_possible_concepts > con2.num_possible_concepts
            ? con1.num_possible_concepts
            : con2.num_possible_concepts;
    combined.concept_coverage =
        combined.num_unique_matched_concepts.doubleValue() / combined.num_possible_concepts;

    return combined;
  }

  public void computeCoverage(Long numValues) {
    column_coverage = num_matched_concepts.doubleValue() / numValues;
    concept_coverage = num_unique_matched_concepts.doubleValue() / num_possible_concepts;
  }

  @Override
  public int compareTo(ConceptObject o) {
    return concept.compareTo(o.concept);
  }

  @Override
  public String toString() {
    ObjectMapper mapper = new ObjectMapper();

    String output = null;
    try {
      output = mapper.writeValueAsString(this);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return output;
  }
}
