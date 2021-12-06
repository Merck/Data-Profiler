package com.dataprofiler.connectionengine.cli;

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
import com.dataprofiler.SparkAccumuloIO;
import com.dataprofiler.connectionengine.ConnectionEngineException;
import com.dataprofiler.connectionengine.config.ConnectionEngineConfig;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Const;
import com.dataprofiler.util.objects.ColumnCountObject;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import com.wantedtech.common.xpresso.strings.SequenceMatcher;
import com.wantedtech.common.xpresso.types.tuples.tuple3;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import scala.Tuple2;

public class ConnectionEngineCli {

  private static final Logger logger = Logger.getLogger(ConnectionEngineCli.class);

  private static final String PRIMARY_KEY = "PK";
  private static final String NOT_PRIMARY_KEY = "NA";

  private ConnectionEngineConfig config;
  private DPSparkContext context;
  private String outputPath;
  private String dataset;

  public static void main(String[] args) throws IOException, BasicAccumuloException {

    try {
      ConnectionEngineCli engine = new ConnectionEngineCli();
      engine.execute(args);
    } catch (ConnectionEngineException e) {
      e.printStackTrace();
      logger.error(e);
      System.exit(1);
    }
  }

  protected ConnectionEngineConfig parseJobArgs(String[] args)
      throws IOException, ConnectionEngineException {
    logger.debug("Creating and verifying job config");
    ConnectionEngineConfig config = new ConnectionEngineConfig();

    if (!config.parse(args) || config.parameters.size() != 2) {
      String err = "Usage: <main_class> [options] <dataset> <output_path>";
      throw new ConnectionEngineException(err);
    }

    this.dataset = config.parameters.get(0);
    this.outputPath = config.parameters.get(1);

    logger.debug("loaded config: " + config);
    return config;
  }

