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
package helpers;

import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.CustomAnnotation;
import com.dataprofiler.util.objects.CustomAnnotationCount;
import com.dataprofiler.util.objects.ObjectScannerIterable;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.StreamSupport.stream;

public class CustomAnnotationHelper {
  private static final Logger logger = LoggerFactory.getLogger(CustomAnnotationHelper.class);

  public CustomAnnotationHelper() {}

  public Set<CustomAnnotation> allColumnAnnotations(Context context) {
    ObjectScannerIterable<CustomAnnotation> annotations =
        new CustomAnnotation().scanColumns(context);
    return stream(annotations.spliterator(), false).collect(sortedByCreatedBy());
  }

  public Set<CustomAnnotation> allColumnAnnotations(Context context, int max) {
    return limitNumberComments(allColumnAnnotations(context), max);
  }

  public Set<CustomAnnotation> allTableAnnotations(Context context) {
    ObjectScannerIterable<CustomAnnotation> annotations =
        new CustomAnnotation().scanTables(context);
    return stream(annotations.spliterator(), false).collect(sortedByCreatedBy());
  }

  public Set<CustomAnnotation> allTableAnnotations(Context context, int max) {
    return limitNumberComments(allTableAnnotations(context), max);
  }

  public Set<CustomAnnotation> allDatasetAnnotations(Context context) {
    ObjectScannerIterable<CustomAnnotation> annotations =
        new CustomAnnotation().scanDatasets(context);
    return stream(annotations.spliterator(), false).collect(sortedByCreatedBy());
  }

  public Set<CustomAnnotation> allDatasetAnnotations(Context context, int max) {
    return limitNumberComments(allDatasetAnnotations(context), max);
  }

  public Set<CustomAnnotation> columnAnnotations(
      Context context, String dataset, String table, String column) {
    ObjectScannerIterable<CustomAnnotation> annotations =
        new CustomAnnotation().scanByColumn(context, dataset, table, column);
    return stream(annotations.spliterator(), false).collect(sortedByCreatedBy());
  }

  public Set<CustomAnnotation> columnAnnotations(
      Context context, String dataset, String table, String column, int max) {
    return limitNumberComments(columnAnnotations(context, dataset, table, column), max);
  }

  public Set<CustomAnnotation> tableAnnotations(Context context, String dataset, String table) {
    ObjectScannerIterable<CustomAnnotation> annotations =
        new CustomAnnotation().scanByTable(context, dataset, table);
    return stream(annotations.spliterator(), false).collect(sortedByCreatedBy());
  }

  public Set<CustomAnnotation> tableAnnotations(
      Context context, String dataset, String table, int max) {
    return limitNumberComments(tableAnnotations(context, dataset, table), max);
  }

  public Set<CustomAnnotation> datasetAnnotations(Context context, String dataset) {
    ObjectScannerIterable<CustomAnnotation> annotations =
        new CustomAnnotation().scanByDataset(context, dataset);
    return stream(annotations.spliterator(), false).collect(sortedByCreatedBy());
  }

  public Set<CustomAnnotation> datasetAnnotations(Context context, String dataset, int max) {
    return limitNumberComments(datasetAnnotations(context, dataset), max);
  }

  public Set<CustomAnnotation> datasetHierarchyAnnotations(Context context, String dataset) {
    ObjectScannerIterable<CustomAnnotation> annotations =
        new CustomAnnotation().scanHierarchyByDataset(context, dataset);
    return stream(annotations.spliterator(), false).collect(sortedByCreatedBy());
  }

  public Set<CustomAnnotation> datasetHierarchyAnnotations(
      Context context, String dataset, int max) {
    return limitNumberComments(datasetHierarchyAnnotations(context, dataset), max);
  }

  public Set<CustomAnnotation> tableHierarchyAnnotations(
      Context context, String dataset, String table) {
    ObjectScannerIterable<CustomAnnotation> annotations =
        new CustomAnnotation().scanHierarchyByTable(context, dataset, table);
    return stream(annotations.spliterator(), false).collect(sortedByCreatedBy());
  }

  public Set<CustomAnnotation> tableHierarchyAnnotations(
      Context context, String dataset, String table, int max) {
    return limitNumberComments(tableHierarchyAnnotations(context, dataset, table), max);
  }

  // start system methods
  public Set<CustomAnnotation> allColumnSystemAnnotations(Context context) {
    return filterForSystemComments(allColumnAnnotations(context));
  }

