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
import com.dataprofiler.util.Const.SortOrder;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.iterators.ClosableIterator;
import com.dataprofiler.util.objects.VersionedDatasetMetadata;
import com.dataprofiler.util.objects.MetadataVersionObject;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import com.dataprofiler.util.objects.DataScanSpec;
import com.dataprofiler.util.objects.VersionedDataScanSpec;
import com.dataprofiler.util.objects.ColumnCountIndexObject;
import com.dataprofiler.util.objects.ColumnCountObject;
import com.dataprofiler.util.objects.DataScanSpec.Type;
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
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import static helpers.AccumuloHelper.decodeForwardSlash;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;

@Authenticated
@AccumuloUserContext
public class AccumuloControllerV1 extends Controller {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private static final String QUERY_PARAM_DATA_SET = "dataset";
  private static final String QUERY_PARAM_TABLE = "table";
  private static final String QUERY_PARAM_COLUMN = "column";
  private static final String QUERY_PARAM_START_INDEX = "start";
  private static final String QUERY_PARAM_END_INDEX = "end";
  private static final String QUERY_PARAM_SORT = "sort";
  private static final String QUERY_PARAM_NORMALIZED = "normalized";
  private static final String QUERY_PARAM_WITH_VIS = "return_visibilities";

  private final Config config;
  private final AccumuloHelper accumulo;

  @Inject
  public AccumuloControllerV1(Config config) throws IOException {
    this.config = config;
    this.accumulo = new AccumuloHelper();
  }
  
  public Result datasets(Request req) {
    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    logger.debug("fetching datasets with context auths: " + context.getAuthorizations());
    MetadataVersionObject version = context.getCurrentMetadataVersion();
    logger.debug("metadata version is: " + version.getId());
    Map<String, VersionedMetadataObject> returnObj =
      stream(new VersionedMetadataObject().scanDatasets(context, version).spliterator(), false)
        .collect(toMap(VersionedMetadataObject::getDataset_name, identity()));

    return ok(Json.toJson(returnObj));
  }

  public Result tables(Request req, String dataset) {
    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    dataset = decodeForwardSlash(dataset);
    
    Map<String, VersionedMetadataObject> allTables =
      stream(
        new VersionedMetadataObject()
          .scanTables(context, context.getCurrentMetadataVersion(), dataset)
          .spliterator(),
        false)
          .collect(toMap(VersionedMetadataObject::getTable_name, identity()));
    
    return ok(Json.toJson(allTables));
  }

  public Result columns(Request req, String dataset, String table) {
    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    dataset = decodeForwardSlash(dataset);
    table = decodeForwardSlash(table);
    
    Map<String, VersionedMetadataObject> allColumns =
      stream(
        new VersionedMetadataObject()
          .scanColumns(context, context.getCurrentMetadataVersion(), dataset, table)
          .spliterator(),
        false)
          .collect(toMap(VersionedMetadataObject::getColumn_name, identity()));
    
      return ok(Json.toJson(allColumns));
  }

  // This is used for /v1/search and /search
  // No perfunctory breaking changes please!
  @BodyParser.Of(BodyParser.TolerantText.class)
  public Result search(Request req)
      throws IOException, VersionedDatasetMetadata.MissingMetadataException {
    String json = req.body().asText();

    DataScanSpec unversionedSpec = DataScanSpec.fromJson(json, Type.SEARCH);
    String errorString = unversionedSpec.checkRequiredFields();

    if (errorString != null) {
      return badRequest(errorString);
    }

    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    VersionedDataScanSpec spec = new VersionedDataScanSpec(context, unversionedSpec);

    Integer limit = spec.getLimit();
    List<ColumnCountIndexObject> result = new ArrayList<>();
    Set<String> intersection_curr = new HashSet<>();
    Set<String> intersection_prev = new HashSet<>();

    int numTerms = spec.getTerm().size();
    for (int i = 0; i < numTerms; i++) {

      // Clear any previous results
      result.clear();

      // Make all but the final term an exact match
      if (i < numTerms - 1) {
        spec.setBegins_with(false);
      }

      try (ClosableIterator<ColumnCountIndexObject> iter =
          new ColumnCountIndexObject().find(context, spec).closeableIterator()) {
        while (iter.hasNext() && result.size() < limit) {
          ColumnCountIndexObject indexObj = iter.next();

          String level = indexObj.getDataset();

          if (spec.getLevel() == VersionedDataScanSpec.Level.DATASET) {
            level = indexObj.getDataset() + indexObj.getTable();
          } else if (spec.getLevel() == VersionedDataScanSpec.Level.TABLE) {
            level = indexObj.getDataset() + indexObj.getTable() + indexObj.getColumn();
          }

          // Don't check the first iteration
          if (i == 0) {
            result.add(indexObj);
            intersection_curr.add(level);
          } else if (intersection_prev.contains(level)) {
            result.add(indexObj);
            intersection_curr.add(level);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        return internalServerError();
      }

      // After searching remove the first term
      spec.getTerm().remove(0);

      // Clear the previous intersection and populate with current values
      intersection_prev.clear();
      intersection_prev.addAll(intersection_curr);
      intersection_curr.clear();
    }

    return ok(Json.toJson(result));
  }

  public Result colCounts(Request req) {
    JsonNode json = req.body().asJson();
    String dataset = json.findPath(QUERY_PARAM_DATA_SET).textValue();
    String table = json.findPath(QUERY_PARAM_TABLE).textValue();
    String column = json.findPath(QUERY_PARAM_COLUMN).textValue();

    if (dataset == null || table == null || column == null) {
      return badRequest("Missing required parameters");
    }

    Long start = json.findPath(QUERY_PARAM_START_INDEX).asLong();
    Long end = json.findPath(QUERY_PARAM_END_INDEX).asLong();
    Long range = end - start;

    if (range < 1 || range > DataScanSpec.COLUMN_COUNT_LIMIT) {
      return badRequest(
          "start and end must be set and no greater than " + DataScanSpec.COLUMN_COUNT_LIMIT);
    }

    SortOrder sortOrder = SortOrder.getEnum(json.findPath(QUERY_PARAM_SORT).asText());
    Boolean normalize = json.findPath(QUERY_PARAM_NORMALIZED).asBoolean(false);
    Boolean includeColVis = json.findPath(QUERY_PARAM_WITH_VIS).asBoolean(false);

    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);

    MetadataVersionObject version = context.getCurrentMetadataVersion();
    VersionedMetadataObject colMetadata =
        new VersionedMetadataObject().fetchColumn(context, version, dataset, table, column);

    return ok(
        Json.toJson(
            new ColumnCountObject()
                .fetchColumnPaged(
                    context, colMetadata, start, end, sortOrder, normalize, includeColVis)));
  }
}