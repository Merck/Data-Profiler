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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

public class RefinedConcept {

  public static final java.util.regex.Pattern CONCEPT_NAME_REGEX =
      java.util.regex.Pattern.compile(CONCEPT_NAME_PATTERN);

  /** The name of the dataset */
  public String dataset;

  /** The name of the table */
  public String table;

  /** The name of the column */
  public String column;

  /** The raw name of the matched concept */
  public String concept;

  /** The concept friendly name */
  public String friendly_name;

  /** The name of this concept's topic */
  public String topic;

  /** Total number of values in the column */
  public Long num_values;

  /** Total number of unique values in the column */
  public Long num_unique_values;

  /** True if a ref column; false otherwise */
  public Boolean ref_indicator;

  /** The size of the set of matched concepts */
  public Long num_unique_matched_concepts;

  /** The number of values in the column that match values in a concepts master list */
  public Long num_matched_concepts;

  /** Number of matched concepts values / number of values in the column */
  public Double column_coverage;

  /** Number of possible values for the concepts */
  public Long num_possible_concepts;

  /** num_matched_concepts / num_possible_concepts */
  public Double concept_coverage;

  /** PET score for the column */
  public Integer pet_score;

  /** The time at which the concept was loaded */
  public Long load_time;

  /** The total number of master lists */
  public Long num_master_lists;

  /** Number of concepts detected for the column */
  public Long num_concepts_detected;

  /** Ratio of (number of master lists detected / total number of master lists) for the column */
  public Double hit_score;

  /** The pet score with hit_score calculation applied */
  public Integer refined_pet_score;

  /** The visibility of the concept */
  public String visibility;

  /** The type of the concept */
  public ConceptType type;

  public RefinedConcept() {}

  public RefinedConcept(
      String dataset,
      String table,
      String column,
      Long columnSize,
      Long columnCardinality,
      Long loadTime,
      Long num_master_lists,
      Long concepts_detected,
      ConceptObject conceptObject) {
    this.dataset = dataset;
    this.table = table;
    this.column = column;
    this.num_values = columnSize;
    this.num_unique_values = columnCardinality;
    this.num_master_lists = num_master_lists;
    this.num_concepts_detected = concepts_detected;
    this.load_time = loadTime;
    this.concept = conceptObject.concept;
    this.friendly_name = conceptObject.friendly_name;

    // Determine the topic
    Matcher m = CONCEPT_NAME_REGEX.matcher(this.concept);
    if (m.matches() && m.groupCount() >= 2) {
      this.topic = m.group(2);
    } else {
      this.topic = "";
    }

    this.num_unique_matched_concepts = conceptObject.num_unique_matched_concepts;
    this.num_matched_concepts = conceptObject.num_matched_concepts;
    this.column_coverage = conceptObject.column_coverage;
    this.num_possible_concepts = conceptObject.num_possible_concepts;
    this.concept_coverage = conceptObject.concept_coverage;
    this.hit_score = this.num_concepts_detected / this.num_master_lists.doubleValue();
    this.ref_indicator = this.num_unique_values.doubleValue() / this.num_values > 0.96;
    this.calculatePET();
    refined_pet_score = (int) (pet_score * (1 - hit_score));
  }

  public RefinedConcept(
      String dataset,
      String table,
      String column,
      Long columnSize,
      Long columnCardinality,
      Long loadTime,
      Long num_master_lists,
      Long concepts_detected,
      ConceptObject conceptObject,
      String visibility,
      ConceptType type) {
    this(
        dataset,
        table,
        column,
        columnSize,
        columnCardinality,
        loadTime,
        num_master_lists,
        concepts_detected,
        conceptObject);
    this.visibility = visibility;
    this.type = type;
  }

  public RefinedConcept(
      String dataset,
      String table,
      String column,
      Long columnSize,
      Long columnCardinality,
      Long loadTime,
      Long num_master_lists,
      Long concepts_detected,
      String pattern,
      PatternObject patternObject) {
    this.dataset = dataset;
    this.table = table;
    this.column = column;
    this.num_values = columnSize;
    this.num_unique_values = columnCardinality;
    this.num_master_lists = num_master_lists;
    this.num_concepts_detected = concepts_detected;
    this.load_time = loadTime;

    this.concept = "PatternObject";
    this.friendly_name = pattern;
    this.topic = "";
    this.num_unique_matched_concepts = patternObject.num_uniq_matches;
    this.num_matched_concepts = patternObject.num_matches;
    this.column_coverage = patternObject.num_matches.doubleValue() / num_values;
    this.hit_score = this.num_concepts_detected / this.num_master_lists.doubleValue();
    this.ref_indicator = this.num_unique_values.doubleValue() / this.num_values > 0.96;
    this.calculatePatternPET();
    refined_pet_score = (int) (pet_score * (1 - hit_score));
  }

