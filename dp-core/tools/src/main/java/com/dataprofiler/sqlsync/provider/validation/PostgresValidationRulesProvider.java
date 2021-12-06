package com.dataprofiler.sqlsync.provider.validation;

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

import static java.lang.Character.isAlphabetic;
import static java.lang.Character.isDigit;
import static java.lang.String.format;
import static java.util.Objects.isNull;

public class PostgresValidationRulesProvider implements ValidatonRulesProvider {

  public static final int MAX_COLUMN_LEN = 63;
  public static final char DEFAULT_CHAR = '_';

  public PostgresValidationRulesProvider() {
    super();
  }

  @Override
  public boolean isValidColumnName(String name) {
    if (isNull(name)) {
      return false;
    }

    return meetsLengthRequirement(name) && meetsCharacterRequirement(name);
  }

  @Override
  public String normalizeColumnName(String columnName) {
    if (isNull(columnName)) {
      return format("%s", DEFAULT_CHAR);
    }

    if (!meetsLengthRequirement(columnName)) {
      int end = Math.min(columnName.length(), MAX_COLUMN_LEN);
      columnName = columnName.substring(0, end);
    }

    StringBuilder builder = new StringBuilder(MAX_COLUMN_LEN * 2);
    for (int i = 0; i < columnName.length(); i++) {
      char current = columnName.charAt(i);
      if (isValidColumnNameChar(current)) {
        builder.append(current);
      } else {
        builder.append(DEFAULT_CHAR);
      }
    }
    return builder.toString();
  }

  protected boolean meetsLengthRequirement(String columnName) {
    return columnName.length() <= MAX_COLUMN_LEN;
  }

  protected boolean meetsCharacterRequirement(String columnName) {
    boolean meetsFirstCharRequirement = false;
    char firstChar = columnName.charAt(0);
    if (isAlphabetic(firstChar) || firstChar == '_') {
      meetsFirstCharRequirement = true;
    }

    boolean meetsCharacterRequirements = false;
    for (int i = 0; i < columnName.length(); i++) {
      char current = columnName.charAt(i);
      if (isValidColumnNameChar(current)) {
        meetsCharacterRequirements = true;
      } else {
        meetsCharacterRequirements = false;
        break;
      }
    }
    return meetsFirstCharRequirement && meetsCharacterRequirements;
  }

  protected boolean isValidColumnNameChar(char current) {
    return isAlphabetic(current) || isDigit(current) || current == '_';
  }
}
