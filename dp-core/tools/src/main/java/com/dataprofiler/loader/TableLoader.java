package com.dataprofiler.loader;

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

import com.clearspring.analytics.util.Lists;
import com.dataprofiler.ActiveVisibilityManager;
import com.dataprofiler.ColumnMetaData;
import com.dataprofiler.DPSparkContext;
import com.dataprofiler.InvalidSchemaFormatException;
import com.dataprofiler.SparkAccumuloIO;
import com.dataprofiler.bulkIngest.AccumuloBulkIngest;
import com.dataprofiler.bulkIngest.AccumuloBulkIngest.ABIConfig;
import com.dataprofiler.index.CreateColumnCountPaginationIndex;
import com.dataprofiler.loader.config.CsvFileParams;
import com.dataprofiler.loader.config.LoaderConfig;
import com.dataprofiler.metadata.CommitMetadata;
import com.dataprofiler.samples.CreateColumnCountSamples;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Const;
import com.dataprofiler.util.Const.LoadType;
import com.dataprofiler.util.Const.Origin;
import com.dataprofiler.util.Const.SortOrder;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.ColumnCountIndexObject;
import com.dataprofiler.util.objects.ColumnCountObject;
import com.dataprofiler.util.objects.DatawaveRowIndexObject;
import com.dataprofiler.util.objects.DatawaveRowObject;
import com.dataprofiler.util.objects.DatawaveRowShardIndexObject;
import com.dataprofiler.util.objects.MetadataVersionObject;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.commons.io.FilenameUtils;
import org.apache.hadoop.io.Text;
import org.apache.log4j.Logger;
import org.apache.spark.TaskContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.json.JSONException;
import org.json.JSONObject;
import scala.Option;
import scala.Tuple2;
import scala.Tuple3;

public class TableLoader {

  private static final Logger logger = Logger.getLogger(TableLoader.class);

  /**
   * Load column counts and rows from a Dataset<Row>.
   *
   * @param context Spark context
   * @param spark Spark session
   * @param origTable dataframe with the data
   * @param version a unique Id for the new metadata version to be used for this load
   * @param datasetName name of the destination dataset
   * @param tableName name of the destination table
   * @param visibility visibility string for the dataset
   * @return true if the table was loaded, false if not
   * @throws Exception
   */
  public static boolean load(
      DPSparkContext context,
      SparkSession spark,
      Dataset<Row> origTable,
      MetadataVersionObject version,
      String datasetName,
      String tableName,
      String visibility,
      String columnVisibilityExpression,
      String rowVisibilityColumnName,
      Integer recordsPerPartition,
      Origin origin,
      boolean commitMetadata,
      boolean fullDatasetLoad,
      boolean printSchema)
      throws Exception {

    if (version == null) {
      if (!commitMetadata) {
        /*
         * If we aren't going to commit metadata right now the caller _must_ know the
         * version id. Otherwise the data will just be lost.
         */
        throw new BasicAccumuloException("A version id is required if commitMetadata is false");
      }
      version = MetadataVersionObject.newRandomMetadataVersion();
    }

    if (commitMetadata && context.getConfig().loadType != LoadType.LIVE) {
      throw new BasicAccumuloException("CommitMetadata requires live ingest");
    }

    // TODO: If you autoformat the file, it removes the cast to Row[] which is required to
    // compile
    // Row[] row = (Row[]) origTable.take(1);
    Row[] row = (Row[]) origTable.take(1);
    if (row.length == 0) {
      logger.warn("Skipping empty file.");
      return false;
    }

    if (printSchema) {
      origTable.printSchema();
    }

    // Even if we don't repartition the data, this math can be used later to generate splits for
    // bulk ingest
    long numRows = origTable.count();
    int numCols = origTable.columns().length;
    long maxRecords = numRows * numCols;

    // When repartitioning, always have at least 1 row per partition
    int numPartitions = (int) Math.ceil((double) maxRecords / recordsPerPartition);
    if (context.getConfig() instanceof LoaderConfig) {
      if (!((LoaderConfig) context.getConfig()).skipRepartition) {
        origTable = origTable.repartition(numPartitions);
      }
    }

    String tableId = String.format("%s-%s", tableName, UUID.randomUUID());

    // Some basic logging
    logger.warn("Table Statistics");
    logger.warn("---------------");
    logger.warn("Dataset: " + datasetName);
    logger.warn("table: " + tableName);
    logger.warn("tableId: " + tableId);
    logger.warn("Num Rows: " + numRows);
    logger.warn("Num Columns: " + numCols);
    logger.warn("Num records (rows * columns): " + maxRecords);
    logger.warn("Records per partition: " + recordsPerPartition);
    logger.warn("Number of partitions: " + numPartitions);
    logger.warn("Metadata version id: " + version.getId());

    logger.warn("Creating map of (colName\\x00colVal, Count)");
    JavaPairRDD<String, Long> colValToCntKvpRdd =
        genColValToCntPairs(origTable, rowVisibilityColumnName);

    int numColCntsPartitions =
        calculateColumnCountTableParittions(
            colValToCntKvpRdd, Const.LOADER_SAMPLE_SIZE, Const.TABLET_SIZE_MAX_BYTES);

    List<String> outPathColCnt =
        genAndLoadColumnCounts(
            context,
            spark,
            colValToCntKvpRdd,
            version,
            datasetName,
            tableId,
            visibility,
            columnVisibilityExpression,
            rowVisibilityColumnName,
            numColCntsPartitions);

    List<String> outPathColCntIdx =
        genAndloadColCntInx(
            context,
            spark,
            colValToCntKvpRdd,
            version,
            datasetName,
            tableName,
            tableId,
            visibility,
            recordsPerPartition);

    List<String> outPathMeta =
        genAndLoadColCntMetadata(
            context,
            spark,
            origTable,
            colValToCntKvpRdd,
            version,
            datasetName,
            tableName,
            tableId,
            visibility,
            origin);

    List<String> outPathRow =
        loadDatawaveRows(
            context,
            spark,
            version,
            origTable,
            datasetName,
            tableId,
            visibility,
            columnVisibilityExpression,
            rowVisibilityColumnName,
            numRows,
            Const.LOADER_SAMPLE_SIZE,
            Const.TABLET_SIZE_MAX_BYTES);

    // If the load type is a split load, now is a safe time to ingest the files
    if (context.getConfig().loadType == LoadType.SPLIT) {
      logger.warn("Bulk ingesting tables from split load");
      bulkLoadDirectories(context, outPathColCnt);
      bulkLoadDirectories(context, outPathColCntIdx);
      bulkLoadDirectories(context, outPathMeta);
      bulkLoadDirectories(context, outPathRow);
    }

    if (commitMetadata) {
      logger.warn("Committing metadata");
      CommitMetadata.commitMetadata(context, version, null, fullDatasetLoad);
    }

    return true;
  }

