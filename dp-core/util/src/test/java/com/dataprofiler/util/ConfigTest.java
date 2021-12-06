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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import org.junit.Test;

public class ConfigTest {

  @Test
  public void testConfig() throws IOException {
    String[] argv = {};
    Config config = new Config();
    config.parse(argv);
    assertEquals(config.accumuloUser, "root");
  }

  @Test
  public void testConfigWithOverride() throws IOException {
    String[] argv = {"--zookeepers", "foo", "--config", "src/test/resources/testconfig.json"};
    Config config = new Config();
    config.parse(argv);
    assertEquals(config.zookeepers, "foo");
    assertEquals(config.accumuloUser, "root");
  }

  @Test
  public void testConfigWithGlobalFile() throws IOException {
    String[] argv = {"--zookeepers", "foo"};
    Config config = new Config();
    config.parse(argv);
    assertEquals(config.zookeepers, "foo");
    assertEquals(config.accumuloUser, "root");
  }

  /**
   * this test expects a env variable to be set
   * ACCUMULO_USER: testAccumuloUser
   *
   * @throws IOException
   * @throws NoSuchFieldException
   * @throws IllegalAccessException
   */
  @Test
  public void testConfigWithEnvParams()
      throws IOException, NoSuchFieldException, IllegalAccessException {
    String[] argv = {};
    Config config = new Config();
    config.parse(argv);
    config.loadFromEnvironmentVariables();
    assertEquals(config.accumuloUser, "testAccumuloUser");
  }
}
