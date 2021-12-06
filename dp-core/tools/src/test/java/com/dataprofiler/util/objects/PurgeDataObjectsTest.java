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
import com.dataprofiler.delete.DeleteData;
import com.dataprofiler.util.Context;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashSet;
import java.util.List;

@Category(IntegrationTest.class)
public class PurgeDataObjectsTest {
  private static final String DATASET_A = "basic-test";
  private static final String TABLE_A = "basic_test_data";
  private static final String DATASET_B = "basic-test2";
  private static final String TABLE_B = "tiny_test_data";
  private static final String TABLE_B_COPY = "tiny_test_data_copy";

  public <T extends AccumuloObject> long count(ObjectScannerIterable<T> scanner) {
    long i = 0;
    for (T o : scanner.setBatch(true)) {
      i++;
    }
    return i;
  }

  private void assertSingleDataset(Context context, MetadataVersionObject version, String dataset) {
    for (VersionedMetadataObject o :
        new VersionedMetadataObject().scan(context, version)) {
      assertEquals(dataset, o.dataset_name);
    }

    for (ColumnCountObject c : new ColumnCountObject().scan(context)) {
      assertEquals(dataset, c.dataset);
    }

    for (DatawaveRowObject d : new DatawaveRowObject().scan(context)) {
      assertEquals(dataset, d.getDatasetName());
    }

    // TODO - when these objects are working, add these tests back
    // for (DatawaveRowShardIndexObject d: new
    // DatawaveRowShardIndexObject().scan(context))
    // {
    // assertEquals(dataset, d.getDatasetName());
    // }

    for (ColumnCountIndexObject c : new ColumnCountIndexObject().scan(context)) {
      assertEquals(dataset, c.getDataset());
    }

    for (ColumnCountPaginationObject c: new ColumnCountPaginationObject().scan(context)) {
      assertEquals(dataset, c.getColumnCountObj().dataset);
    }

    for (ElementAlias e : new ElementAlias().scan(context)) {
      assertEquals(dataset, e.getDataset());
    }

    for (ColumnCountObject s : new ColumnCountSampleObject().scan(context)) {
      assertEquals(dataset, s.dataset);
    }
  }

  @Test
  public void purgeCurrentDataset() throws Exception {
    MiniAccumuloWithData mad = new MiniAccumuloWithData();
    mad.startForTesting();
    mad.loadDataset(DATASET_A, "src/test/resources/basic_test_data.csv");
    mad.loadDataset(DATASET_B, "src/test/resources/tiny_test_data.csv");

    MetadataVersionObject version = mad.getContext().getCurrentMetadataVersion();
    DeleteData.purgeDataset(mad.getContext(), version, DATASET_A, true, true);

    assertSingleDataset(mad.getContext(), version, DATASET_B);

    // Now delete the other table and make certain that everything is gone

    DeleteData.purgeDataset(mad.getContext(), version, DATASET_B, true, true);

    for (VersionedMetadataObject o : new VersionedMetadataObject().scan(mad.getContext(), version)) {
      assertTrue(false);
    }

    for (ColumnCountObject c : new ColumnCountObject().scan(mad.getContext())) {
      assertTrue(false);
    }

    for (DatawaveRowObject d : new DatawaveRowObject().scan(mad.getContext())) {
      assertTrue(false);
    }

    for (ColumnCountIndexObject c : new ColumnCountIndexObject().scan(mad.getContext())) {
      assertTrue(false);
    }

    for (ColumnCountPaginationObject c: new ColumnCountPaginationObject().scan(mad.getContext())) {
      assertTrue(false);
    }

    for (ElementAlias e : new ElementAlias().scan(mad.getContext())) {
      assertTrue(false);
    }

    for (ColumnCountObject s : new ColumnCountSampleObject().scan(mad.getContext())) {
      assertTrue(false);
    }
  }

  @Test
  public void purgeAllVersionsButCurrentOfDataset() throws Exception {
    MiniAccumuloWithData mad = new MiniAccumuloWithData();
    mad.startForTesting();
    mad.loadDataset(DATASET_A, "src/test/resources/basic_test_data.csv");
    mad.loadDataset(DATASET_A, "src/test/resources/version2/basic_test_data.csv");
    mad.loadDataset(DATASET_A, "src/test/resources/basic_test_data.csv");

    // Make certain we have 3 copies of TABLE_A
    List<TableVersionInfo> tables = TableVersionInfo.allVersionsOfTable(mad.getContext(), DATASET_A, TABLE_A);
    assertEquals(3, tables.size());

    // purge all but the current version of DATASET_A
    DeleteData.purgeDatasetAllButCurrentVersions(mad.getContext(), DATASET_A, true, true);
    assertSingleDataset(mad.getContext(), mad.getContext().getCurrentMetadataVersion(), DATASET_A);

    String tableId = tables.get(0).table_id;
    assertSingleTableVersion(mad.getContext(), DATASET_A, tableId);
  }