  private static void bulkLoadDirectories(DPSparkContext context, List<String> outputPath)
      throws BasicAccumuloException, IOException, URISyntaxException {
    for (String currPath : outputPath) {
      ABIConfig ingestConfig = new ABIConfig();
      ingestConfig.inputDir = currPath;

      new AccumuloBulkIngest().ingest(context, ingestConfig);
    }
  }

  public static void activateRowLevelVisibilities(
      ActiveVisibilityManager avm, Dataset<Row> origTable, String rowVisibilityColumnName) {
    JavaRDD<String> visibilityExprStringJavaRDD =
        origTable
            .javaRDD()
            .mapPartitions(
                (Iterator<Row> itr) -> {
                  ArrayList<String> result = new ArrayList<>();
                  while (itr.hasNext()) {
                    Row row = itr.next();
                    List<String> colExprs =
                        new ArrayList<>(
                            getColumnVisibilitiesFromRow(row, rowVisibilityColumnName).values());
                    if (!colExprs.isEmpty()) result.addAll(colExprs);
                  }
                  return result.iterator();
                })
            .distinct();

    List<String> ret = visibilityExprStringJavaRDD.collect();
    for (String s : ret) {
      Set<String> vs = avm.extractVisibilitiesFromExpression(s);
      vs.forEach(
          v -> {
            try {
              logger.info("Setting visibility active: " + v);
              avm.setVisibilitiesFromExpressionAsActive(v);
            } catch (Exception e) {
              logger.error("Failed to activate row level visibility: " + v, e);
            }
          });
    }
  }

  public static List<String> genAndLoadColumnCounts(
      DPSparkContext context,
      SparkSession spark,
      JavaPairRDD<String, Long> colValToCntKvpRdd,
      MetadataVersionObject version,
      String dataset,
      String tableId,
      String visibility,
      String columnVisibilityExpression,
      String rowVisibilityColumnName,
      int numPartitions)
      throws BasicAccumuloException, IOException {

    // Create the column count table if it doesn't already exist
    createTableIfNeeded(context, context.getConfig().accumuloColumnCountsTable);

    SparkAccumuloIO io = new SparkAccumuloIO(context, version.id);
    List<String> outputPaths = new ArrayList<>();

    // Create column counts
    outputPaths.addAll(
        genAndStoreColCnts(
            context,
            io,
            colValToCntKvpRdd,
            SortOrder.CNT_ASC,
            dataset,
            tableId,
            visibility,
            columnVisibilityExpression,
            rowVisibilityColumnName,
            numPartitions));

    outputPaths.addAll(
        genAndStoreColCnts(
            context,
            io,
            colValToCntKvpRdd,
            SortOrder.CNT_DESC,
            dataset,
            tableId,
            visibility,
            columnVisibilityExpression,
            rowVisibilityColumnName,
            numPartitions));

    outputPaths.addAll(
        genAndStoreColCnts(
            context,
            io,
            colValToCntKvpRdd,
            SortOrder.VAL_ASC,
            dataset,
            tableId,
            visibility,
            columnVisibilityExpression,
            rowVisibilityColumnName,
            numPartitions));

    outputPaths.addAll(
        genAndStoreColCnts(
            context,
            io,
            colValToCntKvpRdd,
            SortOrder.VAL_DESC,
            dataset,
            tableId,
            visibility,
            columnVisibilityExpression,
            rowVisibilityColumnName,
            numPartitions));

    return outputPaths;
  }

  public static List<String> genAndloadColCntInx(
      DPSparkContext context,
      SparkSession spark,
      JavaPairRDD<String, Long> colValToCntKvpRdd,
      MetadataVersionObject version,
      String datasetName,
      String tableName,
      String tableId,
      String visibility,
      int recordsPerPartition)
      throws IOException, BasicAccumuloException {

    logger.warn("Generating column count index");
    JavaPairRDD<Key, Value> indexKVP =
        calculateIndex(colValToCntKvpRdd, datasetName, tableId, tableName, visibility);
    Collection<Text> indexSplitPoints = generateSplits(indexKVP, recordsPerPartition);

    return new ArrayList<>(
        Collections.singletonList(
            new SparkAccumuloIO(context, version.id)
                .importToAccumulo(
                    indexKVP, context.getConfig().accumuloIndexTable, indexSplitPoints)));
  }

  public static List<String> genAndLoadColCntMetadata(
      DPSparkContext context,
      SparkSession spark,
      Dataset<Row> origTable,
      JavaPairRDD<String, Long> colValToCntKvpRdd,
      MetadataVersionObject version,
      String datasetName,
      String tableName,
      String tableId,
      String visibility,
      Const.Origin origin)
      throws IOException, BasicAccumuloException {

    // Gather type info
    Map<String, ColumnMetaData> schema = extractSchema(origTable);

    logger.warn("Calculating column metadata");
    JavaPairRDD<String, Tuple2<Long, Long>> metadata = calculateMetadata(colValToCntKvpRdd);

    Map<String, String> originProperties = new HashMap<>();
    originProperties.put(Const.METADATA_PROPERTY_ORIGIN, origin.getCode());

    logger.warn("Generating column enhanced metadata");
    JavaPairRDD<Key, Value> columnMetadata =
        createColumnMetadata(
            context,
            metadata,
            version,
            datasetName,
            tableId,
            tableName,
            schema,
            visibility,
            originProperties,
            context.getConfig().accumuloMetadataTable);
    return new ArrayList<>(
        Collections.singletonList(
            new SparkAccumuloIO(context, version.id)
                .importToAccumulo(
                    columnMetadata, context.getConfig().accumuloMetadataTable, new HashSet<>())));
  }

