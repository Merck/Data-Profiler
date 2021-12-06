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

import static java.lang.String.format;

import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Const.LoadType;
import com.dataprofiler.util.objects.AccumuloObject;
import com.dataprofiler.util.objects.ObjectScannerIterable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.accumulo.core.client.lexicoder.LongLexicoder;
import org.apache.accumulo.core.client.mapreduce.AccumuloFileOutputFormat;
import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.log4j.Logger;
import org.apache.spark.Partitioner;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

public class SparkAccumuloIO {

  private static final Logger logger = Logger.getLogger(SparkAccumuloIO.class);

  private static final LongLexicoder longLex = new LongLexicoder();

  private DPSparkContext dpSparkContext;
  private FileSystem hdfs;
  private String uniqueId; // this is used as a prefix for file names as we save of rfiles

  public SparkAccumuloIO(DPSparkContext sparkContext) {
    init(sparkContext);
  }

  public SparkAccumuloIO(DPSparkContext sparkContext, String uniqueId) {
    init(sparkContext);
    this.uniqueId = uniqueId;
  }

  public DPSparkContext getDpSparkContext() {
    return dpSparkContext;
  }

  private void init(DPSparkContext sparkContext) {
    this.dpSparkContext = sparkContext;

    try {
      hdfs = FileSystem.get(sparkContext.getSparkContext().hadoopConfiguration());
    } catch (IOException e) {
      logger.error("Unable to sparkConnect to HDFS", e);
    }
  }

  /**
   * Creates split points prepended with a lexicographic sorted number. The format of the split is:
   * String.join(Delimiter, components) + Delimiter + number
   *
   * <p>The number of splits is determined by the number of rows and number of partitions.
   *
   * @param numRows The number of rows in the table
   * @param numPartitions The number of partitions
   * @param components The components of the key
   */
  public static Collection<Text> generateNumericSplits(
      long numRows, int numPartitions, String... components) {

    String base = AccumuloObject.joinKeyComponents(components);
    Integer skipSize = (int) (numRows / numPartitions);

    Collection<Text> splits = new TreeSet<>();
    for (long i = 0; i < numRows; i += skipSize) {
      byte[] split = AccumuloObject.joinKeyComponents(base.getBytes(), longLex.encode(i));
      splits.add(new Text(split));
    }

    return splits;
  }

  /**
   * Creates split points prepended with a lexicographic sorted number. The format of the split is:
   * String.join(Delimiter, components) + Delimiter + number
   *
   * <p>The number of splits is determined by the number of partitions.
   *
   * @param numPartitions The number of partitions
   * @param components The components of the key
   */
  public static Collection<Text> generateDatawaveRowsSplits(
      int numPartitions, String... components) {

    String base = AccumuloObject.joinKeyComponents(components);

    Collection<Text> splits = new TreeSet<>();

    for (long i = 0; i < numPartitions; i++) {
      byte[] split = AccumuloObject.joinKeyComponents(base.getBytes(), longLex.encode(i));
      splits.add(new Text(split));
    }

    return splits;
  }

  private Collection<Text> getSplits(String accumuloTable) throws BasicAccumuloException {
    try {
      return dpSparkContext.getConnector().tableOperations().listSplits(accumuloTable);
    } catch (Exception e) {
      throw new BasicAccumuloException(e.toString());
    }
  }

  public <T extends AccumuloObject> JavaPairRDD<Key, Value> pairRDDFromAccumulo(
      ObjectScannerIterable<T> scanner) throws IOException, BasicAccumuloException {

    Job job = scanner.createInputJob();
    Configuration configuration = job.getConfiguration();
    Authorizations auths = dpSparkContext.getAuthorizations();
    if (auths != null) {
      if (logger.isInfoEnabled()) {
        logger.info(format("using scan auths: %s", auths));
      }
      AccumuloInputFormat.setScanAuthorizations(job, auths);
    }
    return dpSparkContext
        .getSparkContext()
        .newAPIHadoopRDD(configuration, AccumuloInputFormat.class, Key.class, Value.class);
  }

  public <T extends AccumuloObject> JavaRDD<T> rddFromAccumulo(ObjectScannerIterable<T> scanner)
      throws IOException, BasicAccumuloException {
    T builder = scanner.getBuilder();
    return pairRDDFromAccumulo(scanner).map(row -> (T) builder.fromKeyAndValue(row._1, row._2));
  }

