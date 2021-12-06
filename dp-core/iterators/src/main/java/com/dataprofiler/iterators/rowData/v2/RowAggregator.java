package com.dataprofiler.iterators.rowData.v2;

/*-
 * 
 * dataprofiler-iterators
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

import static org.slf4j.LoggerFactory.getLogger;

import com.dataprofiler.querylang.EvaluatingVisitor;
import com.dataprofiler.querylang.ExpressionToIteratorTree;
import com.dataprofiler.querylang.expr.Expression;
import com.dataprofiler.querylang.json.Expressions;
import com.dataprofiler.util.Const;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.accumulo.core.data.ArrayByteSequence;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.hadoop.io.Text;
import org.apache.htrace.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.htrace.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

public class RowAggregator implements SortedKeyValueIterator<Key, Value> {
  public static final String CONF_HASH_LARGE_VALUES = "hash_large_values";
  public static final String CONF_INCLUDE_VISIBILITY_WITH_VALUE = "include_visibility";
  private static final Collection<ByteSequence> DOC_COLF =
      Collections.singletonList(new ArrayByteSequence("D"));
  protected static ObjectMapper mapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private final Logger log = getLogger(getClass());
  private IndexIterator indexReader;
  private SortedKeyValueIterator<Key, Value> rowReader;
  private Key tk;
  private Value tv;
  private Expression query;

  private final EvaluatingVisitor evaluator = new EvaluatingVisitor();

  // stores the whole column qualifier when parsing out a column name
  private final Text colqBuffer = new Text();

  // stores the document ID when attempting to parse the column name from a CQ
  private final Text columnNameBuffer = new Text();

  // used when we're reading all the "D" column entries for a document
  private final Text docIDCompareBuffer = new Text();

  // by default we hash big column values. users can toggle this
  private boolean hashLargeValues = true;

  // used to include the columns visibility alongside the value
  private boolean includeVisibility = false;

  @Override
  public void init(
      SortedKeyValueIterator<Key, Value> source,
      Map<String, String> options,
      IteratorEnvironment env) {
    this.query = Expressions.GSON.fromJson(options.get("query"), Expression.class);
    indexReader = new ExpressionToIteratorTree(source, env).handle(this.query);
    if (indexReader == null) {
      log.error("Could not instantiate iterator tree for query {}", options.get("query"));
    }
    rowReader = source;

    hashLargeValues =
        !options.containsKey(CONF_HASH_LARGE_VALUES) || Boolean.parseBoolean(
            options.get(CONF_HASH_LARGE_VALUES));

    includeVisibility =
        options.containsKey(CONF_INCLUDE_VISIBILITY_WITH_VALUE) && Boolean.parseBoolean(
            options.get(CONF_INCLUDE_VISIBILITY_WITH_VALUE));
  }

  @Override
  public boolean hasTop() {
    return tk != null;
  }

  @Override
  public void next() throws IOException {
    tk = null;
    tv = null;

    while (tk == null && indexReader != null && indexReader.hasTop()) {
      this.tk = null;
      Key nextDoc = indexReader.getTopKey();
      Multimap<String, String> rowToEvaluate = indexReader.indexValue();
      indexReader.next();

      Text cq = nextDoc.getColumnQualifier();
      Key tk = new Key(nextDoc.getRow(), nextDoc.getColumnFamily(), cq);
      Range docRange = new Range(nextDoc, null);
      rowReader.seek(docRange, DOC_COLF, true);

      Map<String, String> rowToReturn = new HashMap<>();

      while (rowReader.hasTop()
          && startsWith(
              rowReader.getTopKey().getColumnQualifier(docIDCompareBuffer).getBytes(),
              cq.getBytes())) {
        String columnName =
            getColName(rowReader.getTopKey(), colqBuffer, columnNameBuffer).toString();
        String columnValue =
            hashLargeValues
                ? transformValue(rowReader.getTopValue()).toString()
                : rowReader.getTopValue().toString();
        rowToEvaluate.put(columnName, columnValue);
        rowToReturn.put(
            columnName,
            includeVisibility
                ? colValueWithVis(
                    columnValue, rowReader.getTopKey().getColumnVisibility().toString())
                : columnValue);
        rowReader.next();
      }
      evaluator.reset();

      evaluator.setRecord(rowToEvaluate);
      this.query.accept(evaluator);
      if (evaluator.getLastResult()) {
        this.tk = tk;
        tv = new Value(mapper.writeValueAsBytes(rowToReturn));
      }
    }
  }

  boolean startsWith(byte[] a, byte[] b) {
    for (int i = 0; i < b.length; ++i) {
      int diff = a[i] - b[i];
      switch (diff) {
        case 0:
          continue;
        default:
          return false;
      }
    }
    return true;
  }

  @Override
  public void seek(Range range, Collection<ByteSequence> columnFamilies, boolean inclusive)
      throws IOException {
    indexReader.seek(range, columnFamilies, inclusive);
    next();
  }

  @Override
  public Key getTopKey() {
    return tk;
  }

  @Override
  public Value getTopValue() {
    return tv;
  }

  @Override
  public SortedKeyValueIterator<Key, Value> deepCopy(IteratorEnvironment env) {
    return null;
  }

  public static Text getColName(Key key, Text colq, Text colName) {
    key.getColumnQualifier(colq);
    colName.set(
        colq.getBytes(),
        Const.LONG_LEX_LEN + Common.DELIMITER.getLength(),
        colq.getLength() - Const.LONG_LEX_LEN - Common.DELIMITER.getLength());
    return colName;
  }

  public static Value transformValue(Value v) {
    if (v.getSize() < 1024) {
      return v;
    } else {
      return new Value(v.get(), 0, 1024);
    }
  }

  public static String colValueWithVis(String columnValue, String columnVisibility) {
    return "{val: " + columnValue + ", vis: " + columnVisibility + "}";
  }
}