  public static int calculateColumnCountTableParittions(
      JavaPairRDD<String, Long> columnCounts, int numSamples, long tabletSize) {

    long numColumnCounts = columnCounts.count();

    // Calculate the size for each sample
    List<Tuple2<String, Long>> samples = columnCounts.takeSample(false, numSamples);
    long totalColSize =
        samples.stream().map(r -> (long) r._1.getBytes().length).reduce(0L, Long::sum);

    // Calculate the average size for the samples
    double avgColSize = totalColSize / (double) samples.size();

    // Determine the number of columns per partition
    double colsPerPartition = Math.ceil(tabletSize / avgColSize);
    int numPartitions = (int) Math.ceil(numColumnCounts / colsPerPartition);

    logger.warn("Calculating the number of column count records per tablet");
    logger.warn("----------------------------------------------------------");
    logger.warn("Number of column count records: " + numColumnCounts);
    logger.warn("Requested number of samples: " + numSamples);
    logger.warn("Actual number of samples: " + samples.size());
    logger.warn("Requested tablet size: " + tabletSize);
    logger.warn("Average column size: " + avgColSize);
    logger.warn("Number of partitions to create: " + numPartitions);
    logger.warn("Maximum number of columns per partition: " + (int) colsPerPartition);

    return numPartitions;
  }

  /**
   * Convert column value counts to sortable ColumnCountObjects. The resulting set isn't sorted, it
   * just contains ColumnCountObjects for the count ascending sort order.
   *
   * @param colValCntRDD PairRDD of (colName\x00colVal, count)
   * @param dataset The name of the dataset
   * @param tableId The id of the table
   * @param visibility The visibility of the data
   * @return PairRDD of (ColumnCountObject Key, ColumnCountObject Value)
   */
  private static JavaPairRDD<Key, Value> genCntAscObjPairs(
      JavaPairRDD<String, Long> colValCntRDD,
      String dataset,
      String tableId,
      String visibility,
      String columnVisibilityExpression) {

    return colValCntRDD.mapToPair(
        tuple -> {
          Tuple3<String, String, String> splits = splitColumnValueVisibility(tuple._1);
          ColumnCountObject c =
              new ColumnCountObject(
                  dataset,
                  tableId,
                  splits._1(),
                  splits._3(),
                  tuple._2,
                  addDatasetAndColumnVisibilities(
                      visibility, columnVisibilityExpression, splits._2()));
          return new Tuple2<>(c.createAccumuloKeyCountAscending(), new Value());
        });
  }

  /**
   * Convert column value counts to sortable ColumnCountObjects. The resulting set isn't sorted, it
   * just contains ColumnCountObjects for the count descending sort order.
   *
   * @param colValCntRDD PairRDD of (colName\x00colVal, count)
   * @param dataset The name of the dataset
   * @param tableId The id of the table
   * @param visibility The visibility of the data
   * @return PairRDD of (ColumnCountObject Key, ColumnCountObject Value)
   */
  private static JavaPairRDD<Key, Value> genCntDescObjPairs(
      JavaPairRDD<String, Long> colValCntRDD,
      String dataset,
      String tableId,
      String visibility,
      String columnVisibilityExpression) {

    return colValCntRDD.mapToPair(
        tuple -> {
          Tuple3<String, String, String> splits = splitColumnValueVisibility(tuple._1);
          ColumnCountObject c =
              new ColumnCountObject(
                  dataset,
                  tableId,
                  splits._1(),
                  splits._3(),
                  tuple._2,
                  addDatasetAndColumnVisibilities(
                      visibility, columnVisibilityExpression, splits._2()));
          return new Tuple2<>(c.createAccumuloKeyCountDescending(), new Value());
        });
  }

  /**
   * Convert column value counts to sortable ColumnCountObjects. The resulting set isn't sorted, it
   * just contains ColumnCountObjects for the value ascending sort order.
   *
   * @param colValCntRDD PairRDD of (colName\x00colVal, count)
   * @param dataset The name of the dataset
   * @param tableId The id of the table
   * @param visibility The visibility of the data
   * @return PairRDD of (ColumnCountObject Key, ColumnCountObject Value)
   */
  private static JavaPairRDD<Key, Value> genValueAscObjPairs(
      JavaPairRDD<String, Long> colValCntRDD,
      String dataset,
      String tableId,
      String visibility,
      String columnVisibilityExpression) {

    return colValCntRDD.mapToPair(
        tuple -> {
          Tuple3<String, String, String> splits = splitColumnValueVisibility(tuple._1);
          ColumnCountObject c =
              new ColumnCountObject(
                  dataset,
                  tableId,
                  splits._1(),
                  splits._3(),
                  tuple._2,
                  addDatasetAndColumnVisibilities(
                      visibility, columnVisibilityExpression, splits._2()));
          return new Tuple2<>(c.createAccumuloKeyValueAscending(), new Value());
        });
  }

  /**
   * Convert column value counts to sortable ColumnCountObjects. The resulting set isn't sorted, it
   * just contains ColumnCountObjects for the value descending sort order.
   *
   * @param colValCntRDD PairRDD of (colName\x00colVal, count)
   * @param dataset The name of the dataset
   * @param tableId The id of the table
   * @param visibility The visibility of the data
   * @return PairRDD of (ColumnCountObject Key, ColumnCountObject Value)
   */
  private static JavaPairRDD<Key, Value> genValueDescObjPairs(
      JavaPairRDD<String, Long> colValCntRDD,
      String dataset,
      String tableId,
      String visibility,
      String columnVisibilityExpression) {

    return colValCntRDD.mapToPair(
        tuple -> {
          Tuple3<String, String, String> splits = splitColumnValueVisibility(tuple._1);
          ColumnCountObject c =
              new ColumnCountObject(
                  dataset,
                  tableId,
                  splits._1(),
                  splits._3(),
                  tuple._2,
                  addDatasetAndColumnVisibilities(
                      visibility, columnVisibilityExpression, splits._2()));
          return new Tuple2<>(c.createAccumuloKeyValueDescending(), new Value());
        });
  }