  public RefinedConcept(
      String dataset,
      String table,
      String column,
      Long columnSize,
      Long columnCardinality,
      Long loadTime,
      Long num_master_lists,
      Long concepts_detected,
      String pattern,
      PatternObject patternObject,
      String visibility,
      ConceptType type) {
    this(
        dataset,
        table,
        column,
        columnSize,
        columnCardinality,
        loadTime,
        num_master_lists,
        concepts_detected,
        pattern,
        patternObject);
    this.visibility = visibility;
    this.type = type;
  }

  public static RefinedConcept betterMatch(RefinedConcept conA, RefinedConcept conB) {
    if (conA.pet_score > conB.pet_score) {
      return conA;
    }
    return conB;
  }

  public static RefinedConcept merge(RefinedConcept conA, RefinedConcept conB) {
    RefinedConcept merged = new RefinedConcept();

    // Only keep dataset, table, and column if they are the same for both concepts
    merged.dataset = conA.dataset.equals(conB.dataset) ? conA.dataset : "";
    merged.table = conA.table.equals(conB.table) ? conA.table : "";
    merged.column = conA.column.equals(conB.column) ? conA.column : "";

    // Sum the num_values and num_matched_concepts
    merged.num_values = conA.num_values + conB.num_values;
    merged.num_matched_concepts = conA.num_matched_concepts + conB.num_matched_concepts;

    // Compute the column coverage
    merged.column_coverage = merged.num_matched_concepts.doubleValue() / merged.num_values;

    // Only keep the concept if they are the same for both concepts
    merged.concept = conA.concept.equals(conB.concept) ? conA.concept : "";

    merged.friendly_name = conA.friendly_name.equals(conB.friendly_name) ? conA.friendly_name : "";

    merged.ref_indicator = conA.ref_indicator || conB.ref_indicator;

    // Only keep the highest PET score
    merged.pet_score = conA.pet_score > conB.pet_score ? conA.pet_score : conB.pet_score;

    // Keep the latest load time
    merged.load_time = conA.load_time > conB.load_time ? conA.load_time : conB.load_time;

    // Keep the largest num_concepts_detected, num_master_lists, hit_score, refined_pet_score
    merged.num_concepts_detected =
        conA.num_concepts_detected > conB.num_concepts_detected
            ? conA.num_concepts_detected
            : conB.num_concepts_detected;
    merged.num_master_lists =
        conA.num_master_lists > conB.num_master_lists
            ? conA.num_master_lists
            : conB.num_master_lists;
    merged.hit_score = conA.hit_score > conB.hit_score ? conA.hit_score : conB.hit_score;
    merged.refined_pet_score =
        conA.refined_pet_score > conB.refined_pet_score
            ? conA.refined_pet_score
            : conB.refined_pet_score;

    merged.topic = conA.topic.isEmpty() ? conB.topic : conA.topic;

    merged.visibility = conA.visibility.equals(conB.visibility) ? conA.visibility : "";

    // Keep the type of the highest PET score; overrides and user defined concepts will have a PET
    // of 100
    merged.type = conA.pet_score > conB.pet_score ? conA.type : conB.type;
    return merged;
  }

  private void calculatePET() {
    pet_score = 25;

    Double pairwiseScore = num_unique_values.doubleValue() / num_possible_concepts;

    if (column_coverage > 0.2
        && pairwiseScore > 0.1
        && pairwiseScore < 1.7
        && concept_coverage > 0.25) {
      this.pet_score = 100;
    } else if (column_coverage > 0.1
        && pairwiseScore > 0.05
        && pairwiseScore < 3.4
        && concept_coverage > 0.125) {
      this.pet_score = 75;
    } else if (column_coverage > 0.05
        && pairwiseScore > 0.025
        && pairwiseScore < 6.8
        && concept_coverage > 0.0625) {
      this.pet_score = 50;
    }
  }

  private void calculatePatternPET() {
    pet_score = 25;

    if (column_coverage > 0.49) {
      this.pet_score = 100;
    } else if (column_coverage > 0.3) {
      this.pet_score = 75;
    } else if (column_coverage > 0.2) {
      this.pet_score = 50;
    }
  }

  public enum ConceptType {
    AUTO,
    OVERRIDE,
    PATTERN,
    USER;

    private static final Map<String, ConceptType> valueMap = new HashMap<>();

    static {
      valueMap.put("auto", AUTO);
      valueMap.put("override", OVERRIDE);
      valueMap.put("pattern", PATTERN);
      valueMap.put("user", USER);
    }

    @JsonCreator
    public static ConceptType forValue(String value) {
      return valueMap.get(value.toLowerCase());
    }

    @JsonValue
    public String toValue() {
      for (Map.Entry<String, ConceptType> entry : valueMap.entrySet()) {
        if (entry.getValue() == this) {
          return entry.getKey();
        }
      }

      return null;
    }
  }
}
