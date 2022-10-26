package com.dataprofiler.iterators.rowData;

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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.dataprofiler.util.Const;
import com.dataprofiler.util.TextEncoding;
import java.io.IOException;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.accumulo.core.client.lexicoder.LongLexicoder;
import org.apache.accumulo.core.data.ArrayByteSequence;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.PartialKey;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.hadoop.io.Text;
import org.apache.htrace.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.htrace.fasterxml.jackson.databind.ObjectMapper;

public class DatawaveRowAndIter implements SortedKeyValueIterator<Key, Value> {

  public static final String COLUMN_NAMES_OPTION_NAME = "columnNames";
  public static final String COLUMN_VALUES_OPTION_NAME = "columnValues";
  public static final String NOT_FLAG_OPTION_NAME = "notFlag";
  /** Probably stuff that can be defined in UTIL */
  private static final LongLexicoder longLex = new LongLexicoder();

  private static final String DEFAULT_INDEX_COLF = "I";
  private static final String DEFAULT_DOC_COLF = "D";
  private static final byte[] zero_lexicoded = new LongLexicoder().encode(0L);
  private static final Text LOW_BYTE = new Text("\u0000");
  private static final Text DELIMITER = LOW_BYTE;
  private static final Text HIGH_BYTE = new Text("\uffff");

  protected static ObjectMapper mapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  /** The key to return (RowID, ColF, ColQ) */
  private Key topKey = null;

  /** The value to return `{"col_0":"val_0", "col_1":"val_1", ..., "col_n":"val_n"}` */
  private Value topValue = new Value(new byte[0]);

  /** The source iterator that moves throughout the table partition by partition */
  private SortedKeyValueIterator<Key, Value> docSource;

  /** The Document Column Family: `DEFAULT_DOC_COLF` */
  private Set<ByteSequence> docColF;

  /** The partition (RowID) that is currently being evaluated */
  private Text currentPartition = null;

  /** The Row UID (column index) that is being evaluated */
  private final Text currentRowUID = new Text(zero_lexicoded);

  /** The array of column values to evaluate */
  private ColumnValueSource[] sources;

  /** The number of column values to evaluate */
  private int sourcesCount = 0;

  /** The range of the scan */
  private Range overallRange;

  private SortedKeyValueIterator<Key, Value> source;

  public DatawaveRowAndIter() {}

  private DatawaveRowAndIter(DatawaveRowAndIter other, IteratorEnvironment env) {

    // Copy the source iterator
    if (other.docSource != null) {
      docSource = other.docSource.deepCopy(env);
    }

    // Copy all of the sources
    if (other.sources != null) {
      sourcesCount = other.sourcesCount;
      sources = new ColumnValueSource[sourcesCount];
      for (int i = 0; i < sourcesCount; i++) {
        sources[i] =
            new ColumnValueSource(
                other.sources[i].iter.deepCopy(env),
                other.sources[i].colName,
                other.sources[i].colVal);
      }
    }
  }