  public Set<CustomAnnotation> allColumnSystemAnnotations(Context context, int max) {
    return limitNumberComments(allColumnSystemAnnotations(context), max);
  }

  public Set<CustomAnnotation> allTableSystemAnnotations(Context context) {
    return filterForSystemComments(allTableAnnotations(context));
  }

  public Set<CustomAnnotation> allTableSystemAnnotations(Context context, int max) {
    return limitNumberComments(allTableSystemAnnotations(context), max);
  }

  public Set<CustomAnnotation> allDatasetSystemAnnotations(Context context) {
    return filterForSystemComments(allDatasetAnnotations(context));
  }

  public Set<CustomAnnotation> allDatasetSystemAnnotations(Context context, int max) {
    return limitNumberComments(allDatasetSystemAnnotations(context), max);
  }

  public Set<CustomAnnotation> columnSystemAnnotations(
      Context context, String dataset, String table, String column) {
    return filterForSystemComments(columnAnnotations(context, dataset, table, column));
  }

  public Set<CustomAnnotation> columnSystemAnnotations(
      Context context, String dataset, String table, String column, int max) {
    return limitNumberComments(columnSystemAnnotations(context, dataset, table, column), max);
  }

  public Set<CustomAnnotation> tableSystemAnnotations(
      Context context, String dataset, String table) {
    return filterForSystemComments(tableAnnotations(context, dataset, table));
  }

  public Set<CustomAnnotation> tableSystemAnnotations(
      Context context, String dataset, String table, int max) {
    return limitNumberComments(tableSystemAnnotations(context, dataset, table), max);
  }

  public Set<CustomAnnotation> datasetSystemAnnotations(Context context, String dataset) {
    return filterForSystemComments(datasetAnnotations(context, dataset));
  }

  public Set<CustomAnnotation> datasetSystemAnnotations(Context context, String dataset, int max) {
    return limitNumberComments(datasetSystemAnnotations(context, dataset), max);
  }

  public Set<CustomAnnotation> datasetHierarchySystemAnnotations(Context context, String dataset) {
    return filterForSystemComments(datasetHierarchyAnnotations(context, dataset));
  }

  public Set<CustomAnnotation> datasetHierarchySystemAnnotations(
      Context context, String dataset, int max) {
    return limitNumberComments(datasetHierarchySystemAnnotations(context, dataset), max);
  }

  public Set<CustomAnnotation> tableHierarchySystemAnnotations(
      Context context, String dataset, String table) {
    return filterForSystemComments(tableHierarchyAnnotations(context, dataset, table));
  }

  public Set<CustomAnnotation> tableHierarchySystemAnnotations(
      Context context, String dataset, String table, int max) {
    return limitNumberComments(tableHierarchySystemAnnotations(context, dataset, table), max);
  }

  // start data tour methods
  public Set<CustomAnnotation> allColumnTourAnnotations(Context context) {
    return filterForDataTourComments(allColumnAnnotations(context));
  }

  public Set<CustomAnnotation> allColumnTourAnnotations(Context context, int max) {
    return limitNumberComments(allColumnTourAnnotations(context), max);
  }

  public Set<CustomAnnotation> allTableTourAnnotations(Context context) {
    return filterForDataTourComments(allTableAnnotations(context));
  }

  public Set<CustomAnnotation> allTableTourAnnotations(Context context, int max) {
    return limitNumberComments(allTableTourAnnotations(context), max);
  }

  public Set<CustomAnnotation> allDatasetTourAnnotations(Context context) {
    return filterForDataTourComments(allDatasetAnnotations(context));
  }

  public Set<CustomAnnotation> allDatasetTourAnnotations(Context context, int max) {
    return limitNumberComments(allDatasetTourAnnotations(context), max);
  }

  public Set<CustomAnnotation> columnTourAnnotations(
      Context context, String dataset, String table, String column) {
    return filterForDataTourComments(columnAnnotations(context, dataset, table, column));
  }

  public Set<CustomAnnotation> columnTourAnnotations(
      Context context, String dataset, String table, String column, int max) {
    return limitNumberComments(columnTourAnnotations(context, dataset, table, column), max);
  }

  public Set<CustomAnnotation> tableTourAnnotations(Context context, String dataset, String table) {
    return filterForDataTourComments(tableAnnotations(context, dataset, table));
  }

  public Set<CustomAnnotation> tableTourAnnotations(
      Context context, String dataset, String table, int max) {
    return limitNumberComments(tableTourAnnotations(context, dataset, table), max);
  }

