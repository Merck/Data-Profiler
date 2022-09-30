package com.dataprofiler.util;

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

import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Base64;
import org.apache.accumulo.core.util.TextUtil;
import org.apache.hadoop.io.Text;

public class TextEncoding {
  /**
   * Serialize an array of Text objects to a base 64 encoded string
   *
   * @param values An array of Text objects
   * @return A base 64 encoded string
   */
  public static String encodeStrings(Text[] values) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < values.length; i++) {
      sb.append(Base64.getEncoder().encodeToString(TextUtil.getBytes(values[i])));
      sb.append('\n');
    }
    return sb.toString();
  }

  /**
   * Deserialize a base 64 encoded string to an array of Text objects
   *
   * @param values A base 64 encoded string
   * @return An array of Text objects
   */
  public static Text[] decodeStrings(String values) {
    String[] columnStrings = values.split("\n", -1);
    Text[] columnTexts = new Text[columnStrings.length - 1];
    for (int i = 0; i < columnStrings.length - 1; i++) {
      columnTexts[i] = new Text(Base64.getDecoder().decode(columnStrings[i].getBytes(UTF_8)));
    }
    return columnTexts;
  }
}