  /**
   * Create row visibility expression based on the visibilities of both the dataset and column
   * visibilities.
   *
   * @param visibility dataset visibility
   * @param globalColumnVisibility global column visibility obtained from columnVisibilityExpression
   *     param
   * @param columnVisibility column visibility obtained from RowVisibilities column
   * @return combined row visibility
   */
  private static String addDatasetAndColumnVisibilities(
      String visibility, String globalColumnVisibility, String columnVisibility) {
    // TODO add global column visibility expression
    return columnVisibility.equals("")
        ? visibility
        : String.join("", visibility, "&(", columnVisibility, ")");
  }

  /**
   * Create column counts, column count page indexes, and samples for all 4 sort orders.
   *
   * @param context Spark context
   * @param io SparkAccumuloIO
   * @param colValToCntKvpRdd PairRDD of (colName\x00colVal, count)
   * @param sortOrder The sort order to generate
   * @param dataset The name of the dataset
   * @param tableId The id of the table
   * @param visibility The visibility of the data
   * @param numPartitions The number of partitions
   * @throws IOException
   * @throws BasicAccumuloException
   */
  private static List<String> genAndStoreColCnts(
      DPSparkContext context,
      SparkAccumuloIO io,
      JavaPairRDD<String, Long> colValToCntKvpRdd,
      SortOrder sortOrder,
      String dataset,
      String tableId,
      String visibility,
      String columnVisibilityExpression,
      String rowVisibilityColumnName,
      int numPartitions)
      throws IOException, BasicAccumuloException {

    // Create ColumnCountObject KVPs for each sort order
    logger.warn(
        String.format("Generating sorted column counts for sort order: %s", sortOrder.toString()));
    JavaPairRDD<Key, Value> colCntObjKvpRdd =
        genColCntKvpRDD(
            colValToCntKvpRdd, sortOrder, dataset, tableId, visibility, columnVisibilityExpression);

    JavaPairRDD<Key, Value> sortedColCntObjKvpRdd = colCntObjKvpRdd.sortByKey(true, numPartitions);

    // Get the columnCountPagination Objects
    logger.warn("Generating column count pagination indices");
    List<Map.Entry<Long, Long>> colStartEndIdx = genColStartEndIdx(sortedColCntObjKvpRdd);

    // Add an index to each of the sorted objects
    JavaPairRDD<Tuple2<Key, Value>, Long> sortedColCntIdxKvpRdd =
        sortedColCntObjKvpRdd.zipWithIndex();

    // Generate the ColumnCountPagination objects
    JavaPairRDD<Key, Value> colCntPagKVP =
        CreateColumnCountPaginationIndex.genColumnCountPaginationObjKvpRdd(
            context, sortedColCntIdxKvpRdd, colStartEndIdx, Const.VALUES_PER_PAGE);

    // Get the first record of each partition to use as a split point
    logger.warn("Generating split points");
    Collection<Text> splits = partitionsFirstRecords(sortedColCntObjKvpRdd);

    List<String> outputIds = new ArrayList<>();
    // Combine the column counts and the column count pagination index and ingest them
    logger.warn("Storing column counts and pagination indices");
    outputIds.add(
        io.importToAccumulo(
            sortedColCntObjKvpRdd.union(colCntPagKVP),
            context.getConfig().accumuloColumnCountsTable,
            splits));

    // Generate the ColumnCountSample objects
    logger.warn("Generating column count samples");
    JavaPairRDD<Key, Value> colCntSampleKVP =
        CreateColumnCountSamples.genColumnCountSampleObjKvpRdd(
            context, sortedColCntIdxKvpRdd, colStartEndIdx, context.getConfig().numSamples);

    logger.warn("Storing column count samples");
    outputIds.add(
        io.importToAccumulo(
            colCntSampleKVP, context.getConfig().accumuloSamplesTable, new HashSet<>()));

    return outputIds;
  }

  /**
   * Convert column value counts to sortable ColumnCountObjects. The resulting set isn't sorted, it
   * just contains ColumnCountObjects for each sort order. Currently there are 4 sort orders: count
   * ascending, count descending, value ascending, and value descending.
   *
   * @param colValToCntKvpRdd PairRDD of (colName\x00colVal, count)
   * @param sortOrder The sort order to generate
   * @param dataset The name of the dataset
   * @param tableId The id of the table
   * @param visibility The visibility of the data
   * @return PairRDD of (ColumnCountObject Key, ColumnCountObject Value)
   */
  private static JavaPairRDD<Key, Value> genColCntKvpRDD(
      JavaPairRDD<String, Long> colValToCntKvpRdd,
      SortOrder sortOrder,
      String dataset,
      String tableId,
      String visibility,
      String columnVisibilityExpression) {
    switch (sortOrder) {
      case CNT_ASC:
        return genCntAscObjPairs(
            colValToCntKvpRdd, dataset, tableId, visibility, columnVisibilityExpression);
      case CNT_DESC:
        return genCntDescObjPairs(
            colValToCntKvpRdd, dataset, tableId, visibility, columnVisibilityExpression);
      case VAL_ASC:
        return genValueAscObjPairs(
            colValToCntKvpRdd, dataset, tableId, visibility, columnVisibilityExpression);
      default:
        return genValueDescObjPairs(
            colValToCntKvpRdd, dataset, tableId, visibility, columnVisibilityExpression);
    }
  }

  /**
   * Return the first record in each partition
   *
   * @param sortedColCntObjcKvpRDD PairRDD of (ColumnCountObject Key, ColumnCountObject Value)
   * @return The first record in each partition
   */
  private static Collection<Text> partitionsFirstRecords(
      JavaPairRDD<Key, Value> sortedColCntObjcKvpRDD) {
    return sortedColCntObjcKvpRDD
        .mapPartitions(
            iter -> {
              List<Tuple2<Key, Value>> foo = new ArrayList<>();
              if (iter.hasNext()) {
                foo.add(iter.next());
              }

              return foo.iterator();
            })
        .map(r -> r._1.getRow())
        .collect();
  }

  /** Generate evenly distributed split points */
  public static Collection<Text> generateSplits(
      JavaPairRDD<Key, Value> records, Integer recordsPerSplit) {
    return records
        .sortByKey()
        .zipWithIndex()
        .filter(record -> record._2 % recordsPerSplit == 0)
        .keys()
        .map(r -> r._1.getRow())
        .collect();
  }

