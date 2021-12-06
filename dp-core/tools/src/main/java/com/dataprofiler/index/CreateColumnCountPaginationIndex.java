package com.dataprofiler.index;

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

import com.dataprofiler.DPSparkContext;
import com.dataprofiler.util.objects.ColumnCountObject;
import com.dataprofiler.util.objects.ColumnCountPaginationObject;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import scala.Tuple2;

public class CreateColumnCountPaginationIndex {

  public static final Logger logger = Logger.getLogger(CreateColumnCountPaginationIndex.class);

  /**
   * Generate ColumnCountPagination object KVPs based on ColumnCountObject KVPs.
   *
   * <p>There are a lot of steps to this method, which are well documented below, but a general
   * overview of the steps are:
   *
   * <ol>
   *   <li>Sort the ColumnCountObjects
   *   <li>Get the starting and end index of each column
   *   <li>Generate indexes for the start of each page
   *   <li>Filter the sorted ColumnCountObject by the page indexes
   *   <li>Convert the result to ColumnCountPaginationObjects
   * </ol>
   *
   * @param context Spark context
   * @param colCntObjKvpRdd PairRDD of (ColumnCountObject Key, ColumnCountObject Value)
   * @param valuesPerPage The number of values each page should contain
   * @return PairRDD of (ColumnCountPaginationObject Key, ColumnCountPaginationObject Value)
   */
  public static JavaPairRDD<Key, Value> genColumnCountPaginationObjKvpRdd(
      DPSparkContext context, JavaPairRDD<Key, Value> colCntObjKvpRdd, long valuesPerPage) {

    // Sort the column count objects
    JavaPairRDD<Key, Value> sortedColCntObjKvpRdd = colCntObjKvpRdd.sortByKey();

    // Get the global start and end index for each column
    List<Map.Entry<Long, Long>> colStartEndIdx = genColStartEndIdx(sortedColCntObjKvpRdd);

    // Add an index to each of the sorted objects
    JavaPairRDD<Tuple2<Key, Value>, Long> sortedColCntObjIdxRdd =
        sortedColCntObjKvpRdd.zipWithIndex();

    // Generate the columnCountPagination objects
    JavaPairRDD<Key, Value> colCntPagKvpRdd =
        genColumnCountPaginationObjKvpRdd(
            context, sortedColCntObjIdxRdd, colStartEndIdx, valuesPerPage);

    return colCntPagKvpRdd;
  }

  /**
   * Calculate the global index and local index for each page in the range.
   *
   * <p>Given the global start and end index for a column (3, 5) and 2 values per page, this
   * function would return the two pairs {3:0, 5:2}, representing two pages for the range. The first
   * number in each pair is the global index for the start of the page and the second number in each
   * pair is the index for the start of the page within that range. The first page, {3:0}, starts at
   * the global index of 3 and the 0th element in the range. The second page, {5:2}, starts at the
   * global index of 5 and the 2nd element in the range.
   *
   * @param startEndIdx The global start and end index for a column
   * @param valsPerPage The number of values for each page
   * @return A map of global index to page index for each column
   */
  private static Map<Long, Long> genPaginationIndexes(
      Map.Entry<Long, Long> startEndIdx, long valsPerPage) {

    // Calculate the size of the resulting map to avoid a lot resizing and GC issues
    int size = (int) ((startEndIdx.getValue() - startEndIdx.getKey()) / valsPerPage + 1);

    Map<Long, Long> result = new HashMap<>(size, 1);
    for (Long i = startEndIdx.getKey(); i <= startEndIdx.getValue(); i += valsPerPage) {
      result.put(i, i - startEndIdx.getKey());
    }
    return result;
  }

