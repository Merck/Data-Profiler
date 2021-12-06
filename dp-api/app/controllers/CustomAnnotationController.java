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

import actions.AccumuloUserContext;
import actions.Authenticated;
import com.fasterxml.jackson.databind.JsonNode;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.CustomAnnotation;
import helpers.AccumuloHelper;
import helpers.CustomAnnotationHelper;
import helpers.RulesOfUseHelper.UserAttributes;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;

import javax.inject.Inject;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.BiFunction;

import static helpers.RulesOfUseHelper.ROU_ATTRIBUTES_TYPED_KEY;
import static helpers.RulesOfUseHelper.USER_CONTEXT_TYPED_KEY;
import static java.util.Objects.nonNull;
import static play.libs.Json.toJson;
import static java.lang.String.format;
import static com.fasterxml.jackson.core.JsonEncoding.UTF8;

@Authenticated
@AccumuloUserContext
public class CustomAnnotationController extends Controller {
  private final Logger logger = LoggerFactory.getLogger(CustomAnnotationController.class);
  private final CustomAnnotationHelper service;

  @Inject
  public CustomAnnotationController(CustomAnnotationHelper service) {
    super();
    this.service = service;
  }

  public Result listAllDatasetAnnotations(Request req, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.allDatasetAnnotations(context, limit)
            : service.allDatasetAnnotations(context);
    return ok(toJson(set), UTF8);
  }

  public Result listAllTableAnnotations(Request req, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.allTableAnnotations(context, limit)
            : service.allTableAnnotations(context);
    return ok(toJson(set), UTF8);
  }

  public Result listAllColumnAnnotations(Request req, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.allColumnAnnotations(context, limit)
            : service.allColumnAnnotations(context);
    return ok(toJson(set), UTF8);
  }

  public Result listDatasetHierarchyAnnotations(
      Request req, String dataset, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.datasetHierarchyAnnotations(context, dataset, limit)
            : service.datasetHierarchyAnnotations(context, dataset);
    return ok(toJson(set), UTF8);
  }

  public Result listTableHierarchyAnnotations(
      Request req, String dataset, String table, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    table = AccumuloHelper.decodeForwardSlash(table);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.tableHierarchyAnnotations(context, dataset, table, limit)
            : service.tableHierarchyAnnotations(context, dataset, table);
    return ok(toJson(set), UTF8);
  }

  public Result listDatasetAnnotations(Request req, String dataset, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.datasetAnnotations(context, dataset, limit)
            : service.datasetAnnotations(context, dataset);
    return ok(toJson(set), UTF8);
  }

  public Result listTableAnnotations(
      Request req, String dataset, String table, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    table = AccumuloHelper.decodeForwardSlash(table);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.tableAnnotations(context, dataset, table, limit)
            : service.tableAnnotations(context, dataset, table);
    return ok(toJson(set), UTF8);
  }

  public Result listColumnAnnotations(
      Request req, String dataset, String table, String column, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    table = AccumuloHelper.decodeForwardSlash(table);
    column = AccumuloHelper.decodeForwardSlash(column);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.columnAnnotations(context, dataset, table, column, limit)
            : service.columnAnnotations(context, dataset, table, column);
    return ok(toJson(set), UTF8);
  }

  // start system comments endpoints
  public Result listAllDatasetSystemAnnotations(Request req, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.allDatasetSystemAnnotations(context, limit)
            : service.allDatasetSystemAnnotations(context);
    return ok(toJson(set), UTF8);
  }

  public Result listAllTableSystemAnnotations(Request req, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.allTableSystemAnnotations(context, limit)
            : service.allTableSystemAnnotations(context);
    return ok(toJson(set), UTF8);
  }

  public Result listAllColumnSystemAnnotations(Request req, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.allColumnSystemAnnotations(context, limit)
            : service.allColumnSystemAnnotations(context);
    return ok(toJson(set), UTF8);
  }

  public Result listDatasetHierarchySystemAnnotations(
      Request req, String dataset, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.datasetHierarchySystemAnnotations(context, dataset, limit)
            : service.datasetHierarchySystemAnnotations(context, dataset);
    return ok(toJson(set), UTF8);
  }

  public Result listTableHierarchySystemAnnotations(
      Request req, String dataset, String table, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    table = AccumuloHelper.decodeForwardSlash(table);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.tableHierarchySystemAnnotations(context, dataset, table, limit)
            : service.tableHierarchySystemAnnotations(context, dataset, table);
    return ok(toJson(set), UTF8);
  }

