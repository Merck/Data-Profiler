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
import com.dataprofiler.util.objects.VersionedTableMetadata;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;

/** provider gives helper methods to work with table metadata */
public class TableMetadataProvider extends VersionLineageService {
  private static final Logger logger = Logger.getLogger(TableMetadataProvider.class);

  protected Context context;

  public TableMetadataProvider() {
    super();
  }

  public TableMetadataProvider(Context context) {
    this();
    this.context = context;
  }

  public Set<String> fetchAllTables(String dataset) {
    Instant start = now();
    MetadataVersionObject version = context.getCurrentMetadataVersion();
    Set<String> tables =
        stream(
                new VersionedMetadataObject().scanTables(context, version, dataset).spliterator(),
                false)
            .map(VersionedMetadataObject::getTable_name)
            .collect(toSet());
    Instant end = now();
    if (logger.isDebugEnabled()) {
      Duration duration = between(start, end);
      logger.debug(
          format("fetched %s table names for %s time: %s", tables.size(), dataset, duration));
    }
    return tables;
  }

  public List<VersionedMetadataObject> fetchVersionMetadataForAllTables(String dataset) {
    MetadataVersionObject version = context.getCurrentMetadataVersion();
    return fetchVersionMetadataForAllTables(dataset, version);
  }

  public List<VersionedMetadataObject> fetchVersionMetadataForAllTables(
      String dataset, MetadataVersionObject version) {
    Instant start = now();
    List<VersionedMetadataObject> tables =
        stream(
                new VersionedMetadataObject().scanTables(context, version, dataset).spliterator(),
                false)
            .collect(toList());
    Instant end = now();
    if (logger.isDebugEnabled()) {
      Duration duration = between(start, end);
      logger.debug(
          format("fetched %s table versions for %s time: %s", tables.size(), dataset, duration));
    }
    return tables;
  }

  public VersionedTableMetadata fetchTableMetadata(String dataset, String table) {
    return new VersionedMetadataObject().allMetadataForTable(context, dataset, table);
  }

  public VersionedTableMetadata fetchTableMetadataByVersion(
      String dataset, String table, MetadataVersionObject version) {
    return new VersionedMetadataObject().allMetadataForTable(context, version, dataset, table);
  }

  /**
   * fetch an ordered list of metadata version of the given dataset and table
   *
   * @param dataset
   * @param table
   * @param maxVersions
   * @return
   */
  public List<VersionedMetadataObject> fetchTableVersionLineage(
      String dataset, String table, Integer maxVersions) throws MetadataScanException {
    Instant start = now();
    ObjectScannerIterable<VersionedMetadataObject> scanner =
        new VersionedMetadataObject().scanTableAllVersions(context, dataset, table);
    List<VersionedMetadataObject> list = scanLineage(scanner);
    int numFound = list.size();
    list = list.subList(0, min(numFound, maxVersions + 1));
    Instant end = now();
    if (logger.isInfoEnabled()) {
      Duration duration = between(start, end);
      logger.info(
          format(
              "dataset: %s table: %s has (%s/%s) max versions time: %s",
              dataset, table, list.size(), numFound, duration));
    }
    return list;
  }

  /**
   * fetch an ordered list of metadata version of the given dataset and table
   *
   * @param dataset
   * @param table
   * @return
   */
  public List<VersionedMetadataObject> fetchTableVersionLineage(String dataset, String table)
      throws MetadataScanException {
    return this.fetchTableVersionLineage(dataset, table, Integer.MAX_VALUE);
  }

  public Context getContext() {
    return context;
  }

  public void setContext(Context context) {
    this.context = context;
  }
}
