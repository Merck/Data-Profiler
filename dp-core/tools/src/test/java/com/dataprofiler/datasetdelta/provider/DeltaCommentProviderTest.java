package com.dataprofiler.datasetdelta.provider;

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
import static org.junit.Assert.assertNull;

import com.dataprofiler.datasetdelta.model.DatasetDeltaStub;
import com.dataprofiler.util.objects.CustomAnnotation;
import com.dataprofiler.util.objects.CustomAnnotation.AnnotationType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeltaCommentProviderTest {
  Logger logger = LoggerFactory.getLogger(DeltaCommentProvider.class);

  static final String CREATED_BY = "data profiler system".toLowerCase();
  DeltaCommentProvider deltaCommentProvider;

  @Before
  public void setup() {
    deltaCommentProvider = new DeltaCommentProvider();
  }

  @After
  public void teardown() {
    deltaCommentProvider = null;
  }

  @Test
  public void canary() {
    assertNotNull(deltaCommentProvider);
  }

  @Test
  public void testCommentNote1() {
    String note = deltaCommentProvider.buildNote(DatasetDeltaStub.buildColumnAddedDelta());
    assertNotNull(note);
    assertEquals("Column c1 added to table t1 in dataset ds1", note);
  }

  @Test
  public void testCommentNote2() {
    String note = deltaCommentProvider.buildNote(DatasetDeltaStub.buildColumnRemovedDelta());
    assertNotNull(note);
    assertEquals("Column c1 removed from table t1 in dataset ds1", note);
  }

  @Test
  public void testCommentNote3() {
    String note = deltaCommentProvider.buildNote(DatasetDeltaStub.buildDatasetValuesIncreasedDelta());
    assertNotNull(note);
    assertEquals("Dataset values increased from 11.60K to 431.42M", note);
  }

  @Test
  public void testComment1() {
    CustomAnnotation comment = deltaCommentProvider.buildComment(
        DatasetDeltaStub.buildColumnAddedDelta());
    assertNotNull(comment);
    Assert.assertEquals(AnnotationType.COLUMN, comment.getAnnotationType());
    assertEquals(comment.getDataset(), "ds1");
    assertEquals(comment.getTable(), "t1");
    assertEquals(comment.getColumn(), "c1");
    assertEquals(CREATED_BY, comment.getCreatedBy().toLowerCase());
    assertFalse(comment.getNote().isEmpty());
  }

  @Test
  public void testComment2() {
    CustomAnnotation comment = deltaCommentProvider.buildComment(
        DatasetDeltaStub.buildColumnRemovedDelta());
    assertNotNull(comment);
    Assert.assertEquals(AnnotationType.COLUMN, comment.getAnnotationType());
    assertEquals(comment.getDataset(), "ds1");
    assertEquals(comment.getTable(), "t1");
    assertEquals(comment.getColumn(), "c1");
    assertEquals(CREATED_BY, comment.getCreatedBy().toLowerCase());
    assertFalse(comment.getNote().isEmpty());
  }

  @Test
  public void testComment3() {
    CustomAnnotation comment =
        deltaCommentProvider.buildComment(DatasetDeltaStub.buildDatasetValuesIncreasedDelta());
    assertNotNull(comment);
    Assert.assertEquals(AnnotationType.DATASET, comment.getAnnotationType());
    assertEquals(comment.getDataset(), "ds1");
    assertNull(comment.getTable());
    assertNull(comment.getColumn());
    assertEquals(CREATED_BY, comment.getCreatedBy().toLowerCase());
    assertFalse(comment.getNote().isEmpty());
  }
}
