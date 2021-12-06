package com.dataprofiler.loader.config;

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

import com.beust.jcommander.IStringConverter;
import java.util.HashMap;
import java.util.Map;

public class PropertyListConvertor implements IStringConverter<Map<String, String>> {
  public PropertyListConvertor(String optionName) {}

  @Override
  public Map<String, String> convert(String value) {
    Map<String, String> properties = new HashMap<>();
    for (String entry : value.split(",")) {
      String[] parts = entry.split(":");
      if (parts.length != 2) {
        System.err.println("Property was not in the correct format: " + entry);
        continue;
      }
      properties.put(parts[0], parts[1]);
    }

    return properties;
  }
}