  public Set<CustomAnnotation> datasetTourAnnotations(Context context, String dataset) {
    return filterForDataTourComments(datasetAnnotations(context, dataset));
  }

  public Set<CustomAnnotation> datasetTourAnnotations(Context context, String dataset, int max) {
    return limitNumberComments(datasetTourAnnotations(context, dataset), max);
  }

  public Set<CustomAnnotation> datasetHierarchyTourAnnotations(Context context, String dataset) {
    return filterForDataTourComments(datasetHierarchyAnnotations(context, dataset));
  }

  public Set<CustomAnnotation> datasetHierarchyTourAnnotations(
      Context context, String dataset, int max) {
    return limitNumberComments(datasetHierarchyTourAnnotations(context, dataset), max);
  }

  public Set<CustomAnnotation> tableHierarchyTourAnnotations(
      Context context, String dataset, String table) {
    return filterForDataTourComments(tableHierarchyAnnotations(context, dataset, table));
  }

  public Set<CustomAnnotation> tableHierarchyTourAnnotations(
      Context context, String dataset, String table, int max) {
    // important order, fetch then filter before limit
    // first fetch table hierarchy annotations, then filter on type, then limit the list to max
    return limitNumberComments(tableHierarchyTourAnnotations(context, dataset, table), max);
  }

  public CustomAnnotation newColumnAnnotation(
      Context context,
      String dataset,
      String table,
      String column,
      String note,
      String createdBy,
      long createdOn,
      boolean isDataTour) {
    CustomAnnotation.AnnotationType type = CustomAnnotation.AnnotationType.COLUMN;
    try {
      CustomAnnotation annotation =
          buildAnnotation(type, dataset, table, column, note, createdBy, createdOn, isDataTour);
      annotation.put(context);
      return annotation;
    } catch (Exception e) {
      logger.error("Unable to create Custom " + type + " Annotation", e);
      return null;
    }
  }

  public CustomAnnotation newTableAnnotation(
      Context context,
      String dataset,
      String table,
      String column,
      String note,
      String createdBy,
      long createdOn,
      boolean isDataTour) {
    CustomAnnotation.AnnotationType type = CustomAnnotation.AnnotationType.TABLE;
    try {
      CustomAnnotation annotation =
          buildAnnotation(type, dataset, table, column, note, createdBy, createdOn, isDataTour);
      annotation.put(context);
      return annotation;
    } catch (Exception e) {
      logger.error("Unable to create Custom " + type + " Annotation", e);
      return null;
    }
  }

  public CustomAnnotation newDatasetAnnotation(
      Context context,
      String dataset,
      String table,
      String column,
      String note,
      String createdBy,
      long createdOn,
      boolean isDataTour) {
    CustomAnnotation.AnnotationType type = CustomAnnotation.AnnotationType.DATASET;

    try {
      CustomAnnotation annotation =
          buildAnnotation(type, dataset, table, column, note, createdBy, createdOn, isDataTour);
      annotation.put(context);
      return annotation;
    } catch (Exception e) {
      logger.error("Unable to create Custom " + type + " Annotation", e);
      return null;
    }
  }

  public static Function<CustomAnnotation, Boolean> verifyHasDeleteAuths(
      RulesOfUseHelper.UserAttributes userAttributes) {
    return (CustomAnnotation annotation) -> {
      if (userAttributes == null
          || userAttributes.getUsername() == null
          || userAttributes.getUsername().isEmpty()) {
        return false;
      }
      if (annotation == null) {
        return false;
      }

      final boolean curUserIsAdmin = userAttributes.hasAdminCapability();
      if (curUserIsAdmin) {
        return true;
      }
      final String userName = userAttributes.getUsername();
      return !annotation.getCreatedBy().isEmpty() && annotation.getCreatedBy().equals(userName);
    };
  }

  public Boolean deleteDatasetAnnotation(
      RulesOfUseHelper.UserAttributes userAttributes, Context context, String dataset, String id)
      throws BasicAccumuloException {
    return CustomAnnotation.deleteDatasetAnnotation(
        context, dataset, id, verifyHasDeleteAuths(userAttributes));
  }

  public Boolean deleteTableAnnotation(
      RulesOfUseHelper.UserAttributes userAttributes,
      Context context,
      String dataset,
      String table,
      String id)
      throws BasicAccumuloException {
    return CustomAnnotation.deleteTableAnnotation(
        context, dataset, table, id, verifyHasDeleteAuths(userAttributes));
  }

