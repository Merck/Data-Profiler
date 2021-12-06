package com.dataprofiler.util.objects;

/*-
 * 
 * dataprofiler-util
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
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Const;
import com.dataprofiler.util.Context;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomAnnotation extends PurgableDatasetObject<CustomAnnotation> {
  private static final Logger logger = LoggerFactory.getLogger(CustomAnnotation.class);

  public static final String SYSTEM_COMMENT_AUTHOR = "Data Profiler system";

  private AnnotationType annotationType;
  private String dataset;
  private String table;
  private String column;
  private String note;
  private String createdBy;
  private Long createdOn;
  private UUID uuid;
  private boolean isDataTour;

  @JsonIgnore private static CustomAnnotationVisibilityProvider visibilityProvider;

  static {
    visibilityProvider = new CustomAnnotationVisibilityProvider();
  }

  private static final Function<CustomAnnotation, String> DATASET_COUNT_KEY_GENERATOR =
      (annotation) ->
          format("%s %s", annotation.getAnnotationType(), annotation.getDataset()).trim();

  private static final Function<CustomAnnotation, String> TABLE_COUNT_KEY_GENERATOR =
      (annotation) ->
          format(
                  "%s %s %s",
                  annotation.getAnnotationType(), annotation.getDataset(), annotation.getTable())
              .trim();

  private static final Function<CustomAnnotation, String> COLUMN_COUNT_KEY_GENERATOR =
      (annotation) ->
          format(
                  "%s %s %s %s",
                  annotation.getAnnotationType(),
                  annotation.getDataset(),
                  annotation.getTable(),
                  annotation.getColumn())
              .trim();

  private static final Predicate<CustomAnnotation> SYSTEM_COMMENT_FILTER =
      (annotation) -> nonNull(annotation) && annotation.isSystemComment();

  private static final Predicate<CustomAnnotation> TOUR_COMMENT_FILTER =
      (annotation) -> nonNull(annotation) && annotation.isDataTour();

  private static final Predicate<CustomAnnotation> ACCEPT_ALL_FILTER = (annotation) -> true;

  public enum AnnotationType {
    COLUMN,
    TABLE,
    DATASET,
    ANNOTATION
  }

  public CustomAnnotation() {
    super(Const.ACCUMULO_CUSTOM_ANNOTATIONS_ENV_KEY);
    this.uuid = UUID.randomUUID();
    ZoneOffset zone = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    this.createdOn = LocalDateTime.now().toEpochSecond(zone);
    this.isDataTour = false;
  }

  public CustomAnnotation(
      String annotationType,
      String dataset,
      String table,
      String column,
      String note,
      String createdBy,
      Long createdOn) {
    this(annotationType, dataset, table, column, note, createdBy, createdOn, false);
  }

  public CustomAnnotation(
      String annotationType,
      String dataset,
      String table,
      String column,
      String note,
      String createdBy,
      Long createdOn,
      boolean isDataTour) {
    this();
    this.annotationType = AnnotationType.valueOf(annotationType.toUpperCase().trim());
    this.dataset = dataset;
    this.table = table;
    this.column = column;
    this.note = note;
    this.createdBy = createdBy;
    this.createdOn = createdOn;
    this.isDataTour = isDataTour;
  }

  @Override
  public CustomAnnotation fromEntry(Entry<Key, Value> entry) throws InvalidDataFormat {
    try {
      if (logger.isTraceEnabled()) {
        logger.trace("key=" + entry.getKey().toString() + " value=" + entry.getValue().toString());
      }
      CustomAnnotation annotation =
          mapper.readValue(entry.getValue().toString(), CustomAnnotation.class);
      annotation.updatePropertiesFromEntry(entry);
      return annotation;
    } catch (IOException e) {
      throw new InvalidDataFormat(e.toString());
    }
  }

  @Override
  public Key createAccumuloKey() throws InvalidDataFormat {
    Key key = null;
    switch (this.annotationType) {
      case COLUMN:
        key =
            new Key(
                joinKeyComponents(this.dataset, this.table, this.column),
                this.annotationType.toString(),
                this.uuid.toString(),
                this.visibility);
        break;
      case TABLE:
        key =
            new Key(
                joinKeyComponents(this.dataset, this.table),
                this.annotationType.toString(),
                this.uuid.toString(),
                this.visibility);
        break;
      case DATASET:
        key =
            new Key(
                this.dataset,
                this.annotationType.toString(),
                this.uuid.toString(),
                this.visibility);
        break;
      default:
        throw new InvalidDataFormat("Cannot create accumulo key for CustomAnnotation");
    }
    return key;
  }

  /**
   * lookups the correct visibility and overwrites the supplied visibility then saves to accumulo
   *
   * @param context
   */
  @Override
  public void put(Context context) throws BasicAccumuloException, IOException {
    if (isNull(context)) {
      return;
    }

    CustomAnnotation.AnnotationType type = this.getAnnotationType();
    String visibility = null;
    switch (type) {
      case DATASET:
        visibility =
            visibilityProvider.lookupVisibilityExpressionMetadata(context, this.getDataset());
        break;
      case TABLE:
        visibility =
            visibilityProvider.lookupVisibilityExpressionMetadata(
                context, this.getDataset(), this.getTable());
        break;
      case COLUMN:
        visibility =
            visibilityProvider.lookupVisibilityExpressionMetadata(
                context, this.getDataset(), this.getTable(), this.getColumn());
        break;
      default:
        break;
    }

    put(context, visibility);
  }

  /**
   * lookups the correct visibility and overwrites the supplied visibility then saves to accumulo
   *
   * @param context
   * @param version
   */
  public void put(Context context, MetadataVersionObject version)
      throws BasicAccumuloException, IOException {
    if (isNull(context) || isNull(version)) {
      return;
    }

    if (logger.isDebugEnabled()) {
      logger.debug(format("saving comment for specific version: %s", version.getId()));
    }
    CustomAnnotation.AnnotationType type = this.getAnnotationType();
    String visibility = null;
    switch (type) {
      case DATASET:
        visibility =
            visibilityProvider.lookupVisibilityExpressionMetadata(
                context, version, this.getDataset());
        break;
      case TABLE:
        visibility =
            visibilityProvider.lookupVisibilityExpressionMetadata(
                context, version, this.getDataset(), this.getTable());
        break;
      case COLUMN:
        visibility =
            visibilityProvider.lookupVisibilityExpressionMetadata(
                context, version, this.getDataset(), this.getTable(), this.getColumn());
        break;
      default:
        break;
    }

    put(context, visibility);
  }

  /**
   * overwrites with the supplied visibility then saves to accumulo
   *
   * @param context
   */
  protected void put(Context context, String visibility)
      throws BasicAccumuloException, IOException {
    if (isNull(context)) {
      return;
    }

    if (logger.isDebugEnabled()) {
      logger.debug(format("applying annotation visibility [%s]", visibility));
    }
    this.setVisibility(visibility);
    if (logger.isDebugEnabled()) {
      logger.debug(format("inserting %s", this));
    }
    super.put(context);
  }

  /**
   * scan for all annotations of type column
   *
   * @param context accumulo context
   */
  public ObjectScannerIterable<CustomAnnotation> scanColumns(Context context) {
    return super.scan(context).fetchColumnFamily(AnnotationType.COLUMN.toString());
  }

  /**
   * scan for all annotations of type table
   *
   * @param context accumulo context
   */
  public ObjectScannerIterable<CustomAnnotation> scanTables(Context context) {
    return super.scan(context).fetchColumnFamily(AnnotationType.TABLE.toString());
  }

  /**
   * scan for all annotations of type dataset
   *
   * @param context accumulo context
   */
  public ObjectScannerIterable<CustomAnnotation> scanDatasets(Context context) {
    return super.scan(context).fetchColumnFamily(AnnotationType.DATASET.toString());
  }

  /**
   * scan for all annotations on a given dataset and its subtables and subcolumns
   *
   * @param context accumulo context accumulo context
   * @param dataset name of dataset
   */
  public CustomAnnotationCount countHierarchyWithFilter(Context context, String dataset) {
    Range range =
        new Range(joinKeyComponents(dataset), joinKeyComponents(dataset, Const.HIGH_BYTE));
    Map<String, CustomAnnotationCount> map =
        genericCountWithRange(context, range, DATASET_COUNT_KEY_GENERATOR);
    final long total = map.values().stream().mapToLong(CustomAnnotationCount::getCount).sum();
    return new CustomAnnotationCount(AnnotationType.DATASET, dataset, "", "", total);
  }

  /**
   * scan for all annotations on a given dataset and its subtables and subcolumns, filters to count
   * only system annotations
   *
   * @param context accumulo context accumulo context
   * @param dataset name of dataset
   */
  public CustomAnnotationCount countHierarchyWithSystemFilter(Context context, String dataset) {
    Range range =
        new Range(joinKeyComponents(dataset), joinKeyComponents(dataset, Const.HIGH_BYTE));
    Map<String, CustomAnnotationCount> map =
        genericCountWithRange(context, range, DATASET_COUNT_KEY_GENERATOR, SYSTEM_COMMENT_FILTER);
    final long total = map.values().stream().mapToLong(CustomAnnotationCount::getCount).sum();
    return new CustomAnnotationCount(AnnotationType.DATASET, dataset, "", "", total);
  }

  /**
   * scan for all annotations on a given dataset and its subtables and subcolumns, filters to count
   * only tour annotations
   *
   * @param context accumulo context accumulo context
   * @param dataset name of dataset
   */
  public CustomAnnotationCount countHierarchyWithTourFilter(Context context, String dataset) {
    Range range =
        new Range(joinKeyComponents(dataset), joinKeyComponents(dataset, Const.HIGH_BYTE));
    Map<String, CustomAnnotationCount> map =
        genericCountWithRange(context, range, DATASET_COUNT_KEY_GENERATOR, TOUR_COMMENT_FILTER);
    final long total = map.values().stream().mapToLong(CustomAnnotationCount::getCount).sum();
    return new CustomAnnotationCount(AnnotationType.DATASET, dataset, "", "", total);
  }

  /**
   * scan for all annotations on a given table and its subcolumns
   *
   * @param context accumulo context accumulo context
   * @param dataset name of dataset
   * @param table name of table
   */
  public CustomAnnotationCount countHierarchyWithFilter(
      Context context, String dataset, String table) {
    Range range =
        new Range(
            joinKeyComponents(dataset, table), joinKeyComponents(dataset, table, Const.HIGH_BYTE));
    Map<String, CustomAnnotationCount> map =
        genericCountWithRange(context, range, TABLE_COUNT_KEY_GENERATOR);
    final long total = map.values().stream().mapToLong(CustomAnnotationCount::getCount).sum();
    return new CustomAnnotationCount(AnnotationType.TABLE, dataset, table, "", total);
  }

  /**
   * scan for all annotations on a given table and its subcolumns, filter to count only system
   * annotations
   *
   * @param context accumulo context accumulo context
   * @param dataset name of dataset
   * @param table name of table
   */
  public CustomAnnotationCount countHierarchyWithSystemFilter(
      Context context, String dataset, String table) {
    Range range =
        new Range(
            joinKeyComponents(dataset, table), joinKeyComponents(dataset, table, Const.HIGH_BYTE));
    Map<String, CustomAnnotationCount> map =
        genericCountWithRange(context, range, TABLE_COUNT_KEY_GENERATOR, SYSTEM_COMMENT_FILTER);
    final long total = map.values().stream().mapToLong(CustomAnnotationCount::getCount).sum();
    return new CustomAnnotationCount(AnnotationType.TABLE, dataset, table, "", total);
  }

  /**
   * scan for all annotations on a given table and its subcolumns, filter to count only tour
   * annotations
   *
   * @param context accumulo context accumulo context
   * @param dataset name of dataset
   * @param table name of table
   */
  public CustomAnnotationCount countHierarchyWithTourFilter(
      Context context, String dataset, String table) {
    Range range =
        new Range(
            joinKeyComponents(dataset, table), joinKeyComponents(dataset, table, Const.HIGH_BYTE));
    Map<String, CustomAnnotationCount> map =
        genericCountWithRange(context, range, TABLE_COUNT_KEY_GENERATOR, TOUR_COMMENT_FILTER);
    final long total = map.values().stream().mapToLong(CustomAnnotationCount::getCount).sum();
    return new CustomAnnotationCount(AnnotationType.TABLE, dataset, table, "", total);
  }

  /**
   * count all the dataset annotations, filter to a single dataset
   *
   * @param context accumulo context
   * @param dataset name of dataset
   */
  public Map<String, CustomAnnotationCount> countDatasetAnnotationWithFilter(
      Context context, String dataset) {
    String colFamily = AnnotationType.DATASET.toString();
    Range range =
        new Range(joinKeyComponents(dataset), joinKeyComponents(dataset, Const.HIGH_BYTE));
    return genericCountWithColFamilyAndRange(
        context, colFamily, range, DATASET_COUNT_KEY_GENERATOR);
  }

  /**
   * count all the dataset annotations, filter to a single dataset, filter to count only system
   * annotations
   *
   * @param context accumulo context
   * @param dataset name of dataset
   */
  public Map<String, CustomAnnotationCount> countDatasetSystemAnnotationWithFilter(
      Context context, String dataset) {
    String colFamily = AnnotationType.DATASET.toString();
    Range range =
        new Range(joinKeyComponents(dataset), joinKeyComponents(dataset, Const.HIGH_BYTE));
    return genericCountWithColFamilyAndRange(
        context, colFamily, range, DATASET_COUNT_KEY_GENERATOR, SYSTEM_COMMENT_FILTER);
  }

  /**
   * count all the dataset annotations, filter to a single dataset, filter to count only tour
   * annotations
   *
   * @param context accumulo context
   * @param dataset name of dataset
   */
  public Map<String, CustomAnnotationCount> countDatasetTourAnnotationWithFilter(
      Context context, String dataset) {
    String colFamily = AnnotationType.DATASET.toString();
    Range range =
        new Range(joinKeyComponents(dataset), joinKeyComponents(dataset, Const.HIGH_BYTE));
    return genericCountWithColFamilyAndRange(
        context, colFamily, range, DATASET_COUNT_KEY_GENERATOR, TOUR_COMMENT_FILTER);
  }

  /**
   * count all the dataset annotations, filter to a single table
   *
   * @param context accumulo context
   * @param dataset name of dataset
   */
  public Map<String, CustomAnnotationCount> countTableAnnotationWithFilter(
      Context context, String dataset, String table) {
    String colFamily = AnnotationType.TABLE.toString();
    Range range = new Range(joinKeyComponents(dataset, table));
    return genericCountWithColFamilyAndRange(context, colFamily, range, TABLE_COUNT_KEY_GENERATOR);
  }

  /**
   * count all the dataset annotations, filter to a single table, filter to count system annotations
   *
   * @param context accumulo context
   * @param dataset name of dataset
   */
  public Map<String, CustomAnnotationCount> countTableSystemAnnotationWithFilter(
      Context context, String dataset, String table) {
    String colFamily = AnnotationType.TABLE.toString();
    Range range = new Range(joinKeyComponents(dataset, table));
    return genericCountWithColFamilyAndRange(
        context, colFamily, range, TABLE_COUNT_KEY_GENERATOR, SYSTEM_COMMENT_FILTER);
  }

  /**
   * count all the dataset annotations, filter to a single table, filter to count tour annotations
   *
   * @param context accumulo context
   * @param dataset name of dataset
   */
  public Map<String, CustomAnnotationCount> countTableTourAnnotationWithFilter(
      Context context, String dataset, String table) {
    String colFamily = AnnotationType.TABLE.toString();
    Range range = new Range(joinKeyComponents(dataset, table));
    return genericCountWithColFamilyAndRange(
        context, colFamily, range, TABLE_COUNT_KEY_GENERATOR, TOUR_COMMENT_FILTER);
  }

  /**
   * count all the dataset annotations, filter to a single column
   *
   * @param context accumulo context
   * @param dataset name of dataset
   */
  public Map<String, CustomAnnotationCount> countColumnAnnotationWithFilter(
      Context context, String dataset, String table, String column) {
    String colFamily = AnnotationType.COLUMN.toString();
    Range range = new Range(joinKeyComponents(dataset, table, column));
    return genericCountWithColFamilyAndRange(context, colFamily, range, COLUMN_COUNT_KEY_GENERATOR);
  }

  /**
   * count all the dataset annotations, filter to a single column, filter to count only system
   * annotations
   *
   * @param context accumulo context
   * @param dataset name of dataset
   */
  public Map<String, CustomAnnotationCount> countColumnSystemAnnotationWithFilter(
      Context context, String dataset, String table, String column) {
    String colFamily = AnnotationType.COLUMN.toString();
    Range range = new Range(joinKeyComponents(dataset, table, column));
    return genericCountWithColFamilyAndRange(
        context, colFamily, range, COLUMN_COUNT_KEY_GENERATOR, SYSTEM_COMMENT_FILTER);
  }

  /**
   * count all the dataset annotations, filter to a single column, filter to count only tour
   * annotations
   *
   * @param context accumulo context
   * @param dataset name of dataset
   */
  public Map<String, CustomAnnotationCount> countColumnTourAnnotationWithFilter(
      Context context, String dataset, String table, String column) {
    String colFamily = AnnotationType.COLUMN.toString();
    Range range = new Range(joinKeyComponents(dataset, table, column));
    return genericCountWithColFamilyAndRange(
        context, colFamily, range, COLUMN_COUNT_KEY_GENERATOR, TOUR_COMMENT_FILTER);
  }

  /**
   * count all annotations applied to datasets, return a map of count results
   *
   * @param context accumulo context accumulo context
   * @return map of count results
   */
  public Map<String, CustomAnnotationCount> countDatasetAnnotations(Context context) {
    String colFamily = AnnotationType.DATASET.toString();
    return genericCountWithColFamily(context, colFamily, DATASET_COUNT_KEY_GENERATOR);
  }

  /**
   * count all annotations applied to datasets, return a map of count results. filter to count only
   * system annotations
   *
   * @param context accumulo context accumulo context
   * @return map of count results
   */
  public Map<String, CustomAnnotationCount> countDatasetSystemAnnotations(Context context) {
    String colFamily = AnnotationType.DATASET.toString();
    return genericCountWithColFamily(
        context, colFamily, DATASET_COUNT_KEY_GENERATOR, SYSTEM_COMMENT_FILTER);
  }

  /**
   * count all annotations applied to datasets, return a map of count results. filter to count only
   * tour annotations
   *
   * @param context accumulo context accumulo context
   * @return map of count results
   */
  public Map<String, CustomAnnotationCount> countDatasetTourAnnotations(Context context) {
    String colFamily = AnnotationType.DATASET.toString();
    return genericCountWithColFamily(
        context, colFamily, DATASET_COUNT_KEY_GENERATOR, TOUR_COMMENT_FILTER);
  }

  /**
   * count all annotations applied to tables, return a map of count results
   *
   * @param context accumulo context accumulo context
   * @return map of count results
   */
  public Map<String, CustomAnnotationCount> countTableAnnotations(Context context) {
    String colFamily = AnnotationType.TABLE.toString();
    return genericCountWithColFamily(context, colFamily, TABLE_COUNT_KEY_GENERATOR);
  }

  /**
   * count all annotations applied to tables, return a map of count results. filter to count only
   * system annotations
   *
   * @param context accumulo context accumulo context
   * @return map of count results
   */
  public Map<String, CustomAnnotationCount> countTableSystemAnnotations(Context context) {
    String colFamily = AnnotationType.TABLE.toString();
    return genericCountWithColFamily(
        context, colFamily, TABLE_COUNT_KEY_GENERATOR, SYSTEM_COMMENT_FILTER);
  }

  /**
   * count all annotations applied to tables, return a map of count results. filter to count only
   * tour annotations
   *
   * @param context accumulo context accumulo context
   * @return map of count results
   */
  public Map<String, CustomAnnotationCount> countTableTourAnnotations(Context context) {
    String colFamily = AnnotationType.TABLE.toString();
    return genericCountWithColFamily(
        context, colFamily, TABLE_COUNT_KEY_GENERATOR, TOUR_COMMENT_FILTER);
  }

  /**
   * count all annotations applied to columns, return a map of count results
   *
   * @param context accumulo context accumulo context
   * @return map of count results
   */
  public Map<String, CustomAnnotationCount> countColumnAnnotations(Context context) {
    String colFamily = AnnotationType.COLUMN.toString();
    return genericCountWithColFamily(context, colFamily, COLUMN_COUNT_KEY_GENERATOR);
  }

  /**
   * count all annotations applied to columns, return a map of count results. filter to count only
   * system annotations
   *
   * @param context accumulo context accumulo context
   * @return map of count results
   */
  public Map<String, CustomAnnotationCount> countColumnSystemAnnotations(Context context) {
    String colFamily = AnnotationType.COLUMN.toString();
    return genericCountWithColFamily(
        context, colFamily, COLUMN_COUNT_KEY_GENERATOR, SYSTEM_COMMENT_FILTER);
  }

  /**
   * count all annotations applied to columns, return a map of count results. filter to count only
   * system annotations
   *
   * @param context accumulo context accumulo context
   * @return map of count results
   */
  public Map<String, CustomAnnotationCount> countColumnTourAnnotations(Context context) {
    String colFamily = AnnotationType.COLUMN.toString();
    return genericCountWithColFamily(
        context, colFamily, COLUMN_COUNT_KEY_GENERATOR, TOUR_COMMENT_FILTER);
  }

  /**
   * count all annotations applied any dataset, table, or column, return a map of count results
   *
   * @param context accumulo context accumulo context
   * @return map of count results
   */
  public Map<String, CustomAnnotationCount> countAllAnnotations(Context context) {
    ObjectScannerIterable<CustomAnnotation> iterator = super.scan(context).setBatch(true);
    return this.genericCount(iterator, COLUMN_COUNT_KEY_GENERATOR, ACCEPT_ALL_FILTER);
  }

  /**
   * count all annotations applied any dataset, table, or column, return a map of count results.
   * filter to count only system annotations
   *
   * @param context accumulo context accumulo context
   * @return map of count results
   */
  public Map<String, CustomAnnotationCount> countAllSystemAnnotations(Context context) {
    ObjectScannerIterable<CustomAnnotation> iterator = super.scan(context).setBatch(true);
    return this.genericCount(iterator, COLUMN_COUNT_KEY_GENERATOR, SYSTEM_COMMENT_FILTER);
  }

  /**
   * count all annotations applied any dataset, table, or column, return a map of count results.
   * filter to count only data tour annotations
   *
   * @param context accumulo context accumulo context
   * @return map of count results
   */
  public Map<String, CustomAnnotationCount> countAllTourAnnotations(Context context) {
    ObjectScannerIterable<CustomAnnotation> iterator = super.scan(context).setBatch(true);
    return this.genericCount(iterator, COLUMN_COUNT_KEY_GENERATOR, TOUR_COMMENT_FILTER);
  }

  private Map<String, CustomAnnotationCount> genericCount(
      ObjectScannerIterable<CustomAnnotation> iterator,
      Function<CustomAnnotation, String> genKey,
      Predicate<CustomAnnotation> serverSideFilter) {
    Map<String, CustomAnnotationCount> map = new TreeMap<>(Comparator.naturalOrder());
    for (CustomAnnotation annotation : iterator) {
      boolean shouldProcess = serverSideFilter.test(annotation);
      if (!shouldProcess) {
        continue;
      }
      BiFunction<String, CustomAnnotationCount, CustomAnnotationCount> incrementOrInit =
          (key, value) -> {
            return value == null
                ? new CustomAnnotationCount(
                    annotation.getAnnotationType(),
                    annotation.getDataset(),
                    annotation.getTable(),
                    annotation.getColumn(),
                    1)
                : value.incrementCount();
          };
      String key = genKey.apply(annotation);
      map.compute(key, incrementOrInit);
    }
    return map;
  }

  private Map<String, CustomAnnotationCount> genericCountWithColFamily(
      Context context, String colFamily, Function<CustomAnnotation, String> genKey) {
    ObjectScannerIterable<CustomAnnotation> itr =
        super.scan(context).setBatch(true).fetchColumnFamily(colFamily);
    return this.genericCount(itr, genKey, ACCEPT_ALL_FILTER);
  }

  private Map<String, CustomAnnotationCount> genericCountWithColFamily(
      Context context,
      String colFamily,
      Function<CustomAnnotation, String> genKey,
      Predicate<CustomAnnotation> serverSideFilter) {
    ObjectScannerIterable<CustomAnnotation> itr =
        super.scan(context).setBatch(true).fetchColumnFamily(colFamily);
    return this.genericCount(itr, genKey, serverSideFilter);
  }

  private Map<String, CustomAnnotationCount> genericCountWithColFamilyAndRange(
      Context context, String colFamily, Range range, Function<CustomAnnotation, String> genKey) {
    ObjectScannerIterable<CustomAnnotation> itr =
        super.scan(context).setBatch(true).fetchColumnFamily(colFamily).addRange(range);
    return this.genericCount(itr, genKey, ACCEPT_ALL_FILTER);
  }

  private Map<String, CustomAnnotationCount> genericCountWithColFamilyAndRange(
      Context context,
      String colFamily,
      Range range,
      Function<CustomAnnotation, String> genKey,
      Predicate<CustomAnnotation> serverSideFilter) {
    ObjectScannerIterable<CustomAnnotation> itr =
        super.scan(context).setBatch(true).fetchColumnFamily(colFamily).addRange(range);
    return this.genericCount(itr, genKey, serverSideFilter);
  }

  private Map<String, CustomAnnotationCount> genericCountWithRange(
      Context context, Range range, Function<CustomAnnotation, String> genKey) {
    ObjectScannerIterable<CustomAnnotation> itr =
        super.scan(context).setBatch(true).addRange(range);
    return this.genericCount(itr, genKey, ACCEPT_ALL_FILTER);
  }

  private Map<String, CustomAnnotationCount> genericCountWithRange(
      Context context,
      Range range,
      Function<CustomAnnotation, String> genKey,
      Predicate<CustomAnnotation> serverSideFilter) {
    ObjectScannerIterable<CustomAnnotation> itr =
        super.scan(context).setBatch(true).addRange(range);
    return this.genericCount(itr, genKey, serverSideFilter);
  }

  /**
   * scan for all annotations on a given dataset
   *
   * @param context accumulo context accumulo context
   * @param dataset name of dataset
   */
  public ObjectScannerIterable<CustomAnnotation> scanByDataset(Context context, String dataset) {
    return super.scan(context)
        .fetchColumnFamily(AnnotationType.DATASET.toString())
        .addRange(new Range(joinKeyComponents(dataset)));
  }

  /**
   * scan for all annotations on a given table
   *
   * @param context accumulo context accumulo context
   * @param dataset name of dataset
   * @param table name of table
   */
  public ObjectScannerIterable<CustomAnnotation> scanByTable(
      Context context, String dataset, String table) {
    return super.scan(context)
        .fetchColumnFamily(AnnotationType.TABLE.toString())
        .addRange(new Range(joinKeyComponents(dataset, table)));
  }

  /**
   * scan for all annotations on a given column
   *
   * @param context accumulo context
   * @param dataset name of dataset
   * @param table name of table
   * @param column name of column
   */
  public ObjectScannerIterable<CustomAnnotation> scanByColumn(
      Context context, String dataset, String table, String column) {
    return super.scan(context)
        .fetchColumnFamily(AnnotationType.COLUMN.toString())
        .addRange(new Range(joinKeyComponents(dataset, table, column)));
  }

  /**
   * scan for all annotations on a given dataset and its subtables and subcolumns
   *
   * @param context accumulo context accumulo context
   * @param dataset name of dataset
   */
  public ObjectScannerIterable<CustomAnnotation> scanHierarchyByDataset(
      Context context, String dataset) {
    return super.scan(context)
        .addRange(
            new Range(joinKeyComponents(dataset), joinKeyComponents(dataset, Const.HIGH_BYTE)));
  }

  /**
   * scan for all annotations on a given table and its subcolumns
   *
   * @param context accumulo context accumulo context
   * @param dataset name of dataset
   * @param table name of table
   */
  public ObjectScannerIterable<CustomAnnotation> scanHierarchyByTable(
      Context context, String dataset, String table) {
    ObjectScannerIterable<CustomAnnotation> itr =
        super.scan(context)
            .addRange(
                new Range(
                    joinKeyComponents(dataset, table),
                    joinKeyComponents(dataset, table, Const.HIGH_BYTE)));
    return itr;
  }

  /**
   * fetch a single column annotation
   *
   * @param context accumulo context
   * @param dataset name of dataset
   * @param table name of table
   * @param column name of column
   */
  public static CustomAnnotation fetchColumnAnnotation(
      Context context, String dataset, String table, String column, String id) {
    CustomAnnotation annotation = new CustomAnnotation();
    annotation.annotationType = AnnotationType.COLUMN;
    annotation.dataset = dataset;
    annotation.table = table;
    annotation.column = column;
    annotation.uuid = UUID.fromString(id);
    if (logger.isDebugEnabled()) {
      logger.debug("fetching " + annotation);
    }
    return annotation.fetch(context, annotation.createAccumuloKey());
  }

  /**
   * fetch a single table annotation
   *
   * @param context accumulo context
   * @param dataset name of dataset
   * @param table name of table
   */
  public static CustomAnnotation fetchTableAnnotation(
      Context context, String dataset, String table, String id) {
    CustomAnnotation annotation = new CustomAnnotation();
    annotation.annotationType = AnnotationType.TABLE;
    annotation.dataset = dataset;
    annotation.table = table;
    annotation.uuid = UUID.fromString(id);
    if (logger.isDebugEnabled()) {
      logger.debug("fetching " + annotation);
    }
    return annotation.fetch(context, annotation.createAccumuloKey());
  }

  /**
   * fetch a single dataset annotation
   *
   * @param context accumulo context
   * @param dataset name of dataset
   */
  public static CustomAnnotation fetchDatasetAnnotation(
      Context context, String dataset, String id) {
    CustomAnnotation annotation = new CustomAnnotation();
    annotation.annotationType = AnnotationType.DATASET;
    annotation.dataset = dataset;
    annotation.uuid = UUID.fromString(id);
    if (logger.isDebugEnabled()) {
      logger.debug("fetching " + annotation);
    }
    return annotation.fetch(context, annotation.createAccumuloKey());
  }

  public static Boolean deleteColumnAnnotation(
      Context context,
      String dataset,
      String table,
      String column,
      String id,
      Function<CustomAnnotation, Boolean> hasDeleteAuths)
      throws BasicAccumuloException {
    CustomAnnotation annotation = fetchColumnAnnotation(context, dataset, table, column, id);
    if (hasDeleteAuths.apply(annotation)) {
      annotation.destroy(context);
      return true;
    }
    return false;
  }

  public static Boolean deleteTableAnnotation(
      Context context,
      String dataset,
      String table,
      String id,
      Function<CustomAnnotation, Boolean> hasDeleteAuths)
      throws BasicAccumuloException {
    CustomAnnotation annotation = fetchTableAnnotation(context, dataset, table, id);
    if (hasDeleteAuths.apply(annotation)) {
      annotation.destroy(context);
      return true;
    }
    return false;
  }

  public static Boolean deleteDatasetAnnotation(
      Context context,
      String dataset,
      String id,
      Function<CustomAnnotation, Boolean> hasDeleteAuths)
      throws BasicAccumuloException {
    CustomAnnotation annotation = fetchDatasetAnnotation(context, dataset, id);
    if (hasDeleteAuths.apply(annotation)) {
      annotation.destroy(context);
      return true;
    }
    return false;
  }

  public AnnotationType getAnnotationType() {
    return annotationType;
  }

  public void setAnnotationType(AnnotationType annotationType) {
    this.annotationType = annotationType;
  }

  public String getDataset() {
    return dataset;
  }

  public void setDataset(String dataset) {
    this.dataset = dataset;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public String getColumn() {
    return column;
  }

  public void setColumn(String column) {
    this.column = column;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public Long getCreatedOn() {
    return createdOn;
  }

  public void setCreatedOn(long createdOn) {
    this.createdOn = createdOn;
  }

  public UUID getUuid() {
    return this.uuid;
  }

  public boolean isDataTour() {
    return isDataTour;
  }

  public void setDataTour(boolean dataTour) {
    isDataTour = dataTour;
  }

  public boolean isSystemComment() {
    if (isNull(createdBy)) {
      return false;
    }

    return SYSTEM_COMMENT_AUTHOR.equalsIgnoreCase(createdBy);
  }

  @JsonIgnore
  public CustomAnnotationVisibilityProvider getVisibilityProvider() {
    return visibilityProvider;
  }

  public void setVisibilityProvider(CustomAnnotationVisibilityProvider visibilityProvider) {
    CustomAnnotation.visibilityProvider = visibilityProvider;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("annotationType", annotationType)
        .append("visibility", visibility)
        .append("dataset", dataset)
        .append("table", table)
        .append("column", column)
        .append("uuid", uuid)
        .append("note", note)
        .append("createdBy", createdBy)
        .append("createdOn", createdOn)
        .append("isDataTour", isDataTour)
        .toString();
  }

  @Override
  public int hashCode() {
    // you pick a hard-coded, randomly chosen, non-zero, odd number
    // ideally different for each class
    return new HashCodeBuilder(17, 37)
        .append(annotationType)
        .append(visibility)
        .append(dataset)
        .append(table)
        .append(column)
        .append(uuid)
        .append(note)
        .append(createdBy)
        .append(createdOn)
        .append(isDataTour)
        .toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }
    CustomAnnotation rhs = (CustomAnnotation) obj;
    return new EqualsBuilder()
        .appendSuper(super.equals(obj))
        .append(annotationType, rhs.annotationType)
        .append(visibility, rhs.visibility)
        .append(dataset, rhs.dataset)
        .append(table, rhs.table)
        .append(column, rhs.column)
        .append(uuid, rhs.uuid)
        .append(note, rhs.note)
        .append(createdBy, rhs.createdBy)
        .append(createdOn, rhs.createdOn)
        .append(isDataTour, rhs.isDataTour)
        .isEquals();
  }
}
