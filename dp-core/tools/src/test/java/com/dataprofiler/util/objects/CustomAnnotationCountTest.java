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
import com.dataprofiler.test.IntegrationTest;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.MiniAccumuloContext;
import com.dataprofiler.util.objects.CustomAnnotation.AnnotationType;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A series of tests to exercise the search methods for CustomAnnotations. A fresh Accumulo load is
 * created on class load and not per test, this is to speed up the runtime
 */
@Category(IntegrationTest.class)
public class CustomAnnotationCountTest {
  private static final Logger logger = LoggerFactory.getLogger(CustomAnnotationCountTest.class);

  private static MiniAccumuloWithData mad;
  private static MiniAccumuloContext context;
  private static Map<String, String> datasets;
  private static Map<String, List<CustomAnnotation>> loaded;

  @BeforeClass
  public static void createContext() throws Exception {
    datasets = new HashMap<>();
    datasets.put("ds1", "src/test/resources/annotation_test_data_ds1.csv");
    datasets.put("ds2", "src/test/resources/annotation_test_data_ds2.csv");
    mad = new MiniAccumuloWithData();
    context = mad.startForTesting();
    CustomAnnotationCsvLoader csvLoader = new CustomAnnotationCsvLoader(context);
    loaded = csvLoader.loadTestFiles(datasets);
  }

  @AfterClass
  public static void destroyContext() throws IOException, InterruptedException {
    mad.close();
    mad = null;
    context = null;
    datasets.clear();
    datasets = null;
  }

  @Test
  public void sanity() throws BasicAccumuloException {
    Set<String> tables = context.getConnector().tableOperations().list();
    logger.debug("table " + tables);
    assertTrue(tables.size() > 0);
  }

  @Test
  public void testHierarchyCountAnnotationsByDataset() {
    String dataset = "ds1";
    CustomAnnotation scanner = new CustomAnnotation();
    CustomAnnotationCount count = scanner.countHierarchyWithFilter(context, dataset);
    assertNotNull(count);
    Assert.assertEquals(count.getAnnotationType(), AnnotationType.DATASET);
    assertEquals(count.getDataset(), dataset);
    long num = count.getCount();
    assertEquals(num, 7);
  }

  @Test
  public void testHierarchyCountAnnotationsByTable() {
    String dataset = "ds1";
    String table = "ds1-t1";
    CustomAnnotation scanner = new CustomAnnotation();
    CustomAnnotationCount count = scanner.countHierarchyWithFilter(context, dataset, table);
    assertNotNull(count);
    Assert.assertEquals(count.getAnnotationType(), AnnotationType.TABLE);
    assertEquals(count.getDataset(), dataset);
    long num = count.getCount();
    assertEquals(num, 4);
  }

  @Test
  public void testCountAnnotationsByColumn() {
    // sanity check, verify all column annotations
    CustomAnnotation scanner = new CustomAnnotation();
    Map<String, CustomAnnotationCount> map = scanner.countColumnAnnotations(context);
    map.forEach(
        (key, count) -> {
          assertNotNull(count);
          assertNotNull(count.getAnnotationType());
          Assert.assertEquals(count.getAnnotationType(), AnnotationType.COLUMN);
          assertNotNull(count.getDataset());
          assertFalse(count.getDataset().isEmpty());
          long num = count.getCount();
          assertTrue(num > 0);
        });
    // check counts
    CustomAnnotationCount count1 = map.get("COLUMN ds1 ds1-t1 ds1-t1-c1");
    CustomAnnotationCount count2 = map.get("COLUMN ds1 ds1-t1 ds1-t1-c2");
    CustomAnnotationCount count3 = map.get("COLUMN ds2 ds2-t1 ds2-t1-c1");
    assertNotNull(count1);
    assertNotNull(count2);
    assertNotNull(count3);
    assertEquals(count1.getCount(), 1);
    assertEquals(count2.getCount(), 1);
    assertEquals(count3.getCount(), 3);
  }

  @Test
  public void testCountAnnotationsByTable() {
    // sanity check, verify all table annotations
    CustomAnnotation scanner = new CustomAnnotation();
    Map<String, CustomAnnotationCount> map = scanner.countTableAnnotations(context);
    map.forEach(
        (key, count) -> {
          assertNotNull(count);
          assertNotNull(count.getAnnotationType());
          Assert.assertEquals(count.getAnnotationType(), AnnotationType.TABLE);
          assertNotNull(count.getDataset());
          assertFalse(count.getDataset().isEmpty());
          long num = count.getCount();
          assertTrue(num > 0);
        });
    // check counts
    CustomAnnotationCount count1 = map.get("TABLE ds1 ds1-t1");
    CustomAnnotationCount count2 = map.get("TABLE ds1 ds1-t2");
    CustomAnnotationCount count3 = map.get("TABLE ds2 ds2-t1");
    assertNotNull(count1);
    assertNotNull(count2);
    assertNotNull(count3);
    assertEquals(count1.getCount(), 2);
    assertEquals(count2.getCount(), 1);
    assertEquals(count3.getCount(), 1);
  }