  public Boolean deleteColumnAnnotation(
      RulesOfUseHelper.UserAttributes userAttributes,
      Context context,
      String dataset,
      String table,
      String column,
      String id)
      throws BasicAccumuloException {
    return CustomAnnotation.deleteColumnAnnotation(
        context, dataset, table, column, id, verifyHasDeleteAuths(userAttributes));
  }

  public CustomAnnotationCount countHierarchyAnnotations(Context context, String dataset) {
    return new CustomAnnotation().countHierarchyWithFilter(context, dataset);
  }

  public CustomAnnotationCount countHierarchySystemAnnotations(Context context, String dataset) {
    return new CustomAnnotation().countHierarchyWithSystemFilter(context, dataset);
  }

  public CustomAnnotationCount countHierarchyTourAnnotations(Context context, String dataset) {
    return new CustomAnnotation().countHierarchyWithTourFilter(context, dataset);
  }

  public CustomAnnotationCount countHierarchyAnnotations(
      Context context, String dataset, String table) {
    return new CustomAnnotation().countHierarchyWithFilter(context, dataset, table);
  }

  public CustomAnnotationCount countHierarchySystemAnnotations(
      Context context, String dataset, String table) {
    return new CustomAnnotation().countHierarchyWithSystemFilter(context, dataset, table);
  }

  public CustomAnnotationCount countHierarchyTourAnnotations(
      Context context, String dataset, String table) {
    return new CustomAnnotation().countHierarchyWithTourFilter(context, dataset, table);
  }

  public CustomAnnotationCount countColumnAnnotationsWithFilter(
      Context context, String dataset, String table, String column) {
    return new CustomAnnotation()
        .countColumnAnnotationWithFilter(context, dataset, table, column).values().stream()
            .findFirst()
            .orElse(new CustomAnnotationCount(CustomAnnotation.AnnotationType.COLUMN));
  }

  public CustomAnnotationCount countColumnSystemAnnotationsWithFilter(
      Context context, String dataset, String table, String column) {
    return new CustomAnnotation()
        .countColumnSystemAnnotationWithFilter(context, dataset, table, column).values().stream()
            .findFirst()
            .orElse(CustomAnnotationCount.emptyColumnCount(dataset, table, column));
  }

  public CustomAnnotationCount countColumnTourAnnotationsWithFilter(
      Context context, String dataset, String table, String column) {
    return new CustomAnnotation()
        .countColumnTourAnnotationWithFilter(context, dataset, table, column).values().stream()
            .findFirst()
            .orElse(CustomAnnotationCount.emptyColumnCount(dataset, table, column));
  }

  public CustomAnnotationCount countTableAnnotationsWithFilter(
      Context context, String dataset, String table) {
    return new CustomAnnotation()
        .countTableAnnotationWithFilter(context, dataset, table).values().stream()
            .findFirst()
            .orElse(CustomAnnotationCount.emptyTableCount(dataset, table));
  }

  public CustomAnnotationCount countTableSystemAnnotationsWithFilter(
      Context context, String dataset, String table) {
    return new CustomAnnotation()
        .countTableSystemAnnotationWithFilter(context, dataset, table).values().stream()
            .findFirst()
            .orElse(CustomAnnotationCount.emptyTableCount(dataset, table));
  }

  public CustomAnnotationCount countTableTourAnnotationsWithFilter(
      Context context, String dataset, String table) {
    return new CustomAnnotation()
        .countTableTourAnnotationWithFilter(context, dataset, table).values().stream()
            .findFirst()
            .orElse(CustomAnnotationCount.emptyTableCount(dataset, table));
  }

  public CustomAnnotationCount countDatasetAnnotationsWithFilter(Context context, String dataset) {
    return new CustomAnnotation()
        .countDatasetAnnotationWithFilter(context, dataset).values().stream()
            .findFirst()
            .orElse(CustomAnnotationCount.emptyDatasetCount(dataset));
  }

  public CustomAnnotationCount countDatasetSystemAnnotationsWithFilter(
      Context context, String dataset) {
    return new CustomAnnotation()
        .countDatasetSystemAnnotationWithFilter(context, dataset).values().stream()
            .findFirst()
            .orElse(CustomAnnotationCount.emptyDatasetCount(dataset));
  }

