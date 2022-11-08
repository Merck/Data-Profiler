package com.dataprofiler.util.objects;

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
import com.dataprofiler.MiniAccumuloWithData;
import com.dataprofiler.test.IntegrationTest;
import com.dataprofiler.util.Const;
import com.dataprofiler.util.iterators.ClosableIterator;
import com.dataprofiler.loader.TableLoader;
import com.dataprofiler.loader.config.CsvFileParams;
import com.dataprofiler.loader.datatypes.CsvLoader;
import com.dataprofiler.loader.datatypes.Loader;

import net.jcip.annotations.NotThreadSafe;
import org.apache.accumulo.core.client.lexicoder.LongLexicoder;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.hadoop.io.Text;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

@NotThreadSafe
@Category(IntegrationTest.class)
public class DatawaveRowShardIndexObjectTest {
  private static final String DATASET_NAME = "Chuck";
  private static final String TABLE_NAME = "Norris";
  private static final String VISIBILITY = "LIST.PUBLIC_DATA";
  private static final String DATASET = "src/test/resources/basic_test_data.csv";
  private static MiniAccumuloWithData mad;

  @BeforeClass
  public static void setup() throws Exception {
    mad = new MiniAccumuloWithData();
    mad.startForTesting();

    // Only the Rows table
    SparkSession spark = null;

    try {
      DPSparkContext context = new DPSparkContext(mad.getContext(), "test");
      spark = context.createSparkSession();

      CsvFileParams params = new CsvFileParams();
      params.setCharset("utf-8");
      params.setDelimiter(",");
      params.setEscape("\"");
      params.setQuote("\"");
      params.setInputFilename(DATASET);

      // The CSV loader is called specifically to define the num samples and tablet size
      CsvLoader loader =
          new CsvLoader(context, spark, DATASET_NAME, TABLE_NAME, VISIBILITY, "", "", params);
      Dataset<Row> origTable = Loader.readCSV(spark, params);

      long numRows = origTable.count();

      TableLoader.loadDatawaveRows(
          context,
          spark,
          new MetadataVersionObject("foo"),
          origTable,
          DATASET_NAME,
          TABLE_NAME,
          VISIBILITY,
          "",
          "",
          numRows,
          10,
          1);
    } finally {
      if (spark != null) {
        spark.close();
      }
    }
  }

  @AfterClass
  public static void teardown() throws IOException, InterruptedException {
    mad.close();
  }

  private ClosableIterator<DatawaveRowShardIndexObject> datawaveRowShardIter(
      String dataset, String table) {
    return new DatawaveRowShardIndexObject()
        .scan(mad.getContext())
        .addRange(new Range(AccumuloObject.joinKeyComponents(dataset, table)))
        .closeableIterator();
  }

  private ClosableIterator<DatawaveRowObject> datawaveRowIter(String dataset, String table) {
    return new DatawaveRowObject()
        .scan(mad.getContext())
        .addRange(
            new Range(
                new Key(dataset + "\u0000" + table),
                new Key(dataset + "\u0000" + table + "\uffff")))
        .closeableIterator();
  }

  @Test
  public void testShardsMatch() throws Exception {

    long idxShardFirst = Long.MAX_VALUE;
    long idxShardLast = Long.MIN_VALUE;
    Set<Long> idxShardSet = new HashSet<>();

    long rowShardFirst = Long.MAX_VALUE;
    long rowShardLast = Long.MIN_VALUE;
    Set<Long> rowShardSet = new HashSet<>();

    try (ClosableIterator<DatawaveRowShardIndexObject> iter =
        datawaveRowShardIter(DATASET_NAME, TABLE_NAME)) {

      while (iter.hasNext()) {
        long shardId = iter.next().getShard();
        idxShardSet.add(shardId);
        if (shardId < idxShardFirst) {
          idxShardFirst = shardId;
        }
        if (shardId > idxShardLast) {
          idxShardLast = shardId;
        }
      }
    }

    try (ClosableIterator<DatawaveRowObject> iter =
        datawaveRowIter(DATASET_NAME, TABLE_NAME)) {

      while (iter.hasNext()) {
        long shardId = iter.next().getShard();
        rowShardSet.add(shardId);
        if (shardId < rowShardFirst) {
          rowShardFirst = shardId;
        }
        if (shardId > rowShardLast) {
          rowShardLast = shardId;
        }
      }
    }

    assertEquals(idxShardFirst, rowShardFirst);
    assertEquals(idxShardLast, rowShardLast);
    assertEquals(idxShardSet, rowShardSet);
  }

  @Test
  public void testShardsCorrect() throws Exception {

    LongLexicoder longLex = new LongLexicoder();

    try (ClosableIterator<DatawaveRowShardIndexObject> iter =
        datawaveRowShardIter(DATASET_NAME, TABLE_NAME)) {

      while (iter.hasNext()) {

        DatawaveRowShardIndexObject shard = iter.next();

        byte[] rowId =
            AccumuloObject.joinKeyComponents(
                shard.getDatasetName().getBytes(),
                shard.getTableName().getBytes(),
                longLex.encode(shard.getShard()));
        byte[] colQual =
            AccumuloObject.joinKeyComponents(
                longLex.encode(shard.getRowIdx()), "CBSA Code".getBytes());
        Key start = new Key(new Text(rowId), new Text(Const.COL_FAM_DATA), new Text(colQual));

        byte[] colQual2 =
            AccumuloObject.joinKeyComponents(
                longLex.encode(shard.getRowIdx() + 1), "CBSA Code".getBytes());
        Key end = new Key(new Text(rowId), new Text(Const.COL_FAM_DATA), new Text(colQual2));

        try (ClosableIterator<DatawaveRowObject> rowIter =
            new DatawaveRowObject()
                .scan(mad.getContext())
                .addRange(new Range(start, end))
                .closeableIterator()) {

          if (rowIter.hasNext()) {
            DatawaveRowObject rowObj = rowIter.next();

            if (rowObj.getShard() != shard.getShard() || rowObj.getRowIdx() != shard.getRowIdx()) {
              fail();
            }

          } else {
            fail();
          }
        }
      }
    }

    assertTrue(true);
  }
}
