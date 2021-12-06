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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.dataprofiler.MiniAccumuloWithData;
import com.dataprofiler.test.IntegrationTest;
import com.dataprofiler.util.Const.SortOrder;
import com.dataprofiler.util.objects.VersionedDatasetMetadata.MissingMetadataException;
import com.dataprofiler.util.objects.ui.ValueCountObject;
import com.dataprofiler.querylang.json.Expressions;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public class DataObjectsTest {
  private static final String DATASET_A = "basic-test";
  private static final String TABLE_A = "basic_test_data";
  private static final String DATASET_B = "tiny-test";
  private static final String TABLE_B = "tiny_test_data";
  private static MiniAccumuloWithData mad;
  private static MetadataVersionObject firstVersion;
  private static MetadataVersionObject secondVersion;

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
  }

  @AfterClass
  public static void teardown() throws IOException, InterruptedException {
    mad.close();
  }

  void checkRows(VersionedDataScanSpec versionedSpec, int numRows, int numCols) {
    int i = 0;
    HashSet<Long> ids = new HashSet<>(2048);
    for (DatawaveRowObject row : new DatawaveRowObject().find(mad.getContext(), versionedSpec)) {
      i++;
    }
    assertEquals(numRows, i);
  }

  @Test
  public void testBooleanLogic() throws Throwable {
    DataScanSpec spec = new DataScanSpec();
    spec.setDataset(DATASET_A);
    spec.setTable(TABLE_A);

    VersionedTableMetadata tableMetadata =
        new VersionedMetadataObject()
            .allMetadataForTable(mad.getContext(), firstVersion, DATASET_A, TABLE_A);


    VersionedDataScanSpec vSpec;
    spec.setV2Query(Expressions.parse("{'$eq': {'type': 'string', 'column': 'Central/Outlying County', 'value': 'Central'}}"));
    vSpec = new VersionedDataScanSpec(tableMetadata, spec);
    checkRows(vSpec, 1339, -1);

    // Verify we get teh same record count if we wrap the above $eq clause in an $and or $or
    spec.setV2Query(Expressions.parse("{'$and': [{'$eq': {'type': 'string', 'column': 'Central/Outlying County', 'value': 'Central'}}]}"));
    vSpec = new VersionedDataScanSpec(tableMetadata, spec);
    checkRows(vSpec, 1339, -1);

    spec.setV2Query(Expressions.parse("{'$or': [{'$eq': {'type': 'string', 'column': 'Central/Outlying County', 'value': 'Central'}}]}"));
    vSpec = new VersionedDataScanSpec(tableMetadata, spec);
    checkRows(vSpec, 1339, -1);

    // The 'Central' subtree contains all of the items in the 'Aguadilla-Isabela' subtree, so we
    // expect the same number of results as before
    spec.setV2Query(Expressions.parse("{ '$or': [{'$eq': {'type': 'string', 'column': 'Central/Outlying County', 'value': 'Central'}}, " +
                              "{'$eq': {'type': 'string', 'column': 'CBSA Title', 'value': 'Aguadilla-Isabela, PR'}}]}"));
    vSpec = new VersionedDataScanSpec(tableMetadata, spec);
    checkRows(vSpec, 1339, -1);

    // Intersecting here shall only return the 'Aguadilla-Isabela' results
    spec.setV2Query(Expressions.parse("{ '$and': [{'$eq': {'type': 'string', 'column': 'Central/Outlying County', 'value': 'Central'}}, " +
                               "{'$eq': {'type': 'string', 'column': 'CBSA Title', 'value': 'Aguadilla-Isabela, PR'}}]}"));
    vSpec = new VersionedDataScanSpec(tableMetadata, spec);
    checkRows(vSpec, 9, -1);

    // We want to verify the filtering works
    spec.setV2Query(Expressions.parse("{'$and':[{'$eq':{'column':'Central/Outlying County','value':'Central'}},{'$gte':{'column':'FIPS State Code','value':'00'}},{'$lte':{'column':'FIPS State Code','value':'20'}}]}"));
    vSpec = new VersionedDataScanSpec(tableMetadata, spec);
    checkRows(vSpec, 289, -1);

    // We want to verify that paging works
    spec.setV2Query(Expressions.parse("{'$and':[{'$eq':{'column':'Central/Outlying County','value':'Central'}},{'$gte':{'column':'FIPS State Code','value':'00'}},{'$lte':{'column':'FIPS State Code','value':'20'}}]}"));
    vSpec = new VersionedDataScanSpec(tableMetadata, spec);
    vSpec.setPageSize(25);
    checkRows(vSpec, 289, -1);

    // Intersecting here shall only return the 'Aguadilla-Isabela' results. Note we're querying on 'aguadilla-isbella' to test tokenization
    spec.setV2Query(Expressions.parse("{ '$and': [{'$eq': {'type': 'string', 'column': 'Central/Outlying County', 'value': 'Central'}}, " +
        "{'$eq': {'type': 'string', 'column': 'CBSA Title', 'value': 'aguadilla'}}]}"));
    vSpec = new VersionedDataScanSpec(tableMetadata, spec);
    checkRows(vSpec, 9, -1);
  }

  @Test
  public void basicRowsFiltering() throws MissingMetadataException {
    DataScanSpec spec = new DataScanSpec();
    spec.setDataset(DATASET_A);
    spec.setTable(TABLE_A);

    // This is really basic - but this just checks that we can get the default data
    // version and a specific one and the shape of the rows looks about right and total number
    // is right.

    // Second version
    VersionedDataScanSpec secondVersionedSpec = new VersionedDataScanSpec(mad.getContext(), spec);
    checkRows(secondVersionedSpec, 982, 11);

    // First version
    VersionedTableMetadata tableMetadata =
        new VersionedMetadataObject()
            .allMetadataForTable(mad.getContext(), firstVersion, DATASET_A, TABLE_A);
    VersionedDataScanSpec firstVersionedSpec = new VersionedDataScanSpec(tableMetadata, spec);
    checkRows(firstVersionedSpec, 1882, 12);
  }

  @Test
  public void testColumnCounts() {
    MetadataVersionObject version = mad.getContext().getCurrentMetadataVersion();
    VersionedMetadataObject colMetadata =
        new VersionedMetadataObject()
            .fetchColumn(mad.getContext(), version, DATASET_A, TABLE_A, "State Name");

    List<ValueCountObject> countsA =
        new ColumnCountObject()
            .fetchColumnPaged(mad.getContext(), colMetadata, 0, 25, SortOrder.CNT_ASC, true, false);

    assertEquals(25, countsA.size());

    List<ValueCountObject> countsB =
        new ColumnCountObject()
            .fetchColumnPaged(mad.getContext(), colMetadata, 25, 50, SortOrder.CNT_ASC, true, false);
    assertEquals(25, countsB.size());

    for (int i = 0; i < countsA.size(); i++) {
      assertNotEquals(countsA.get(i).n, countsB.get(i).n);
    }
  }
}