  public static Collection<Text> generateSplitsPresorted(
      JavaPairRDD<Tuple2<Key, Value>, Long> records, Integer recordsPerSplit) {
    return records
        .filter(record -> record._2 % recordsPerSplit == 0)
        .keys()
        .map(r -> r._1.getRow())
        .collect();
  }

  /**
   * Load a CSV into a DataFrame.
   *
   * @return DataFrame of rows from the CSV file.
   */
  public static Dataset<Row> readCSV(SparkSession spark, CsvFileParams file)
      throws InvalidSchemaFormatException {
    DataFrameReader reader =
        spark
            .read()
            .format("csv")
            .option("mode", "DROPMALFORMED")
            .option("delimiter", file.getDelimiter())
            .option("charset", file.getCharset())
            .option("quote", file.getQuote())
            .option("escape", file.getEscape())
            .option("multiLine", file.isMultiline());
    // .option("escape", "\"");

    if (file.getHasHeader()) {
      reader.option("header", "true");
    } else {
      reader.option("header", "false");
    }

    if (file.getInferSchema()) {
      reader.option("inferSchema", "true");
    }

    Dataset<Row> rows = reader.load(file.getInputFilename());

    if (file.getSchemaFilename() != null) {
      rows = applySchemaFromFile(spark, rows, file.getSchemaFilename());
    }

    return rows;
  }

  public static Dataset<Row> applySchemaFromFile(
      SparkSession spark, Dataset<Row> rows, String schemaFilename)
      throws InvalidSchemaFormatException {
    List<Row> columnNamesFromCsv = getColumnNamesFromCsv(spark, schemaFilename);
    logger.warn("Applying schema from file: " + schemaFilename);
    if (rows.columns().length != columnNamesFromCsv.size()) {
      String err =
          String.format(
              "Schema from schema file %s has a different number of columns "
                  + "than data: %d columns "
                  + "vs %d names",
              schemaFilename, rows.columns().length, columnNamesFromCsv.size());
      logger.warn(err);
      throw new InvalidSchemaFormatException(err);
    }

    for (int i = 0; i < rows.columns().length; i++) {
      String columnName = columnNamesFromCsv.get(i).getString(0);
      logger.warn(String.format("Renaming column %d to %s.", i, columnName));
      rows = rows.withColumnRenamed(rows.columns()[i], columnName);
    }

    return rows;
  }

  public static Dataset<Row> readCSV(SparkSession spark, String inputFile)
      throws InvalidSchemaFormatException {
    return readCSV(spark, new CsvFileParams(inputFile));
  }

  /** */
  public static List<Row> getColumnNamesFromCsv(SparkSession spark, String schemaFileName)
      throws InvalidSchemaFormatException {
    CsvFileParams params = new CsvFileParams(schemaFileName);
    params.setHasHeader(false);
    Dataset<Row> columnNames = readCSV(spark, params);

    if (columnNames.columns().length != 1) {
      throw new InvalidSchemaFormatException("Schema files should have only 1 column");
    }
    columnNames.show();

    return columnNames.select("_c0").collectAsList();
  }

  /**
   * Convert a Dataset of rows (what produced from loading a CSV) into a PairRDD of (Column Name
   * DELIM Column Value, 1L) and then sum the counts for for duplicate Column Name/Column Value
   * combinations
   *
   * @param rows DataFrame of rows
   * @return PairRDD of (Column Name DELIM Column Value, Count)
   */
  public static JavaPairRDD<String, Long> genColValToCntPairs(
      Dataset<Row> rows, String rowVisibilityColumnName) {
    // Remove any whitespace or unprintable chars around the column names
    String[] columns = normalizeValues(rows.columns());
    final Option<Object> rowVisibilityIndex = rows.schema().getFieldIndex(rowVisibilityColumnName);

    return rows.javaRDD()
        .flatMapToPair(
            row -> {
              List<Tuple2<String, Long>> result = new ArrayList<>();
              Map<String, String> visibilityOverrideColumns =
                  getColumnVisibilitiesFromRow(row, rowVisibilityColumnName);
              for (int j = 0; j < row.length(); j++) {
                // Filter RowVisibilities column
                if (rowVisibilityIndex.isDefined()) {
                  if ((Integer) rowVisibilityIndex.get() == j) {
                    continue;
                  }
                }
                String colVal = "";
                if (row.get(j) != null) {
                  // Trim any surrounding non-printable chars and remove the delimiter character
                  colVal = sanitizeString(row.get(j).toString().trim());
                }
                // Only keep values that are less than a certain length
                if (colVal.length() < Const.INDEX_MAX_LENGTH) {
                  String visibility = visibilityOverrideColumns.getOrDefault(columns[j], "");
                  result.add(
                      new Tuple2<>(
                          String.join(Const.DELIMITER, columns[j], colVal, visibility), 1L));
                }
              }
              return result.iterator();
            })
        .reduceByKey(Long::sum);
  }

  /**
   * Creates a map of columns in the provided row to its visibility. The visibility expression is
   * parsed from the ROW_VISIBILITIES column.
   *
   * @param row Row
   * @return Map of column name and its row-specific visibility in the provided row.
   */
  public static Map<String, String> getColumnVisibilitiesFromRow(
      Row row, String rowVisibilityColumnName) {
    final Option<Object> rowVisibilityIndex = row.schema().getFieldIndex(rowVisibilityColumnName);
    // Map of columns -> visibility

    if (rowVisibilityIndex.isDefined()) {
      int index = (Integer) rowVisibilityIndex.get();
      String rowVisValue = row.getString(index);
      if (rowVisValue != null && !rowVisValue.isEmpty()) {
        String rowVisibility = getRowVisibilityFromRow(row, rowVisibilityColumnName);
        String colVisJson = row.get((Integer) rowVisibilityIndex.get()).toString();
        JSONObject columnJsonObject;
        Map<String, String> visibilityOverrideColumns = new HashMap<>();
        try {
          columnJsonObject =
              new JSONObject(colVisJson.substring(colVisJson.indexOf("{"))).getJSONObject("column");
          for (String cn : row.schema().fieldNames()) {
            String value = null;
            if (rowVisibility != null) {
              if (columnJsonObject.has(cn)) {
                value =
                    String.join(
                        "&", rowVisibility, "(" + columnJsonObject.get(cn).toString() + ")");
              } else {
                value = rowVisibility;
              }
            } else {
              if (columnJsonObject.has(cn)) {
                value = columnJsonObject.get(cn).toString();
              }
            }
            if (value != null) {
              visibilityOverrideColumns.put(cn, value);
            }
          }
        } catch (JSONException e) {
          logger.warn(e.getMessage());
        }
        return visibilityOverrideColumns;
      }
    }
    return Collections.emptyMap();
  }