  /**
   * This will ingest into Accumulo using the method selected by the configuration. This can be bulk
   * ingest, live ingest, or just saving the rfile to a destination set by config / commandline.
   *
   * @param records
   * @param accumuloTable
   * @param splits
   * @return The ID of the output path
   * @throws IOException
   * @throws BasicAccumuloException
   */
  public String importToAccumulo(
      JavaPairRDD<Key, Value> records, String accumuloTable, Collection<Text> splits)
      throws IOException, BasicAccumuloException {
    LoadType l = dpSparkContext.getConfig().loadType;
    if (l == LoadType.LIVE) {
      logger.warn("Live importing data into Accumulo");
      liveImportToAccumulo(records, accumuloTable);
      return null;
    } else if (l == LoadType.SPLIT) {
      logger.warn("Split import; writing data to HDFS");
      return splitImportToHdfs(records, accumuloTable, splits);
    } else {
      logger.warn("Bulk importing data into Accumulo");
      bulkImportToAccumulo(records, accumuloTable, splits);
      return null;
    }
  }

  private <T extends AccumuloObject> void liveImportToAccumulo(
      JavaRDD<T> accumuloObjects, String accumuloTable) throws IOException, BasicAccumuloException {
    if (accumuloObjects.isEmpty()) {
      return;
    }
    T builder = accumuloObjects.first();
    JavaPairRDD<Text, Mutation> mutations =
        accumuloObjects.mapToPair(cc -> new Tuple2<>(new Text(accumuloTable), cc.createMutation()));

    liveImportToAccumulo(dpSparkContext.createOutputJob(accumuloTable), mutations);
  }

  private void liveImportToAccumulo(JavaPairRDD<Key, Value> accumuloRecords, String accumuloTable)
      throws IOException, BasicAccumuloException {
    if (accumuloRecords.isEmpty()) {
      return;
    }

    JavaPairRDD<Text, Mutation> mutations =
        accumuloRecords.mapToPair(
            record -> {
              Key key = record._1;
              Value val = record._2;
              Mutation mut = new Mutation(key.getRow());
              mut.put(
                  key.getColumnFamily(),
                  key.getColumnQualifier(),
                  key.getColumnVisibilityParsed(),
                  val);

              return new Tuple2<>(new Text(accumuloTable), mut);
            });

    liveImportToAccumulo(dpSparkContext.createOutputJob(accumuloTable), mutations);
  }

  private void liveImportToAccumulo(Job job, JavaPairRDD<Text, Mutation> mutations)
      throws IOException {
    mutations.saveAsNewAPIHadoopFile(
        getTempDir().toString(),
        Text.class,
        Mutation.class,
        AccumuloOutputFormat.class,
        job.getConfiguration());
  }

  private String splitImportToHdfs(
      JavaPairRDD<Key, Value> records, String accumuloTable, Collection<Text> splits)
      throws IOException {
    JavaPairRDD<Key, Value> partionedData = partitionAndSortBySplits(records, splits);
    if (uniqueId == null) {
      throw new IOException("unique id cannot be null for split output");
    }

    // auto-apply splits that align with the RFiles we're writing out
    if (splits != null
        && !splits.isEmpty()
        && this.dpSparkContext.getConfig().allowAccumuloConnection) {
      try {
        this.createSplits(splits, accumuloTable);
      } catch (BasicAccumuloException e) {
        logger.error("Failed to add splits to Accumulo. Still attemping load.");
      }
    }

    Path dataDir =
        new Path(
            format(
                "%s/%s/%s/%s",
                dpSparkContext.getConfig().loadOutputDest,
                uniqueId,
                accumuloTable,
                UUID.randomUUID()));
    outputRFile(partionedData, dataDir);
    return dataDir.toString();
  }

  private JavaPairRDD<Key, Value> partitionAndSortBySplits(
      JavaPairRDD<Key, Value> accumuloRecords, Collection<Text> splits) {
    Partitioner accumuloPartitioner = new AccumuloRangePartitioner(splits);
    return accumuloRecords.repartitionAndSortWithinPartitions(accumuloPartitioner);
  }

  private <T extends AccumuloObject> void bulkImportToAccumulo(
      JavaRDD<T> accumuloRecords, String accumuloTable, Integer recordsPerSplit)
      throws IOException, BasicAccumuloException {
    JavaPairRDD<Key, Value> records =
        accumuloRecords.mapToPair(
            (T row) -> new Tuple2<>(row.createAccumuloKey(), row.createAccumuloValue()));
    bulkImportToAccumulo(records, accumuloTable, recordsPerSplit);
  }

