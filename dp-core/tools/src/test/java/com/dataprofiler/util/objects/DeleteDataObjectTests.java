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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dataprofiler.MiniAccumuloWithData;
import com.dataprofiler.test.IntegrationTest;
import com.dataprofiler.metadata.CommitMetadata;
import com.dataprofiler.delete.DeleteData;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public class DeleteDataObjectTests {
  private static final String DATASET_A = "basic-test";
  private static final String TABLE_A = "basic_test_data";
  private static final String DATASET_B = "tiny-test";
  private static final String TABLE_B = "tiny_test_data";
  private static final String TABLE_B_COPY = "tiny_test_data_copy";
  private static MiniAccumuloWithData mad;

  @BeforeClass
  public static void setup() throws Exception {
    mad = new MiniAccumuloWithData();
    mad.startForTesting();
    mad.loadDataset(DATASET_A, "src/test/resources/basic_test_data.csv");
    mad.loadDataset(DATASET_B, "src/test/resources/tiny_test_data.csv");
    mad.loadDataset(DATASET_A, "src/test/resources/version2/basic_test_data.csv");
    mad.loadDataset(DATASET_B, "src/test/resources/tiny_test_data_copy.csv");
  }

  @Test
  public void deleteTableB() throws Exception {
    VersionedAllMetadata allMetadata = new VersionedMetadataObject().allMetadata(mad.getContext(), mad.getContext().getCurrentMetadataVersion());
    assertEquals(2, allMetadata.metadata.size());

    assertTrue(allMetadata.metadata.containsKey(DATASET_A));
    assertTrue(allMetadata.metadata.containsKey(DATASET_B));
    VersionedDatasetMetadata b = allMetadata.metadata.get(DATASET_B);
    assertEquals(2, b.getTableMetadata().size());
    assertTrue(b.getTableMetadata().containsKey(TABLE_B));
    assertTrue(b.getTableMetadata().containsKey(TABLE_B_COPY));

    CommitMetadata.Deletions deletions = new CommitMetadata.Deletions();
    deletions.addTable(DATASET_B, TABLE_B);
    DeleteData.softDeleteDatasetsOrTables(mad.getContext(), deletions);

    allMetadata = new VersionedMetadataObject().allMetadata(mad.getContext(), mad.getContext().getCurrentMetadataVersion());
    assertEquals(2, allMetadata.metadata.size());
    System.out.println(allMetadata.metadata.keySet());
    assertTrue(allMetadata.metadata.containsKey(DATASET_A));
    assertTrue(allMetadata.metadata.containsKey(DATASET_B));

    b = allMetadata.metadata.get(DATASET_B);
    assertEquals(1, b.getTableMetadata().size());
    assertFalse(b.getTableMetadata().containsKey(TABLE_B));
    assertTrue(b.getTableMetadata().containsKey(TABLE_B_COPY));

  }
}