  /**
   * Calculate number of unique values (so number of values ignoring count of each value) and number
   * of values (so summing the count of all values) of a PairRDD.
   *
   * @param reducedColNameValueMap PairRDD (Column Name DELIM Column Value, Count)
   * @return number of unique values for the column as (Column Name, Tuple(Unique, Count))
   */
  public static JavaPairRDD<String, Tuple2<Long, Long>> calculateMetadata(
      JavaPairRDD<String, Long> reducedColNameValueMap) {
    return reducedColNameValueMap
        .mapToPair(
            tuple -> {
              Tuple3<String, String, String> splits = splitColumnValueVisibility(tuple._1);
              String column = splits._1();

              return new Tuple2<>(column, new Tuple2<>(1L, tuple._2));
            })
        .reduceByKey(
            (tuplea, tupleb) -> {
              long unique = tuplea._1 + tupleb._1;
              long values = tuplea._2 + tupleb._2;

              return new Tuple2<>(unique, values);
            });
  }

  public static Map<String, ColumnMetaData> extractSchema(Dataset<Row> table) {
    StructType schema = table.schema();
    HashMap<String, ColumnMetaData> t = new HashMap<>();
    int i = 0;
    for (StructField field : schema.fields()) {
      ColumnMetaData c = new ColumnMetaData(field.name().trim(), field.dataType(), true, i);
      t.put(field.name().trim(), c);
      i++;
    }

    return t;
  }

  public static Map<String, Tuple2<String, Integer>> schemaToStringSchema(
      Map<String, ColumnMetaData> schema) {
    Map<String, Tuple2<String, Integer>> stringMetaData = new HashMap<>();

    for (Entry<String, ColumnMetaData> entry : schema.entrySet()) {
      stringMetaData.put(
          entry.getKey(),
          new Tuple2<>(entry.getValue().getTypeString(), entry.getValue().getColumnNumber()));
    }

    return stringMetaData;
  }

  public static JavaPairRDD<Key, Value> createColumnMetadata(
      Context context,
      JavaPairRDD<String, Tuple2<Long, Long>> metadata,
      MetadataVersionObject version,
      String datasetName,
      String tableId,
      String tableName,
      Map<String, ColumnMetaData> schema,
      String visibility,
      Map<String, String> properties,
      String accumuloTable) {

    // DataType is not serializable, so just pass the strings into the closure
    Map<String, Tuple2<String, Integer>> columnTypes = schemaToStringSchema(schema);

    return metadata.mapToPair(
        tuple -> {
          Long numUniqueValues = tuple._2._1;
          Long numValues = tuple._2._2;
          String columnName = tuple._1;

          VersionedMetadataObject m =
              new VersionedMetadataObject(version, datasetName, tableId, tableName, columnName);
          m.metadata_level = VersionedMetadataObject.COLUMN;
          m.load_time = System.currentTimeMillis();
          m.update_time = m.load_time;
          m.num_columns = 1L;
          m.data_type = columnTypes.get(columnName)._1;
          m.num_unique_values = numUniqueValues;
          m.num_values = numValues;
          m.column_num = columnTypes.get(columnName)._2;
          m.properties.putAll(properties);
          m.setVisibility(visibility);

          return new Tuple2<>(m.createAccumuloKey(), m.createAccumuloValue());
        });
  }

  /**
   * Serializes the ColumnCountIndex from the counts
   *
   * @param colValCnts PairRDD (Column Name DELIM Column Value, Count)
   * @return PairRDD of Accumulo Key and Value
   */
  public static JavaPairRDD<Key, Value> calculateIndex(
      JavaPairRDD<String, Long> colValCnts,
      String dataset,
      String tableId,
      String tableName,
      String visibility) {
    return colValCnts
        .filter(
            tuple -> {
              Tuple3<String, String, String> splits = splitColumnValueVisibility(tuple._1);
              String value = splits._3();
              return value != null && !value.isEmpty();
            })
        .flatMapToPair(
            tuple -> {
              Tuple3<String, String, String> splits = splitColumnValueVisibility(tuple._1);
              String column = splits._1();
              String value = splits._3();

              List<Tuple2<Key, Value>> indexEntries = new ArrayList<>();
              List<String> valuesToIndex = new ArrayList<>();
              boolean sizeOK = value.length() < Const.INDEX_MAX_LENGTH;
              String originalValue = value;
              if (sizeOK) {
                valuesToIndex.add(value.toLowerCase().trim());
              } else {
                originalValue = value.substring(0, 256) + "...";
              }
              valuesToIndex.addAll(SolrTokenizer.tokenize(value));
              String vis =
                  splits._2().isEmpty() ? visibility : visibility + "&(" + splits._2() + ")";
              for (String valueToIndex : valuesToIndex) {
                ColumnCountIndexObject c =
                    new ColumnCountIndexObject(
                        dataset, tableName, tableId, column, originalValue, valueToIndex, tuple._2);
                c.setVisibility(vis);
                indexEntries.addAll(createAllIndecies(c));
              }
              return indexEntries.iterator();
            });
  }

  public static List<Tuple2<Key, Value>> createAllIndecies(ColumnCountIndexObject index)
      throws IOException {
    List<Tuple2<Key, Value>> indices = new ArrayList<>();

    index.indexTypeGlobal();
    indices.add(new Tuple2<>(index.createAccumuloKey(), index.createAccumuloValue()));

    index.indexTypeDataset();
    indices.add(new Tuple2<>(index.createAccumuloKey(), index.createAccumuloValue()));

    index.indexTypeTable();
    indices.add(new Tuple2<>(index.createAccumuloKey(), index.createAccumuloValue()));

    index.indexTypeColumn();
    indices.add(new Tuple2<>(index.createAccumuloKey(), index.createAccumuloValue()));

    return indices;
  }

