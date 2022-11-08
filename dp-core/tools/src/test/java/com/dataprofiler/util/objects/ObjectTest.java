package com.dataprofiler.util.objects;

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

import com.dataprofiler.MiniAccumuloWithData;
import com.dataprofiler.util.MiniAccumuloContext;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class ObjectTest {
  private static MiniAccumuloWithData mad;
  private static MiniAccumuloContext context;

  @BeforeClass
  public static void createContext()
      throws Exception {
    mad = new MiniAccumuloWithData();
    HashMap<String, List<String>> datasets = new HashMap<>();
    datasets.put("basic-test", Arrays.asList("src/test/resources/basic_test_data.csv"));
    context = mad.startForTesting();
    mad.loadDatasets(datasets);
  }

  @AfterClass
  public static void destroyContext() {
    context = null;
  }

}
