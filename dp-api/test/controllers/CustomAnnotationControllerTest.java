/**
*  Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
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
**/
package controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.CustomAnnotation;
import helpers.CustomAnnotationHelper;
import helpers.RulesOfUseHelper;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.typedmap.TypedEntry;
import play.libs.typedmap.TypedMap;
import play.mvc.Http.Request;
import play.mvc.Result;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static play.test.Helpers.contentAsString;
import static helpers.RulesOfUseHelper.ROU_ATTRIBUTES_TYPED_KEY;
import static helpers.RulesOfUseHelper.USER_CONTEXT_TYPED_KEY;

public class CustomAnnotationControllerTest {
  private final Logger logger = LoggerFactory.getLogger(CustomAnnotationControllerTest.class);

  protected CustomAnnotationHelper service;
  protected ObjectMapper mapper;

  @Before
  public void before() {
    this.mapper = new ObjectMapper();
    this.service = mock(CustomAnnotationHelper.class);
  }

  @Test
  public void canary() {
    assertTrue(true);
  }

  @Test
  public void testCreateDatasetSuccess() throws Exception {
    Request reqMock = mockRequestWithAttributes();
    when(reqMock.body().asJson()).thenReturn(createDatasetJson());
    when(this.service.newDatasetAnnotation(
            any(Context.class),
            eq("ds1"),
            eq(""),
            eq(""),
            eq("note"),
            eq("local-developer"),
            any(Long.class),
            any(Boolean.class)))
        .thenReturn(genDatasetAnnotationStub());
    Result result = new CustomAnnotationController(this.service).create(reqMock);
    verify(this.service, times(1))
        .newDatasetAnnotation(
            any(Context.class),
            eq("ds1"),
            eq(""),
            eq(""),
            eq("note"),
            eq("local-developer"),
            anyLong(),
            any(Boolean.class));
    assertEquals(OK, result.status());
    assertEquals("application/json", result.contentType().get());
    String body = contentAsString(result);
    assertNotNull(body);
    assertTrue(body.contains("dataset"));
    CustomAnnotation resp = this.mapper.readValue(body, CustomAnnotation.class);
    assertEquals(resp.getDataset(), "d1");
    assertEquals(resp.getTable(), "");
    assertEquals(resp.getColumn(), "");
    assertEquals(resp.getAnnotationType().toString(), "DATASET");
  }

  @Test
  public void testCreateTableSuccess() throws Exception {
    Request reqMock = mockRequestWithAttributes();
    when(reqMock.body().asJson()).thenReturn(createTableJson());
    when(this.service.newTableAnnotation(
            any(Context.class),
            eq("ds1"),
            eq("t1"),
            eq(""),
            eq("note"),
            eq("local-developer"),
            any(Long.class),
            any(Boolean.class)))
        .thenReturn(genTableAnnotationStub());
    Result result = new CustomAnnotationController(this.service).create(reqMock);
    verify(this.service, times(1))
        .newTableAnnotation(
            any(Context.class),
            eq("ds1"),
            eq("t1"),
            eq(""),
            eq("note"),
            eq("local-developer"),
            anyLong(),
            any(Boolean.class));
    assertEquals(OK, result.status());
    assertEquals("application/json", result.contentType().get());
    String body = contentAsString(result);
    assertNotNull(body);
    assertTrue(body.contains("dataset"));
    CustomAnnotation resp = this.mapper.readValue(body, CustomAnnotation.class);
    assertEquals(resp.getDataset(), "d1");
    assertEquals(resp.getTable(), "t1");
    assertEquals(resp.getColumn(), "");
    assertEquals(resp.getAnnotationType().toString(), "TABLE");
  }

