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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dataprofiler.MiniAccumuloWithData;
import com.dataprofiler.test.IntegrationTest;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.objects.CustomAnnotation.AnnotationType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.dataprofiler.util.MiniAccumuloContext;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A series of tests to exercise the search methods for CustomAnnotations. A fresh Accumulo load is
 * created on class load and not per test, this is to speed up the runtime
 */
@Category(IntegrationTest.class)
public class CustomAnnotationTest {
  private static final Logger logger = LoggerFactory.getLogger(CustomAnnotationTest.class);

  private static MiniAccumuloWithData mad;
  private static MiniAccumuloContext context;
  private static Map<String, String> datasets;
  private static Map<String, List<CustomAnnotation>> loaded;
  private CustomAnnotation scanner;

  @BeforeClass
  public static void createContext() throws Exception {
    datasets = new HashMap<>();
    datasets.put("ds1", "src/test/resources/annotation_test_data_ds1.csv");
    datasets.put("ds2", "src/test/resources/annotation_test_data_ds2.csv");
    mad = new MiniAccumuloWithData();
    context = mad.startForTesting();
    List<String> tables = context.getConfig().getAllTables();
    logger.info(tables.toString());
    CustomAnnotationCsvLoader csvLoader = new CustomAnnotationCsvLoader(context);
    loaded = csvLoader.loadTestFiles(datasets);
    logger.info(loaded.toString());
  }

  @AfterClass
  public static void destroyContext() throws IOException, InterruptedException {
    mad.close();
    mad = null;
    context = null;
    datasets.clear();
    datasets = null;
  }

  @Before
  public void setup() {
    this.scanner = CustomAnnotationMock.scannerWithMock();
  }

  @After
  public void teardown() {
    this.scanner = null;
  }

  @Test
  public void sanity() throws BasicAccumuloException {
    Set<String> tables = context.getConnector().tableOperations().list();
    logger.debug("table " + tables);
    assertTrue(tables.size() > 0);
  }

  @Test
  public void testFromEntry() {
    String json =
        "{\"visibility\":\"\",\"timestamp\":0,\"annotationType\":\"DATASET\","
            + "\"dataset\":\"ds1\",\"table\":\"\",\"column\":\"\",\"note\":\"this is a note on ds1\","
            + "\"createdBy\":\"created by garfield\","
            + "\"createdOn\":1579882669175,\"uuid\":\"e72f8aa8-c5f4-4b03-a973-d2fd1cfb3339\"}";
    Value value = new Value(json.getBytes(StandardCharsets.UTF_8));
    Map.Entry<Key, Value> entry = new AbstractMap.SimpleEntry<>(new Key("key1"), value);
    CustomAnnotation ca = new CustomAnnotation();
    CustomAnnotation deserializedAnnotation = ca.fromEntry(entry);
    assertNotNull(deserializedAnnotation);
    assertNotNull(deserializedAnnotation.getUuid());
    assertEquals(
        deserializedAnnotation.getUuid(), UUID.fromString("e72f8aa8-c5f4-4b03-a973-d2fd1cfb3339"));
    assertNotNull(deserializedAnnotation.getAnnotationType());
    Assert.assertEquals(deserializedAnnotation.getAnnotationType(), AnnotationType.DATASET);
    assertNotNull(deserializedAnnotation.getDataset());
    assertEquals(deserializedAnnotation.getDataset(), "ds1");
  }

  @Test
  public void testSearchByDatasets() {
    assertNotNull(datasets);
    assertFalse(datasets.isEmpty());
    datasets.keySet().forEach(this::searchByDataset);
  }

