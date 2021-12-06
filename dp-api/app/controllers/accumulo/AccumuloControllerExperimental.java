/**
*  Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
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
**/
package controllers.accumulo;

import actions.AccumuloUserContext;
import actions.Authenticated;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dataprofiler.querylang.json.Expressions;
import com.dataprofiler.util.Const.SortOrder;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.iterators.ClosableIterator;
import com.dataprofiler.util.objects.MetadataVersionObject;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import com.dataprofiler.util.objects.ColumnCountObject;
import com.dataprofiler.util.objects.DataScanSpec;
import com.dataprofiler.util.objects.VersionedDataScanSpec;
import com.dataprofiler.util.objects.DatawaveRowObject;
import com.dataprofiler.util.objects.ui.JoinObject;
import com.typesafe.config.Config;
import helpers.AccumuloHelper;
import helpers.RulesOfUseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import static java.util.stream.Collectors.toList;

@Authenticated
@AccumuloUserContext
public class AccumuloControllerExperimental extends Controller {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private static final ObjectMapper mapper = new ObjectMapper();
  private final Config config;
  private final AccumuloHelper accumulo;

  @Inject
  public AccumuloControllerExperimental(Config config) throws IOException {
    this.config = config;
    this.accumulo = new AccumuloHelper();
  }

  private Map<String, String> changeColNames(String s, Map<String, String> row) {
    Map<String, String> result = new HashMap<>();

    for (String col : row.keySet()) {
      result.put(String.join(".", s, col), row.get(col));
    }
    return result;
  }

  private Map<String, Long> getColCounts(
      Context context,
      MetadataVersionObject version,
      String datasetname,
      String tableName,
      String columnName,
      int limit)
    throws Exception {

    VersionedMetadataObject metaColA =
      new VersionedMetadataObject()
          .fetchColumn(context, version, datasetname, tableName, columnName);

    Map<String, Long> colCounts = new HashMap<>();

    try (ClosableIterator<ColumnCountObject> iter =
      new ColumnCountObject()
          .fetchColumn(context, metaColA, SortOrder.CNT_DESC)
          .closeableIterator()) {

      for (int i = 0; iter.hasNext() && i < limit; i++) {
        ColumnCountObject currCol = iter.next();
        colCounts.put(currCol.getValue(), currCol.getCount());
      }
    }

    return colCounts;
  }

  private Map<String, List<Map<String, String>>> getRowsGroupedByValue(
      Context context,
      String datasetname,
      String tableName,
      String columnName,
      List<String> terms,
      int limit)
    throws Exception {

    DataScanSpec spec = new DataScanSpec(datasetname, tableName);

    // Only query data that will be used on the join
    List<JsonNode> columns =
      terms.stream().map(val -> DataScanSpec.v2EqClause(columnName, val)).collect(toList());

    JsonNode query = DataScanSpec.v2AndOrGroup("$or", columns);
    spec.setV2Query(Expressions.parse(mapper.writeValueAsString(query)));

    VersionedDataScanSpec versionedSpec = new VersionedDataScanSpec(context, spec);

    Map<String, List<Map<String, String>>> result = new HashMap<>();

    try (ClosableIterator<DatawaveRowObject> iter =
      new DatawaveRowObject().find(context, versionedSpec).closeableIterator()) {

      for (int i = 0; iter.hasNext() && i < limit; i++) {
        Map<String, String> currRow = iter.next().getRow();
        String currTerm = currRow.get(columnName);

        // Update names of columns
        currRow = changeColNames(tableName, currRow);

        if (result.containsKey(currTerm)) {
          result.get(currTerm).add(currRow);
        } else {
          List<Map<String, String>> l = new ArrayList<>();
          l.add(currRow);
          result.put(currTerm, l);
        }
      }
    }

    return result;
  }

  @BodyParser.Of(BodyParser.TolerantText.class)
  public Result joinStats(Request req) throws Exception {
    String json = req.body().asText();
    JsonNode jsonRes = mapper.readTree(json);
  
    String datasetA = jsonRes.get("dataset_a").asText();
    String tableA = jsonRes.get("table_a").asText();
    String colA = jsonRes.get("col_a").asText();
  
    String datasetB = jsonRes.get("dataset_b").asText();
    String tableB = jsonRes.get("table_b").asText();
    String colB = jsonRes.get("col_b").asText();
  
    if (datasetA == null
        || datasetA.isEmpty()
        || tableA == null
        || tableA.isEmpty()
        || colA == null
        || colA.isEmpty()
        || datasetB == null
        || datasetB.isEmpty()
        || tableB == null
        || tableB.isEmpty()
        || colB == null
        || colB.isEmpty()) {
      return badRequest("dataset, table, and column required for join");
    }
  
    int limit = jsonRes.get("limit") == null ? 1000 : jsonRes.get("limit").asInt();
  
    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    MetadataVersionObject version = context.getCurrentMetadataVersion();
  
    // Get column counts for both tables
    Map<String, Long> cntsColA = getColCounts(context, version, datasetA, tableA, colA, limit);
    Map<String, Long> cntsColB = getColCounts(context, version, datasetB, tableB, colB, limit);
  
    // Find intersection
    Map<String, Long> matchTermCnt = new HashMap<>();
  
    for (String currKey : cntsColA.keySet()) {
      if (cntsColB.containsKey(currKey)) {
        matchTermCnt.put(currKey, cntsColA.get(currKey) * cntsColB.get(currKey));
      }
    }
  
    JoinObject result = new JoinObject();
  
    // Get number of rows
    long numMatches = matchTermCnt.values().stream().mapToLong(Long::longValue).sum();
  
    if (numMatches > 0) {
  
      // Limit the matching terms so the join does not exceed limit
      List<String> terms = new ArrayList<>();
      int cnt = 0;
      for (String term : matchTermCnt.keySet()) {
        terms.add(term);
        cnt += matchTermCnt.get(term);
        if (cnt > limit) {
          break;
        }
      }
  
      // Get rows for both tables
      Map<String, List<Map<String, String>>> rowsA =
          getRowsGroupedByValue(context, datasetA, tableA, colA, terms, limit);
      Map<String, List<Map<String, String>>> rowsB =
          getRowsGroupedByValue(context, datasetB, tableB, colB, terms, limit);

      for (String currVal : rowsA.keySet()) {
        for (Map<String, String> rowA : rowsA.get(currVal)) {
          for (Map<String, String> rowB : rowsB.get(currVal)) {
            rowA.putAll(rowB);
            result.getSample().add(rowA);
          }
        }
      }
  
      Map<String, Object> metadata = new HashMap<>();
      metadata.put("num_rows", numMatches);
      metadata.put("column_names", result.getSample().get(0).keySet());
      metadata.put("num_columns", result.getSample().get(0).keySet().size());
      metadata.put("value_matches", matchTermCnt);
  
      result.setMeta(metadata);
    }
  
    return ok(Json.toJson(result));
  }
}