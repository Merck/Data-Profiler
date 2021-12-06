package com.dataprofiler.provider;

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

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;

import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.MetadataVersionObject;
import com.dataprofiler.util.objects.ObjectScannerIterable;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;

/** provider gives helper methods to work with column metadata */
public class ColumnMetadataProvider extends VersionLineageService {
  private static final Logger logger = Logger.getLogger(ColumnMetadataProvider.class);

  protected Context context;

  public ColumnMetadataProvider() {
    super();
  }

  public ColumnMetadataProvider(Context context) {
    this();
    this.context = context;
  }

  public Set<String> fetchAllColumns(String dataset, String table) {
    Instant start = now();
    MetadataVersionObject version = context.getCurrentMetadataVersion();
    Set<String> columns =
        stream(
                new VersionedMetadataObject()
                    .scanColumns(context, version, dataset, table)
                    .spliterator(),
                false)
            .map(VersionedMetadataObject::getColumn_name)
            .collect(toSet());
    Instant end = now();
    if (logger.isDebugEnabled()) {
      Duration duration = between(start, end);
      logger.debug(
          format(
              "fetched %s columns names for %s %s time: %s",
              columns.size(), dataset, table, duration));
    }
    return columns;
  }

  public List<VersionedMetadataObject> fetchVersionMetadataForAllColumns(
      String dataset, String table) {
    MetadataVersionObject version = context.getCurrentMetadataVersion();
    return fetchVersionMetadataForAllColumns(dataset, table, version);
  }

  public List<VersionedMetadataObject> fetchVersionMetadataForAllColumns(
      String dataset, String table, MetadataVersionObject version) {
    Instant start = now();
    List<VersionedMetadataObject> columns =
        stream(
                new VersionedMetadataObject()
                    .scanColumns(context, version, dataset, table)
                    .spliterator(),
                false)
            .collect(toList());
    Instant end = now();
    if (logger.isDebugEnabled()) {
      Duration duration = between(start, end);
      logger.debug(
          format(
              "fetched %s column versions for %s %s time: %s",
              columns.size(), dataset, table, duration));
    }
    return columns;
  }

  /**
   * fetch an ordered list of metadata version of the given dataset, table, and column
   *
   * @param dataset
   * @param table
   * @param maxVersions
   * @return
   */
  public List<VersionedMetadataObject> fetchColumnVersionLineage(
      String dataset, String table, String column, Integer maxVersions)
      throws MetadataScanException {
    Instant start = now();
    ObjectScannerIterable<VersionedMetadataObject> scanner =
        new VersionedMetadataObject().scanColumnAllVersions(context, dataset, table, column);
    List<VersionedMetadataObject> list = scanLineage(scanner);
    int numFound = list.size();
    list = list.subList(0, min(numFound, maxVersions + 1));
    Instant end = now();
    if (logger.isInfoEnabled()) {
      Duration duration = between(start, end);
      logger.info(
          format(
              "dataset: %s table: %s column: %s has (%s/%s) max versions time: %s",
              dataset, table, column, list.size(), numFound, duration));
    }
    return list;
  }

  /**
   * fetch an ordered list of metadata version of the given dataset, table, and column
   *
   * @param dataset
   * @param table
   * @return
   */
  public List<VersionedMetadataObject> fetchColumnVersionLineage(
      String dataset, String table, String column) throws MetadataScanException {
    return this.fetchColumnVersionLineage(dataset, table, column, Integer.MAX_VALUE);
  }

  public Context getContext() {
    return context;
  }

  public void setContext(Context context) {
    this.context = context;
  }
}