  /**
   * Calculate the global start and end index for each column sort order.
   *
   * @param sortedColCntObjcKvpRdd pre-sorted columnCount
   * @return List of start and end index pairs
   */
  public static List<Map.Entry<Long, Long>> genColStartEndIdx(
      JavaPairRDD<Key, Value> sortedColCntObjcKvpRdd) {

    // The first step is to get number of unique values in each column for each sort order. To do
    // this we first transform the KVP -> (column\x00SortOrder, KVP)
    JavaPairRDD<String, Tuple2<Key, Value>> colToKvpRddKvp =
        sortedColCntObjcKvpRdd.mapToPair(
            t -> new Tuple2<>(ColumnCountObject.buildKey(t._1.getRow()), t));

    // To get the number of values in each column for each sort order we sum the number of values
    // for each key. Doing this with an aggreateByKey is the most efficient way to accomplish this
    // in spark and is similar to the word count example.
    JavaPairRDD<String, Long> colCntsRddKvp =
        colToKvpRddKvp.aggregateByKey(0L, (cnt, key) -> cnt + 1L, Long::sum);

    // For the next steps we will calculate global offsets, so we need to pull the column counts
    // back to the driver. The column counts needs to be sorted so the offsets will match with the
    // indexes in the sortedColCntObjcKvpRdd object (indexes will be added to the
    // sortedColCntObjcKvpRdd in the next few steps).
    Map<String, Long> colCntsLocal = new TreeMap<>(colCntsRddKvp.collectAsMap());

    // Generate the start and end indexes for each column sort order. These will be used to create
    // indexes to make the pagination lookup efficient.
    List<Map.Entry<Long, Long>> colStartEndIdx = new ArrayList<>();

    Long prev = 0L;
    for (Map.Entry<String, Long> entry : colCntsLocal.entrySet()) {
      Long len = entry.getValue();
      colStartEndIdx.add(new AbstractMap.SimpleEntry<>(prev, len + prev - 1L));
      prev = len + prev;
    }
    return colStartEndIdx;
  }

  /**
   * Generate ColumnCountPagination object KVPs based on sorted ColumnCountObject KVPs.
   *
   * <p>There are a lot of steps to this method, which are well documented below, but a general
   * overview of the steps are:
   *
   * <ol>
   *   <li>Generate indexes for the start of each page
   *   <li>Filter the sorted ColumnCountObject by the page indexes
   *   <li>Convert the result to ColumnCountPaginationObjects
   * </ol>
   *
   * @param context Spark context
   * @param sortedColCntObjIdxRDD Sorted PairRDD of (ColumnCountObject Key, ColumnCountObject Value)
   * @param colStartEndIdx The start and end index of each column sort order
   * @param valuesPerPage The number of values each page should contain
   * @return PairRDD of (ColumnCountPaginationObject Key, ColumnCountPaginationObject Value)
   */
  public static JavaPairRDD<Key, Value> genColumnCountPaginationObjKvpRdd(
      DPSparkContext context,
      JavaPairRDD<Tuple2<Key, Value>, Long> sortedColCntObjIdxRDD,
      List<Map.Entry<Long, Long>> colStartEndIdx,
      long valuesPerPage) {

    // Calculate the size of the resulting map to avoid a lot resizing and GC issues
    int size = 0;
    for (Map.Entry<Long, Long> entry : colStartEndIdx) {
      size += (entry.getValue() - entry.getKey()) / valuesPerPage + 1;
    }

    // Generate global index to page index pairs. The global index is the index at which an index
    // page will be generated and the page index is the page offset for that column. For example, if
    // there are 2 sort orders and column A has 3 unique values and we generate page indexes for
    // every 2 values this function will produce the set {0,0}, {2,2}, {3,0}, {5,2} because A's
    // first sort order will have the global start index of 0 and A's second sort order will have
    // the global start index of 3.
    Map<Long, Long> offsetToPageIdx = new HashMap<>(size, 1);
    for (Map.Entry<Long, Long> entry : colStartEndIdx) {
      offsetToPageIdx.putAll(genPaginationIndexes(entry, valuesPerPage));
    }

    // Pass the offset to page index around so each executor can use it.
    JavaSparkContext jsc = new JavaSparkContext(context.createSparkSession().sparkContext());
    Broadcast<Map<Long, Long>> offstToPageIdxBcast = jsc.broadcast(offsetToPageIdx);

    // Apply the offset to page index as a filter to the indexed sorted ColumnCount KVP to get the
    // ColumnCountObject KVPs that should be pagination indexes.
    JavaPairRDD<Tuple2<Key, Value>, Long> colCntToPageIdxRDD =
        sortedColCntObjIdxRDD
            .filter(t -> offstToPageIdxBcast.value().containsKey(t._2))
            .mapToPair(x -> new Tuple2<>(x._1, offstToPageIdxBcast.value().get(x._2)));

    // Convert the ColumnCountObjects to ColumnCountPaginationObjects
    return colCntToPageIdxRDD.mapToPair(
        t -> {
          ColumnCountObject ccObj =
              new ColumnCountObject().fromEntry(new AbstractMap.SimpleEntry<>(t._1._1, t._1._2));
          ColumnCountPaginationObject ccpagObj = new ColumnCountPaginationObject(t._2, ccObj);
          return new Tuple2<>(ccpagObj.createAccumuloKey(), ccpagObj.createAccumuloValue());
        });
  }
}