  public void execute(String[] args)
      throws IOException, BasicAccumuloException, ConnectionEngineException {

    this.config = this.parseJobArgs(args);
    this.context = new DPSparkContext(config, "SuperTableCalculations");

    if (logger.isInfoEnabled()) {
      logger.info("context app name: " + context.getAppName());
      logger.info("context auths: " + context.getAuthorizations());
      logger.info("context spark config: " + context.getSparkConfig().toDebugString());
      logger.info("dataset name: " + this.dataset);
      logger.info("spark output path: " + this.outputPath);
    }

    long starTime = System.currentTimeMillis();

    logger.info("Calculating statistics for dataset: " + dataset);

    List<String> tables = new ArrayList<>();
    for (VersionedMetadataObject m :
        new VersionedMetadataObject()
            .scanTables(context, context.getCurrentMetadataVersion(), dataset)) {
      tables.add(m.table_name);
    }

    if (tables.size() > 0) {
      logger.warn("Found the following tables");
      tables.forEach(System.out::println);
    } else {
      logger.warn("No tables found for dataset '" + dataset + "'");
      return;
    }

    // Exit if only one table exists
    if (tables.size() < 2) {
      logger.warn("Dataset must contain at least 2 tables to perform calculations");
      return;
    }

    Set<String> ignoredTerms = new HashSet<>();
    ignoredTerms.add("null");
    ignoredTerms.add("");

    if (ignoredTerms.size() > 0) {
      System.out.println("Ignoring the following terms");
      ignoredTerms.forEach(System.out::println);
    }

    SparkAccumuloIO io = new SparkAccumuloIO(context);

    logger.warn("Output file: " + this.outputPath);
    BufferedWriter writer = Files.newBufferedWriter(Paths.get(this.outputPath));
    CSVPrinter csvPrinter =
        new CSVPrinter(
            writer,
            CSVFormat.RFC4180.withHeader(
                "table_1",
                "column_1",
                "table_2",
                "column_2",
                "col_1_type",
                "col_2_type",
                "edit_distance",
                "same_name",
                "name_score",
                "intersection",
                "overlap_%_1",
                "overlap_%_2",
                "pk_overlap",
                "fk_overlap",
                "jaccard",
                "join_score"));

    for (int tableAidx = 0; tableAidx < tables.size(); tableAidx++) {
      String tableA = tables.get(tableAidx);

      // Get columns for tableA
      List<String> colNamesA = columnNames(context, dataset, tableA);

      // Get the columns for tableA: col_name -> (value, count)
      JavaPairRDD<String, Tuple2<String, Long>> colsARDD =
          columns(io, context, dataset, tableA, ignoredTerms).cache();

      // Get the column values without the count: column -> value
      // Then invert: value -> column
      JavaPairRDD<String, String> colAValsRDD = columnValues(colsARDD);
      JavaPairRDD<String, String> valsColARDD = invertKVP(colAValsRDD);

      // Get counts for each columns: col_name -> (card, len)
      JavaPairRDD<String, Tuple2<Long, Long>> colAStatsRDD = columnStatistics(colsARDD);

      // Get a map of column name to cardinality
      Map<String, Long> colACards = columnCardinality(colAStatsRDD);

      // Determine if columns are primary keys
      Map<String, String> colAPK = primaryKeys(colAStatsRDD);

      for (int tableBidx = tableAidx + 1; tableBidx < tables.size(); tableBidx++) {
        String tableB = tables.get(tableBidx);

        // Get columns for tableB
        List<String> colNamesB = columnNames(context, dataset, tableB);

        // Get the columns for tableB: col_name -> (value, count)
        JavaPairRDD<String, Tuple2<String, Long>> colsBRDD =
            columns(io, context, dataset, tableB, ignoredTerms);

        // Get the column values without the count: column -> value
        // Then invert: value -> column
        JavaPairRDD<String, String> colBValsRDD = columnValues(colsBRDD);
        JavaPairRDD<String, String> valsColBRDD = invertKVP(colBValsRDD);

        // Get counts for each columns: col_name -> (card, len)
        JavaPairRDD<String, Tuple2<Long, Long>> colBStatsRDD = columnStatistics(colsBRDD);

        // Get a map of column name to cardinality
        Map<String, Long> colBCards = columnCardinality(colBStatsRDD);

        // Determine if columns are primary keys
        Map<String, String> colBPK = primaryKeys(colBStatsRDD);

        // join to get: value -> (column_a, column_b)
        JavaPairRDD<String, Long> intersection =
            valsColARDD
                .join(valsColBRDD)
                .mapToPair(
                    val -> new Tuple2<>(String.join(Const.LOW_BYTE, val._2._1, val._2._2), 1L))
                .reduceByKey(Long::sum);

        Map<String, Long> intersectionLocal = intersection.collectAsMap();

        // Get all column combinations
        List<String> colCombos = new ArrayList<>();
        for (String colA : colNamesA) {
          for (String colB : colNamesB) {
            colCombos.add(
                String.join(
                    Const.LOW_BYTE, removeCarriageReturn(colA), removeCarriageReturn(colB)));
          }
        }

        // Get output
        for (String colCombo : colCombos) {
          String[] columns = colCombo.split(Const.LOW_BYTE);

          // Calculate edit distance
          Integer editDistance = new LevenshteinDistance().apply(columns[0], columns[1]);

          String sameName = editDistance == 0 ? "true" : "false";

          Double nameScore = nameScore(columns[0], columns[1]);

          Long intersectionLen = intersectionLocal.getOrDefault(colCombo, 0L);

          Double colAOverlap =
              intersectionLen / colACards.getOrDefault(columns[0], 0L).doubleValue();
          Double colBOverlap =
              intersectionLen / colBCards.getOrDefault(columns[1], 0L).doubleValue();

          Double pkOverlap = 0.0;
          Double fkOverlap = 0.0;

          // Only calculate if one of the columns is a primary key
          if (colAPK.getOrDefault(columns[0], NOT_PRIMARY_KEY).equals(PRIMARY_KEY)
              || colBPK.getOrDefault(columns[1], NOT_PRIMARY_KEY).equals(PRIMARY_KEY)) {

            // If colA is a PK, PK overlap is from colA and FK overlap is overlap from colB
            if (colAPK.getOrDefault(columns[0], NOT_PRIMARY_KEY).equals(PRIMARY_KEY)) {
              pkOverlap = colAOverlap;
              fkOverlap = colBOverlap;
            }

            // if colB is a PK, PK overlap is from colB and FK overlap is from colA
            if (colBPK.getOrDefault(columns[1], NOT_PRIMARY_KEY).equals(PRIMARY_KEY)) {
              pkOverlap = colBOverlap;
              fkOverlap = colAOverlap;
            }
          }

          Double jaccardIndex =
              intersectionLen.doubleValue()
                  / (colACards.getOrDefault(columns[0], 0L)
                      + colBCards.getOrDefault(columns[1], 0L)
                      - intersectionLen);

          Double joinScore =
              joinScore(
                  colAOverlap,
                  colBOverlap,
                  colAPK.getOrDefault(columns[0], NOT_PRIMARY_KEY),
                  colBPK.getOrDefault(columns[1], NOT_PRIMARY_KEY));

          csvPrinter.printRecord(
              removeCarriageReturn(tableA),
              removeCarriageReturn(columns[0]),
              removeCarriageReturn(tableB),
              removeCarriageReturn(columns[1]),
              colAPK.getOrDefault(columns[0], NOT_PRIMARY_KEY),
              colBPK.getOrDefault(columns[1], NOT_PRIMARY_KEY),
              editDistance,
              sameName,
              nameScore,
              intersectionLen,
              colAOverlap,
              colBOverlap,
              pkOverlap,
              fkOverlap,
              jaccardIndex,
              joinScore);
        }
        colsBRDD.unpersist();
      }
      colsARDD.unpersist();
    }

    csvPrinter.flush();
    logger.info(
        String.format(
            "It took %d ms to compute calculations", (System.currentTimeMillis() - starTime)));
  }