  private void bulkImportToAccumulo(
      JavaPairRDD<Key, Value> accumuloRecords, String accumuloTable, Collection<Text> splits)
      throws BasicAccumuloException, IOException {

    JavaPairRDD<Key, Value> partitionedData = partitionAndSortBySplits(accumuloRecords, splits);

    rawBulkImportAccumuloRecords(partitionedData, accumuloTable);
  }

  private void bulkImportToAccumulo(
      JavaPairRDD<Key, Value> accumuloRecords, String accumuloTable, Integer recordsPerSplit)
      throws BasicAccumuloException, IOException {

    // Get the new split points
    List<Text> localSplits =
        accumuloRecords
            .sortByKey()
            .zipWithIndex()
            .filter(record -> record._2 % recordsPerSplit == 0)
            .keys()
            .map(r -> r._1.getRow())
            .collect();

    // Import the data
    bulkImportToAccumulo(accumuloRecords, accumuloTable, localSplits);
  }

  /**
   * Add split points to the Accumulo table
   *
   * @param splits The splits to add
   * @param accumuloTable The table to add splits
   */
  private void createSplits(Collection<Text> splits, String accumuloTable)
      throws BasicAccumuloException {
    TableOperations tops = dpSparkContext.getConnector().tableOperations();
    TreeSet<Text> sortedSplits = new TreeSet<>(splits);

    try {
      tops.addSplits(accumuloTable, sortedSplits);
    } catch (Exception e) {
      throw new BasicAccumuloException(e.toString());
    }
  }

  private void outputRFile(JavaPairRDD<Key, Value> accumuloRecords, Path dataDir)
      throws IOException {
    logger.warn("Saving to path: " + dataDir.toString());

    Job job = Job.getInstance(dpSparkContext.getSparkContext().hadoopConfiguration());
    AccumuloFileOutputFormat.setOutputPath(job, dataDir);
    accumuloRecords.saveAsNewAPIHadoopFile(
        dataDir.toString(),
        Key.class,
        Value.class,
        AccumuloFileOutputFormat.class,
        job.getConfiguration());
    logger.warn(format("Wrote data for bulk import to directory: %s", dataDir));
  }

  private void rawBulkImportAccumuloRecords(
      JavaPairRDD<Key, Value> accumuloRecords, String accumuloTable)
      throws IOException, BasicAccumuloException {
    logger.warn("In raw bulk ingest");

    Path importDir =
        new Path(
            format(
                "%s/%s/%s/%s",
                dpSparkContext.getConfig().loadOutputDest,
                uniqueId,
                accumuloTable,
                UUID.randomUUID()));

    // Create HDFS directories
    Path dataDir = new Path(importDir, "data");
    Path failDir = new Path(importDir, "fail");

    // if (!hdfs.exists(dataDir)) {
    //   hdfs.mkdirs(dataDir);
    // }
    if (!hdfs.exists(failDir)) {
      hdfs.mkdirs(failDir);
    }

    outputRFile(accumuloRecords, dataDir);

    // Import data into HDFS
    try {
      dpSparkContext
          .getConnector()
          .tableOperations()
          .importDirectory(accumuloTable, dataDir.toString(), failDir.toString(), true);
    } catch (Exception e) {
      throw new BasicAccumuloException(e.toString());
    }

    // Throw exception if failures directory contains files
    if (hdfs.listFiles(failDir, true).hasNext()) {
      throw new IllegalStateException(
          "Bulk import failed!  Found files that failed to import "
              + "in failures directory: "
              + failDir);
    }

    logger.warn(
        format(
            "Successfully bulk imported data in %s to '%s' Accumulo table",
            dataDir, accumuloTable));

    // Delete temp directories
    hdfs.delete(importDir, true);
    logger.warn(format("Deleted HDFS import directory created for bulk import: %s", importDir));
  }

  private Path getPossibleTempDir(String dir) {
    return new Path(dir + "/" + UUID.randomUUID());
  }

  private Path getTempDir() throws IOException {

    Path tempDir = getPossibleTempDir(dpSparkContext.getConfig().sparkIngestBaseDir);

    while (hdfs.exists(tempDir)) {
      logger.warn(
          format("Temp directory '%s' already exists, creating a new temp directory", tempDir));
      tempDir = getPossibleTempDir(dpSparkContext.getConfig().sparkIngestBaseDir);
    }

    return tempDir;
  }
}
