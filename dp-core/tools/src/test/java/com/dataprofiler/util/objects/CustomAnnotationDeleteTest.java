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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dataprofiler.MiniAccumuloWithData;
import com.dataprofiler.util.objects.CustomAnnotation.AnnotationType;
import com.dataprofiler.test.IntegrationTest;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.MiniAccumuloContext;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A series of tests to exercise the delete methods for CustomAnnotations These tests are slower
 * since we bring up an Accumulo for every test A fresh Accumulo load is needed every test so we
 * have a stable state in the database
 */
@Category(IntegrationTest.class)
public class CustomAnnotationDeleteTest {
  private static final Logger logger = LoggerFactory.getLogger(CustomAnnotationDeleteTest.class);
  private static MiniAccumuloWithData mad;
  private static MiniAccumuloContext context;
  private static Map<String, String> datasets;
  private static Map<String, List<CustomAnnotation>> loaded;

  @BeforeClass
  public static void createContext() throws Exception {
    datasets = new HashMap<>();
    datasets.put("ds1", "src/test/resources/annotation_test_data_ds1.csv");
    mad = new MiniAccumuloWithData();
    context = mad.startForTesting();
    List<String> tables = context.getConfig().getAllTables();
    logger.info(tables.toString());
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
  public void testDeleteByDataset() throws BasicAccumuloException {
    String dataset = "ds1";
    List<CustomAnnotation> insertions = loaded.get(dataset);
    Optional<CustomAnnotation> maybeAnnotation =
        insertions.stream()
            .filter(el -> el.getAnnotationType().equals(AnnotationType.DATASET))
            .findFirst();
    if (!maybeAnnotation.isPresent()) {
      fail("failed to find a suitable annotation to test");
    }
    UUID firstId = maybeAnnotation.get().getUuid();
    String createdBy = maybeAnnotation.get().getCreatedBy();
    String id = firstId.toString();
    if (logger.isDebugEnabled()) {
      logger.debug("search id: " + id + " created by " + createdBy);
    }
    boolean isDeleted =
        CustomAnnotation.deleteDatasetAnnotation(context, dataset, id, (el) -> true);
    assertTrue(isDeleted);
    CustomAnnotation annotation = CustomAnnotation.fetchDatasetAnnotation(context, dataset, id);
    logger.debug(format("%s", annotation));
    assertNull(annotation);
  }

  @Test
  public void testDeleteByTable() throws BasicAccumuloException {
    String dataset = "ds1";
    List<CustomAnnotation> insertions = loaded.get(dataset);
    Optional<CustomAnnotation> maybeAnnotation =
        insertions.stream()
            .filter(el -> el.getAnnotationType().equals(AnnotationType.TABLE))
            .findFirst();
    if (!maybeAnnotation.isPresent()) {
      fail("failed to find a suitable annotation to test");
    }
    CustomAnnotation firstAnnotation = maybeAnnotation.get();
    String id = firstAnnotation.getUuid().toString();
    String createdBy = maybeAnnotation.get().getCreatedBy();
    if (logger.isDebugEnabled()) {
      logger.debug("search id: " + id + " created by " + createdBy);
    }
    boolean isDeleted =
        CustomAnnotation.deleteTableAnnotation(
            context, dataset, firstAnnotation.getTable(), id, (el) -> true);
    assertTrue(isDeleted);
    CustomAnnotation annotation =
        CustomAnnotation.fetchTableAnnotation(context, dataset, firstAnnotation.getTable(), id);
    logger.debug(format("%s", annotation));
    assertNull(annotation);
  }

  @Test
  public void testDeleteByColumn() throws BasicAccumuloException {
    String dataset = "ds1";
    List<CustomAnnotation> insertions = loaded.get(dataset);
    Optional<CustomAnnotation> maybeAnnotation =
        insertions.stream()
            .filter(el -> el.getAnnotationType().equals(AnnotationType.COLUMN))
            .findFirst();
    if (!maybeAnnotation.isPresent()) {
      fail("failed to find a suitable annotation to test");
    }
    CustomAnnotation firstAnnotation = maybeAnnotation.get();
    String id = firstAnnotation.getUuid().toString();
    String createdBy = maybeAnnotation.get().getCreatedBy();
    if (logger.isDebugEnabled()) {
      logger.debug("search id: " + id + " created by " + createdBy);
    }
    boolean isDeleted =
        CustomAnnotation.deleteColumnAnnotation(
            context,
            dataset,
            firstAnnotation.getTable(),
            firstAnnotation.getColumn(),
            id,
            (el) -> true);
    assertTrue(isDeleted);
    CustomAnnotation annotation =
        CustomAnnotation.fetchColumnAnnotation(
            context, dataset, firstAnnotation.getTable(), firstAnnotation.getColumn(), id);
    logger.debug(format("%s", annotation));
    assertNull(annotation);
  }
}
