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

public class ResolutionObject implements Comparable<ResolutionObject> {

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

  /** Number of matched concepts values / number of unique values in the column */
  public Double column_coverage;

  /** The type of resolution */
  public String resolution;

  /** Sub type of resolution */
  public String type;

  public ResolutionObject() {}

  public ResolutionObject(RefinedConcept refConcept) {
    dataset = refConcept.dataset;
    table = refConcept.table;
    column = refConcept.column;
    concept = refConcept.concept;
    friendly_name = refConcept.friendly_name;
    column_coverage = refConcept.column_coverage;
  }

  @Override
  public int compareTo(ResolutionObject o) {
    return o.column_coverage > column_coverage ? 1 : -1;
  }
}
