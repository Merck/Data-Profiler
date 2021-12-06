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
import com.dataprofiler.util.Context;
import com.dataprofiler.util.iterators.ColumnCountVisibilityIterator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.accumulo.core.client.BatchDeleter;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.user.RegExFilter;
import org.apache.commons.collections.map.HashedMap;
import org.apache.hadoop.io.Text;

public class ColumnCountIndexObject extends VersionedPurgableDatasetObject<ColumnCountIndexObject> {

  private static final String accumuloTable = Const.ACCUMULO_INDEX_TABLE_ENV_KEY;

  // Cell Level Visibility Iterator Setting
  private static final IteratorSetting visibilityIteratorSetting =
      new IteratorSetting(21, "colCntVisItr", ColumnCountVisibilityIterator.class);

  private String dataset;
  private String tableId;
  // This is the table name - but it's called table for historical reasons (and old data has this
  // stored
  // in accumulo so it needs to not change).
  private String table;
  private String column;
  private String value;
  private Long count;
  private IndexType index;
  private String normalizedValue;

  private Map<String, String> activeTables;

  public ColumnCountIndexObject() {
    super(accumuloTable);
  }

  public ColumnCountIndexObject(
      String dataset,
      String tableName,
      String tableId,
      String column,
      String value,
      String normalizedValue,
      Long count) {
    this(dataset, tableName, tableId, column, value, count);
    this.normalizedValue = normalizedValue;
  }

  public ColumnCountIndexObject(
      String dataset, String tableName, String tableId, String column, String value, Long count) {
    super(accumuloTable);
    this.dataset = dataset;
    this.tableId = tableId;
    this.table = tableName;
    this.column = column;
    this.value = value;
    this.count = count;
  }

  @Override
  public ColumnCountIndexObject fromEntry(Entry<Key, Value> entry) {
    ColumnCountIndexObject c = null;
    try {
      c = mapper.readValue(entry.getValue().get(), this.getClass());
      // This handles old records that only store the table and not the table id. For records of
      // that
      // age the table name is the table id, so this works correctly.
      if (c.tableId == null) {
        c.tableId = c.table;
      }
      if (activeTables != null) {
        String key = joinKeyComponents(c.dataset, c.tableId);
        assert (activeTables.containsKey(key));
        c.table = activeTables.get(key);
      }
      c.updatePropertiesFromEntry(entry);
    } catch (IOException e) {
      throw new InvalidDataFormat(e.toString());
    }

    return c;
  }

  private ObjectScannerIterable<ColumnCountIndexObject> addActiveTableFilter(
      ObjectScannerIterable<ColumnCountIndexObject> scanner, Map<String, String> activeTables) {
    this.activeTables = activeTables;
    IteratorSetting iter =
        new IteratorSetting(
            100, "filterActiveTables", "com.dataprofiler.iterators.indexData.ActiveTableFilter");
    String activeTableConf = String.join("__&&__", activeTables.keySet());
    iter.addOption("active_tables", activeTableConf);
    scanner.addScanIterator(iter);
    return scanner;
  }

  /**
   * * Find entries in the index matching the provided term. This will only return tables in the map
   * of provided tables (map should be tableids -> table names).
   *
   * @param context dataprofiler context
   * @param activeTables map of table ids -> table names for tables that should be returned
   * @param term term to search for
   * @param exclusive whether to match the term exacly or as a prefix
   * @return
   */
  public ObjectScannerIterable<ColumnCountIndexObject> find(
      Context context,
      Map<String, String> activeTables,
      String term,
      String index,
      Boolean exclusive) {
    Range range;

    term = term.toLowerCase();

    if (exclusive) {
      range = new Range(new Range(term, term + Const.LOW_BYTE));
    } else {
      range = new Range(new Range(term, term + Const.HIGH_BYTE));
    }

    return addActiveTableFilter(
        scan(context)
            .addRange(range)
            .fetchColumnFamily(index)
            .addScanIterator(visibilityIteratorSetting),
        activeTables);
  }