  protected void searchByDataset(String dataset) {
    List<CustomAnnotation> insertions = loaded.get(dataset);
    UUID firstId = insertions.get(0).getUuid();
    String id = firstId.toString();
    if (logger.isDebugEnabled()) {
      logger.debug("searching dataset: " + dataset);
      logger.debug("search id: " + id);
      logger.debug("search auths: " + context.getAuthorizations());
    }
    CustomAnnotation annotation = CustomAnnotation.fetchDatasetAnnotation(context, dataset, id);
    logger.debug(annotation.toString());
    assertNotNull(annotation);
    assertKeys(annotation);
    assertEquals(annotation.getDataset(), dataset);
    assertEquals(annotation.getUuid().toString(), id);
    assertNotNull(annotation.getNote());
    assertTrue(annotation.getNote().contains(dataset));
    assertNotNull(annotation.getCreatedBy());
    assertTrue(annotation.getCreatedOn() > 0);
  }

  @Test
  public void testSearchHierarchyAnnotationsByDataset() throws Exception {
    assertNotNull(datasets);
    assertFalse(datasets.isEmpty());
    int count = 0;
    Set<String> datasetsFound = new HashSet<>();
    String datasetName = "hierarchy-ds-1";
    logger.info("search from dataset: " + datasetName);
    for (CustomAnnotation annotation : scanner.scanHierarchyByDataset(context, datasetName)) {
      if (logger.isTraceEnabled()) {
        logger.trace("annotation " + annotation.toString());
      }
      assertNotNull(annotation);
      assertNotNull(annotation.getAnnotationType());
      assertKeys(annotation);
      String curDataset = annotation.getDataset();
      assertNotNull(curDataset);
      datasetsFound.add(curDataset);
      count++;
    }
    logger.debug("found " + count + " items");
    assertEquals(3, count);

    assertNotNull(datasetsFound);
    assertFalse(datasetsFound.size() <= 0);
    assertTrue(datasetsFound.contains(datasetName));
  }

  @Test
  public void testSearchHierarchyAnnotationsByDatasetPartialMatch() {
    assertNotNull(datasets);
    assertFalse(datasets.isEmpty());
    int count = 0;
    String datasetName = "hierarchy-ds";
    logger.info("search from dataset: " + datasetName);
    for (CustomAnnotation annotation : scanner.scanHierarchyByDataset(context, datasetName)) {
      logger.info("annotation " + annotation.toString());
      count++;
    }
    logger.debug("found " + count + " items");
    // this should be 0, we dont want partial matchs
    assertEquals(0, count);
  }

  @Test
  public void testSearchHierarchyAnnotationsByTable() {
    assertNotNull(datasets);
    assertFalse(datasets.isEmpty());
    int count = 0;
    Set<String> datasetsFound = new HashSet<>();
    String datasetName = "hierarchy-ds-1";
    String tableName = "table1";
    logger.info("search from dataset: " + datasetName);
    for (CustomAnnotation annotation :
        scanner.scanHierarchyByTable(context, datasetName, tableName)) {
      if (logger.isTraceEnabled()) {
        logger.trace("annotation " + annotation.toString());
      }
      assertNotNull(annotation);
      assertNotNull(annotation.getAnnotationType());
      assertKeys(annotation);
      String curDataset = annotation.getDataset();
      assertNotNull(curDataset);
      datasetsFound.add(curDataset);
      count++;
    }
    logger.debug("found " + count + " items");
    assertEquals(1, count);

    assertNotNull(datasetsFound);
    assertFalse(datasetsFound.size() <= 0);
    assertTrue(datasetsFound.contains(datasetName));
  }

  @Test
  public void testSearchAllDatasetAnnotations() {
    assertNotNull(datasets);
    assertFalse(datasets.isEmpty());
    int count = 0;
    Set<String> datasetsFound = new HashSet<>();
    for (CustomAnnotation annotation : scanner.scanDatasets(context)) {
      assertNotNull(annotation);
      assertNotNull(annotation.getAnnotationType());
      assertEquals(annotation.getAnnotationType(), AnnotationType.DATASET);
      assertKeys(annotation);
      String curDataset = annotation.getDataset();
      assertNotNull(curDataset);
      datasetsFound.add(curDataset);
      count++;
    }
    logger.debug("found " + count + " items");
    assertEquals(count, 6);

    assertNotNull(datasetsFound);
    assertFalse(datasetsFound.size() <= 0);
    assertTrue(datasetsFound.containsAll(Arrays.asList("ds1", "ds2", "hierarchy-ds-1")));
  }

