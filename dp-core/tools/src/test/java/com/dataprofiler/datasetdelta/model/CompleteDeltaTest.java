package com.dataprofiler.datasetdelta.model;

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

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompleteDeltaTest {
  protected static final Logger logger = LoggerFactory.getLogger(CompleteDeltaTest.class);

  protected CompleteDelta completeDelta;

  @Before
  public void setUp() {
    completeDelta = buildCompleteDelta();
  }

  @After
  public void tearDown() {
    completeDelta = null;
  }

  @Test
  public void equalsTest() {
    CompleteDelta delta = buildCompleteDelta();
    assertTrue(completeDelta.equals(delta));
  }

  @Test
  public void mergeDeltasTest() {
    CompleteDelta delta = buildCompleteDelta();
    completeDelta.mergeDeltas(delta);
    assertEquals(1, completeDelta.getDeltas().size());
  }

  @Test
  public void mergeUnlinkedDeltasTest() {
    CompleteDelta delta = buildCompleteDelta();
    Set<DatasetDelta> unlinkedDeltas =
        delta.getDeltas().stream()
            .map(
                el -> {
                  el.setComment(null);
                  return el;
                })
            .collect(toSet());
    logger.debug(unlinkedDeltas.toString());
    delta.setDeltas(unlinkedDeltas);
    delta = completeDelta.mergeDeltas(delta);
    logger.debug(delta.getDeltas().toString());
    assertEquals(1, completeDelta.getDeltas().size());
    Optional<DatasetDelta> datasetDelta = completeDelta.getDeltas().stream().findFirst();
    assertTrue(datasetDelta.isPresent());
    // make sure we picked the delta with a comment linked
    assertTrue(datasetDelta.get().hasLinkedComment());
  }

  @Test
  public void mergeDiffLatestVersionInfoTest() {
    CompleteDelta delta = buildCompleteDelta();
    DatasetVersion version = new DatasetVersion();
    version.setDataset("ds1");
    version.setVersion("00f517ec-ac35-4745-a5b5-c89394c027e0");
    Map<String, TableVersion> tables = new HashMap<>();
    TableVersion tableVersion = new TableVersion();
    tableVersion.setTable("t1");
    tableVersion.setVersion("1ce234da-e83f-37d3-a046-6f4fbb31ff0f");
    Map<String, String> columns = new HashMap<>();
    columns.put("column2", "0142e55d-322a-4986-b96f-a0632ca85810");
    tableVersion.setColumns(columns);
    tables.put("t1", tableVersion);
    version.setTables(tables);
    delta.setLastKnownVersions(version);
    logger.debug("original: " + completeDelta.toString());
    CompleteDelta mergedDelta = completeDelta.mergeDeltas(delta);
    logger.debug(mergedDelta.toString());
    Map<String, TableVersion> mergedTables = mergedDelta.getLastKnownVersions().getTables();
    Map<String, String> mergedColumns = mergedTables.get("t1").getColumns();
    assertNotNull(mergedColumns);
    assertEquals(2, mergedColumns.size());
    String value1 = "104d3c5e-ed96-4d21-926a-44f00ec319b3";
    String value2 = "0142e55d-322a-4986-b96f-a0632ca85810";
    assertEquals(value1, mergedColumns.get("c1"));
    assertEquals(value2, mergedColumns.get("column2"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void mergeDifferentDatasetsFailureTest() {
    CompleteDelta delta = buildCompleteDelta();
    DatasetVersion version = new DatasetVersion();
    version.setDataset("ds2");
    version.setVersion("00f517ec-ac35-4745-a5b5-c89394c027e0");
    delta.setLastKnownVersions(version);
    completeDelta.mergeDeltas(delta);
    fail("expected an exception when merging different datasets");
  }

  protected CompleteDelta buildCompleteDelta() {
    long time = 1613757735916L;
    DatasetVersion version = new DatasetVersion();
    version.setDataset("ds1");
    version.setVersion("00580da7-2c73-4c2f-b733-0daf56e6febe");
    Map<String, TableVersion> tables = new HashMap<>();
    TableVersion tableVersion = new TableVersion();
    tableVersion.setTable("t1");
    tableVersion.setVersion("1ce234da-e83f-37d3-a046-6f4fbb31ff0f");
    Map<String, String> columns = new HashMap<>();
    columns.put("c1", "104d3c5e-ed96-4d21-926a-44f00ec319b3");
    tableVersion.setColumns(columns);
    tables.put("t1", tableVersion);
    version.setTables(tables);
    CompleteDelta completeDelta = new CompleteDelta();
    completeDelta.setUpdatedOnMillis(time);
    completeDelta.setLastKnownVersions(version);
    completeDelta.setDeltas(Arrays.asList(DatasetDeltaStub.buildDatasetDelta()));
    return completeDelta;
  }

}