  public ObjectScannerIterable<ColumnCountIndexObject> find(
      Context context, VersionedDataScanSpec spec) {

    this.activeTables = spec.getActiveTables();

    Range range;
    ObjectScannerIterable<ColumnCountIndexObject> scanner = scan(context);
    String term = spec.firstTerm().toLowerCase();
    String rowId = null;
    String columnFam = null;

    if (spec.getDataset() == null) {

      rowId = term;
      columnFam = Const.INDEX_GLOBAL;
      addActiveTableFilter(scanner, activeTables);
    } else if (spec.getDataset() != null) {
      rowId = joinKeyComponents(spec.getDataset(), term);
      columnFam = Const.INDEX_DATASET;

      if (spec.getTable() != null) {
        columnFam = Const.INDEX_TABLE;

        if (spec.getColumn() != null) {
          columnFam = Const.INDEX_COLUMN;
          if (spec.getSubstring_match() == true) {
            rowId = joinKeyComponents(spec.getDataset(), spec.getTable(), spec.getColumn());
            IteratorSetting substringRegex =
                new IteratorSetting(100, "substringMatchFilter", RegExFilter.class);
            substringRegex.addOption(RegExFilter.ROW_REGEX, Pattern.quote(term));
            substringRegex.addOption(RegExFilter.MATCH_SUBSTRING, "true");
            scanner.addScanIterator(substringRegex);
          } else {
            rowId = joinKeyComponents(spec.getDataset(), spec.getTable(), spec.getColumn(), term);
          }
        } else if (spec.getSubstring_match() == true) {
          rowId = joinKeyComponents(spec.getDataset(), spec.getTable());
          IteratorSetting substringRegex =
              new IteratorSetting(100, "substringMatchFilter", RegExFilter.class);
          substringRegex.addOption(RegExFilter.ROW_REGEX, Pattern.quote(term));
          substringRegex.addOption(RegExFilter.MATCH_SUBSTRING, "true");
          scanner.addScanIterator(substringRegex);
        } else {
          rowId = joinKeyComponents(spec.getDataset(), spec.getTable(), term);
        }
      } else {
        addActiveTableFilter(scanner, activeTables);
      }
    }

    if (!spec.getBegins_with() && !spec.getSubstring_match()) {
      range = new Range(new Range(rowId, rowId + Const.LOW_BYTE));
    } else {
      range = new Range(new Range(rowId, rowId + Const.HIGH_BYTE));
    }

    scanner.addRange(range);
    scanner.fetchColumnFamily(columnFam);
    scanner.addScanIterator(visibilityIteratorSetting);

    return scanner;
  }

  /**
   * Value for Column Count Index is the JSON object that contains: * dataset * table * value *
   * count
   *
   * @return Accumulo Value
   */
  @Override
  public Value createAccumuloValue() throws InvalidDataFormat, IOException {
    return new Value(mapper.writeValueAsBytes(this));
  }

  /**
   * Keys for the Column Count Index Objects are
   *
   * <p>RowID: value
   *
   * <p>Col Fam: dataset DELIM tableId
   *
   * <p>Col Qual: dataset DELIM tableId DELIM column
   *
   * @return Accumulo Key
   */
  @Override
  public Key createAccumuloKey() throws InvalidDataFormat {
    String valueNorm =
        this.normalizedValue == null ? value.toLowerCase().trim() : this.normalizedValue;
    String rowId;
    String colFam;

    if (index.equals(IndexType.GLOBAL)) {
      rowId = valueNorm;
      colFam = Const.INDEX_GLOBAL;
    } else if (index.equals(IndexType.DATASET)) {
      rowId = joinKeyComponents(dataset, valueNorm);
      colFam = Const.INDEX_DATASET;
    } else if (index.equals(IndexType.TABLE)) {
      rowId = joinKeyComponents(dataset, tableId, valueNorm);
      colFam = Const.INDEX_TABLE;
    } else {
      rowId = joinKeyComponents(dataset, tableId, column, valueNorm);
      colFam = Const.INDEX_COLUMN;
    }

    return new Key(rowId, colFam, joinKeyComponents(dataset, tableId, column), visibility);
  }

  public void indexTypeGlobal() {
    this.index = IndexType.GLOBAL;
  }

