package com.dataprofiler.datasetdelta.diff;

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

import org.apache.commons.text.diff.CommandVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogDiffs implements CommandVisitor<Character> {
  private static final Logger logger = LoggerFactory.getLogger(LogDiffs.class);
  protected Character deltaSymbol = "\uD835\uDEAB".charAt(0);
  protected int DEFAULT_BUILDER_SIZE = 256;
  protected StringBuilder stringBuilder = new StringBuilder(DEFAULT_BUILDER_SIZE);

  @Override
  public void visitInsertCommand(Character s) {
    if (deltaSymbol.equals(s)) {
      if (logger.isTraceEnabled()) {
        logger.trace(format("found delimiter %s", s));
      }
      String insert = stringBuilder.toString();
      stringBuilder = new StringBuilder(DEFAULT_BUILDER_SIZE);
      logger.info("insert: " + insert);
    } else {
      stringBuilder.append(s);
    }
  }

  @Override
  public void visitKeepCommand(Character s) {
    logger.debug("keep: " + s);
  }

  @Override
  public void visitDeleteCommand(Character s) {
    logger.debug("delete: " + s);
  }
}