  private static Tuple2<String, String> splitColumnAndValue(String key) throws Exception {
    String[] parts = key.split(Const.DELIMITER);
    String column;
    String value;
    if (parts.length == 2) {
      column = parts[0];
      value = parts[1];
    } else if (parts.length == 1) {
      column = parts[0];
      value = "";
    } else {
      throw new Exception(
          String.format(
              "Invalid loader internal data format (num parts: %d, key:" + " %s",
              parts.length, key));
    }

    return new Tuple2<>(column, value);
  }

  /**
   * Utility function that creates a Tuple 3 of column, visibility, and value.
   *
   * @param key delimited string of column, value, and visibility (in that order).
   * @return Tuple3 containing column, visibility, and value (in that order).
   * @throws Exception
   */
  private static Tuple3<String, String, String> splitColumnValueVisibility(String key)
      throws Exception {
    String[] parts = key.split(Const.DELIMITER);
    String column;
    String value;
    String visibility;
    if (parts.length == 3) {
      column = parts[0];
      value = parts[1];
      visibility = parts[2];
    } else if (parts.length == 2) {
      column = parts[0];
      value = parts[1];
      visibility = "";
    } else if (parts.length == 1) {
      column = parts[0];
      value = "";
      visibility = "";
    } else {
      throw new Exception(
          String.format(
              "Invalid loader internal data format (num parts: %d, key:" + " %s",
              parts.length, key));
    }

    return new Tuple3<>(column, visibility, value);
  }

  public static void createTableIfNeeded(DPSparkContext context, String table)
      throws BasicAccumuloException {
    if (context.getConfig().loadType != LoadType.LIVE) {
      return;
    }

    TableOperations tops = context.getConnector().tableOperations();

    // Create the table if it doesn't exist
    if (!tops.exists(table)) {
      try {
        if (logger.isInfoEnabled()) {
          logger.info("table does not exist - creating table " + table);
        }
        context.getConnector().tableOperations().create(table);
      } catch (Exception e) {
        throw new BasicAccumuloException(e.toString());
      }
    }
  }

  public static List<String> loadDatawaveRows(
      DPSparkContext context,
      SparkSession spark,
      MetadataVersionObject version,
      Dataset<Row> origTable,
      String datasetName,
      String tableId,
      String visibility,
      String columnVisibilityExpression,
      String rowVisibilityColumnName,
      long numRows,
      int numSamples,
      long tabletSize)
      throws BasicAccumuloException, IOException {

    String accumuloTable = context.getConfig().accumuloDatawaveRowsTable;
    createTableIfNeeded(context, accumuloTable);

    // Get the name of columns
    final String[] columns = normalizeValues(origTable.columns());

    Map<String, ColumnMetaData> schema = extractSchema(origTable);
    logger.warn("Schema: " + schema);
    Map<String, Tuple2<String, Integer>> stringSchema = schemaToStringSchema(schema);
    logger.warn("String Schema: " + stringSchema);

    int numPartitions = calculateRowTableParittions(origTable, numRows, numSamples, tabletSize);

    // Repartition the data to be evenly distributed in Accumulo
    JavaRDD<Row> partitionedData = origTable.javaRDD().repartition(numPartitions);

    // Give each row an index number
    JavaPairRDD<Row, Long> indexedData = partitionedData.zipWithIndex();

    logger.warn("Generating Row and Shard Data");
    // Create the row data and index records
    JavaPairRDD<Key, Value> rowData =
        generateDatawaveRows(
            indexedData,
            columns,
            datasetName,
            tableId,
            stringSchema,
            visibility,
            columnVisibilityExpression,
            rowVisibilityColumnName);

    // Create the shardID records
    JavaPairRDD<Key, Value> shardData =
        generateShardIndex(indexedData, datasetName, tableId, visibility);

    SparkAccumuloIO io = new SparkAccumuloIO(context, version.id);

    // Generate split points based on the data size
    logger.warn("Generating split points");
    Collection<Text> splits =
        SparkAccumuloIO.generateDatawaveRowsSplits(numPartitions, datasetName, tableId);

    List<String> outputPaths = new ArrayList<>();
    outputPaths.add(io.importToAccumulo(rowData, accumuloTable, splits));
    outputPaths.add(io.importToAccumulo(shardData, accumuloTable, new HashSet<>()));

    return outputPaths;
  }

  public static int calculateRowTableParittions(
      Dataset<Row> dataTable, long numRows, int numSamples, long tabletSize) {

    logger.warn("Calculating the number of rows per tablet");
    logger.warn("-----------------------------------------");
    logger.warn("Number of rows: " + numRows);
    logger.warn("Number of samples: " + numSamples);
    logger.warn("Tablet size: " + tabletSize);

    // Get the top n rows and the number of the sample size if less than n
    Dataset<Row> sample = dataTable.limit(numSamples);
    long sampleSize = sample.count();

    // Determine the size of each row, e.g. the sum of the number of bytes for each row
    JavaRDD<Long> rowSize =
        sample
            .javaRDD()
            .map(
                r -> {
                  long size = 0;

                  for (int i = 0; i < r.length(); i++) {
                    if (r.get(i) != null) {
                      size += r.get(i).toString().getBytes().length;
                    }
                  }

                  return size;
                });

    // Get the average row size for the top n rows
    long totalRowSize = rowSize.collect().stream().reduce(0L, Long::sum);
    double avgRowSize = totalRowSize / (double) sampleSize;
    logger.warn(
        String.format("The top %d rows have an average size of %f bytes", sampleSize, avgRowSize));

    // Get the size of the columns, e.g. the sum of the number of bytes to store the column names
    String[] columns = dataTable.columns();
    int colSize = Arrays.stream(columns).map(c -> c.getBytes().length).reduce(0, Integer::sum);

    // A row in Accumulo consists of a lot of things, but this will simplify the estimated storage.
    // The calculation is 3 times the space needed to store the column name and value. The storage
    // of a data row contains a data record and an index record for each column and the index record
    // stores column name and column value twice. Hence, the multiplier of 3. The other parts of the
    // Accumulo key are ignored because they will be small compared to the actual data being stored.
    // double avgRowSizeWithEncoding = 3 * (colSize + avgRowSize);
    double avgRowSizeWithEncoding = (colSize + avgRowSize);
    logger.warn(
        String.format("The average size of a row with encoding: %f bytes", avgRowSizeWithEncoding));

    // Determine the number of rows per partition
    double rowsPerPartition = Math.ceil(tabletSize / avgRowSizeWithEncoding);
    int numPartitions = (int) Math.ceil(numRows / rowsPerPartition);
    logger.warn(
        String.format(
            "Creating %d partitions with max %d rows", numPartitions, (int) rowsPerPartition));

    return numPartitions;
  }