  @Test
  public void testSearchAllTableAnnotations() {
    int count = 0;
    Set<String> tablesFound = new HashSet<>();
    for (CustomAnnotation annotation : scanner.scanTables(context)) {
      assertNotNull(annotation);
      assertNotNull(annotation.getAnnotationType());
      assertEquals(annotation.getAnnotationType(), AnnotationType.TABLE);
      assertKeys(annotation);
      assertNotNull(annotation.getDataset());
      String curDataset = annotation.getDataset();
      if (curDataset.startsWith("hierarchy")) {
        continue;
      }
      String table = annotation.getTable();
      assertNotNull(table);
      tablesFound.add(table);
      assertTrue(table.startsWith(curDataset));
      assertFalse(annotation.getNote().isEmpty());
      assertTrue(annotation.getNote().contains(curDataset.replace("-", ".")));
      count++;
    }
    logger.debug("found " + count + " items");
    assertEquals(count, 4);

    assertNotNull(tablesFound);
    assertFalse(tablesFound.size() <= 0);
    logger.debug("tables found: " + tablesFound.toString());
    assertTrue(tablesFound.containsAll(Arrays.asList("ds1-t1", "ds1-t2", "ds2-t1")));
  }

  @Test
  public void testFilterAnnotationsByDataset() {
    assertNotNull(datasets);
    assertFalse(datasets.isEmpty());
    datasets
        .keySet()
        .forEach(
            dataset -> {
              logger.debug("searching dataset " + dataset);
              int count = 0;
              for (CustomAnnotation annotation : scanner.scanByDataset(context, dataset)) {
                assertNotNull(annotation);
                assertNotNull(annotation.getAnnotationType());
                assertEquals(annotation.getAnnotationType(), AnnotationType.DATASET);
                assertKeys(annotation);
                assertTrue(annotation.getDataset().contains(dataset));
                count++;
              }
              logger.debug("found " + count + " items");
              assertEquals(count, 2);
            });
  }

  @Test
  public void testFilterAnnotationsByTable() {
    assertNotNull(datasets);
    assertFalse(datasets.isEmpty());
    datasets
        .keySet()
        .forEach(
            dataset -> {
              int count = 0;
              String table = dataset + "-t1";
              logger.debug("searching dataset " + dataset);
              for (CustomAnnotation annotation : scanner.scanByTable(context, dataset, table)) {
                assertNotNull(annotation);
                assertNotNull(annotation.getAnnotationType());
                assertEquals(annotation.getAnnotationType(), AnnotationType.TABLE);
                assertKeys(annotation);
                assertTrue(annotation.getDataset().contains(dataset));
                assertFalse(annotation.getNote().isEmpty());
                assertTrue(annotation.getTable().startsWith(dataset));
                assertTrue(annotation.getNote().contains(dataset.replace("-", ".")));
                count++;
              }
              logger.debug("found " + count + " items");
              switch (dataset) {
                case "ds1":
                  assertEquals(count, 2);
                  break;
                case "ds2":
                  assertEquals(count, 1);
                  break;
                default:
                  logger.warn("unknown dataset, ignoring count test");
              }
            });
  }

  private void assertKeys(CustomAnnotation annotation) {
    assertNotNull(annotation);
    CustomAnnotation.AnnotationType type = annotation.getAnnotationType();
    assertFalse(annotation.getDataset().isEmpty());
    if (type == AnnotationType.COLUMN) {
      assertFalse(annotation.getTable().isEmpty());
      assertFalse(annotation.getColumn().isEmpty());
    } else if (type == AnnotationType.TABLE) {
      assertFalse(annotation.getTable().isEmpty());
    }
  }
}
