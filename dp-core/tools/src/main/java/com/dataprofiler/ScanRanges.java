package com.dataprofiler;

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

import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Const;
import com.dataprofiler.util.objects.AccumuloObject;
import com.dataprofiler.util.objects.MetadataVersionObject;
import com.dataprofiler.util.objects.ObjectScannerIterable;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.accumulo.core.data.Range;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

public class ScanRanges {
  /**
   * Get a set of ranges to use when creating masterLists
   *
   * <p>The returned set consists of map entries where the Key is a scan range ane the value is a
   * tuple containing the data source name concatenated with the table name and the Accumulo
   * visibility.
   *
   * <p>Each entry's range is used to ensure only a single column from a table is scanned when
   * masterLists are computed. The number of ranges returned will be equal to the number of columns
   * for which masterLists are computed.
   *
   * <p>Format of the returned set is: Range(col_val\x00, col_val\x00\xff),
   * "dataset_name\x00table_name"
   *
   * @return A set of ranges to limit
   */
  public static Set<Range> getScanRanges(DPSparkContext context, String dataset, String table) {

    ObjectScannerIterable<VersionedMetadataObject> metadata;
    MetadataVersionObject version = context.getCurrentMetadataVersion();

    if (dataset != null && !dataset.isEmpty()) {
      if (table != null && !table.isEmpty()) {
        metadata =
            new VersionedMetadataObject().scanAllLevelsForTable(context, version, dataset, table);
      } else {
        metadata = new VersionedMetadataObject().scanAllLevelsForDataset(context, version, dataset);
      }
    } else {
      metadata = new VersionedMetadataObject().scan(context);
    }

    // Iterate over the metadata to build the set of ranges
    return StreamSupport.stream(metadata.spliterator(), false)
        .filter(m -> m.metadata_level == VersionedMetadataObject.COLUMN)
        .map(
            m ->
                AccumuloObject.createInclusiveRange(
                    m.dataset_name, m.table_name, m.column_name, Const.SortOrder.CNT_ASC.GetCode()))
        .collect(Collectors.toSet());
  }

  public static JavaPairRDD<String, Set<String>> getTablesAndColumns(
      DPSparkContext context, String dataset, String table)
      throws IOException, BasicAccumuloException {
    ObjectScannerIterable<VersionedMetadataObject> iterable;
    MetadataVersionObject version = context.getCurrentMetadataVersion();

    if (table != null && !table.isEmpty()) {
      iterable =
          new VersionedMetadataObject().scanAllLevelsForTable(context, version, dataset, table);
    } else {
      iterable = new VersionedMetadataObject().scanAllLevelsForDataset(context, version, dataset);
    }

    SparkAccumuloIO io = new SparkAccumuloIO(context);
    JavaRDD<VersionedMetadataObject> metadata = io.rddFromAccumulo(iterable);

    // Convert metadata objects into pairs of (table, set{column_name})
    JavaPairRDD<String, Set<String>> tableColumnMap =
        metadata.flatMapToPair(
            metadataObject -> {
              List<Tuple2<String, Set<String>>> result = new ArrayList<>();
              if (metadataObject.metadata_level == VersionedMetadataObject.COLUMN) {
                Set<String> columns = new HashSet<>();
                columns.add(metadataObject.column_name);
                result.add(new Tuple2<>(metadataObject.table_name, columns));
              }

              return result.iterator();
            });

    // Combine columns for each table
    return tableColumnMap.reduceByKey(
        (colA, colB) -> {
          Set<String> out = new HashSet<>();
          out.addAll(colA);
          out.addAll(colB);
          return out;
        });
  }

  public static JavaPairRDD<String, Set<String>> getDatasetAndTables(
      DPSparkContext context, String dataset, String table)
      throws IOException, BasicAccumuloException {
    ObjectScannerIterable<VersionedMetadataObject> iterable;
    MetadataVersionObject version = context.getCurrentMetadataVersion();

    if (table != null && !table.isEmpty()) {
      iterable =
          new VersionedMetadataObject().scanAllLevelsForTable(context, version, dataset, table);
    } else if (dataset != null && !dataset.isEmpty()) {
      iterable = new VersionedMetadataObject().scanAllLevelsForDataset(context, version, dataset);
    } else {
      iterable = new VersionedMetadataObject().scan(context, version);
    }

    SparkAccumuloIO io = new SparkAccumuloIO(context);
    JavaRDD<VersionedMetadataObject> metadata = io.rddFromAccumulo(iterable);

    // Convert metadata objects into pairs of (table, set{column_name})
    JavaPairRDD<String, Set<String>> datasetTableMap =
        metadata.flatMapToPair(
            metadataObject -> {
              List<Tuple2<String, Set<String>>> result = new ArrayList<>();
              if (metadataObject.metadata_level == VersionedMetadataObject.TABLE) {
                Set<String> tables = new HashSet<>();
                tables.add(metadataObject.table_name);
                result.add(new Tuple2<>(metadataObject.dataset_name, tables));
              }

              return result.iterator();
            });

    // Combine tables for each dataset
    return datasetTableMap.reduceByKey(
        (tableA, tableB) -> {
          Set<String> out = new HashSet<>();
          out.addAll(tableA);
          out.addAll(tableB);
          return out;
        });
  }
}