  private static JavaPairRDD<Key, Value> generateDatawaveRows(
      JavaPairRDD<Row, Long> df,
      String[] columns,
      String dataSetName,
      String tableId,
      Map<String, Tuple2<String, Integer>> stringMetaData,
      String visibility,
      String columnVisibilityExpression,
      String rowVisibilityColumnName) {

    // Create a RowObject and RowIndexObject for each column value
    return df.flatMapToPair(
        row -> {
          List<Tuple2<Key, Value>> result = new ArrayList<>();

          // Use the current partition's id as the shard ID
          int shardId = TaskContext.getPartitionId();

          Map<String, String> visibilityOverrideColumns =
              getColumnVisibilitiesFromRow(row._1, rowVisibilityColumnName);

          // Iterate over all of the columns in a row to create a data and index record for each
          // column
          for (int i = 0; i < row._1.length(); i++) {

            String colName = columns[i];
            String colVal = "";
            if (row._1.get(i) != null) {
              colVal = sanitizeString(row._1.get(i).toString().trim());
            }

            String columnVisibility = visibilityOverrideColumns.getOrDefault(columns[i], "");

            DatawaveRowObject col =
                new DatawaveRowObject(shardId, dataSetName, tableId, colName, colVal, row._2);
            col.setVisibility(
                addDatasetAndColumnVisibilities(
                    visibility, columnVisibilityExpression, columnVisibility));
            result.add(new Tuple2<>(col.createAccumuloKey(), col.createAccumuloValue()));

            String dataType = stringMetaData.get(colName)._1;

            List<DatawaveRowIndexObject> indexEntries = Lists.newArrayList();
            DatawaveRowIndexObject indexObj =
                new DatawaveRowIndexObject(
                    shardId, dataSetName, tableId, colName, colVal, dataType, row._2);
            indexObj.setVisibility(
                addDatasetAndColumnVisibilities(
                    visibility, columnVisibilityExpression, columnVisibility));
            indexEntries.add(indexObj);

            if ("string".equalsIgnoreCase(dataType)) {
              SolrTokenizer.tokenize(colVal)
                  .forEach(
                      token -> {
                        DatawaveRowIndexObject tokenIndexObj =
                            new DatawaveRowIndexObject(
                                shardId, dataSetName, tableId, colName, token, dataType, row._2);
                        tokenIndexObj.setVisibility(
                            addDatasetAndColumnVisibilities(
                                visibility, columnVisibilityExpression, columnVisibility));
                        indexEntries.add(tokenIndexObj);
                      });
            }

            indexEntries.forEach(
                colIdx -> {
                  try {
                    result.add(
                        new Tuple2<>(colIdx.createAccumuloKey(), colIdx.createAccumuloValue()));
                  } catch (IOException e) {
                    logger.error("Exception raised while creating an Accumulo value!", e);
                  }
                });
          }

          return result.iterator();
        });
  }

  private static JavaPairRDD<Key, Value> generateShardIndex(
      JavaPairRDD<Row, Long> df, String dataSetName, String tableId, String visibility) {

    return df.mapPartitionsToPair(
        records -> {
          // Use the current partition's id as the shard ID
          int shardId = TaskContext.getPartitionId();
          long startIdx = Long.MAX_VALUE;

          // Find the lowest value index
          while (records.hasNext()) {
            Tuple2<Row, Long> curr = records.next();

            if (curr._2 < startIdx) {
              startIdx = curr._2;
            }
          }

          if (startIdx != Long.MAX_VALUE) {
            DatawaveRowShardIndexObject shardIdx =
                new DatawaveRowShardIndexObject(shardId, dataSetName, tableId, startIdx);
            shardIdx.setVisibility(visibility);
            return Collections.singleton(
                    new Tuple2<>(shardIdx.createAccumuloKey(), shardIdx.createAccumuloValue()))
                .iterator();
          } else {
            return Collections.emptyIterator();
          }
        },
        true);
  }

  /**
   * Remove leading and trailing whitespace from elements in an array
   *
   * @param values An array of strings
   * @return An array of strings with leading and trailing whitespace removed
   */
  public static String[] normalizeValues(String[] values) {
    for (int i = 0; i < values.length; i++) {
      values[i] = values[i].trim();
    }
    return values;
  }

  public static String[] splitext(String path) {
    Path inPath = Paths.get(path);
    String fname = inPath.getFileName().toString();

    String[] split = fname.split("\\.(?=[^\\.]+$)");
    assert (split.length == 2);

    return split;
  }

  public static String tableNameFromFileName(String path) {
    String[] splitFname = splitext(path);

    if (splitFname.length > 1 && splitFname[1].equals(".gz")) {
      splitFname = splitext(splitFname[0]);
    }

    return FilenameUtils.getBaseName(splitFname[0]);
  }

  public static String sanitizeString(String in) {
    return in.replaceAll(Const.DELIMITER, "");
  }

  public static String getRowVisibilityFromRow(Row row, String rowVisibilityColumnName) {
    final int rowVisibilityIndex = row.schema().fieldIndex(rowVisibilityColumnName);
    String rowVisibility = null;
    if (row.get(rowVisibilityIndex) != null) {
      try {
        String visibilityJson = row.get(rowVisibilityIndex).toString();
        rowVisibility =
            new JSONObject(visibilityJson.substring(visibilityJson.indexOf("{"))).getString("row");
      } catch (JSONException e) {
        logger.debug(String.format("failed to parse row visibility from json: {}", e.getMessage()));
      }
    }
    return rowVisibility;
  }
}