  @Test
  public void testCreateColumnSuccess() throws Exception {
    Request reqMock = mockRequestWithAttributes();
    when(reqMock.body().asJson()).thenReturn(createColumnJson());
    when(this.service.newColumnAnnotation(
            any(Context.class),
            eq("ds1"),
            eq("t1"),
            eq("c1"),
            eq("note"),
            eq("local-developer"),
            any(Long.class),
            any(Boolean.class)))
        .thenReturn(genColumnAnnotationStub());
    Result result = new CustomAnnotationController(this.service).create(reqMock);
    verify(this.service, times(1))
        .newColumnAnnotation(
            any(Context.class),
            eq("ds1"),
            eq("t1"),
            eq("c1"),
            eq("note"),
            eq("local-developer"),
            anyLong(),
            any(Boolean.class));
    assertEquals(OK, result.status());
    assertEquals("application/json", result.contentType().get());
    String body = contentAsString(result);
    assertNotNull(body);
    assertTrue(body.contains("dataset"));
    CustomAnnotation resp = this.mapper.readValue(body, CustomAnnotation.class);
    assertEquals(resp.getDataset(), "d1");
    assertEquals(resp.getTable(), "t1");
    assertEquals(resp.getColumn(), "c1");
    assertEquals(resp.getAnnotationType().toString(), "COLUMN");
  }

  @Test
  public void listDatasetAnnotationsSuccess() throws Exception {
    Request reqMock = mockRequestWithAttributes();
    Set<CustomAnnotation> respSet = new HashSet<>();
    respSet.add(genDatasetAnnotationStub());
    when(this.service.datasetAnnotations(any(Context.class), eq("ds1"))).thenReturn(respSet);
    Result result =
        new CustomAnnotationController(this.service)
            .listAllDatasetAnnotations(reqMock, Optional.empty());
    verify(this.service, times(1)).datasetAnnotations(any(Context.class), eq("ds1"));
    assertEquals(OK, result.status());
    assertEquals("application/json", result.contentType().get());
    String body = contentAsString(result);
    assertNotNull(body);
    assertTrue(body.contains("dataset"));
    Set<CustomAnnotation> received =
        this.mapper.readValue(body, new TypeReference<Set<CustomAnnotation>>() {});
    assertNotNull(received);
    assertEquals(received.size(), 1);
    CustomAnnotation dsAnnotation = received.stream().findFirst().orElse(new CustomAnnotation());
    assertEquals(dsAnnotation.getDataset(), "d1");
    assertEquals(dsAnnotation.getNote(), "note");
    assertEquals(dsAnnotation.getAnnotationType().toString(), "DATASET");
    assertNotNull(dsAnnotation.getUuid());
  }

  @Test
  public void listTableAnnotationsSuccess() throws Exception {
    Request reqMock = mockRequestWithAttributes();
    Set<CustomAnnotation> respSet = new HashSet<>();
    respSet.add(genTableAnnotationStub());
    when(this.service.tableAnnotations(any(Context.class), eq("ds1"), eq("t1")))
        .thenReturn(respSet);
    Result result =
        new CustomAnnotationController(this.service)
            .listAllTableAnnotations(reqMock, Optional.empty());
    verify(this.service, times(1)).tableAnnotations(any(Context.class), eq("ds1"), eq("t1"));
    assertEquals(OK, result.status());
    assertEquals("application/json", result.contentType().get());
    String body = contentAsString(result);
    assertNotNull(body);
    Set<CustomAnnotation> received =
        this.mapper.readValue(body, new TypeReference<Set<CustomAnnotation>>() {});
    assertNotNull(received);
    assertEquals(received.size(), 1);
    CustomAnnotation dsAnnotation = received.stream().findFirst().orElse(new CustomAnnotation());
    assertEquals(dsAnnotation.getDataset(), "d1");
    assertEquals(dsAnnotation.getTable(), "t1");
    assertEquals(dsAnnotation.getNote(), "note");
    assertEquals(dsAnnotation.getAnnotationType().toString(), "TABLE");
    assertNotNull(dsAnnotation.getUuid());
  }