  /**
   * Serialize an array of booleans into a base 64 encoded string
   *
   * @param values An array of booleans
   * @return A base 64 encoded string
   */
  public static String encodeBooleans(boolean[] values) {
    byte[] bytes = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      if (values[i]) {
        bytes[i] = 1;
      } else {
        bytes[i] = 0;
      }
    }
    return Base64.getEncoder().encodeToString(bytes);
  }

  /**
   * Deserialize a base 64 encoded string to an array of booleans
   *
   * @param values A base 64 encoded string
   * @return An array of booleans
   */
  public static boolean[] decodeBooleans(String values) {
    if (values == null) {
      return null;
    }
    byte[] bytes = Base64.getDecoder().decode(values.getBytes(UTF_8));
    boolean[] bFlags = new boolean[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      bFlags[i] = bytes[i] == 1;
    }
    return bFlags;
  }

  public void init(
      SortedKeyValueIterator<Key, Value> source,
      Map<String, String> options,
      IteratorEnvironment env)
      throws IOException {

    // Get options
    Text[] colNames = TextEncoding.decodeStrings(options.get(COLUMN_NAMES_OPTION_NAME));
    Text[] colValues = TextEncoding.decodeStrings(options.get(COLUMN_VALUES_OPTION_NAME));
    boolean[] notFlag = decodeBooleans(options.get(NOT_FLAG_OPTION_NAME));

    if (colNames.length < 1 && (colNames.length != colValues.length)) {
      throw new IllegalArgumentException(
          "AndIDatawaveRowAndIterter requires one or more columns families");
    }

    // Set all the flags to false if not flags is not specified
    if (notFlag == null) {
      notFlag = new boolean[colNames.length];
      for (int i = 0; i < colNames.length; i++) {
        notFlag[i] = false;
      }
    }

    // Scan the not flags.
    // There must be at least one colVal that isn't negated
    // And we are going to re-order such that the first colVal is not a ! colVal
    if (notFlag[0]) {
      for (int i = 1; i < notFlag.length; i++) {
        if (!notFlag[i]) {
          Text swapNames = new Text(colNames[0]);
          colNames[0].set(colNames[i]);
          colNames[i].set(swapNames);

          Text swapValues = new Text(colValues[0]);
          colValues[0].set(colValues[i]);
          colValues[i].set(swapValues);

          notFlag[0] = false;
          notFlag[i] = true;
          break;
        }
      }
      if (notFlag[0]) {
        throw new IllegalArgumentException(
            "DatawaveRowAndIter requires at lest one column family without not");
      }
    }

    // TODO changed so testing would work
    //    docSource = source;
    docSource = source.deepCopy(env);

    sources = new ColumnValueSource[colValues.length];
    sources[0] = new ColumnValueSource(source, colNames[0], colValues[0]);
    for (int i = 1; i < colValues.length; i++) {
      sources[i] =
          new ColumnValueSource(source.deepCopy(env), colNames[i], colValues[i], notFlag[i]);
    }
    sourcesCount = colValues.length;
  }

  public boolean hasTop() {
    return currentPartition != null;
  }

  public void next() throws IOException {
    if (currentPartition == null) {
      return;
    }
    // precondition: the current row is set up and the sources all have the same column qualifier
    // while we don't have a match, seek in the source with the smallest column qualifier
    sources[0].iter.next();
    advanceToIntersection();
  }

  public void seek(Range range, Collection<ByteSequence> collection, boolean b) throws IOException {
    overallRange = new Range(range);
    currentPartition = new Text();
    currentRowUID.set(zero_lexicoded);

    // Seek each of the sources to the right column family within the row given by key
    for (int i = 0; i < sourcesCount; i++) {
      Key sourceKey;
      if (range.getStartKey() != null) {

        if (range.getStartKey().getColumnQualifier() != null) {
          // Build a column qualifier
          Text sourceColQ = new Text(sources[i].colVal);

          Text rangeColQ = range.getStartKey().getColumnQualifier();
          int firstDelimIdx = rangeColQ.find(DELIMITER.toString());

          if (firstDelimIdx < 0) {
            sourceKey = buildKey(getPartition(range.getStartKey()), sources[i].colFam, sourceColQ);
          } else {
            int secondDelimIdx = rangeColQ.find(DELIMITER.toString(), firstDelimIdx + 1);

            sourceColQ.append(DELIMITER.getBytes(), 0, DELIMITER.getLength());
            sourceColQ.append(
                rangeColQ.getBytes(),
                firstDelimIdx + DELIMITER.getLength(),
                secondDelimIdx - firstDelimIdx - DELIMITER.getLength());

            sourceColQ.append(DELIMITER.getBytes(), 0, DELIMITER.getLength());
            sourceColQ.append(
                rangeColQ.getBytes(),
                secondDelimIdx + DELIMITER.getLength(),
                rangeColQ.getLength() - secondDelimIdx - DELIMITER.getLength());

            sourceKey = buildKey(getPartition(range.getStartKey()), sources[i].colFam, sourceColQ);
          }
        } else {
          sourceKey = buildKey(getPartition(range.getStartKey()), sources[i].colFam, DELIMITER);
        }
        // Seek only to the colVal for this source as a column family
        sources[i].iter.seek(new Range(sourceKey, true, null, false), sources[i].seekColfams, true);
      } else {
        // Seek only to the colVal for this source as a column family
        sources[i].iter.seek(range, sources[i].seekColfams, true);
      }
    }
    advanceToIntersection();
  }

  private void advanceToIntersection() throws IOException {
    boolean cursorChanged = true;
    while (cursorChanged) {
      // seek all of the sources to at least the highest seen column qualifier in the current row
      cursorChanged = false;
      for (int i = 0; i < sourcesCount; i++) {
        if (currentPartition == null) {
          topKey = null;
          return;
        }
        if (seekOneSource(i)) {
          cursorChanged = true;
          break;
        }
      }
    }

    // Build a top key with a 0x00 appended to the end to move the cursor forward
    Key sourceTop = sources[0].iter.getTopKey();
    Text qual = sourceTop.getColumnQualifier();
    qual.append(LOW_BYTE.getBytes(), 0, LOW_BYTE.getBytes().length);

    topKey = buildKey(sourceTop.getRow(), sourceTop.getColumnFamily(), qual);

    // Build a key to seek to the document
    Key docKey = buildDocKey();
    docSource.seek(new Range(docKey, true, null, false), docColF, true);

    //    if (docSource.hasTop() && docKey.equals(docSource.getTopKey(),
    // PartialKey.ROW_COLFAM_COLQUAL)) {
    //      topValue = docSource.getTopValue();
    //    }

    Map<String, String> output = new HashMap<>();
    while (docSource.hasTop()
        && docKey.equals(docSource.getTopKey(), PartialKey.ROW_COLFAM)
        && getRowID(docSource.getTopKey()).equals(currentRowUID)) {
      output.put(getColName(docSource.getTopKey()).toString(), docSource.getTopValue().toString());
      docSource.next();
    }

    topValue = new Value(mapper.writeValueAsBytes(output));
  }

  private Key buildDocKey() {
    Text colf = new Text(DEFAULT_DOC_COLF);
    docColF = Collections.singleton(new ArrayByteSequence(colf.getBytes(), 0, colf.getLength()));

    Text colq = new Text();
    colq.set(currentRowUID.getBytes(), 0, currentRowUID.getLength());
    return new Key(currentPartition, colf, colq);
  }

  private boolean seekOneSource(int sourceID) throws IOException {

    boolean advancedCursor = false;

    if (sources[sourceID].notFlag) {
      while (true) {

        // An empty column is a valid condition
        if (!sources[sourceID].iter.hasTop()) {
          break;
        }

        // Check if the source is past the end of the range
        if (overallRange.getEndKey() != null) {
          int endCompare =
              overallRange
                  .getEndKey()
                  .getRow()
                  .compareTo(sources[sourceID].iter.getTopKey().getRow());
          if ((!overallRange.isEndKeyInclusive() && endCompare <= 0) || endCompare < 0) {
            break;
          }
        }

        // Determine if the source is in the correct partition
        int partitionCompare =
            currentPartition.compareTo(getPartition(sources[sourceID].iter.getTopKey()));

        // If the source is before the current partition, seek to the correct RowID and ColF
        if (partitionCompare > 0) {
          // seek to at least the currentRow
          Key seekKey =
              buildKey(currentPartition, sources[sourceID].colFam, sources[sourceID].colVal);
          sources[sourceID].iter.seek(
              new Range(seekKey, true, null, false), sources[sourceID].seekColfams, true);
          continue;
        } else if (partitionCompare < 0) {
          break;
        }

        // Ensure the source is at the correct ColVal (ColQ)
        if (sources[sourceID].colVal != null) {

          int colValCompare =
              sources[sourceID].colVal.compareTo(getColValue(sources[sourceID].iter.getTopKey()));

          // If the source is before the current value seek to the required value
          if (colValCompare > 0) {
            Key seekKey =
                buildKey(
                    currentPartition,
                    sources[sourceID].colFam,
                    sources[sourceID].colVal,
                    currentRowUID);
            sources[sourceID].iter.seek(
                new Range(seekKey, true, null, false), sources[sourceID].seekColfams, true);
            continue;
          } else if (colValCompare < 0) {
            break;
          }
        }

        // Ensure the source is at the correct RowUID (UID for the row in the relational table)
        Text rowUID = getRowID(sources[sourceID].iter.getTopKey());
        int rowUIDCompare = currentRowUID.compareTo(rowUID);

        // If the source's row UID is before the current row UID, seek to the row UID
        if (rowUIDCompare > 0) {
          Key seekKey =
              buildKey(
                  currentPartition,
                  sources[sourceID].colFam,
                  sources[sourceID].colVal,
                  currentRowUID);
          sources[sourceID].iter.seek(
              new Range(seekKey, true, null, false), sources[sourceID].seekColfams, true);
          continue;
        } else if (rowUIDCompare < 0) {
          break;
        }

        // Force the entire process to go to the next row because this is an invalid result
        sources[0].iter.next();
        advancedCursor = true;
        break;
      }
    } else {
      while (true) {

        // Advance the cursor if the source has no more keys
        if (!sources[sourceID].iter.hasTop()) {
          currentPartition = null;
          return true;
        }

        int endCompare = -1;

        // Check if the source is past the end of the range
        if (overallRange.getEndKey() != null) {
          endCompare =
              overallRange
                  .getEndKey()
                  .getRow()
                  .compareTo(sources[sourceID].iter.getTopKey().getRow());

          // Advance the cursor if the source is past the end of the range
          if ((!overallRange.isEndKeyInclusive() && endCompare <= 0) || endCompare < 0) {
            currentPartition = null;
            return true;
          }
        }

        // Ensure the source is in the proper partition (RowID) and correct index (ColF)
        int partitionCompare =
            currentPartition.compareTo(getPartition(sources[sourceID].iter.getTopKey()));

        // If the compare is positive, the current partition is past the source's partition
        // Seek to the correct RowID and ColF
        if (partitionCompare > 0) {
          Key seekKey =
              buildKey(currentPartition, sources[sourceID].colFam, sources[sourceID].colVal);
          sources[sourceID].iter.seek(
              new Range(seekKey, true, null, false), sources[sourceID].seekColfams, true);
          continue;
        }

        // If the compare is negative, the source's partition is past the current partition
        // Advance the cursor
        if (partitionCompare < 0) {
          currentPartition.set(getPartition(sources[sourceID].iter.getTopKey()));
          currentRowUID.set(zero_lexicoded);
          advancedCursor = true;
          continue;
        }

        // Ensure the source is at the correct ColVal (ColQ)
        if (sources[sourceID].colVal != null) {

          int colValCompare =
              sources[sourceID].colVal.compareTo(getColValue(sources[sourceID].iter.getTopKey()));

          // If the required value is before the source's current value, seek to the required value
          if (colValCompare > 0) {
            // TODO check to see if CurrentRowUID is needed
            //            Key seekKey = buildKey(currentPartition, sources[sourceID].colFam,
            //                sources[sourceID].colVal,
            //                currentRowUID);
            Key seekKey =
                buildKey(currentPartition, sources[sourceID].colFam, sources[sourceID].colVal);
            sources[sourceID].iter.seek(
                new Range(seekKey, true, null, false), sources[sourceID].seekColfams, true);
            continue;
          }

          // If the source's value is beyond the required value advance the iterator
          if (colValCompare < 0) {

            // If the source is at the end of the range, advance the cursor
            if (endCompare == 0) {
              currentPartition = null;
              return true;
            }

            // Seek to the next partition
            Key seekKey = buildFollowingPartitionKey(sources[sourceID].iter.getTopKey());
            sources[sourceID].iter.seek(
                new Range(seekKey, true, null, false), sources[sourceID].seekColfams, true);
            continue;
          }
        }

        // Ensure the source is at the correct RowUID (UID for the row in the relational table)
        Text rowUID = getRowID(sources[sourceID].iter.getTopKey());

        int rowUIDCompare = currentRowUID.compareTo(rowUID);

        // If the source's row UID is beyond the current row UID, advance the cursor
        if (rowUIDCompare < 0) {
          currentRowUID.set(rowUID);
          advancedCursor = true;
          break;
        }

        // If the source's row UID is before the current row UID, seek to the row UID
        if (rowUIDCompare > 0) {
          Key seekKey =
              buildKey(
                  currentPartition,
                  sources[sourceID].colFam,
                  sources[sourceID].colVal,
                  currentRowUID);
          sources[sourceID].iter.seek(
              new Range(seekKey, true, null, false), sources[sourceID].seekColfams, true);
          continue;
        }

        // The source is at the current partition, index column, and column value
        break;
      }
    }
    return advancedCursor;
  }

  private Key buildFollowingPartitionKey(Key key) {
    return key.followingKey(PartialKey.ROW);
  }

  public static Text getRowID(Key key) {

    if (key.getColumnFamily().equals(new Text(DEFAULT_DOC_COLF))) {
      Text colq = key.getColumnQualifier();
      Text docID = new Text();

      docID.set(colq.getBytes(), 0, Const.LONG_LEX_LEN);
      return docID;
    }

    Text colq = key.getColumnQualifier();
    int delimiterLen = DELIMITER.getLength();

    int firstDelimIdx = colq.find(DELIMITER.toString());
    if (firstDelimIdx < 0) {
      throw new IllegalArgumentException("bad docid: " + key);
    }

    Text docID = new Text();
    try {
      docID.set(colq.getBytes(), firstDelimIdx + delimiterLen, Const.LONG_LEX_LEN);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException(
          "bad indices for docid: "
              + key
              + " "
              + firstDelimIdx
              + " "
              + firstDelimIdx
              + Const.LONG_LEX_LEN);
    }
    return docID;
  }

  public Key getTopKey() {
    return topKey;
  }

  public Value getTopValue() {
    return topValue;
  }

  public SortedKeyValueIterator<Key, Value> deepCopy(IteratorEnvironment iteratorEnvironment) {
    // TODO this might be wrong
    return new DatawaveRowAndIter(this, iteratorEnvironment);
    //
    //    AndIter newInstance;
    //    try {
    //      newInstance = this.getClass().newInstance();
    //    } catch (Exception e) {
    //      throw new RuntimeException(e);
    //    }
    //
    //    newInstance.source = source.deepCopy(iteratorEnvironment);
    //
    //    return newInstance;
  }

  private Text getPartition(Key key) {
    return key.getRow();
  }

  public static Text getColValue(Key key) {
    Text colq = key.getColumnQualifier();
    int zeroIndex = colq.find(DELIMITER.toString());
    Text colValue = new Text();
    colValue.set(colq.getBytes(), 0, zeroIndex);
    return colValue;
  }

  public static Text getColName(Key key) {

    if (key.getColumnFamily().equals(new Text(DEFAULT_DOC_COLF))) {
      Text colq = key.getColumnQualifier();
      Text colName = new Text();
      colName.set(
          colq.getBytes(),
          Const.LONG_LEX_LEN + DELIMITER.getLength(),
          colq.getBytes().length - Const.LONG_LEX_LEN - DELIMITER.getLength());
      return colName;
    }

    Text colf = key.getColumnFamily();
    int zeroIndex = colf.find(DELIMITER.toString());
    Text colName = new Text();
    colName.set(colf.getBytes(), zeroIndex + DELIMITER.getLength(), colf.getBytes().length);
    return colName;
  }

  private Key buildKey(Text partition, Text colFam, Text colValue, Text docID) {
    Text colq = new Text(colValue);
    colq.append(DELIMITER.getBytes(), 0, DELIMITER.getLength());
    colq.append(docID.getBytes(), 0, docID.getLength());
    colq.append(DELIMITER.getBytes(), 0, DELIMITER.getLength());
    return new Key(partition, colFam, colq);
  }

  private Key buildKey(Text partition, Text colFam, Text colVal) {
    Text colq = new Text(colVal);
    return new Key(partition, colFam, colq);
  }

  private Key buildKey(Text partition, Text colName) {
    return new Key(partition, colName);
  }

  public static class ColumnValueSource {

    SortedKeyValueIterator<Key, Value> iter;
    Text colName;
    Text colVal;
    Text colFam;
    Collection<ByteSequence> seekColfams;
    boolean notFlag;

    public ColumnValueSource(ColumnValueSource other) {
      this.iter = other.iter;
      this.colName = other.colName;
      this.colVal = other.colVal;
      this.colFam = other.colFam;
      this.seekColfams = other.seekColfams;
      this.notFlag = other.notFlag;
    }

    ColumnValueSource(SortedKeyValueIterator<Key, Value> iter, Text colName, Text colVal) {
      this(iter, colName, colVal, false);
    }

    ColumnValueSource(
        SortedKeyValueIterator<Key, Value> iter, Text colName, Text colVal, boolean notFlag) {
      this.iter = iter;
      this.colName = colName;
      this.colVal = colVal;

      // Create the indexed colFam
      this.colFam = new Text(DEFAULT_INDEX_COLF);
      colFam.append(DELIMITER.getBytes(), 0, DELIMITER.getLength());
      colFam.append(colName.getBytes(), 0, colName.getLength());

      // The desired column families for this source is the colVal itself
      this.seekColfams =
          Collections.singletonList(
              new ArrayByteSequence(colFam.getBytes(), 0, colFam.getLength()));
      this.notFlag = notFlag;
    }

    public String getTermString() {
      return (this.colVal == null) ? "Iterator" : this.colVal.toString();
    }
  }
}
