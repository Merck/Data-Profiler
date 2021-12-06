package com.dataprofiler.datasetquality.model;

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

import java.util.Objects;

public enum QualityEnum {
  MISSING_VALUES,
  NULL_VALUES,
  COLUMN_TYPES,
  UNKNOWN;

  public static boolean isNullness(String value) {
    if (Objects.isNull(value)) {
      return false;
    }
    String val = value.toLowerCase().trim();
    return "null".equals(val);
  }

  public static boolean isEmpty(String value) {
    return Objects.nonNull(value) && "".equals(value);
  }

  public static QualityEnum lookup(String value) {
    if (Objects.isNull(value)) {
      return UNKNOWN;
    }

    if (isNullness(value)) {
      return NULL_VALUES;
    } else if (isEmpty(value)) {
      return MISSING_VALUES;
    } else {
      return UNKNOWN;
    }
  }
}
