package com.dataprofiler.util.functional;

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

import com.dataprofiler.MiniAccumuloWithData;
import com.dataprofiler.util.objects.ColumnCountIndexObject;
import com.dataprofiler.util.objects.DataScanSpec;
import com.dataprofiler.util.objects.DataScanSpec.Type;
import com.dataprofiler.util.objects.MetadataVersionObject;
import com.dataprofiler.util.objects.VersionedDataScanSpec;
import com.dataprofiler.util.objects.VersionedDatasetMetadata.MissingMetadataException;
import com.dataprofiler.util.MiniAccumuloContext;
import com.dataprofiler.test.IntegrationTest;
import org.apache.accumulo.core.security.Authorizations;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Category(IntegrationTest.class)
public class ColumnCountIndexVisibilityTest {
  private static final String DATASET_B = "tiny-test";
  private static final String TABLE_B = "cell_level_visibilities_test_data";
  private static MiniAccumuloWithData mad;
  private static MiniAccumuloContext context;
  private static MetadataVersionObject version;
  private static Map<String, String> activeTablesVersion;

  private static final String[] rootAuths = new String[]{
      "hr.manager.karl \\\"the big bad wolf\\\" o'mac",
      "LIST.Nick_1",
      "LIST.PUBLIC_DATA",
      "LIST.HR",
      "LIST.TOPFLOOR",
      "LIST.TECHMANAGER",
      "LIST.SALESMANAGER",
      "LIST.EMPLOYEE"
  };

  @BeforeClass
  public static void setup() throws Exception {
    mad = new MiniAccumuloWithData(rootAuths);
    context = mad.startForTesting();
    mad.loadDataset(DATASET_B, "src/test/resources/cell_level_visibilities_test_data.csv");
    version = mad.getContext().getCurrentMetadataVersion();
  }

  @AfterClass
  public static void teardown() throws IOException, InterruptedException {
    mad.close();
  }

  @Test
  public void testGlobalLevelSearch() throws MissingMetadataException {
    DataScanSpec spec = new DataScanSpec();
    spec.setType(Type.SEARCH);
    spec.addTerm("h hunte");

    VersionedDataScanSpec versionedSpec =
        new VersionedDataScanSpec(mad.getContext(), version, spec);

    Map<String, Long> expectedOne = new HashMap<String, Long>() {{
      put(DATASET_B + TABLE_B + "Name" + "H HUNTER", 1L);
    }};

    mad.getContext().setAuthorizations(new Authorizations(rootAuths));
    testResults(versionedSpec, expectedOne);

    mad.getContext().setAuthorizations(
        new Authorizations("LIST.PUBLIC_DATA", "LIST.HR", "LIST.TOPFLOOR", "LIST.EMPLOYEE"));
    testResults(versionedSpec, expectedOne);

    mad.getContext()
        .setAuthorizations(new Authorizations("LIST.PUBLIC_DATA", "LIST.HR", "LIST.EMPLOYEE"));
    testResults(versionedSpec, expectedOne);

    mad.getContext().setAuthorizations(
        new Authorizations("LIST.PUBLIC_DATA", "LIST.TOPFLOOR", "LIST.EMPLOYEE"));
    testResults(versionedSpec, expectedOne);

    mad.getContext().setAuthorizations(new Authorizations("LIST.PUBLIC_DATA"));
    testResults(versionedSpec, new HashMap<>());

    mad.getContext().setAuthorizations(new Authorizations("LIST.PUBLIC_DATA", "LIST.TOPFLOOR"));
    testResults(versionedSpec, new HashMap<>());

    mad.getContext().setAuthorizations(new Authorizations("LIST.PUBLIC_DATA", "LIST.HR"));
    testResults(versionedSpec, new HashMap<>());
  }

  @Test
  public void testGlobalLevelSearchWithRowColumnVis()
      throws MissingMetadataException {
    DataScanSpec spec = new DataScanSpec();
    spec.setType(Type.SEARCH);
    spec.addTerm("j ceo");

    VersionedDataScanSpec versionedSpec = new VersionedDataScanSpec(mad.getContext(), version,
        spec);

    mad.getContext().setAuthorizations(new Authorizations(rootAuths));
    testResults(versionedSpec, new HashMap<String, Long>() {{
      put(DATASET_B + TABLE_B + "Manager" + "J CEO", 3L);
      put(DATASET_B + TABLE_B + "Name" + "J CEO", 2L);
    }});

    mad.getContext().setAuthorizations(
        new Authorizations("LIST.PUBLIC_DATA", "LIST.HR", "LIST.TOPFLOOR", "LIST.EMPLOYEE"));
    testResults(versionedSpec, new HashMap<String, Long>() {{
      put(DATASET_B + TABLE_B + "Manager" + "J CEO", 3L);
      put(DATASET_B + TABLE_B + "Name" + "J CEO", 2L);
    }});

    mad.getContext().setAuthorizations(new Authorizations("LIST.PUBLIC_DATA", "LIST.EMPLOYEE"));
    testResults(versionedSpec, new HashMap<String, Long>() {{
      put(DATASET_B + TABLE_B + "Manager" + "J CEO", 3L);
      put(DATASET_B + TABLE_B + "Name" + "J CEO", 1L);
    }});

    mad.getContext().setAuthorizations(new Authorizations("LIST.PUBLIC_DATA", "LIST.HR"));
    testResults(versionedSpec, new HashMap<String, Long>() {{
      put(DATASET_B + TABLE_B + "Manager" + "J CEO", 1L);
      put(DATASET_B + TABLE_B + "Name" + "J CEO", 1L);
    }});

    mad.getContext().setAuthorizations(new Authorizations("LIST.PUBLIC_DATA"));
    testResults(versionedSpec, new HashMap<String, Long>() {{
      put(DATASET_B + TABLE_B + "Manager" + "J CEO", 1L);
      put(DATASET_B + TABLE_B + "Name" + "J CEO", 1L);
    }});
  }

