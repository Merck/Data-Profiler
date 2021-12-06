package com.dataprofiler.config.cli;

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

import static java.util.Comparator.comparing;

import com.dataprofiler.util.Config;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** print config params in json, key value, and command line switch formats */
public class ConfigParamsCli {
  private static final Logger logger = LoggerFactory.getLogger(ConfigParamsCli.class);

  private Config config;

  public ConfigParamsCli() {
    super();
  }

  public static void main(String[] args) {
    try {
      ConfigParamsCli configParams = new ConfigParamsCli();
      configParams.printFormats(args);
    } catch (Exception e) {
      e.printStackTrace();
      logger.warn(e.toString());
      System.exit(1);
    }
    System.exit(0);
  }

  public void printFormats(String[] args) throws Exception {
    init(args);
    printJson(config);
    printKeyValue(config);
    printCommandLineArgs(config);
    printAsEnvExports(config);
  }

  public void printJson(Config config) throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    String json = objectMapper.writer(new DefaultPrettyPrinter()).writeValueAsString(config);
    logger.info(json);
  }

  public void printKeyValue(Config config) throws Exception {
    Map<String, Object> names = config.asKeyValues();
    names.entrySet().stream()
        .sorted(comparing(Entry::getKey))
        .forEach(
            entry -> {
              String name = entry.getKey();
              Object value = entry.getValue();
              logger.info(name + ": " + value);
            });
  }

  public void printCommandLineArgs(Config config) throws Exception {
    Map<String, Object> names = config.asKeyValues();
    names.entrySet().stream()
        .sorted(comparing(Entry::getKey))
        .forEach(
            entry -> {
              String name = entry.getKey();
              Object value = entry.getValue();
              logger.info("--" + name + "=" + value);
            });
  }

  public void init(String[] args) throws Exception {
    logger.debug("initializing...");
    config = parseJobArgs(args);
  }

  public void printAsEnvExports(Config config) throws Exception {
    Map<String, Object> names = config.asKeyValues();
    names.entrySet().stream()
        .sorted(comparing(Entry::getKey))
        .forEach(
            entry -> {
              String name = entry.getKey();
              String key = config.camelToSnakeCase(name).toUpperCase();
              Object value = entry.getValue();
              logger.info("export " + key + "=" + value);
            });
  }

  protected Config parseJobArgs(String[] args) throws Exception {
    logger.debug("creating and verifying job config");
    Config config = new Config();
    boolean parsed = false;
    config.loadFromEnvironmentVariables();
    parsed = config.parse(args);
    return config;
  }
}