  @Test
  public void testCountDatasetAnnotations() {
    // sanity check, verify all dataset annotations
    CustomAnnotation scanner = new CustomAnnotation();
    Map<String, CustomAnnotationCount> map = scanner.countDatasetAnnotations(context);
    map.forEach(
        (key, count) -> {
          assertNotNull(count);
          assertNotNull(count.getAnnotationType());
          Assert.assertEquals(count.getAnnotationType(), AnnotationType.DATASET);
          assertNotNull(count.getDataset());
          assertFalse(count.getDataset().isEmpty());
          long num = count.getCount();
          assertTrue(num > 0);
        });
    logger.debug(map.toString());

    // check counts
    CustomAnnotationCount count1 = map.get("DATASET ds1");
    CustomAnnotationCount count2 = map.get("DATASET ds2");
    assertNotNull(count1);
    assertNotNull(count2);
    assertEquals(count1.getCount(), 2);
    assertEquals(count2.getCount(), 2);
  }

  @Test
  public void testCountAnnotations() {
    // sanity check, verify all dataset annotations
    CustomAnnotation scanner = new CustomAnnotation();
    Map<String, CustomAnnotationCount> map = scanner.countAllAnnotations(context);
    logger.debug(map.toString());
    map.forEach(
        (key, count) -> {
          logger.trace(key + " " + count.getCount());
          assertNotNull(count);
          assertNotNull(count.getAnnotationType());
          assertNotNull(count.getDataset());
          assertFalse(count.getDataset().isEmpty());
          long num = count.getCount();
          assertTrue(num > 0);
        });
    // check counts
    assertEquals(map.get("DATASET ds1").getCount(), 2);
    assertEquals(map.get("DATASET ds2").getCount(), 2);
    assertEquals(map.get("TABLE ds1 ds1-t1").getCount(), 2);
    assertEquals(map.get("TABLE ds1 ds1-t2").getCount(), 1);
    assertEquals(map.get("TABLE ds2 ds2-t1").getCount(), 1);
    assertEquals(map.get("COLUMN ds1 ds1-t1 ds1-t1-c1").getCount(), 1);
    assertEquals(map.get("COLUMN ds1 ds1-t1 ds1-t1-c2").getCount(), 1);
    assertEquals(map.get("COLUMN ds2 ds2-t1 ds2-t1-c1").getCount(), 3);
  }

  @Test
  public void testCountAnnotationsFilterOnDataset() {
    assertNotNull(datasets);
    assertFalse(datasets.isEmpty());
    datasets.forEach(
        (dataset, value) -> {
          logger.debug("counting dataset " + dataset);
          // sanity check, verify all dataset annotations
          CustomAnnotation scanner = new CustomAnnotation();
          Map<String, CustomAnnotationCount> map =
              scanner.countDatasetAnnotationWithFilter(context, dataset);
          assertNotNull(map);
          assertEquals(map.size(), 1);
          String curKey = "DATASET " + dataset;
          CustomAnnotationCount count = map.get(curKey);
          assertNotNull(count);
          assertNotNull(count.getAnnotationType());
          Assert.assertEquals(count.getAnnotationType(), AnnotationType.DATASET);
          assertNotNull(count.getDataset());
          assertFalse(count.getDataset().isEmpty());
          long num = count.getCount();
          if (curKey.equals("DATASET ds1")) {
            assertEquals(num, 2);
          } else if (curKey.equals("DATASET ds2")) {
            assertEquals(num, 2);
          } else {
            fail("unknown case for key " + curKey);
          }
        });
  }

  @Test
  public void testTableAnnotationsFilterOnTable() {
    CustomAnnotation scanner = new CustomAnnotation();
    Map<String, CustomAnnotationCount> map =
        scanner.countTableAnnotationWithFilter(context, "ds1", "ds1-t1");
    assertNotNull(map);
    assertEquals(map.size(), 1);
    map.values().forEach(el -> logger.info(el.toString()));
    String curKey = "TABLE " + "ds1" + " ds1-t1";
    CustomAnnotationCount count = map.get(curKey);
    assertNotNull(count);
    assertNotNull(count.getAnnotationType());
    Assert.assertEquals(count.getAnnotationType(), AnnotationType.TABLE);
    assertNotNull(count.getDataset());
    assertFalse(count.getDataset().isEmpty());
    long num = count.getCount();
    assertEquals(num, 2);
  }

  @Test
  public void testCountAnnotationsFilterOnColumn() {
    CustomAnnotation scanner = new CustomAnnotation();
    Map<String, CustomAnnotationCount> map =
        scanner.countColumnAnnotationWithFilter(context, "ds1", "ds1-t1", "ds1-t1-c1");
    assertNotNull(map);
    assertEquals(map.size(), 1);
    String curKey = "COLUMN " + "ds1" + " ds1-t1" + " ds1-t1-c1";
    CustomAnnotationCount count = map.get(curKey);
    assertNotNull(count);
    assertNotNull(count.getAnnotationType());
    Assert.assertEquals(count.getAnnotationType(), AnnotationType.COLUMN);
    assertNotNull(count.getDataset());
    assertFalse(count.getDataset().isEmpty());
    long num = count.getCount();
    assertEquals(num, 1);
  }
}
