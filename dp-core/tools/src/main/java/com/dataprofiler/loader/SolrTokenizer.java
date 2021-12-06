package com.dataprofiler.loader;

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

import com.dataprofiler.util.Const;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class SolrTokenizer {

  // probably need to make this an external resource
  private static final List<String> STOPWORDS =
      ImmutableList.copyOf(
          new String[] {
            "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is",
            "it", "no", "not", "of", "on", "or", "s", "such", "t", "that", "the", "their", "then",
            "there", "these", "they", "this", "to", "was", "will", "with"
          });

  public static Set<String> tokenize(String input) throws IOException {
    StandardTokenizer tokenizer = new StandardTokenizer();
    tokenizer.setReader(new StringReader(input));
    LowerCaseFilter lcFilter = new LowerCaseFilter(tokenizer);
    StopFilter stopwords = new StopFilter(lcFilter, StopFilter.makeStopSet(STOPWORDS));
    Set<String> tokens = new HashSet<>();
    try {
      stopwords.reset();
      while (stopwords.incrementToken()) {
        CharTermAttribute attr = stopwords.getAttribute(CharTermAttribute.class);
        String token = attr.toString();
        if (token.length() >= Const.MIN_TOKEN_LENGTH) {
          tokens.add(attr.toString());
        }
      }
    } finally {
      stopwords.close();
    }
    return tokens;
  }

  public static void main(String[] args) throws Exception {
    System.out.println(tokenize("aguadilla-isabela, pr"));
  }
}