  //  public static void main(String[] args) throws IOException, BasicAccumuloException {
  //
  //    try {
  //      ConnectionEngineCli engine = new ConnectionEngineCli();
  //      engine.execute();
  //    } catch (ConnectionEngineException e) {
  //      e.printStackTrace();
  //      logger.error(e);
  //      System.exit(1);
  //    }
  //
  //
  //    Config config = new Config();
  //    if (!config.parse(args)) {
  //      System.exit(1);
  //    }
  //
  //    if (config.parameters.size() != 2) {
  //      System.err.println("Usage: <main_class> [options] <dataset> <output_file>");
  //      config.printHelp();
  //      System.exit(1);
  //    }
  //
  //    String datasetName = config.parameters.get(0);
  //    String outputFile = config.parameters.get(1);
  //
  //    DPSparkContext context = new DPSparkContext("SuperTableCalculations", args);
  //
  //    execute(context, datasetName, outputFile);
  //  }

  /**
   * Remove a carriage return from the end of a string
   *
   * @param s A string
   * @return A string without a carriage return
   */
  private static String removeCarriageReturn(String s) {
    if (s.endsWith("\r")) {
      return s.substring(0, s.length() - 1);
    }
    return s;
  }

  /**
   * Retrieve all of the columns and values for a table
   *
   * @param io
   * @param context
   * @param dataset
   * @param table
   * @return A key value RDD that is of format: column_name -> (value, count)
   * @throws IOException
   * @throws BasicAccumuloException
   */
  private static JavaPairRDD<String, Tuple2<String, Long>> columns(
      SparkAccumuloIO io,
      DPSparkContext context,
      String dataset,
      String table,
      Set<String> ignoredTerms)
      throws IOException, BasicAccumuloException {

    JavaSparkContext jsc = new JavaSparkContext(context.createSparkSession().sparkContext());
    Broadcast<Set<String>> ignoredTermsBcast = jsc.broadcast(ignoredTerms);

    List<String> columns = new ArrayList<>();
    for (VersionedMetadataObject m :
        new VersionedMetadataObject()
            .scanColumns(context, context.getCurrentMetadataVersion(), dataset, table)) {
      columns.add(m.column_name);
    }

    VersionedMetadataObject vmo =
        new VersionedMetadataObject()
            .fetchTable(context, context.getCurrentMetadataVersion(), dataset, table);

    return io.rddFromAccumulo(
            new ColumnCountObject()
                .fetchColumns(context, vmo.dataset_name, vmo.table_id, columns)
                .setBatch(true))
        .filter(col -> !ignoredTermsBcast.getValue().contains(removeCarriageReturn(col.value)))
        .mapToPair(
            col ->
                new Tuple2<>(
                    removeCarriageReturn(col.column),
                    new Tuple2<>(removeCarriageReturn(col.value), col.count)));
  }

