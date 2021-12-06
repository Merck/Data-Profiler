package com.dataprofiler.util.objects;

/*-
 * 
 * dataprofiler-util
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
import com.dataprofiler.util.Const.SortOrder;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.iterators.ClosableIterator;
import com.dataprofiler.util.iterators.ColumnCountVisibilityIterator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.lexicoder.LongLexicoder;
import org.apache.accumulo.core.client.lexicoder.ReverseLexicoder;
import org.apache.accumulo.core.client.lexicoder.StringLexicoder;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.user.RegExFilter;
import org.apache.hadoop.io.Text;

public class ColumnCountObject extends VersionedPurgableDatasetObject<ColumnCountObject> {

  private static final Text DELIMITER = new Text(Const.DELIMITER);
  private static final int DELIMITER_LEN = DELIMITER.getLength();

  // Lexicoders
  private static final StringLexicoder stringLex = new StringLexicoder();
  private static final ReverseLexicoder reverseStringLex = new ReverseLexicoder<>(stringLex);
  private static final LongLexicoder longLex = new LongLexicoder();
  private static final ReverseLexicoder reverseLongLex = new ReverseLexicoder<>(longLex);

  private static final String accumuloTable = Const.ACCUMULO_COLUMN_COUNTS_TABLE_ENV_KEY;

  // Cell Level Visibility Iterator Setting
  private static final IteratorSetting visibilityIteratorSetting =
      new IteratorSetting(21, "colCntVisItr", ColumnCountVisibilityIterator.class);

  public String value;
  public Long count;
  public String dataset;
  public String tableId;
  public String column;
  public SortOrder sortOrder;

  public ColumnCountObject(
      String dataset, String tableId, String column, String value, Long count, String visibility) {
    this(dataset, tableId, column, value, count);
    this.setVisibility(visibility);
  }

  public ColumnCountObject(
      String dataset, String tableId, String column, String value, Long count) {
    this();
    this.dataset = dataset;
    this.tableId = tableId;
    this.column = column;
    this.value = value;
    this.count = count;
  }

  public ColumnCountObject() {
    super(accumuloTable);
  }

  protected ColumnCountObject(String accumuloTable) {
    super(accumuloTable);
  }

  /**
   * Combine the counts of similar values stored in the same column. E.g. a column containing
   * ("foo",1) and ("foo ",2) should have value ("foo",3) but this might not be the case. This
   * method is mainly used in indexing to combine the counts of similar values.
   *
   * @param c1 A ColumnCountObject
   * @param c2 A ColumnCountObject
   * @return A ColumnCountObject with the count's summed
   * @throws BasicAccumuloException An exception is thrown if the objects being merged are not from
   *     the same dataset, table, and column
   */
  public static ColumnCountObject combineColumns(ColumnCountObject c1, ColumnCountObject c2)
      throws BasicAccumuloException {
    ColumnCountObject result = new ColumnCountObject();

    if (c1.getDataset().equals(c2.getDataset())) {
      result.setDataset(c1.getDataset());
    } else {
      throw new BasicAccumuloException(
          "Dataset names do not match: " + c1.getDataset() + " != " + c2.getDataset());
    }

    if (c1.getTableId().equals(c2.getTableId())) {
      result.setTableId(c1.getTableId());
    } else {
      throw new BasicAccumuloException(
          String.format(
              "Table ids do not match for dataset '%s': %s != %s",
              c1.getDataset(), c1.getTableId(), c2.getTableId()));
    }

    if (c1.getColumn().equals(c2.getColumn())) {
      result.setColumn(c1.getColumn());
    } else {
      throw new BasicAccumuloException(
          String.format(
              "Column names do not match for dataset '%s' table '%s': %s != %s",
              c1.getDataset(), c1.getTableId(), c1.getColumn(), c2.getColumn()));
    }

    if (c1.getValue().trim().equals(c2.getValue().trim())) {
      result.setValue(c1.getValue().trim());
    } else {
      throw new BasicAccumuloException(
          String.format(
              "Values do not match for dataset '%s' table '%s' column '%s': %s != %s\" ",
              c1.getDataset(), c1.getTableId(), c1.getColumn(), c1.getValue(), c2.getValue()));
    }

    if (c1.sortOrder == c2.sortOrder) {
      result.sortOrder = c1.sortOrder;
    } else {
      throw new BasicAccumuloException(
          String.format(
              "Sort orders do not match for dataset '%s' table '%s' column '%s' value '%s': %s != %s",
              c1.getDataset(),
              c1.getTableId(),
              c1.getColumn(),
              c1.getValue(),
              c1.sortOrder,
              c2.sortOrder));
    }

    if (c1.getVisibility().equals(c2.getVisibility())) {
      result.setVisibility(c1.getVisibility());
    } else {
      throw new BasicAccumuloException(
          String.format(
              "Visibilities do not match for dataset '%s' table '%s' column '%s' value '%s': %s != %s",
              c1.getDataset(),
              c1.getTableId(),
              c1.getColumn(),
              c1.getValue(),
              c1.getVisibility(),
              c2.getVisibility()));
    }

    result.setCount(c1.getCount() + c2.getCount());

    return result;
  }

  /**
   * Build a key for the loader
   *
   * @param rowId The rowID for a ColumnCountObject
   * @return dataset\x00table\x00column\x00sortOrder
   */
  public static String buildKey(Text rowId) {

    StringBuilder sb = new StringBuilder();

    // Get the dataset
    int firstDelimIdx = rowId.find(DELIMITER.toString());
    if (firstDelimIdx < 0) {
      throw new InvalidDataFormat("Key with wrong number of fields");
    }
    sb.append(new String(Arrays.copyOfRange(rowId.getBytes(), 0, firstDelimIdx)))
        .append(DELIMITER);

    // Get the table id
    int secondDelimIdx = rowId.find(DELIMITER.toString(), firstDelimIdx + 1);
    if (secondDelimIdx < 0) {
      throw new InvalidDataFormat("Key with wrong number of fields");
    }
    sb.append(
            new String(
                Arrays.copyOfRange(
                    rowId.getBytes(), firstDelimIdx + DELIMITER_LEN, secondDelimIdx)))
        .append(DELIMITER);

    // Get column
    int thirdDelimIdx = rowId.find(DELIMITER.toString(), secondDelimIdx + 1);
    if (thirdDelimIdx < 0) {
      throw new InvalidDataFormat("Key with wrong number of fields");
    }
    sb.append(
            new String(
                Arrays.copyOfRange(
                    rowId.getBytes(), secondDelimIdx + DELIMITER_LEN, thirdDelimIdx)))
        .append(DELIMITER);

    // Get sort method
    int fourthDelimIdx = rowId.find(DELIMITER.toString(), thirdDelimIdx + 1);
    if (fourthDelimIdx < 0) {
      throw new InvalidDataFormat("Key with wrong number of fields");
    }

    sb.append(
        new String(
            Arrays.copyOfRange(rowId.getBytes(), thirdDelimIdx + DELIMITER_LEN, fourthDelimIdx)));

    return sb.toString();
  }

  @Override
  public ObjectScannerIterable<ColumnCountObject> scan(Context context) {
    return super.scan(context).fetchColumnFamily(Const.COL_FAM_DATA);
  }

  /**
   * Keys for the Count Objects are:
   *
   * <p>RowId: dataset DELIM tableId DELIM column DELIM value DELIM sortType DELIM value|count
   *
   * <p>Col Fam: D
   *
   * <p>Col Qual: {"value":value,"count":count}
   */
  @Override
  @JsonIgnore
  public Key createAccumuloKey() throws InvalidDataFormat {
    if (sortOrder.equals(SortOrder.CNT_ASC)) {
      return createAccumuloKeyCountAscending();
    } else if (sortOrder.equals(SortOrder.CNT_DESC)) {
      return createAccumuloKeyCountDescending();
    } else if (sortOrder.equals(SortOrder.VAL_ASC)) {
      return createAccumuloKeyValueAscending();
    } else if (sortOrder.equals(SortOrder.VAL_DESC)) {
      return createAccumuloKeyValueDescending();
    } else {
      throw new InvalidDataFormat("Unknown sort type");
    }
  }

  @JsonIgnore
  public Key createAccumuloKeyCountAscending() {
    byte[] encCnt = longLex.encode(count);
    byte[] encVal = stringLex.encode(value.toLowerCase());
    byte[] rowID =
        joinKeyComponents(
            dataset.getBytes(),
            tableId.getBytes(),
            column.getBytes(),
            SortOrder.CNT_ASC.GetCode().getBytes(),
            encCnt,
            encVal);

    return buildAccumuloKey(rowID);
  }

  @JsonIgnore
  public Key createAccumuloKeyCountDescending() {
    byte[] encCnt = reverseLongLex.encode(count);
    byte[] encVal = reverseStringLex.encode(value.toLowerCase());
    byte[] rowID =
        joinKeyComponents(
            dataset.getBytes(),
            tableId.getBytes(),
            column.getBytes(),
            SortOrder.CNT_DESC.GetCode().getBytes(),
            encCnt,
            encVal);

    return buildAccumuloKey(rowID);
  }

  @JsonIgnore
  public Key createAccumuloKeyValueAscending() {
    byte[] encVal = stringLex.encode(value.toLowerCase());
    byte[] rowID =
        joinKeyComponents(
            dataset.getBytes(),
            tableId.getBytes(),
            column.getBytes(),
            SortOrder.VAL_ASC.GetCode().getBytes(),
            encVal);

    return buildAccumuloKey(rowID);
  }

  @JsonIgnore
  public Key createAccumuloKeyValueDescending() {
    byte[] encVal = reverseStringLex.encode(value.toLowerCase());
    byte[] rowID =
        joinKeyComponents(
            dataset.getBytes(),
            tableId.getBytes(),
            column.getBytes(),
            SortOrder.VAL_DESC.GetCode().getBytes(),
            encVal);

    return buildAccumuloKey(rowID);
  }

  @JsonIgnore
  private Key buildAccumuloKey(byte[] rowID) {
    ValueCountObject valCnt = new ValueCountObject(value, count);

    try {
      return new Key(
          new Text(rowID),
          new Text(Const.COL_FAM_DATA),
          new Text(mapper.writeValueAsBytes(valCnt)),
          new Text(visibility));
    } catch (Exception e) {
      throw new InvalidDataFormat("Unable to convert value/cnt as JSON object");
    }
  }

  /** Value is just the count */
  @Override
  @JsonIgnore
  public Value createAccumuloValue() throws InvalidDataFormat {
    return new Value(Long.toString(count).getBytes());
  }

  @Override
  public ColumnCountObject fromEntry(Entry<Key, Value> entry) {
    ColumnCountObject c = new ColumnCountObject();
    Key key = entry.getKey();

    Text rowId = key.getRow();

    // Get the dataset
    int firstDelimIdx = rowId.find(DELIMITER.toString());
    if (firstDelimIdx < 0) {
      throw new InvalidDataFormat("Key with wrong number of fields");
    }
    c.dataset = new String(Arrays.copyOfRange(rowId.getBytes(), 0, firstDelimIdx));

    // Get the tableId
    int secondDelimIdx = rowId.find(DELIMITER.toString(), firstDelimIdx + 1);
    if (secondDelimIdx < 0) {
      throw new InvalidDataFormat("Key with wrong number of fields");
    }
    c.tableId =
        new String(
            Arrays.copyOfRange(rowId.getBytes(), firstDelimIdx + DELIMITER_LEN, secondDelimIdx));

    // Get column
    int thirdDelimIdx = rowId.find(DELIMITER.toString(), secondDelimIdx + 1);
    if (thirdDelimIdx < 0) {
      throw new InvalidDataFormat("Key with wrong number of fields");
    }
    c.column =
        new String(
            Arrays.copyOfRange(rowId.getBytes(), secondDelimIdx + DELIMITER_LEN, thirdDelimIdx));

    // Get sort method
    int fourthDelimIdx = rowId.find(DELIMITER.toString(), thirdDelimIdx + 1);
    if (fourthDelimIdx < 0) {
      throw new InvalidDataFormat("Key with wrong number of fields");
    }
    String sortType =
        new String(
            Arrays.copyOfRange(rowId.getBytes(), thirdDelimIdx + DELIMITER_LEN, fourthDelimIdx));
    c.sortOrder = SortOrder.getEnum(sortType);

    // Get the value and count
    try {
      ValueCountObject valCnt =
          mapper.readValue(key.getColumnQualifier().getBytes(), ValueCountObject.class);
      c.value = valCnt.getValue();
      c.count = valCnt.getCount();
    } catch (Exception ex) {
      throw new InvalidDataFormat(
          "Unable to convert value/cnt as JSON object: "
              + key.getColumnQualifier().toString()
              + " "
              + ex);
    }

    c.updatePropertiesFromEntry(entry);

    return c;
  }

  public ObjectScannerIterable<ColumnCountObject> find(Context context, Key startKey, Key endKey) {
    return find(context, new Range(startKey, endKey));
  }

  public ObjectScannerIterable<ColumnCountObject> find(Context context, Range range) {
    return scan(context)
        .fetchColumnFamily(Const.COL_FAM_DATA)
        .addRange(range)
        .addScanIterator(visibilityIteratorSetting);
  }

  public ObjectScannerIterable<ColumnCountObject> findMatchingValues(
      Context context, VersionedMetadataObject colMetadata, String value) {
    return scan(context)
        .addRange(
            createInclusiveRange(
                colMetadata.dataset_name,
                colMetadata.table_id,
                colMetadata.column_name,
                SortOrder.VAL_ASC.GetCode(),
                value.toLowerCase()))
        .fetchColumnFamily(Const.COL_FAM_DATA)
        .addScanIterator(visibilityIteratorSetting);
  }

  public ObjectScannerIterable<ColumnCountObject> fetchColumn(
      Context context, VersionedMetadataObject colMetadata, SortOrder sortOrder) {

    ObjectScannerIterable<ColumnCountObject> scanner = scan(context);

    if (sortOrder == null) {
      scanner.addRange(
          createInclusiveRange(
              colMetadata.dataset_name, colMetadata.table_id, colMetadata.column_name));
    } else {
      scanner.addRange(
          createInclusiveRange(
              colMetadata.dataset_name,
              colMetadata.table_id,
              colMetadata.column_name,
              sortOrder.GetCode()));
    }

    return scanner.fetchColumnFamily(Const.COL_FAM_DATA).addScanIterator(visibilityIteratorSetting);
  }

  public ObjectScannerIterable<ColumnCountObject> fetchColumn(
      Context context, VersionedMetadataObject colMetadata) {
    return fetchColumn(context, colMetadata, SortOrder.CNT_ASC);
  }

  public List<com.dataprofiler.util.objects.ui.ValueCountObject> fetchColumnPaged(
      Context context,
      VersionedMetadataObject columnMetadata,
      long start,
      long end,
      SortOrder sortOrder,
      boolean normalize,
      boolean includeColVis) {

    // Check input for errors
    if (start < 0) {
      throw new IllegalArgumentException(
          "Start index must be a positive integer greater than or equal to 0");
    }

    if (end < -1) {
      throw new IllegalArgumentException("End index must be -1 or a positive integer");
    }

    if (end != -1 && end < start) {
      throw new IllegalArgumentException("Start index must be less than end index");
    }

    // Get the start and end index
    long startIdx = start / Const.VALUES_PER_PAGE * Const.VALUES_PER_PAGE;
    int startOffset = (int) (start - startIdx);

    long endIdx = Long.MAX_VALUE;
    if (end != -1) {
      endIdx =
          (long) (Math.ceil(end / Const.VALUES_PER_PAGE.doubleValue()) * Const.VALUES_PER_PAGE);
    }

    ObjectScannerIterable<ColumnCountPaginationObject> pages =
        new ColumnCountPaginationObject().getIndicies(context, columnMetadata, sortOrder);

    // Set the start and end to maximum range
    String hash =
        String.join(
            Const.DELIMITER,
            columnMetadata.dataset_name,
            columnMetadata.table_id,
            columnMetadata.column_name,
            sortOrder.GetCode());
    Key startKey = new Key(hash + Const.DELIMITER);
    Key endKey = new Key(hash + Const.HIGH_BYTE);

    // Get the nearest index position greater than start and larger than end

    try (ClosableIterator<ColumnCountPaginationObject> iter = pages.closeableIterator()) {
      while (iter.hasNext()) {
        ColumnCountPaginationObject page = iter.next();
        if (page.getIndex() == startIdx) {
          startKey.set(page.getColumnCountObj().createAccumuloKey());
        } else if (page.getIndex() == endIdx) {
          endKey.set(page.getColumnCountObj().createAccumuloKey());
          break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    ObjectScannerIterable<ColumnCountObject> colCntObjs =
        new ColumnCountObject().find(context, startKey, endKey);

    int idx = 0;
    long count = 0L;
    long numRecords = end == -1 ? Long.MAX_VALUE : end - start;
    List<com.dataprofiler.util.objects.ui.ValueCountObject> result = new ArrayList<>();

    try (ClosableIterator<ColumnCountObject> iter = colCntObjs.closeableIterator()) {
      while (iter.hasNext()) {
        ColumnCountObject col = iter.next();
        // Exit early if we have reached the number of requested records
        if (result.size() >= numRecords) {
          break;
        }

        // Skip any records before the start offset
        if (idx >= startOffset) {
          result.add(
              new com.dataprofiler.util.objects.ui.ValueCountObject(col, normalize, includeColVis));
          count += col.count;
        }
        idx++;
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

    return result;
  }

  public ObjectScannerIterable<ColumnCountObject> fetchColumns(
      Context context, String dataset, String table, List<String> columns) {
    ObjectScannerIterable<ColumnCountObject> scanner = scan(context);
    for (String column : columns) {
      scanner.addRange(createInclusiveRange(dataset, table, column, SortOrder.VAL_ASC.GetCode()));
    }
    return scanner.fetchColumnFamily(Const.COL_FAM_DATA);
  }

  public ObjectScannerIterable<ColumnCountObject> fetchTable(
      Context context, VersionedMetadataObject tableMetadata) {
    ObjectScannerIterable<ColumnCountObject> scanner = scan(context);
    scanner.addRange(createInclusiveRange(tableMetadata.dataset_name, tableMetadata.table_id));
    scanner.fetchColumnFamily(Const.COL_FAM_DATA);

    return scanner;
  }

  public ObjectScannerIterable<ColumnCountObject> fetchTable(
      Context context, VersionedMetadataObject tableMetadata, SortOrder sortOrder) {

    ObjectScannerIterable<ColumnCountObject> scanner = fetchTable(context, tableMetadata);

    if (sortOrder == null) {
      return scanner;
    }

    return filterBySortOrder(scanner, sortOrder);
  }

  public ObjectScannerIterable<ColumnCountObject> fetchDataset(Context context, String dataset) {
    ObjectScannerIterable<ColumnCountObject> scanner = scan(context);
    scanner.addRange(createInclusiveRange(dataset));
    scanner.fetchColumnFamily(Const.COL_FAM_DATA);
    scanner.setBatch(true);
    return scanner;
  }

  public ObjectScannerIterable<ColumnCountObject> fetchDataset(
      Context context, String dataset, SortOrder sortOrder) {
    ObjectScannerIterable<ColumnCountObject> scanner = fetchDataset(context, dataset);

    if (sortOrder == null) {
      return scanner;
    }

    return filterBySortOrder(scanner, sortOrder);
  }

  /**
   * Using this will return all of the data from the column count table and is overall probably the
   * wrong thing to do. You have been warned!
   *
   * @param context
   * @return
   */
  public ObjectScannerIterable<ColumnCountObject> fetchEverything(Context context) {
    ObjectScannerIterable<ColumnCountObject> scanner = scan(context);
    scanner.fetchColumnFamily(Const.COL_FAM_DATA);
    scanner.setBatch(true);
    return scanner;
  }

  public ObjectScannerIterable<ColumnCountObject> fetchEverything(
      Context context, SortOrder sortOrder) {
    ObjectScannerIterable<ColumnCountObject> scanner = fetchEverything(context);

    if (sortOrder == null) {
      return scanner;
    }

    return filterBySortOrder(scanner, sortOrder);
  }

  private ObjectScannerIterable<ColumnCountObject> filterBySortOrder(
      ObjectScannerIterable<ColumnCountObject> scanner, SortOrder sortOrder) {
    // Use a RegEx iterator to match a specific sort order
    IteratorSetting filterSortOrder =
        new IteratorSetting(100, "sortOrderFilter", RegExFilter.class);
    filterSortOrder.addOption(RegExFilter.ROW_REGEX, sortOrder.GetCode());
    filterSortOrder.addOption(RegExFilter.MATCH_SUBSTRING, "true");
    scanner.addScanIterator(filterSortOrder);
    return scanner;
  }

  @Override
  public void bulkPurgeTable(Context context, String datasetName, String tableName, String tableId)
      throws BasicAccumuloException {
    Text start = new Text(joinKeyComponentsEndDelimited(datasetName, tableId));
    Text end = new Text(joinKeyComponentsEndDelimited(datasetName, tableId) + Const.HIGH_BYTE);
    bulkPurgeRange(context, start, end);
  }

  @Override
  public String toString() {
    return "ColumnCountObject{"
        + "value='"
        + value
        + '\''
        + ", count="
        + count
        + ", dataset='"
        + dataset
        + '\''
        + ", tableId='"
        + tableId
        + '\''
        + ", column='"
        + column
        + '\''
        + ", sortOrder="
        + sortOrder
        + ", visibility='"
        + visibility
        + '\''
        + '}';
  }

  public String toJson() throws JsonProcessingException {
    return mapper.writeValueAsString(this);
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Long getCount() {
    return count;
  }

  public void setCount(Long count) {
    this.count = count;
  }

  public String getDataset() {
    return dataset;
  }

  public void setDataset(String dataset) {
    this.dataset = dataset;
  }

  public String getTableId() {
    return tableId;
  }

  public void setTableId(String tableId) {
    this.tableId = tableId;
  }

  public String getColumn() {
    return column;
  }

  public void setColumn(String column) {
    this.column = column;
  }

  public static class ValueCountObject {

    public String value;
    public Long count;

    public ValueCountObject() {}

    public ValueCountObject(String value, Long count) {
      this.value = value;
      this.count = count;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public Long getCount() {
      return count;
    }

    public void setCount(Long count) {
      this.count = count;
    }

    public String toString() {
      return "{ \"value\": " + value + ", \"count\": " + count + "}";
    }
  }
}
