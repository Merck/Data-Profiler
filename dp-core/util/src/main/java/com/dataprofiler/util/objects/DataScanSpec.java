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

import com.dataprofiler.querylang.expr.Expression;
import com.dataprofiler.querylang.json.Expressions;
import com.dataprofiler.querylang.json.jackson.ExpressionDeserializer;
import com.dataprofiler.querylang.json.jackson.ExpressionSerializer;
import com.dataprofiler.util.objects.VersionedDatasetMetadata.MissingMetadataException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class holds all of the options for a scan of the rows or column count, including filtering.
 * It will likely expand in the future. But for now, it's mainly here to help with serializing to /
 * from json.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataScanSpec {

  public static final long ROWS_LIMIT = 10000;
  public static final long SEARCH_LIMIT = 1000000;
  public static final long COLUMN_COUNT_LIMIT = 10000;

  protected static ObjectMapper mapper;
  protected static TypeReference<DataScanSpec> staticTypeReference =
      new TypeReference<DataScanSpec>() {};

  static {
    mapper = new ObjectMapper();
    SimpleModule expressionSerDeModule = new SimpleModule();
    expressionSerDeModule.addSerializer(Expression.class, new ExpressionSerializer());
    expressionSerDeModule.addDeserializer(Expression.class, new ExpressionDeserializer());
    mapper.registerModule(expressionSerDeModule);
  }

  protected Type type = Type.ROW;
  protected Level level = Level.GLOBAL;
  protected Level groupingLevel = Level.COLUMN;
  protected String dataset;
  protected String table;
  protected String column;
  protected List<String> term = new ArrayList<>();
  protected boolean begins_with = true;
  protected boolean substring_match = false;
  protected Integer limit = 10000;
  protected Map<String, List<String>> filters = new HashMap<>();
  protected List<String> columnOrder;
  protected Expression v2Query;
  protected Integer pageSize;
  protected String startLocation;
  protected Boolean returnFullValues = false;
  protected Boolean returnVisibilities = false;

  public DataScanSpec() {}

  public DataScanSpec(String dataset, String table) {
    this.dataset = dataset;
    this.table = table;
  }

  public DataScanSpec(DataScanSpec dataScanSpec) {
    this.type = dataScanSpec.getType();
    this.level = dataScanSpec.getLevel();
    this.groupingLevel = dataScanSpec.getGroupingLevel();
    this.dataset = dataScanSpec.getDataset();
    this.table = dataScanSpec.getTable();
    this.column = dataScanSpec.getColumn();
    this.term = dataScanSpec.getTerm();
    this.begins_with = dataScanSpec.getBegins_with();
    this.limit = dataScanSpec.getLimit();
    this.filters = dataScanSpec.getFilters();
    this.columnOrder = dataScanSpec.getColumnOrder();
    this.substring_match = dataScanSpec.getSubstring_match();
    this.v2Query = dataScanSpec.getV2Query();
    this.pageSize = dataScanSpec.getPageSize();
    this.startLocation = dataScanSpec.getStartLocation();
    this.returnFullValues = dataScanSpec.getReturnFullValues();
    this.returnVisibilities = dataScanSpec.getReturnVisibilities();
  }

  public static DataScanSpec fromJson(String json) throws IOException {
    DataScanSpec ds = mapper.readValue(json, staticTypeReference);
    ds.upgradeFilters();
    return ds;
  }

  public static DataScanSpec fromJson(String json, Type type) throws IOException {
    DataScanSpec spec = fromJson(json);
    spec.setType(type);
    return spec;
  }

  public static JsonNode v2GenericOperationClause(String op, String column, String value) {
    ObjectNode inner = mapper.createObjectNode();
    inner.put("type", "string");
    inner.put("column", column);
    inner.put("value", value);
    ObjectNode outer = mapper.createObjectNode();
    outer.put(op, inner);
    return outer;
  }

  public static JsonNode v2LtClause(String column, String value) {
    return DataScanSpec.v2GenericOperationClause("$lt", column, value);
  }

  public static JsonNode v2GtClause(String column, String value) {
    return DataScanSpec.v2GenericOperationClause("$gt", column, value);
  }

  public static JsonNode v2LteClause(String column, String value) {
    return DataScanSpec.v2GenericOperationClause("$lte", column, value);
  }

  public static JsonNode v2GteClause(String column, String value) {
    return DataScanSpec.v2GenericOperationClause("$gte", column, value);
  }

  public static JsonNode v2EqClause(String column, String value) {
    return DataScanSpec.v2GenericOperationClause("$eq", column, value);
  }

  public static JsonNode v2AndOrGroup(String type, List<JsonNode> clauses) {
    ObjectNode node = mapper.createObjectNode();
    ArrayNode mappedClauses = mapper.valueToTree(clauses);
    node.put(type, mappedClauses);
    return node;
  }

  public void upgradeFilters() throws IOException {
    Map<String, List<String>> filters = this.getFilters();
    if (filters != null && !filters.isEmpty()) {
      if (this.getV2Query() != null) {
        throw new IOException("Cannot include both filters and v2Query");
      }
      List<JsonNode> andGroups = new ArrayList<>();
      for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
        String column = entry.getKey();
        List<String> values = entry.getValue();
        if (values.size() == 1) {
          andGroups.add(v2EqClause(column, values.get(0)));
        } else {
          List<JsonNode> colOrGroup = new ArrayList<>();
          for (String value : values) {
            colOrGroup.add(v2EqClause(column, value));
          }
          andGroups.add(v2AndOrGroup("$or", colOrGroup));
        }
      }
      this.resetFilters();
      this.setV2Query(
          Expressions.parse(mapper.writeValueAsString(v2AndOrGroup("$and", andGroups))));
    }
  }

  @Override
  public String toString() {
    return "DataScanSpec{"
        + "type="
        + type
        + ", level="
        + level
        + ", groupLevel="
        + groupingLevel
        + ", dataset='"
        + dataset
        + '\''
        + ", table='"
        + table
        + '\''
        + ", column='"
        + column
        + '\''
        + ", term="
        + term
        + ", begins_with="
        + begins_with
        + ", limit="
        + limit
        + ", filters="
        + filters
        + ", substring_match="
        + substring_match
        + ", return_visibilities="
        + returnVisibilities
        + ", columnOrder="
        + columnOrder
        + ", pageSize="
        + pageSize
        + ", startLocation="
        + startLocation
        + '}';
  }

  public Level getLevel() {
    return level;
  }

  public void setLevel(Level level) {
    this.level = level;
  }

  public Level getGroupingLevel() {
    return groupingLevel;
  }

  public void setGroupingLevel(Level groupingLevel) {
    this.groupingLevel = groupingLevel;
  }

  public List<String> getTerm() {
    return term;
  }

  public void setTerm(List<String> term) {
    this.term.addAll(term);
  }

  public void addTerm(String term) {
    this.term.add(term);
  }

  public String firstTerm() {
    return this.term.get(0);
  }

  public Boolean getBegins_with() {
    return begins_with;
  }

  public void setBegins_with(Boolean begins_with) {
    this.begins_with = begins_with;
  }

  public Integer getLimit() {
    return limit;
  }

  public void setLimit(Integer limit) {
    this.limit = limit;
  }

  public Boolean getSubstring_match() {
    return substring_match;
  }

  public void setSubstring_match(Boolean substring_match) {
    this.substring_match = substring_match;
  }

  public Boolean getReturnVisibilities() {
    return returnVisibilities;
  }

  public void setReturnVisibilities(Boolean returnVisibilities) {
    this.returnVisibilities = returnVisibilities;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public void setPageSize(Integer pageSize) {
    this.pageSize = pageSize;
  }

  public String getStartLocation() {
    return startLocation;
  }

  public void setStartLocation(String startLocation) {
    this.startLocation = startLocation;
  }

  public String toJson() throws JsonProcessingException {
    return mapper.writeValueAsString(this);
  }

  public String checkRequiredFields() {
    if (type == Type.SEARCH) {
      if (limit > SEARCH_LIMIT) {
        return "limit must be less than " + SEARCH_LIMIT;
      }
      // All searches require a `term` to be specified
      // A dataset level search requires `dataset` to be specified
      // A table level search requires `dataset` and `table` to be specified
      // A column level search requires `dataset`, `table`, and `column` to be specified
      // A column level search is the only type of search that can have substring_match
      if (substring_match == true) {
        if (column == null) {
          return "`substring_match` only available for column level searches";
        }
        if (term == null || term.size() != 1) {
          return "`substring_match` only works with one term";
        }
      }
      if (term == null) {
        return "`term` is required to perform a search";
      } else if (table != null && dataset == null) {
        return "`dataset` is required to search at the table level";
      } else if (column != null && (table == null || dataset == null)) {
        return "`dataset` and `table` are required to search for at the column level";
      } else {
        return null;
      }
    }

    if (type == Type.ROW) {
      if ((pageSize != null && pageSize > ROWS_LIMIT) || limit > ROWS_LIMIT) {
        return "maximum rows is " + ROWS_LIMIT;
      }
    }

    if (type == Type.COLUMN_COUNT) {
      if (limit > COLUMN_COUNT_LIMIT) {
        return "maximum limit is " + COLUMN_COUNT_LIMIT;
      }
    }

    if (dataset == null || table == null) {
      return "dataset and table are required";
    }

    if (type == Type.COLUMN_COUNT && column == null) {
      return "Column is required for column count";
    }

    return null;
  }

  public String checkRequiredMultiSearchFields() {
    if (type == Type.SEARCH) {
      // All searches require a `term` to be specified
      // A dataset level search requires `dataset` to be specified
      // A table level search requires `dataset` and `table` to be specified
      // A column level search requires `dataset`, `table`, and `column` to be specified
      if (term == null) {
        return "`term` is required to perform a search";
      } else if (table != null && dataset == null) {
        return "`dataset` is required to search at the table level";
      } else if (column != null && (table == null || dataset == null)) {
        return "`dataset` and `table` are required to search for at the column level";
      } else {
        return null;
      }
    }

    if (dataset == null || table == null) {
      return "dataset and table are required";
    }

    if (type == Type.COLUMN_COUNT && column == null) {
      return "Column is required for column count";
    }

    return null;
  }

  public String getDataset() {
    return dataset;
  }

  public void setDataset(String datasetName) {
    this.dataset = datasetName;
  }

  public String getTable() {
    return table;
  }

  public String getTableId(VersionedDatasetMetadata metadata) throws MissingMetadataException {
    if (!metadata.getTableMetadata().containsKey(table)) {
      throw new MissingMetadataException("Metadata does not contain table " + table);
    }

    return metadata.getTableMetadata().get(table).table_id;
  }

  public void setTable(String tableName) {
    this.table = tableName;
  }

  public Map<String, List<String>> getFilters() {
    return filters;
  }

  public void setFilters(Map<String, List<String>> filters) {
    this.filters.putAll(filters);
  }

  public void resetFilters() {
    this.filters = new HashMap<>();
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public String getColumn() {
    return column;
  }

  public void setColumn(String column) {
    this.column = column;
  }

  public List<String> getColumnOrder() {
    return columnOrder;
  }

  public void setColumnOrder(List<String> columnOrder) {
    this.columnOrder = columnOrder;
  }

  public Expression getV2Query() {
    return v2Query;
  }

  public void setV2Query(Expression v2Query) {
    this.resetFilters();
    this.v2Query = v2Query;
  }

  public Boolean getReturnFullValues() {
    return returnFullValues;
  }

  public void setReturnFullValues(Boolean returnFullValues) {
    this.returnFullValues = returnFullValues;
  }

  @JsonIgnore
  public String[] getColumnArray() {
    return new ArrayList<>(filters.keySet()).toArray(new String[0]);
  }

  @JsonIgnore
  public String[] getValueArray() {
    // TODO - for now we are just asserting that there is only 1 value per column
    // because we can only handle that.
    ArrayList<String> out = new ArrayList<>();
    for (List<String> values : filters.values()) {
      assert (values.size() == 1);
      out.add(values.get(0));
    }

    return out.toArray(new String[0]);
  }

  public enum Type {
    ROW,
    COLUMN_COUNT,
    SEARCH;

    private static final Map<String, Type> valueMap = new HashMap<>();

    static {
      valueMap.put("row", ROW);
      valueMap.put("column_count", COLUMN_COUNT);
      valueMap.put("search", SEARCH);
    }

    @JsonCreator
    public static Type forValue(String value) {
      return valueMap.get(value.toLowerCase());
    }

    @JsonValue
    public String toValue() {
      for (Entry<String, Type> entry : valueMap.entrySet()) {
        if (entry.getValue() == this) {
          return entry.getKey();
        }
      }

      return null;
    }
  }

  public enum Level {
    GLOBAL,
    DATASET,
    TABLE,
    COLUMN;

    private static final Map<String, Level> valueMap = new HashMap<>();

    static {
      valueMap.put("global", GLOBAL);
      valueMap.put("dataset", DATASET);
      valueMap.put("table", TABLE);
      valueMap.put("column", COLUMN);
    }

    @JsonCreator
    public static Level forValue(String value) {
      return valueMap.get(value.toLowerCase());
    }

    @JsonValue
    public String toValue() {
      for (Entry<String, Level> entry : valueMap.entrySet()) {
        if (entry.getValue() == this) {
          return entry.getKey();
        }
      }

      return null;
    }
  }
}