  @Test
  public void purgeAllVersionsOfDataset() throws Exception {
    MiniAccumuloWithData mad = new MiniAccumuloWithData();
    mad.startForTesting();
    mad.loadDataset(DATASET_A, "src/test/resources/basic_test_data.csv");
    mad.loadDataset(DATASET_A, "src/test/resources/version2/basic_test_data.csv");
    mad.loadDataset(DATASET_A, "src/test/resources/basic_test_data.csv");
    mad.loadDataset(DATASET_B, "src/test/resources/tiny_test_data.csv");

    // Make certain we have 3 copies of TABLE_A
    List<TableVersionInfo> tables = TableVersionInfo.allVersionsOfTable(mad.getContext(), DATASET_A, TABLE_A);
    assertEquals(3, tables.size());

    // purge all of the DATASET_A
    DeleteData.purgeDatasetAllVersions(mad.getContext(), DATASET_A, true, true);
    assertSingleDataset(mad.getContext(), mad.getContext().getCurrentMetadataVersion(), DATASET_B);
  }

  public void assertSingleTableVersion(Context context, String dataset, String tableId) {
    for (VersionedMetadataObject o :
        new VersionedMetadataObject().scan(context)) {
        if (o.dataset_name.equals(dataset) && o.metadata_level != VersionedMetadataObject.DATASET) {
          assertEquals(tableId, o.table_id);
        }
    }

    for (ColumnCountObject c : new ColumnCountObject().scan(context)) {
      if (c.dataset.equals(dataset)) {
        assertEquals(tableId, c.tableId);
      }
    }

    for (DatawaveRowObject d : new DatawaveRowObject().scan(context)) {
      if (d.getDatasetName().equals(dataset)) {
        assertEquals(tableId, d.getTableName());
      }
    }
  }

  @Test
  public void purgeSingleVersionOfTable() throws Exception {
    MiniAccumuloWithData mad = new MiniAccumuloWithData();
    mad.startForTesting();
    mad.loadDataset(DATASET_A, "src/test/resources/basic_test_data.csv");
    mad.loadDataset(DATASET_A, "src/test/resources/version2/basic_test_data.csv");
    mad.loadDataset(DATASET_A, "src/test/resources/basic_test_data.csv");

    // Make certain we have 3 copies of TABLE_A
    List<TableVersionInfo> tables = TableVersionInfo.allVersionsOfTable(mad.getContext(), DATASET_A, TABLE_A);
    assertEquals(3, tables.size());

    // Now delete 1 version of the table
    DeleteData.purgeTableVersions(mad.getContext(), DATASET_A, TABLE_A, tables.get(1).table_id, DeleteData.PurgeVersions.All, true, true);

    HashSet<String> tableIds = new HashSet<>();
    tableIds.add(tables.get(0).table_id);
    tableIds.add(tables.get(2).table_id);

    // Make certain only the other table ids are present
    for (VersionedMetadataObject o : new VersionedMetadataObject().scan(mad.getContext())) {
      if (o.dataset_name.equals(DATASET_A) && o.metadata_level != VersionedMetadataObject.DATASET) {
        assertTrue(tableIds.contains(o.table_id));
      }
    }

    for (ColumnCountObject c : new ColumnCountObject().scan(mad.getContext())) {
      if (c.dataset.equals(DATASET_A)) {
        assertTrue(tableIds.contains(c.getTableId()));
      }
    }

    for (DatawaveRowObject d : new DatawaveRowObject().scan(mad.getContext())) {
      if (d.getDatasetName().equals(DATASET_A)) {
        assertTrue(tableIds.contains(d.getTableName()));
      }
    }
  }

  @Test
  public void purgePreviousVersionsOfTable() throws Exception {
    MiniAccumuloWithData mad = new MiniAccumuloWithData();
    mad.startForTesting();
    mad.loadDataset(DATASET_A, "src/test/resources/basic_test_data.csv");
    mad.loadDataset(DATASET_A, "src/test/resources/version2/basic_test_data.csv");
    mad.loadDataset(DATASET_A, "src/test/resources/basic_test_data.csv");

    // Make certain we have 3 copies of TABLE_A
    List<TableVersionInfo> tables = TableVersionInfo.allVersionsOfTable(mad.getContext(), DATASET_A, TABLE_A);
    assertEquals(3, tables.size());
    String tableId = tables.get(0).table_id;

    // Now delete all but the current version
    DeleteData.purgeTableVersions(mad.getContext(), DATASET_A, TABLE_A, tableId, DeleteData.PurgeVersions.Previous, true, true);

    assertSingleTableVersion(mad.getContext(), DATASET_A, tableId);
  }
}
