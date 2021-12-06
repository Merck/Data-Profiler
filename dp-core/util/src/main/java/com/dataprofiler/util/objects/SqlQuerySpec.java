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

import com.dataprofiler.util.Const;
import com.dataprofiler.util.Context;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;

public class SqlQuerySpec extends AccumuloObject<SqlQuerySpec> {
  public static final String TIME_SERIES = "time_series";
  public static final String SIMPLE = "simple";

  private String jobId;
  private String queryType; // only time_series for now
  private String dateColumn; // required for type time_series
  private String query;
  /** Visibilities to use when runnings the query */
  private Set<String> queryVisibilities = new HashSet<>();
  /**
   * Visibility expression to use to store the result. This is just a temporary measure. In the
   * future the visibility expression should be derived from the source data.
   */
  private String outputVisibility;

  private List<SqlTableSpec> tables = new ArrayList<>();
  private SqlQueryGroupSpec group = new SqlQueryGroupSpec();
  private List<SqlQueryWindowSpec> windows = new ArrayList<>();
  private String outputDataset;
  private String outputTable; // only used for simple queries
  private boolean deleteBeforeReload = true;
  private Map<String, String> columnProperties = new HashMap<>();
  private Map<String, String> tableProperties = new HashMap<>();
  private Map<String, String> datasetProperties = new HashMap<>();

  private static final TypeReference<SqlQuerySpec> staticTypeReference =
      new TypeReference<SqlQuerySpec>() {};

  public SqlQuerySpec() {
    super(Const.ACCUMULO_DATA_LOAD_JOBS_TABLE_ENV_KEY);
  }

  public static SqlQuerySpec fetchByJobId(Context context, String jobId) {
    SqlQuerySpec s = new SqlQuerySpec();
    s.setJobId(jobId);

    return s.fetch(context, s.createAccumuloKey());
  }

  @Override
  public SqlQuerySpec fromEntry(Entry<Key, Value> entry) {
    SqlQuerySpec s = null;

    try {
      s = fromJson(entry.getValue().toString());
    } catch (IOException e) {
      throw new InvalidDataFormat(e.toString());
    }

    Key key = entry.getKey();
    s.setJobId(key.getRow().toString());

    s.updatePropertiesFromEntry(entry);

    return s;
  }

  /**
   * Key for SqlQuerySpec consists of the following:
   *
   * <p>RowID - jobId
   *
   * <p>ColFam - COL_FAM_SQL_QUERY_JOB_SPEC
   *
   * <p>ColQual - ""
   *
   * @return Key
   * @throws InvalidDataFormat
   */
  @Override
  public Key createAccumuloKey() throws InvalidDataFormat {
    return new Key(jobId, Const.COL_FAM_SQL_QUERY_JOB_SPEC, "", visibility);
  }

  public void checkRequiredFields() throws Exception {
    if (jobId == null
        || queryType == null
        || query == null
        || outputVisibility == null
        || tables.size() < 1
        || outputDataset == null) {
      throw new Exception("Missing required fields");
    }
  }

  public static SqlQuerySpec fromJson(String json) throws IOException {
    return mapper.readValue(json, staticTypeReference);
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public String getQueryType() {
    return queryType;
  }

  public void setQueryType(String queryType) {
    this.queryType = queryType;
  }

  public String getDateColumn() {
    return dateColumn;
  }

  public void setDateColumn(String dateColumn) {
    this.dateColumn = dateColumn;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public List<SqlTableSpec> getTables() {
    return tables;
  }

  public void setTables(List<SqlTableSpec> tables) {
    this.tables = tables;
  }

  public SqlQueryGroupSpec getGroup() {
    return group;
  }

  public void setGroup(SqlQueryGroupSpec group) {
    this.group = group;
  }

  public List<SqlQueryWindowSpec> getWindows() {
    return windows;
  }

  public void setWindows(List<SqlQueryWindowSpec> windows) {
    this.windows = windows;
  }

  public Set<String> getQueryVisibilities() {
    return queryVisibilities;
  }

  public void setQueryVisibilities(Set<String> queryVisibilities) {
    this.queryVisibilities = queryVisibilities;
  }

  public String getOutputVisibility() {
    return outputVisibility;
  }

  public void setOutputVisibility(String outputVisibility) {
    this.outputVisibility = outputVisibility;
  }

  public String getOutputDataset() {
    return outputDataset;
  }

  public void setOutputDataset(String outputDataset) {
    this.outputDataset = outputDataset;
  }

  public String getOutputTable() {
    return outputTable;
  }

  public void setOutputTable(String outputTable) {
    this.outputTable = outputTable;
  }

  public boolean getDeleteBeforeReload() {
    return deleteBeforeReload;
  }

  public void setDeleteBeforeReload(boolean deleteBeforeReload) {
    this.deleteBeforeReload = deleteBeforeReload;
  }

  public Map<String, String> getColumnProperties() {
    return columnProperties;
  }

  public void setColumnProperties(Map<String, String> columnProperties) {
    this.columnProperties = columnProperties;
  }

  public Map<String, String> getTableProperties() {
    return tableProperties;
  }

  public void setTableProperties(Map<String, String> tableProperties) {
    this.tableProperties = tableProperties;
  }

  public Map<String, String> getDatasetProperties() {
    return datasetProperties;
  }

  public void setDatasetProperties(Map<String, String> datasetProperties) {
    this.datasetProperties = datasetProperties;
  }
}
