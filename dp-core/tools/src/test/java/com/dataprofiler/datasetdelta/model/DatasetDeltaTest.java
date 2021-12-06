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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DatasetDeltaTest {

  protected DatasetDelta datasetDelta;

  @Before
  public void setUp() {
    datasetDelta = DatasetDeltaStub.buildDatasetDelta();
  }

  @After
  public void tearDown() {
    datasetDelta = null;
  }

  @Test
  public void equalsTest() {
    DatasetDelta delta = DatasetDeltaStub.buildDatasetDelta();
    assertTrue(datasetDelta.equals(delta));
  }

  @Test
  public void equalsNoCommentTest() {
    DatasetDelta delta = DatasetDeltaStub.buildDatasetDelta();
    delta.setComment(null);
    datasetDelta.setComment(null);
    assertEquals(datasetDelta, delta);
  }

  @Test
  public void linkedCommentTest() {
    assertTrue(datasetDelta.hasLinkedComment());
  }

  @Test
  public void unlinkedCommentTest() {
    datasetDelta.setComment(null);
    assertFalse(datasetDelta.hasLinkedComment());
  }

  @Test
  public void equalsEmptyCommentTest() {
    // dataset delta ignores comments in equals
    DatasetDelta delta = DatasetDeltaStub.buildDatasetDelta();
    delta.setComment(null);
    assertEquals(datasetDelta, delta);
  }

  @Test
  public void equalsIgnoreDiffCommentTest() {
    // dataset delta ignores empty comments
    DatasetDelta delta = DatasetDeltaStub.buildDatasetDelta();
    delta.setComment(new CommentUUID(UUID.randomUUID()));
    assertEquals(delta, datasetDelta);
  }

  @Test
  public void addToSetTest() {
    Set<DatasetDelta> deltas = new TreeSet<>();
    DatasetDelta delta = DatasetDeltaStub.buildDatasetDelta();
    deltas.add(datasetDelta);
    deltas.add(delta);
    assertEquals(1, deltas.size());
    assertTrue(deltas.contains(delta));
  }

  @Test
  public void addToSetIgnoreEmptyCommentTest() {
    Set<DatasetDelta> deltas = new TreeSet<>();
    DatasetDelta delta = DatasetDeltaStub.buildDatasetDelta();
    delta.setComment(new CommentUUID());
    deltas.add(datasetDelta);
    assertTrue(deltas.contains(delta));
  }

  @Test
  public void addToSetIgnoreNullCommentTest() {
    Set<DatasetDelta> deltas = new TreeSet<>();
    DatasetDelta delta = DatasetDeltaStub.buildDatasetDelta();
    delta.setComment(null);
    deltas.add(datasetDelta);
    assertTrue(deltas.contains(delta));
  }

  @Test
  public void testCompareTo() {
    DatasetDelta delta = new DatasetDelta();
    delta.setDatasetUpdatedOnMillis(Integer.MIN_VALUE);
    int sort = datasetDelta.compareTo(delta);
    assertEquals(1, sort);
  }

}