  public CustomAnnotationCount countDatasetTourAnnotationsWithFilter(
      Context context, String dataset) {
    return new CustomAnnotation()
        .countDatasetTourAnnotationWithFilter(context, dataset).values().stream()
            .findFirst()
            .orElse(CustomAnnotationCount.emptyDatasetCount(dataset));
  }

  public Set<CustomAnnotationCount> countColumnAnnotations(Context context) {
    return new CustomAnnotation()
        .countColumnAnnotations(context).values().stream().collect(sortedByCount());
  }

  public Set<CustomAnnotationCount> countColumnSystemAnnotations(Context context) {
    return new CustomAnnotation()
        .countColumnSystemAnnotations(context).values().stream().collect(sortedByCount());
  }

  public Set<CustomAnnotationCount> countColumnTourAnnotations(Context context) {
    return new CustomAnnotation()
        .countColumnTourAnnotations(context).values().stream().collect(sortedByCount());
  }

  public Set<CustomAnnotationCount> countTableAnnotations(Context context) {
    return new CustomAnnotation()
        .countTableAnnotations(context).values().stream().collect(sortedByCount());
  }

  public Set<CustomAnnotationCount> countTableSystemAnnotations(Context context) {
    return new CustomAnnotation()
        .countTableSystemAnnotations(context).values().stream().collect(sortedByCount());
  }

  public Set<CustomAnnotationCount> countTableTourAnnotations(Context context) {
    return new CustomAnnotation()
        .countTableTourAnnotations(context).values().stream().collect(sortedByCount());
  }

  public Set<CustomAnnotationCount> countDatasetAnnotations(Context context) {
    return new CustomAnnotation()
        .countDatasetAnnotations(context).values().stream().collect(sortedByCount());
  }

  public Set<CustomAnnotationCount> countDatasetSystemAnnotations(Context context) {
    return new CustomAnnotation()
        .countDatasetSystemAnnotations(context).values().stream().collect(sortedByCount());
  }

  public Set<CustomAnnotationCount> countDatasetTourAnnotations(Context context) {
    return new CustomAnnotation()
        .countDatasetTourAnnotations(context).values().stream().collect(sortedByCount());
  }

  public Set<CustomAnnotationCount> countAllAnnotations(Context context) {
    return new CustomAnnotation()
        .countAllAnnotations(context).values().stream().collect(sortedByCount());
  }

  public Set<CustomAnnotationCount> countAllSystemAnnotations(Context context) {
    return new CustomAnnotation()
        .countAllSystemAnnotations(context).values().stream().collect(sortedByCount());
  }

  public Set<CustomAnnotationCount> countAllTourAnnotations(Context context) {
    return new CustomAnnotation()
        .countAllTourAnnotations(context).values().stream().collect(sortedByCount());
  }

  private CustomAnnotation buildAnnotation(
      CustomAnnotation.AnnotationType annotationType,
      String dataset,
      String table,
      String column,
      String note,
      String createdBy,
      long createdOn,
      boolean isDataTour) {
    CustomAnnotation annotation = new CustomAnnotation();
    annotation.setAnnotationType(annotationType);
    annotation.setDataset(dataset);
    annotation.setTable(table);
    annotation.setColumn(column);
    annotation.setNote(note);
    annotation.setCreatedOn(createdOn);
    annotation.setCreatedBy(createdBy);
    annotation.setDataTour(isDataTour);
    return annotation;
  }

  private Set<CustomAnnotation> filterForSystemComments(Set<CustomAnnotation> set) {
    return set.stream().filter(CustomAnnotation::isSystemComment).collect(sortedByCreatedBy());
  }

  private Set<CustomAnnotation> filterForDataTourComments(Set<CustomAnnotation> set) {
    return set.stream().filter(CustomAnnotation::isDataTour).collect(sortedByCreatedBy());
  }

  private Set<CustomAnnotation> limitNumberComments(Set<CustomAnnotation> set, int max) {
    return set.stream().limit(max).collect(sortedByCreatedBy());
  }

  private Collector<CustomAnnotation, ?, SortedSet<CustomAnnotation>> sortedByCreatedBy() {
    return toCollection(
        () -> new TreeSet<>(comparingLong(CustomAnnotation::getCreatedOn).reversed()));
  }

  private Collector<CustomAnnotationCount, ?, SortedSet<CustomAnnotationCount>> sortedByCount() {
    return toCollection(
        () -> new TreeSet<>(comparingLong(CustomAnnotationCount::getCount).reversed()));
  }
}
