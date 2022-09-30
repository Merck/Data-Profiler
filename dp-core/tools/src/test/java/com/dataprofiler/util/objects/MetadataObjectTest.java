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

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.dataprofiler.MiniAccumuloWithData;
import com.dataprofiler.test.IntegrationTest;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Const;
import com.dataprofiler.util.Const.Origin;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public class MetadataObjectTest {

  private static final Logger logger = Logger.getLogger(MetadataObjectTest.class);
  private static MiniAccumuloWithData mad;
  private static MetadataVersionObject zeroVersion;
  private static MetadataVersionObject firstVersion;

  private static final String DATASET_A = "basic-test";
  private static final String TABLE_A = "basic_test_data";
  private static final String TABLE_B = "tiny_test_data";

  @BeforeClass
  public static void setup() throws Exception {
    mad = new MiniAccumuloWithData();
    mad.startForTesting();
    mad.loadDataset(DATASET_A, "src/test/resources/basic_test_data.csv");
    zeroVersion = mad.getContext().getCurrentMetadataVersion();
    mad.loadDataset(DATASET_A, "src/test/resources/tiny_test_data.csv");
    firstVersion = mad.getContext().getCurrentMetadataVersion();
  }

  @AfterClass
  public static void teardown() throws IOException, InterruptedException {
    mad.close();
  }

  @Test
  public void sanity() throws BasicAccumuloException {
    Set<String> tables = mad.getContext().getClient().tableOperations().list();
    logger.info(tables);
    assertTrue(tables.size() > 0);
  }

  @Test
  public void testMetadataObject() throws Exception {
    VersionedDatasetMetadata metadata = new VersionedMetadataObject()
        .allMetadataForDataset(mad.getContext(),
            firstVersion, DATASET_A);
    checkFirstMetadata(metadata);
  }

  public void checkFirstMetadata(VersionedDatasetMetadata metadata) {
    boolean found_dataset = false;
    boolean found_table = false;
    boolean found_column = false;
    for (VersionedMetadataObject m : new VersionedMetadataObject()
        .scanAllLevelsForDataset(mad.getContext(),
            firstVersion, DATASET_A)) {
      switch (m.metadata_level) {
        case VersionedMetadataObject.DATASET:
          found_dataset = true;
          break;
        case VersionedMetadataObject.TABLE:
          found_table = true;
          break;
        case VersionedMetadataObject.COLUMN:
          found_column = true;
          break;
      }
      assertEquals(DATASET_A, m.dataset_name);
    }
    assertTrue(found_dataset);
    assertTrue(found_table);
    assertTrue(found_column);

    assertEquals(firstVersion.getId(), metadata.getDatasetMetadata().version_id);
    assertEquals(VersionedMetadataObject.DATASET, metadata.getDatasetMetadata().metadata_level);
    assertEquals(DATASET_A, metadata.getDatasetMetadata().dataset_name);
    assertEquals(2, metadata.getDatasetMetadata().num_tables);
    assertEquals(14, metadata.getDatasetMetadata().num_columns);
    assertEquals(22590, metadata.getDatasetMetadata().num_values);

    assertEquals(metadata.getDatasetMetadata(),
        new VersionedMetadataObject().fetchDataset(mad.getContext(), firstVersion, DATASET_A));

    List<VersionedMetadataObject> allDatasets = StreamSupport
        .stream(new VersionedMetadataObject().scanDatasets(mad.getContext(), firstVersion)
            .spliterator(), true)
        .collect(Collectors.toList());
    assertEquals(1, allDatasets.size());
    assertEquals(metadata.getDatasetMetadata(), allDatasets.get(0));

    Assert.assertEquals(Origin.UPLOAD.getCode(),
        metadata.getDatasetMetadata().properties.get(Const.METADATA_PROPERTY_ORIGIN));

    for (VersionedMetadataObject m : new VersionedMetadataObject()
        .scanDatasetsAndTables(mad.getContext(),
            firstVersion)) {
      assertNotEquals(VersionedMetadataObject.COLUMN, m.metadata_level);
    }

    assertEquals(2, metadata.getTableMetadata().size());
    VersionedMetadataObject table = metadata.getTableMetadata().get(TABLE_A);
    assertEquals(firstVersion.getId(), table.version_id);
    assertEquals(VersionedMetadataObject.TABLE, table.metadata_level);
    assertEquals(DATASET_A, table.dataset_name);
    assertEquals(TABLE_A, table.table_name);
    assertEquals(12, table.num_columns);
    assertEquals(22584, table.num_values);
    assertEquals(Origin.UPLOAD.getCode(), table.properties.get(Const.METADATA_PROPERTY_ORIGIN));

    assertEquals(table, new VersionedMetadataObject()
        .fetchTable(mad.getContext(), firstVersion, DATASET_A, TABLE_A));

    List<VersionedMetadataObject> allTables = StreamSupport
        .stream(new VersionedMetadataObject().scanTables(mad.getContext(), firstVersion, DATASET_A)
            .spliterator(), true)
        .collect(Collectors.toList());
    assertEquals(2, allTables.size());

    assertEquals(14, metadata.getColumnMetadata().size());

    Map<String, VersionedMetadataObject> allColumns = StreamSupport.stream(
        new VersionedMetadataObject()
            .scanColumns(mad.getContext(), firstVersion, DATASET_A, TABLE_A).spliterator(),
        true)
        .collect(Collectors.toMap(VersionedMetadataObject::getColumn_name, Function.identity()));

    for (VersionedMetadataObject c : metadata.getColumnMetadata().values()) {
      // only test TABLE_A
      if (!c.table_name.equals(TABLE_A)) {
        continue;
      }
      assertEquals(VersionedMetadataObject.COLUMN, c.metadata_level);
      assertEquals(firstVersion.getId(), c.version_id);
      assertEquals(c,
          new VersionedMetadataObject()
              .fetchColumn(mad.getContext(), firstVersion, DATASET_A, TABLE_A, c.column_name));

      assertEquals(c, allColumns.get(c.column_name));
      switch (c.column_num) {
        case 0:
          assertEquals("CBSA Code", c.column_name);
          assertEquals(929, c.num_unique_values);
          assertEquals(1882, c.num_values);
          break;
        case 1:
          assertEquals("Metro Division Code", c.column_name);
          assertEquals(32, c.num_unique_values);
          assertEquals(1882, c.num_values);
          break;

        case 2:
          assertEquals("CSA Code", c.column_name);
          assertEquals(170, c.num_unique_values);
          assertEquals(1882, c.num_values);
          break;

        case 3:
          assertEquals("CBSA Title", c.column_name);
          assertEquals(929, c.num_unique_values);
          assertEquals(1882, c.num_values);
          break;

        case 4:
          assertEquals("Metropolitan/Micropolitan Statistical Area", c.column_name);
          assertEquals(2, c.num_unique_values);
          assertEquals(1882, c.num_values);
          break;

        case 5:
          assertEquals("Metropolitan Division Title", c.column_name);
          assertEquals(32, c.num_unique_values);
          assertEquals(1882, c.num_values);
          break;

        case 6:
          assertEquals("CSA Title", c.column_name);
          assertEquals(170, c.num_unique_values);
          assertEquals(1882, c.num_values);
          break;

        case 7:
          assertEquals("County/County Equivalent", c.column_name);
          assertEquals(1299, c.num_unique_values);
          assertEquals(1882, c.num_values);
          break;

        case 8:
          assertEquals("State Name", c.column_name);
          assertEquals(52, c.num_unique_values);
          assertEquals(1882, c.num_values);
          break;

        case 9:
          assertEquals("FIPS State Code", c.column_name);
          assertEquals(52, c.num_unique_values);
          assertEquals(1882, c.num_values);
          break;

        case 10:
          assertEquals("FIPS County Code", c.column_name);
          assertEquals(238, c.num_unique_values);
          assertEquals(1882, c.num_values);
          break;

        case 11:
          assertEquals("Central/Outlying County", c.column_name);
          assertEquals(2, c.num_unique_values);
          assertEquals(1882, c.num_values);
          break;
        default:
          assertTrue(false);
      }
    }

    int i = 0;
    for (VersionedMetadataObject m : new VersionedMetadataObject()
        .scanColumns(mad.getContext(), firstVersion, DATASET_A, TABLE_B)) {
      assertEquals(TABLE_B, m.table_name);
      i++;
    }
    assertEquals(2, i);
  }

  @Test
  public void testSortedColumnsForTable() {
    VersionedDatasetMetadata metadata = new VersionedMetadataObject()
        .allMetadataForDataset(mad.getContext(),
            firstVersion, DATASET_A);
    List<String> columnNames = metadata.sortedColumnNames(TABLE_A);

    List<String> names = Arrays.asList("CBSA Code", "Metro Division Code", "CSA Code", "CBSA Title",
        "Metropolitan/Micropolitan Statistical Area", "Metropolitan Division Title", "CSA Title",
        "County/County Equivalent", "State Name", "FIPS State Code", "FIPS County Code",
        "Central/Outlying County");

    assertEquals(names.size(), columnNames.size());

    for (int i = 0; i < names.size(); i++) {
      assertEquals(names.get(i), columnNames.get(i));
    }
  }

  @Test
  public void testTableMetadata() {
    VersionedTableMetadata table = new VersionedMetadataObject()
        .allMetadataForTable(mad.getContext(), firstVersion,
            DATASET_A, TABLE_A);

    assertEquals(firstVersion.getId(), table.getVersion().getId());

    assertEquals(firstVersion.getId(), table.getTable().version_id);
    assertEquals(DATASET_A, table.getTable().dataset_name);
    assertEquals(TABLE_A, table.getTable().table_name);

    for (VersionedMetadataObject m : table.getColumns().values()) {
      assertEquals(firstVersion.getId(), m.version_id);
      assertEquals(DATASET_A, m.dataset_name);
      assertEquals(TABLE_A, m.table_name);
    }
  }

  @Test
  public void testMetadataUpdate() throws Exception {
    mad.loadDataset(DATASET_A, "src/test/resources/version2/basic_test_data.csv");
    MetadataVersionObject secondVersion = mad.getContext().getCurrentMetadataVersion();
    System.out.println(secondVersion);
    assertNotEquals(secondVersion.getId(), firstVersion.getId());
    assertEquals(secondVersion.getPreviousId(), firstVersion.getId());
    for (MetadataVersionObject m : new MetadataVersionObject().scan(mad.getContext())) {
      System.out.println(m);
    }

    VersionedAllMetadata allMetadata = new VersionedMetadataObject()
        .allMetadata(mad.getContext(), secondVersion);

    assertEquals(1, allMetadata.metadata.size());

    VersionedDatasetMetadata metadata = new VersionedMetadataObject()
        .allMetadataForDataset(mad.getContext(),
            firstVersion, DATASET_A);

    // Check the first version again to make sure it's still the same
    checkFirstMetadata(metadata);

    metadata = new VersionedMetadataObject()
        .allMetadataForDataset(mad.getContext(), secondVersion, DATASET_A);

    assertEquals(secondVersion.getId(), metadata.getDatasetMetadata().version_id);
    assertEquals(VersionedMetadataObject.DATASET, metadata.getDatasetMetadata().metadata_level);
    assertEquals(DATASET_A, metadata.getDatasetMetadata().dataset_name);
    assertEquals(2, metadata.getDatasetMetadata().num_tables);
    assertEquals(13, metadata.getDatasetMetadata().num_columns);
    assertEquals(10808, metadata.getDatasetMetadata().num_values);

    assertEquals(Origin.UPLOAD.getCode(),
        metadata.getDatasetMetadata().properties.get(Const.METADATA_PROPERTY_ORIGIN));

    assertEquals(2, metadata.getTableMetadata().size());
    VersionedMetadataObject table = metadata.getTableMetadata().values().iterator().next();
    assertEquals(secondVersion.getId(), table.version_id);
    assertEquals(VersionedMetadataObject.TABLE, table.metadata_level);
    assertEquals(DATASET_A, table.dataset_name);
    assertEquals(11, table.num_columns);
    assertEquals(10802, table.num_values);
    assertEquals(Origin.UPLOAD.getCode(), table.properties.get(Const.METADATA_PROPERTY_ORIGIN));

    assertEquals(13, metadata.getColumnMetadata().size());

    for (VersionedMetadataObject c : metadata.getColumnMetadata().values()) {
      // Only test TABLE_A
      if (!c.table_name.equals(TABLE_A)) {
        continue;
      }
      assertEquals(VersionedMetadataObject.COLUMN, c.metadata_level);
      assertEquals(secondVersion.getId(), c.version_id);
      switch (c.column_num) {
        case 0:
          assertEquals("CBSA Code", c.column_name);
          assertEquals(496, c.num_unique_values);
          assertEquals(982, c.num_values);
          break;

        case 1:
          assertEquals("Metro Division Code", c.column_name);
          assertEquals(14, c.num_unique_values);
          assertEquals(982, c.num_values);
          break;

        case 2:
          assertEquals("CSA Code", c.column_name);
          assertEquals(144, c.num_unique_values);
          assertEquals(982, c.num_values);
          break;

        case 3:
          assertEquals("CBSA Title", c.column_name);
          assertEquals(496, c.num_unique_values);
          assertEquals(982, c.num_values);
          break;

        case 4:
          assertEquals("Metropolitan/Micropolitan Statistical Area", c.column_name);
          assertEquals(2, c.num_unique_values);
          assertEquals(982, c.num_values);
          break;

        case 5:
          assertEquals("Metropolitan Division Title", c.column_name);
          assertEquals(14, c.num_unique_values);
          assertEquals(982, c.num_values);
          break;

        case 6:
          assertEquals("CSA Title", c.column_name);
          assertEquals(144, c.num_unique_values);
          assertEquals(982, c.num_values);
          break;

        case 7:
          assertEquals("County/County Equivalent", c.column_name);
          assertEquals(703, c.num_unique_values);
          assertEquals(982, c.num_values);
          break;

        case 8:
          assertEquals("State Name", c.column_name);
          assertEquals(50, c.num_unique_values);
          assertEquals(982, c.num_values);
          break;

        case 9:
          assertEquals("FIPS State Code", c.column_name);
          assertEquals(50, c.num_unique_values);
          assertEquals(982, c.num_values);
          break;

        case 10:
          assertEquals("FIPS County Code", c.column_name);
          assertEquals(176, c.num_unique_values);
          assertEquals(982, c.num_values);
          break;
        default:
          assertTrue(false);
      }
    }

    // Test column name search - this should really be a separate test, but it
    // depends on the
    // second dataset being loaded and I don't want to order the test right now.

    // This column name is not in the second version
    String columnName = "Central/Outlying County";

    for (VersionedMetadataObject m : new VersionedMetadataObject()
        .searchColumnNames(mad.getContext(), secondVersion,
            columnName, false)) {
      System.out.println(m);
      assertTrue(false);
    }

    int i = 0;
    for (VersionedMetadataObject m : new VersionedMetadataObject()
        .searchColumnNames(mad.getContext(), firstVersion,
            "Central/Outlying County", false)) {
      i++;
      assertEquals(columnName, m.getColumn_name());
    }

    assertEquals(1, i);

    i = 0;
    for (VersionedMetadataObject m : new VersionedMetadataObject()
        .searchTableNames(mad.getContext(), firstVersion,
            TABLE_A, false)) {
      i++;
      assertEquals(TABLE_A, m.getTable_name());
      assertEquals(DATASET_A, m.getDataset_name());
    }
    assertEquals(1, i);

    i = 0;
    for (VersionedMetadataObject m: new VersionedMetadataObject().scanTableAllVersions(mad.getContext(), DATASET_A, TABLE_A)) {
      i++;
      assertEquals(TABLE_A, m.getTable_name());
      assertEquals(DATASET_A, m.getDataset_name());
      assertEquals(VersionedMetadataObject.TABLE, m.metadata_level);
    }
    assertEquals(3, i);


    List<MetadataVersionObject> metadataVersions = new MetadataVersionObject().allMetadataVersions(mad.getContext());
    assertEquals(3, metadataVersions.size());
    assertEquals(secondVersion.id, metadataVersions.get(0).id);
    assertEquals(firstVersion.id, metadataVersions.get(1).id);
    assertEquals(zeroVersion.id, metadataVersions.get(2).id);

    i = 0;
    for (VersionedMetadataObject m: new VersionedMetadataObject().scanDatasetAllVersions(mad.getContext(), DATASET_A)) {
      i++;
      assertEquals(DATASET_A, m.getDataset_name());
      assertEquals(VersionedMetadataObject.DATASET, m.metadata_level);
    }
    assertEquals(3, i);

    i = 0;
    HashSet<String> tableSet = new HashSet<>();
    tableSet.add(TABLE_A);
    tableSet.add(TABLE_B);
    for (VersionedMetadataObject m: new VersionedMetadataObject().scanAllTablesAllVersions(mad.getContext(), DATASET_A)) {
      i++;
      assertTrue(tableSet.contains(m.table_name));
    }
    assertEquals(5, i);
  }

  @Test
  public void fetchcolumn() {
    String columnName = "Central/Outlying County";
    VersionedMetadataObject column = new VersionedMetadataObject()
        .fetchColumn(mad.getContext(), firstVersion,
            DATASET_A, TABLE_A, columnName);
    assertEquals(DATASET_A, column.getDataset_name());
    assertEquals(TABLE_A, column.getTable_name());
    assertEquals(columnName, column.getColumn_name());
    assertEquals(firstVersion.getId(), column.version_id);
  }

  @Test
  public void testSearchColumnNames() {
//    String columnTerm = "Division";
    String columnTerm = "Metro";
    ObjectScannerIterable<VersionedMetadataObject> columns = new VersionedMetadataObject()
        .searchColumnNames(mad.getContext(), firstVersion, columnTerm, false);
    int count = 0;
    for (VersionedMetadataObject metadataObject : columns) {
      count++;
      if (logger.isDebugEnabled()) {
        logger.debug(format("%d: %s", count, metadataObject.toString()));
      }
    }
    logger.info(format("found %d column(s) with term [%s]", count, columnTerm));
    assertEquals(count, 3);
  }

  @Test
  public void testSearchColumnNamesWithArray() {
    List<String> columnTerms = Arrays.asList("Metro", "Division");
    ObjectScannerIterable<VersionedMetadataObject> columns = new VersionedMetadataObject()
        .searchColumnNames(mad.getContext(), firstVersion, columnTerms, false);
    int count = 0;
    for (VersionedMetadataObject metadataObject : columns) {
      count++;
      if (logger.isDebugEnabled()) {
        logger.debug(format("%d: %s", count, metadataObject.toString()));
      }
    }
    logger.info(format("found %d column(s) with term %s", count, columnTerms));
    assertEquals(count, 2);
  }

  @Test
  public void testSearchTableNames() {
    String tableTerm = "basic_t";
    ObjectScannerIterable<VersionedMetadataObject> tables = new VersionedMetadataObject()
        .searchTableNames(mad.getContext(), firstVersion, tableTerm, false);
    int count = 0;
    for (VersionedMetadataObject metadataObject : tables) {
      count++;
      if (logger.isDebugEnabled()) {
        logger.debug(format("%d: %s", count, metadataObject.toString()));
      }
    }
    logger.info(format("found %d tables(s) with term [%s]", count, tableTerm));
    assertEquals(count, 1);
  }

  @Test
  public void testSearchTableNamesWithArray() {
    List<String> tableTerms = Arrays.asList("basic", "test");
    ObjectScannerIterable<VersionedMetadataObject> tables = new VersionedMetadataObject()
        .searchTableNames(mad.getContext(), firstVersion, tableTerms, false);
    int count = 0;
    for (VersionedMetadataObject metadataObject : tables) {
      count++;
      if (logger.isDebugEnabled()) {
        logger.debug(format("%d: %s", count, metadataObject.toString()));
      }
    }
    logger.info(format("found %d tables(s) with term %s", count, tableTerms));
    assertEquals(count, 1);
  }

  @Test
  public void testSearchDatasetNames() {
    String datasetTerm = "basic-t";
    ObjectScannerIterable<VersionedMetadataObject> datasets = new VersionedMetadataObject()
        .searchDatasetNames(mad.getContext(), firstVersion, datasetTerm, false);
    int count = 0;
    for (VersionedMetadataObject metadataObject : datasets) {
      count++;
      if (logger.isDebugEnabled()) {
        logger.debug(format("%d: %s", count, metadataObject.toString()));
      }
    }
    logger.info(format("found %d dataset(s) with term [%s]", count, datasetTerm));
    assertEquals(count, 1);
  }

  @Test
  public void testSearchDatasetNamesWithArray() {
    List<String> datasetTerms = Arrays.asList("basic", "test");
    ObjectScannerIterable<VersionedMetadataObject> datasets = new VersionedMetadataObject()
        .searchDatasetNames(mad.getContext(), firstVersion, datasetTerms, false);
    int count = 0;
    for (VersionedMetadataObject metadataObject : datasets) {
      count++;
      if (logger.isDebugEnabled()) {
        logger.debug(format("%d: %s", count, metadataObject.toString()));
      }
    }
    logger.info(format("found %d dataset(s) with term %s", count, datasetTerms));
    assertEquals(count, 1);
  }
}