  /**
   * Convert key -> value to value -> key
   *
   * @param input A key value RDD
   * @return A key value RDD
   */
  private static JavaPairRDD<String, String> invertKVP(JavaPairRDD<String, String> input) {
    return input.mapToPair(i -> new Tuple2<>(i._2, i._1));
  }

  /**
   * Calculate the column cardinality and length
   *
   * @param column A key value RDD that is of format: column_name -> (value, count)
   * @return A key value RDD that is of the format: column_name -> (cardinality, length)
   */
  private static JavaPairRDD<String, Tuple2<Long, Long>> columnStatistics(
      JavaPairRDD<String, Tuple2<String, Long>> column) {
    return column
        .mapToPair(col -> new Tuple2<>(col._1, new Tuple2<>(1L, col._2._2)))
        .reduceByKey((a, b) -> new Tuple2<>(a._1 + b._1, a._2 + b._2));
  }

  /**
   * Convert column_name -> (value, count) to column_name -> value
   *
   * @param column A key value RDD that is of format: column_name -> (value, count)
   * @return A key value RDD that is of format: column_name -> value
   */
  private static JavaPairRDD<String, String> columnValues(
      JavaPairRDD<String, Tuple2<String, Long>> column) {
    return column.mapToPair(col -> new Tuple2<>(col._1, col._2._1));
  }

  /**
   * Calculate the cardinality for each column
   *
   * @param columnStats A key value RDD that is of the format: column_name -> (cardinality, length)
   * @return A key value RDD that is of format: column_name -> cardinality
   */
  private static Map<String, Long> columnCardinality(
      JavaPairRDD<String, Tuple2<Long, Long>> columnStats) {
    return columnStats.mapToPair(col -> new Tuple2<>(col._1, col._2._1)).collectAsMap();
  }

  private static Map<String, String> primaryKeys(
      JavaPairRDD<String, Tuple2<Long, Long>> columnCountRDD) {
    return columnCountRDD
        .mapToPair(
            col -> {
              if (col._2._1 / col._2._2.doubleValue() > 0.95) {
                return new Tuple2<>(col._1, PRIMARY_KEY);
              } else {
                return new Tuple2<>(col._1, NOT_PRIMARY_KEY);
              }
            })
        .collectAsMap();
  }

  private static Double joinScore(
      Double colAOverlap, Double colBOverlap, String isColAPK, String isColBPK) {

    if (isColAPK.equals(isColBPK)) {
      return 0.0;
    } else if (isColAPK.equals(PRIMARY_KEY)) {
      return colBOverlap * colBOverlap;
    } else {
      return colAOverlap * colAOverlap;
    }
  }

  private static List<String> columnNames(DPSparkContext context, String dataset, String table) {
    List<String> columnNames = new ArrayList<>();
    for (VersionedMetadataObject m :
        new VersionedMetadataObject()
            .scanColumns(context, context.getCurrentMetadataVersion(), dataset, table)) {
      columnNames.add(removeCarriageReturn(m.column_name));
    }
    return columnNames;
  }

  private static double nameScore(String colAName, String colBName) {

    SequenceMatcher s = new SequenceMatcher(colAName, colBName);
    int lenA = colAName.length();
    int lenB = colBName.length();

    tuple3<Integer, Integer, Integer> match = s.find_longest_match(0, lenA, 0, lenB);

    if (match.right <= 1) {
      return 0.0;
    }

    if (lenA > lenB) {
      return match.right / (double) lenB;
    }

    return match.right / (double) lenA;
  }
}
