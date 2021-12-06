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

public class PatternObject {

  /** The number of times a pattern was matched */
  public Long num_uniq_matches;

  /** The number of values in the column that matched the pattern */
  public Long num_matches;

  public PatternObject() {
    num_uniq_matches = 0L;
    num_matches = 0L;
  }

  public PatternObject(Long uniqMatches, Long matches) {
    num_uniq_matches = uniqMatches;
    num_matches = matches;
  }

  public static PatternObject combinePatterns(PatternObject p1, PatternObject p2) {
    PatternObject combined = new PatternObject();

    combined.num_uniq_matches = p1.num_uniq_matches + p2.num_uniq_matches;
    combined.num_matches = p1.num_matches = p2.num_matches;

    return combined;
  }
}