  public Result listDatasetSystemAnnotations(Request req, String dataset, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.datasetSystemAnnotations(context, dataset, limit)
            : service.datasetSystemAnnotations(context, dataset);
    return ok(toJson(set), UTF8);
  }

  public Result listTableSystemAnnotations(
      Request req, String dataset, String table, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    table = AccumuloHelper.decodeForwardSlash(table);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.tableSystemAnnotations(context, dataset, table, limit)
            : service.tableSystemAnnotations(context, dataset, table);
    return ok(toJson(set), UTF8);
  }

  public Result listColumnSystemAnnotations(
      Request req, String dataset, String table, String column, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    table = AccumuloHelper.decodeForwardSlash(table);
    column = AccumuloHelper.decodeForwardSlash(column);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.columnSystemAnnotations(context, dataset, table, column, limit)
            : service.columnSystemAnnotations(context, dataset, table, column);
    return ok(toJson(set), UTF8);
  }

  // start tour comments endpoints
  public Result listAllDatasetTourAnnotations(Request req, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.allDatasetTourAnnotations(context, limit)
            : service.allDatasetTourAnnotations(context);
    return ok(toJson(set), UTF8);
  }

  public Result listAllTableTourAnnotations(Request req, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.allTableTourAnnotations(context, limit)
            : service.allTableTourAnnotations(context);
    return ok(toJson(set), UTF8);
  }

  public Result listAllColumnTourAnnotations(Request req, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.allColumnTourAnnotations(context, limit)
            : service.allColumnTourAnnotations(context);
    return ok(toJson(set), UTF8);
  }

  public Result listDatasetHierarchyTourAnnotations(
      Request req, String dataset, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.datasetHierarchyTourAnnotations(context, dataset, limit)
            : service.datasetHierarchyTourAnnotations(context, dataset);
    return ok(toJson(set), UTF8);
  }

  public Result listTableHierarchyTourAnnotations(
      Request req, String dataset, String table, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    table = AccumuloHelper.decodeForwardSlash(table);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.tableHierarchyTourAnnotations(context, dataset, table, limit)
            : service.tableHierarchyTourAnnotations(context, dataset, table);
    return ok(toJson(set), UTF8);
  }

  public Result listDatasetTourAnnotations(Request req, String dataset, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.datasetTourAnnotations(context, dataset, limit)
            : service.datasetTourAnnotations(context, dataset);
    return ok(toJson(set), UTF8);
  }

  public Result listTableTourAnnotations(
      Request req, String dataset, String table, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    table = AccumuloHelper.decodeForwardSlash(table);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.tableTourAnnotations(context, dataset, table, limit)
            : service.tableTourAnnotations(context, dataset, table);
    return ok(toJson(set), UTF8);
  }