  @Test
  public void listColumnAnnotationsSuccess() throws Exception {
    Request reqMock = mockRequestWithAttributes();
    Set<CustomAnnotation> respSet = new HashSet<>();
    respSet.add(genColumnAnnotationStub());
    when(this.service.columnAnnotations(any(Context.class), eq("ds1"), eq("t1"), eq("c1")))
        .thenReturn(respSet);
    Result result =
        new CustomAnnotationController(this.service)
            .listAllColumnAnnotations(reqMock, Optional.empty());
    verify(this.service, times(1))
        .columnAnnotations(any(Context.class), eq("ds1"), eq("t1"), eq("c1"));
    assertEquals(OK, result.status());
    assertEquals("application/json", result.contentType().get());
    String body = contentAsString(result);
    assertNotNull(body);
    Set<CustomAnnotation> received =
        this.mapper.readValue(body, new TypeReference<Set<CustomAnnotation>>() {});
    assertNotNull(received);
    assertEquals(received.size(), 1);
    CustomAnnotation dsAnnotation = received.stream().findFirst().orElse(new CustomAnnotation());
    assertEquals(dsAnnotation.getDataset(), "d1");
    assertEquals(dsAnnotation.getTable(), "t1");
    assertEquals(dsAnnotation.getColumn(), "c1");
    assertEquals(dsAnnotation.getNote(), "note");
    assertEquals(dsAnnotation.getAnnotationType().toString(), "COLUMN");
    assertNotNull(dsAnnotation.getUuid());
  }

  protected Request mockRequestWithAttributes() {
    Request reqMock = mock(Request.class, Mockito.RETURNS_DEEP_STUBS);
    Context contextMock = mock(Context.class);
    TypedEntry<Context> contextEntry = new TypedEntry<Context>(USER_CONTEXT_TYPED_KEY, contextMock);
    RulesOfUseHelper rouHelper = new RulesOfUseHelper("", "");
    RulesOfUseHelper.UserAttributes userAttributes =
        new RulesOfUseHelper.UserAttributes("local-developer");
    TypedEntry<RulesOfUseHelper.UserAttributes> userAttrEntry =
        new TypedEntry<RulesOfUseHelper.UserAttributes>(ROU_ATTRIBUTES_TYPED_KEY, userAttributes);
    TypedMap attrMap = TypedMap.create(contextEntry, userAttrEntry);
    when(reqMock.attrs()).thenReturn(attrMap);
    return reqMock;
  }

  protected JsonNode createDatasetJson() throws IOException {
    return mapper.readTree(
        "{\n"
            + "\t\"dataset\": \"ds1\",\n"
            + "\t\"note\": \"note\",\n"
            + "\t\"annotationType\": \"DATASET\"\n"
            + "}");
  }

  protected JsonNode createTableJson() throws IOException {
    return mapper.readTree(
        "{\n"
            + "\t\"dataset\": \"ds1\",\n"
            + "\t\"table\": \"t1\",\n"
            + "\t\"column\": \"\",\n"
            + "\t\"note\": \"note\",\n"
            + "\t\"annotationType\": \"TABLE\"\n"
            + "}");
  }

  protected JsonNode createColumnJson() throws IOException {
    return mapper.readTree(
        "{\n"
            + "\t\"dataset\": \"ds1\",\n"
            + "\t\"table\": \"t1\",\n"
            + "\t\"column\": \"c1\",\n"
            + "\t\"note\": \"note\",\n"
            + "\t\"annotationType\": \"COLUMN\"\n"
            + "}");
  }

  protected CustomAnnotation genDatasetAnnotationStub() {
    CustomAnnotation annotation = new CustomAnnotation();
    annotation.setDataset("d1");
    annotation.setTable("");
    annotation.setColumn("");
    annotation.setNote("note");
    annotation.setAnnotationType(CustomAnnotation.AnnotationType.DATASET);
    return annotation;
  }

  protected CustomAnnotation genTableAnnotationStub() {
    CustomAnnotation annotation = this.genDatasetAnnotationStub();
    annotation.setTable("t1");
    annotation.setAnnotationType(CustomAnnotation.AnnotationType.TABLE);
    return annotation;
  }

  protected CustomAnnotation genColumnAnnotationStub() {
    CustomAnnotation annotation = this.genTableAnnotationStub();
    annotation.setColumn("c1");
    annotation.setAnnotationType(CustomAnnotation.AnnotationType.COLUMN);
    return annotation;
  }
}
