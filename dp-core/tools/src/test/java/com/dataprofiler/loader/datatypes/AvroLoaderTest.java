package com.dataprofiler.loader.datatypes;

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
import com.dataprofiler.util.objects.ColumnCountIndexObject;
import com.dataprofiler.util.objects.MetadataVersionObject;
import com.dataprofiler.util.objects.VersionedDatasetMetadata.MissingMetadataException;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import com.dataprofiler.util.Const;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AvroLoaderTest {
  private static Logger logger = Logger.getLogger(AvroLoaderTest.class);

  private static MiniAccumuloWithData mad;

  private static final String DATASET = "src/test/resources/userdata.avro";
  private static MetadataVersionObject firstVersion;
  private static Map<String, String> activeTablesFirstVersion;

  @BeforeClass
  public static void setUp() throws Exception {
    String[] rootAuths = new String[]{
        "hr.manager.karl \\\"the big bad wolf\\\" o'mac",
        "LIST.Nick_1",
        "LIST.PUBLIC_DATA",
    };
    mad = new MiniAccumuloWithData(rootAuths);
    logger.info("Starting MiniAccumuloCluster");
    mad.startForTesting();
    mad.loadDataset(DATASET, "src/test/resources/userdata.avro");
    firstVersion = mad.getContext().getCurrentMetadataVersion();
    activeTablesFirstVersion =
        new VersionedMetadataObject().allTableNamesForVersion(mad.getContext(), firstVersion);

  }

  @AfterClass
  public static void tearDown() throws IOException, InterruptedException {
    mad.close();
  }

  @Test
  public void testSuccessfulLoad() throws MissingMetadataException {
    logger.info("test avro load");

    int i = 0;
    for (ColumnCountIndexObject idx: new ColumnCountIndexObject()
        .find(mad.getContext(), activeTablesFirstVersion, "cburns4@miitbeian.gov.cn",
            Const.INDEX_GLOBAL, true)) {
      System.out.println("index count: " + idx.getCount());
      i += idx.getCount();
    }
    assertEquals(1, i);
  }

  // TODO ADD MORE TESTS

}