  public void indexTypeDataset() {
    this.index = IndexType.DATASET;
  }

  public void indexTypeTable() {
    this.index = IndexType.TABLE;
  }

  public void indexTypeColumn() {
    this.index = IndexType.COLUMN;
  }

  @Override
  public void bulkPurgeTable(Context context, String datasetName, String tableName, String tableId)
      throws BasicAccumuloException {
    // We can only bulk purge some data - other we have to batch purge. We are going to do this
    // all from one interface - maybe this is a bad idea, but I want to do this simply and correctly
    // rather than fast.

    // This will take out the table and column level indexes
    Text start = new Text(joinKeyComponentsEndDelimited(datasetName, tableId));
    Text end = new Text(joinKeyComponentsEndDelimited(datasetName, tableId) + Const.HIGH_BYTE);
    bulkPurgeRange(context, start, end);

    // Global and dataset
    String colQ = joinKeyComponents(datasetName, tableId);
    try {
      BatchDeleter deleter = context.createBatchDeleter(getTable(context));
      deleter.fetchColumnFamily(new Text(Const.INDEX_GLOBAL));
      deleter.fetchColumnFamily(new Text(Const.INDEX_DATASET));
      deleter.setRanges(Collections.singleton(new Range()));

      IteratorSetting iterSetting = new IteratorSetting(100, "filterColQ", RegExFilter.class);
      iterSetting.addOption(RegExFilter.COLQ_REGEX, colQ);
      iterSetting.addOption(RegExFilter.MATCH_SUBSTRING, "true");

      deleter.addScanIterator(iterSetting);

      deleter.delete();
      deleter.close();
    } catch (Exception e) {
      throw new BasicAccumuloException(e.getMessage());
    }
  }

  public void bulkDeleteContainingTable(Context context, MetadataVersionObject version)
      throws BasicAccumuloException {
    super.bulkDeleteContainingTable(context);
    String currTable = getTable(context);

    try {
      TableOperations tops = context.getConnector().tableOperations();

      Map<String, Set<Text>> groups = new HashedMap();

      for (IndexType type : IndexType.values()) {
        String group = type.toValue().toUpperCase();
        groups.put(group, new HashSet<>(Arrays.asList(new Text(group))));
      }

      tops.setLocalityGroups(currTable, groups);
    } catch (Exception e) {
      throw new BasicAccumuloException(e.getMessage());
    }
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

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public String getColumn() {
    return column;
  }

  public void setColumn(String column) {
    this.column = column;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getNormalizedValue() {
    return this.normalizedValue;
  }

  public void setNormalizedValue(String normalizedValue) {
    this.normalizedValue = normalizedValue;
  }

  public Long getCount() {
    return count;
  }

  public void setCount(Long count) {
    this.count = count;
  }

  public IndexType getIndex() {
    return index;
  }

  public void setIndex(IndexType index) {
    this.index = index;
  }

  @Override
  public String toString() {
    return "ColumnCountIndexObject{"
        + "dataset='"
        + dataset
        + '\''
        + ", tableId='"
        + tableId
        + '\''
        + ", table='"
        + table
        + '\''
        + ", column='"
        + column
        + '\''
        + ", value='"
        + value
        + '\''
        + ", normalizedValue='"
        + normalizedValue
        + '\''
        + ", count="
        + count
        + ", index="
        + index
        + ", activeTables="
        + activeTables
        + '}';
  }

  public enum IndexType {
    GLOBAL,
    DATASET,
    TABLE,
    COLUMN;

    private static final Map<String, IndexType> valueMap = new HashMap<>();

    static {
      valueMap.put("global", GLOBAL);
      valueMap.put("dataset", DATASET);
      valueMap.put("table", TABLE);
      valueMap.put("column", COLUMN);
    }

    @JsonCreator
    public static IndexType forValue(String value) {
      return valueMap.get(value.toLowerCase());
    }

    @JsonValue
    public String toValue() {
      for (Entry<String, IndexType> entry : valueMap.entrySet()) {
        if (entry.getValue() == this) {
          return entry.getKey();
        }
      }

      return null;
    }
  }
}