  @Test
  public void testDatasetLevelSearch() throws MissingMetadataException {
    DataScanSpec spec = new DataScanSpec();
    spec.setType(Type.SEARCH);
    spec.setDataset(DATASET_B);
    spec.addTerm("123-456");

    VersionedDataScanSpec versionedSpec = new VersionedDataScanSpec(mad.getContext(), version,
        spec);
    mad.getContext().setAuthorizations(new Authorizations(rootAuths));
    testResults(versionedSpec, new HashMap<String, Long>() {{
      put(DATASET_B + TABLE_B + "Phone" + "123-456-7890", 1L);
      put(DATASET_B + TABLE_B + "Phone" + "123-456-0987", 1L);
      put(DATASET_B + TABLE_B + "Phone" + "123-456-1010", 1L);
    }});
  }

  @Test
  public void testTableLevelSearch() throws MissingMetadataException {
    DataScanSpec spec = new DataScanSpec();
    spec.setType(Type.SEARCH);
    spec.setDataset(DATASET_B);
    spec.setTable(TABLE_B);
    spec.addTerm("t");

    VersionedDataScanSpec versionedSpec = new VersionedDataScanSpec(mad.getContext(), version,
        spec);
    mad.getContext().setAuthorizations(new Authorizations(rootAuths));
    testResults(versionedSpec, new HashMap<String, Long>() {{
      put(DATASET_B + TABLE_B + "Name" + "T BOSS", 1L);
      put(DATASET_B + TABLE_B + "Manager" + "T BOSS", 1L);
      put(DATASET_B + TABLE_B + "Name" + "T GUY", 1L);
      put(DATASET_B + TABLE_B + "Name" + "T LEADER", 1L);
      put(DATASET_B + TABLE_B + "Manager" + "T LEADER", 1L);
      put(DATASET_B + TABLE_B + "Location" + "TX", 3L);
    }});
  }

  @Test
  public void testColumnLevelSearch() throws MissingMetadataException {
    DataScanSpec spec = new DataScanSpec();
    spec.setType(Type.SEARCH);
    spec.setDataset(DATASET_B);
    spec.setTable(TABLE_B);
    spec.setColumn("Salary");
    spec.addTerm("5");

    VersionedDataScanSpec versionedSpec = new VersionedDataScanSpec(mad.getContext(), version,
        spec);

    mad.getContext().setAuthorizations(new Authorizations(rootAuths));
    testResults(versionedSpec, new HashMap<String, Long>() {{
      put(DATASET_B + TABLE_B + "Salary" + "50000", 2L);
    }});

    mad.getContext().setAuthorizations(
        new Authorizations("LIST.PUBLIC_DATA", "LIST.TECHMANAGER", "LIST.SALESMANAGER"));
    testResults(versionedSpec, new HashMap<String, Long>() {{
      put(DATASET_B + TABLE_B + "Salary" + "50000", 2L);
    }});

    mad.getContext().setAuthorizations(new Authorizations("LIST.PUBLIC_DATA", "LIST.SALESMANAGER"));
    testResults(versionedSpec, new HashMap<String, Long>() {{
      put(DATASET_B + TABLE_B + "Salary" + "50000", 1L);
    }});

    mad.getContext().setAuthorizations(new Authorizations("LIST.PUBLIC_DATA", "LIST.TECHMANAGER"));
    testResults(versionedSpec, new HashMap<String, Long>() {{
      put(DATASET_B + TABLE_B + "Salary" + "50000", 1L);
    }});

    mad.getContext().setAuthorizations(new Authorizations("LIST.PUBLIC_DATA", "LIST.EMPLOYEE"));
    testResults(versionedSpec, new HashMap<>());

    mad.getContext().setAuthorizations(new Authorizations());
    testResults(versionedSpec, new HashMap<>());
  }

  private void testResults(VersionedDataScanSpec spec, Map<String, Long> expected) {
    int i = 0;
    for (ColumnCountIndexObject c : new ColumnCountIndexObject().find(mad.getContext(), spec)) {
      i++;
      assertEquals(DATASET_B, c.getDataset());
      assertEquals(TABLE_B, c.getTable());
      assertEquals(
          expected.get(c.getDataset() + c.getTable() + c.getColumn() + c.getValue()).longValue(),
          c.getCount().longValue());
    }
    assertEquals(expected.size(), i);
  }
}
