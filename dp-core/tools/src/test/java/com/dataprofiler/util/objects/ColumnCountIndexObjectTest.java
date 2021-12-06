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
import com.dataprofiler.util.objects.DataScanSpec.Type;
import com.dataprofiler.util.objects.VersionedDatasetMetadata.MissingMetadataException;
import com.dataprofiler.test.IntegrationTest;
import com.dataprofiler.util.Const;
import java.io.IOException;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public class ColumnCountIndexObjectTest {

  private static final String DATASET_A = "basic-test";
  private static final String TABLE_A = "basic_test_data";
  private static final String DATASET_B = "tiny-test";
  private static final String TABLE_B = "tiny_test_data";
  private static final String TABLE_C = "tiny_test_data_copy";
  private static MiniAccumuloWithData mad;
  private static MetadataVersionObject firstVersion;
  private static MetadataVersionObject secondVersion;
  private static Map<String, String> activeTablesFirstVersion;
  private static Map<String, String> activeTablesSecondVersion;

  @BeforeClass
  public static void setup() throws Exception {
    mad = new MiniAccumuloWithData();
    mad.startForTesting();
    mad.loadDataset(DATASET_A, "src/test/resources/basic_test_data.csv");
    mad.loadDataset(DATASET_B, "src/test/resources/tiny_test_data.csv");
    firstVersion = mad.getContext().getCurrentMetadataVersion();
    mad.loadDataset(DATASET_A, "src/test/resources/version2/basic_test_data.csv");
    mad.loadDataset(DATASET_B, "src/test/resources/tiny_test_data_copy.csv");
    secondVersion = mad.getContext().getCurrentMetadataVersion();

    activeTablesFirstVersion =
        new VersionedMetadataObject().allTableNamesForVersion(mad.getContext(), firstVersion);

    activeTablesSecondVersion =
        new VersionedMetadataObject().allTableNamesForVersion(mad.getContext(), secondVersion);

//    VersionedAllMetadata metadata = new VersionedMetadataObject().allMetadata(mad.getContext(), firstVersion);
  }

  @AfterClass
  public static void teardown() throws IOException, InterruptedException {
    mad.close();
  }

  @Test
  public void testV1() {

    int i = 0;
    for (ColumnCountIndexObject c :
        new ColumnCountIndexObject()
            .find(
                mad.getContext(), activeTablesFirstVersion, "georgia", Const.INDEX_GLOBAL, true)) {
      i++;
      if (c.getDataset().equals(DATASET_A)) {
        assertTrue(activeTablesFirstVersion.containsKey(DATASET_A + "\u0000" + c.getTableId()));
        assertFalse(activeTablesSecondVersion.containsKey(DATASET_A + "\u0000" + c.getTableId()));
        assertEquals(c.getTable(), TABLE_A);
      } else {
        assertTrue(activeTablesFirstVersion.containsKey(DATASET_B + "\u0000" + c.getTableId()));
        assertTrue(activeTablesSecondVersion.containsKey(DATASET_B + "\u0000" + c.getTableId()));
        assertEquals(TABLE_B, c.getTable());
      }
    }
    assertEquals(2, i);
  }

  @Test
  public void testV2() {

    int i = 0;
    for (ColumnCountIndexObject c :
        new ColumnCountIndexObject()
            .find(
                mad.getContext(), activeTablesSecondVersion, "georgia", Const.INDEX_GLOBAL, true)) {
      i++;
      if (c.getDataset().equals(DATASET_A)) {
        assertFalse(activeTablesFirstVersion.containsKey(DATASET_A + "\u0000" + c.getTableId()));
        assertTrue(activeTablesSecondVersion.containsKey(DATASET_A + "\u0000" + c.getTableId()));
        assertEquals(TABLE_A, c.getTable());
      } else if (c.getTable().equals(TABLE_B)) {
        assertTrue(activeTablesFirstVersion.containsKey(DATASET_B + "\u0000" + c.getTableId()));
        assertTrue(activeTablesSecondVersion.containsKey(DATASET_B + "\u0000" + c.getTableId()));
        assertEquals(TABLE_B, c.getTable());
      } else {
        assertFalse(activeTablesFirstVersion.containsKey(DATASET_B + "\u0000" + c.getTableId()));
        assertTrue(activeTablesSecondVersion.containsKey(DATASET_B + "\u0000" + c.getTableId()));
        assertEquals(TABLE_C, c.getTable());
      }
    }
    assertEquals(3, i);
  }

  @Test
  public void testGlobalLevelSearch() throws MissingMetadataException {
    DataScanSpec spec = new DataScanSpec();
    spec.setType(Type.SEARCH);
    spec.addTerm("georgia");
    VersionedDataScanSpec versionedSpec =
        new VersionedDataScanSpec(mad.getContext(), secondVersion, spec);

    int i = 0;
    for (ColumnCountIndexObject c :
        new ColumnCountIndexObject().find(mad.getContext(), versionedSpec)) {
      i++;
      if (c.getDataset().equals(DATASET_A)) {
        assertFalse(activeTablesFirstVersion.containsKey(DATASET_A + "\u0000" + c.getTableId()));
        assertTrue(activeTablesSecondVersion.containsKey(DATASET_A + "\u0000" + c.getTableId()));
        assertEquals(TABLE_A, c.getTable());
      } else if (c.getTable().equals(TABLE_B)) {
        assertTrue(activeTablesFirstVersion.containsKey(DATASET_B + "\u0000" + c.getTableId()));
        assertTrue(activeTablesSecondVersion.containsKey(DATASET_B + "\u0000" + c.getTableId()));
        assertEquals(TABLE_B, c.getTable());
      } else {
        assertFalse(activeTablesFirstVersion.containsKey(DATASET_B + "\u0000" + c.getTableId()));
        assertTrue(activeTablesSecondVersion.containsKey(DATASET_B + "\u0000" + c.getTableId()));
        assertEquals(TABLE_C, c.getTable());
      }
    }
    assertEquals(3, i);
  }

  @Test
  public void testDatasetLevelSearch() throws MissingMetadataException {
    DataScanSpec spec = new DataScanSpec();
    spec.setType(Type.SEARCH);
    spec.addTerm("georgia");
    spec.setDataset(DATASET_A);
    VersionedDataScanSpec versionedSpec =
        new VersionedDataScanSpec(mad.getContext(), secondVersion, spec);

    int i = 0;
    for (ColumnCountIndexObject c :
        new ColumnCountIndexObject().find(mad.getContext(), versionedSpec)) {
      i++;
      assertEquals(DATASET_A, c.getDataset());
      assertFalse(activeTablesFirstVersion.containsKey(DATASET_A + "\u0000" + c.getTableId()));
      assertTrue(activeTablesSecondVersion.containsKey(DATASET_A + "\u0000" + c.getTableId()));
      assertEquals(TABLE_A, c.getTable());
    }
    assertEquals(1, i);
  }

  @Test
  public void testTableLevelSearch() throws MissingMetadataException {
    DataScanSpec spec = new DataScanSpec();
    spec.setType(Type.SEARCH);
    spec.addTerm("georgia");
    spec.setDataset(DATASET_B);
    spec.setTable(TABLE_B);
    VersionedDataScanSpec versionedSpec =
        new VersionedDataScanSpec(mad.getContext(), secondVersion, spec);

    int i = 0;
    for (ColumnCountIndexObject c :
        new ColumnCountIndexObject().find(mad.getContext(), versionedSpec)) {
      i++;
      assertEquals(DATASET_B, c.getDataset());
      assertTrue(activeTablesSecondVersion.containsKey(DATASET_B + "\u0000" + c.getTableId()));
      assertEquals(TABLE_B, c.getTable());
    }
    assertEquals(1, i);
  }

  @Test
  public void testColumnLevelSearch() throws MissingMetadataException {
    DataScanSpec spec = new DataScanSpec();
    spec.setType(Type.SEARCH);
    spec.addTerm("good");
    spec.setDataset(DATASET_B);
    spec.setTable(TABLE_B);
    spec.setColumn("rating");
    VersionedDataScanSpec versionedSpec =
        new VersionedDataScanSpec(mad.getContext(), secondVersion, spec);

    int i = 0;
    for (ColumnCountIndexObject c :
        new ColumnCountIndexObject().find(mad.getContext(), versionedSpec)) {
      i++;
      assertEquals(DATASET_B, c.getDataset());
      assertTrue(activeTablesSecondVersion.containsKey(DATASET_B + "\u0000" + c.getTableId()));
      assertEquals(TABLE_B, c.getTable());
      assertEquals(2L, c.getCount().longValue());
    }
    assertEquals(1, i);
  }
}
