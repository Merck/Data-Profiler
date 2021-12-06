package com.dataprofiler.samples;

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

import static com.dataprofiler.index.CreateColumnCountPaginationIndex.genColStartEndIdx;

import com.dataprofiler.DPSparkContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import scala.Tuple2;

public class CreateColumnCountSamples {

  private static final Logger logger = Logger.getLogger(CreateColumnCountSamples.class);

  /**
   * Generate ColumnCountSampleObject KVPs for ColumnCountObject KVPs.
   *
   * @param context Spark context
   * @param colCntObjKvpRdd PairRDD of (ColumnCountObject Key, ColumnCountObject Value)
   * @param numSamples The maximum number of samples to produce for each column
   * @return PairRDD of (ColumnCountSampleObject Key, ColumnCountSampleObject Value)
   */
  public static JavaPairRDD<Key, Value> genColumnCountSampleObjKvpRdd(
      DPSparkContext context, JavaPairRDD<Key, Value> colCntObjKvpRdd, int numSamples) {

    // Sort the column count objects
    JavaPairRDD<Key, Value> sortedColCntObjKvpRdd = colCntObjKvpRdd.sortByKey();

    // Get the global start and end index for each column
    List<Map.Entry<Long, Long>> colStartEndIdx = genColStartEndIdx(sortedColCntObjKvpRdd);

    // Add an index to each of the sorted objects
    JavaPairRDD<Tuple2<Key, Value>, Long> sortedColCntObjIdxRDD =
        sortedColCntObjKvpRdd.zipWithIndex();

    // Generate the the columnCountSample objects
    JavaPairRDD<Key, Value> colCntSampleKvpRdd =
        genColumnCountSampleObjKvpRdd(context, sortedColCntObjIdxRDD, colStartEndIdx, numSamples);

    return colCntSampleKvpRdd;
  }

  /**
   * Generate ColumnCountSampleObject KVPs for ColumnCountObject KVPs.
   *
   * @param context Spark context
   * @param sortedColCntObjIdxRdd Sorted PairRDD of (ColumnCountObject Key, ColumnCountObject Value)
   * @param colStartEndIdx The start and end index of each column sort order
   * @param numSamples The maximum number of samples to produce for each column
   * @return
   */
  public static JavaPairRDD<Key, Value> genColumnCountSampleObjKvpRdd(
      DPSparkContext context,
      JavaPairRDD<Tuple2<Key, Value>, Long> sortedColCntObjIdxRdd,
      List<Map.Entry<Long, Long>> colStartEndIdx,
      int numSamples) {

    // Calculate the global indexes from which samples
    Set<Long> sampleIndexes =
        colStartEndIdx.stream()
            .parallel()
            .map(range -> generateSampleIdx(numSamples, range.getKey(), range.getValue()))
            .flatMap(Set::stream)
            .collect(Collectors.toSet());

    // Pass the offset to page index around so each executor can use it.
    JavaSparkContext jsc = new JavaSparkContext(context.createSparkSession().sparkContext());
    Broadcast<Set<Long>> sampleIndexesBcast = jsc.broadcast(sampleIndexes);

    // Apply the offset to page index as a filter to the indexed sorted ColumnCount KVP to get the
    // ColumnCountObject KVPs that should be pagination indexes.
    return sortedColCntObjIdxRdd
        .filter(t -> sampleIndexesBcast.value().contains(t._2))
        .mapToPair(x -> new Tuple2<>(x._1._1, x._1._2));
  }

  /**
   * Generate the indices for samples between the start and end index.
   *
   * @param numSamples Maximum number of samples to generate
   * @param startIdx Index of the first elements
   * @param endIdx Index of the last element
   * @return Set of indices that correspond to
   */
  private static Set<Long> generateSampleIdx(int numSamples, long startIdx, long endIdx) {
    long size = endIdx - startIdx;

    // If the size is less than the number of samples, pass back all of the element indexes
    if (size <= numSamples) {
      return LongStream.rangeClosed(startIdx, endIdx).boxed().collect(Collectors.toSet());
    }

    final int skip = size % numSamples != 0 ? (int) size / numSamples + 1 : (int) size / numSamples;

    System.out.println("skip: " + skip);
    return LongStream.iterate(startIdx, i -> i + skip)
        .limit(numSamples)
        .boxed()
        .collect(Collectors.toSet());
  }
}