  public Result listColumnTourAnnotations(
      Request req, String dataset, String table, String column, Optional<Integer> max) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    table = AccumuloHelper.decodeForwardSlash(table);
    Integer limit = nonNull(max) && max.isPresent() ? max.get() : -1;
    Set<CustomAnnotation> set =
        limit > 0
            ? service.columnTourAnnotations(context, dataset, table, column, limit)
            : service.columnTourAnnotations(context, dataset, table, column);
    return ok(toJson(set), UTF8);
  }

  public Result countAllDatasetAnnotations(Request req) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    return ok(toJson(service.countDatasetAnnotations(context)), UTF8);
  }

  public Result countAllDatasetSystemAnnotations(Request req) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    return ok(toJson(service.countDatasetSystemAnnotations(context)), UTF8);
  }

  public Result countAllDatasetTourAnnotations(Request req) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    return ok(toJson(service.countDatasetTourAnnotations(context)), UTF8);
  }

  public Result countAllTableAnnotations(Request req) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    return ok(toJson(service.countTableAnnotations(context)), UTF8);
  }

  public Result countAllTableSystemAnnotations(Request req) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    return ok(toJson(service.countTableSystemAnnotations(context)), UTF8);
  }

  public Result countAllTableTourAnnotations(Request req) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    return ok(toJson(service.countTableTourAnnotations(context)), UTF8);
  }

  public Result countAllColumnAnnotations(Request req) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    return ok(toJson(service.countColumnAnnotations(context)), UTF8);
  }

  public Result countAllColumnSystemAnnotations(Request req) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    return ok(toJson(service.countColumnSystemAnnotations(context)), UTF8);
  }

  public Result countAllColumnTourAnnotations(Request req) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    return ok(toJson(service.countColumnTourAnnotations(context)), UTF8);
  }

  public Result countAllAnnotations(Request req) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    return ok(toJson(service.countAllAnnotations(context)), UTF8);
  }

  public Result countAllSystemAnnotations(Request req) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    return ok(toJson(service.countAllSystemAnnotations(context)), UTF8);
  }

  public Result countAllTourAnnotations(Request req) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    return ok(toJson(service.countAllTourAnnotations(context)), UTF8);
  }

  public Result countAnnotationsForDatasetHierarchy(Request req, String dataset) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);

    return ok(toJson(service.countHierarchyAnnotations(context, dataset)), UTF8);
  }

  public Result countSystemAnnotationsForDatasetHierarchy(Request req, String dataset) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);

    return ok(toJson(service.countHierarchySystemAnnotations(context, dataset)), UTF8);
  }

  public Result countTourAnnotationsForDatasetHierarchy(Request req, String dataset) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);

    return ok(toJson(service.countHierarchyTourAnnotations(context, dataset)), UTF8);
  }

  public Result countAnnotationsForTableHierarchy(Request req, String dataset, String table) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    table = AccumuloHelper.decodeForwardSlash(table);

    return ok(toJson(service.countHierarchyAnnotations(context, dataset, table)), UTF8);
  }

  public Result countSystemAnnotationsForTableHierarchy(Request req, String dataset, String table) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    table = AccumuloHelper.decodeForwardSlash(table);

    return ok(toJson(service.countHierarchySystemAnnotations(context, dataset, table)), UTF8);
  }

  public Result countTourAnnotationsForTableHierarchy(Request req, String dataset, String table) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    table = AccumuloHelper.decodeForwardSlash(table);

    return ok(toJson(service.countHierarchyTourAnnotations(context, dataset, table)), UTF8);
  }

  public Result countAnnotationsForDataset(Request req, String dataset) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);

    return ok(toJson(service.countDatasetAnnotationsWithFilter(context, dataset)), UTF8);
  }

  public Result countSystemAnnotationsForDataset(Request req, String dataset) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);

    return ok(toJson(service.countDatasetSystemAnnotationsWithFilter(context, dataset)), UTF8);
  }

  public Result countTourAnnotationsForDataset(Request req, String dataset) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);

    return ok(toJson(service.countDatasetTourAnnotationsWithFilter(context, dataset)), UTF8);
  }

  public Result countAnnotationsForTable(Request req, String dataset, String table) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    table = AccumuloHelper.decodeForwardSlash(table);

    return ok(toJson(service.countTableAnnotationsWithFilter(context, dataset, table)), UTF8);
  }

  public Result countSystemAnnotationsForTable(Request req, String dataset, String table) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    table = AccumuloHelper.decodeForwardSlash(table);

    return ok(toJson(service.countTableSystemAnnotationsWithFilter(context, dataset, table)), UTF8);
  }

  public Result countTourAnnotationsForTable(Request req, String dataset, String table) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    table = AccumuloHelper.decodeForwardSlash(table);

    return ok(toJson(service.countTableTourAnnotationsWithFilter(context, dataset, table)), UTF8);
  }

  public Result countAnnotationsForColumn(
      Request req, String dataset, String table, String column) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    table = AccumuloHelper.decodeForwardSlash(table);
    column = AccumuloHelper.decodeForwardSlash(column);

    return ok(
        toJson(service.countColumnAnnotationsWithFilter(context, dataset, table, column)), UTF8);
  }

  public Result countSystemAnnotationsForColumn(
      Request req, String dataset, String table, String column) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    table = AccumuloHelper.decodeForwardSlash(table);
    column = AccumuloHelper.decodeForwardSlash(column);

    return ok(
        toJson(service.countColumnSystemAnnotationsWithFilter(context, dataset, table, column)),
        UTF8);
  }

  public Result countTourAnnotationsForColumn(
      Request req, String dataset, String table, String column) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    table = AccumuloHelper.decodeForwardSlash(table);
    column = AccumuloHelper.decodeForwardSlash(column);

    return ok(
        toJson(service.countColumnTourAnnotationsWithFilter(context, dataset, table, column)),
        UTF8);
  }

  public Result deleteDatasetAnnotation(Request req, String dataset, String id) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    UserAttributes curUserAttrs = (UserAttributes) req.attrs().get(ROU_ATTRIBUTES_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);

    try {
      boolean isDeleted = service.deleteDatasetAnnotation(curUserAttrs, context, dataset, id);
      return ok(toJson(isDeleted), UTF8);
    } catch (BasicAccumuloException e) {
      logger.warn(e.getMessage());
      String msg = format("Failed to delete %s %s", dataset, id);
      return internalServerError(toJson(msg));
    }
  }

  public Result deleteTableAnnotation(Request req, String dataset, String table, String id) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    UserAttributes curUserAttrs = (UserAttributes) req.attrs().get(ROU_ATTRIBUTES_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    table = AccumuloHelper.decodeForwardSlash(table);

    try {
      boolean isDeleted = service.deleteTableAnnotation(curUserAttrs, context, dataset, table, id);
      return ok(toJson(isDeleted), UTF8);
    } catch (BasicAccumuloException e) {
      logger.warn(e.getMessage());
      String msg = format("Failed to delete %s %s %s", dataset, table, id);
      return internalServerError(toJson(msg), UTF8);
    }
  }

  public Result deleteColumnAnnotation(
      Request req, String dataset, String table, String column, String id) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    UserAttributes curUserAttrs = (UserAttributes) req.attrs().get(ROU_ATTRIBUTES_TYPED_KEY);
    dataset = AccumuloHelper.decodeForwardSlash(dataset);
    table = AccumuloHelper.decodeForwardSlash(table);
    column = AccumuloHelper.decodeForwardSlash(column);

    try {
      boolean isDeleted =
          service.deleteColumnAnnotation(curUserAttrs, context, dataset, table, column, id);
      return ok(toJson(isDeleted), UTF8);
    } catch (BasicAccumuloException e) {
      logger.warn(e.getMessage());
      String msg = format("Failed to delete %s %s %s %s", dataset, table, column, id);
      return internalServerError(toJson(msg), UTF8);
    }
  }

  @BodyParser.Of(BodyParser.Json.class)
  public Result create(Request req) {
    Context context = (Context) req.attrs().get(USER_CONTEXT_TYPED_KEY);
    UserAttributes curUserAttrs = (UserAttributes) req.attrs().get(ROU_ATTRIBUTES_TYPED_KEY);
    String createdBy = curUserAttrs.getUsername();
    JsonNode reqBody = req.body().asJson();
    BiFunction<JsonNode, String, String> textValueOrEmpty =
        (body, fieldName) -> {
          JsonNode node = body.path(fieldName);
          return (!node.isMissingNode() && !node.isNull() && node.isTextual()) ? node.asText() : "";
        };
    BiFunction<JsonNode, String, Boolean> booleanValueOrFalse =
        (body, fieldName) -> {
          JsonNode node = body.path(fieldName);
          return (!node.isMissingNode() && !node.isNull() && node.isBoolean())
              ? node.asBoolean()
              : false;
        };

    String dataset = textValueOrEmpty.apply(reqBody, "dataset");
    String table = textValueOrEmpty.apply(reqBody, "table");
    String column = textValueOrEmpty.apply(reqBody, "column");
    String note = textValueOrEmpty.apply(reqBody, "note");
    String annotationTypeStr = textValueOrEmpty.apply(reqBody, "annotationType").toUpperCase();
    LocalDateTime dtg = LocalDateTime.now();
    long createdOn = dtg.atZone(ZoneId.systemDefault()).toEpochSecond();
    Boolean isDataTour = booleanValueOrFalse.apply(reqBody, "isDataTour");

    if (logger.isDebugEnabled()) {
      logger.debug("creating annotation type: " + annotationTypeStr);
    }

    try {
      CustomAnnotation.AnnotationType annotationType =
          CustomAnnotation.AnnotationType.valueOf(annotationTypeStr);
      CustomAnnotation annotation = null;
      switch (annotationType) {
        case DATASET:
          annotation =
              service.newDatasetAnnotation(
                  context, dataset, table, column, note, createdBy, createdOn, isDataTour);
          break;
        case TABLE:
          annotation =
              service.newTableAnnotation(
                  context, dataset, table, column, note, createdBy, createdOn, isDataTour);
          break;
        case COLUMN:
          annotation =
              service.newColumnAnnotation(
                  context, dataset, table, column, note, createdBy, createdOn, isDataTour);
          break;
        default:
          logger.warn("unknown annotation type");
      }

      if (annotation == null) {
        logger.warn("Create annotation resulted in null");
        Result cannot_create_custom_annotation =
            internalServerError(toJson("Cannot create custom annotation"), UTF8);
        return cannot_create_custom_annotation;
      }
      return ok(toJson(annotation), UTF8);
    } catch (Exception e) {
      return internalServerError(toJson("Cannot create custom column name"), UTF8);
    }
  }
}
