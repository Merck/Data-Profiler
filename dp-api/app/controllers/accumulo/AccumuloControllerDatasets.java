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
import com.dataprofiler.util.Context;
import com.dataprofiler.util.iterators.ClosableIterator;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import com.dataprofiler.util.objects.DataScanSpec;
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
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiFunction;

import static java.util.stream.Collectors.toList;

@Authenticated
@AccumuloUserContext
public class AccumuloControllerDatasets extends Controller {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Config config;
  private final AccumuloHelper accumulo;

  @Inject
  public AccumuloControllerDatasets(Config config) throws IOException {
    this.config = config;
    this.accumulo = new AccumuloHelper();
  }

  private List<Map<String, String>> iteratorToMap(
    ClosableIterator<VersionedMetadataObject> iter, final DataScanSpec spec) throws Exception {
    // check for field match, ignore on null or empty
    BiFunction<String, String, Boolean> isFieldMatch =
      (a, b) -> {
        boolean aIsDefined = a != null && !a.isEmpty();
        return aIsDefined ? a.trim().equalsIgnoreCase(b != null ? b.trim() : "") : Boolean.TRUE;
      };
    // does the spec match or are the spec filters null; then return true
    BiFunction<DataScanSpec, VersionedMetadataObject, Boolean> isSpecEmptyOrMatch =
      (datascanSpec, metadataObject) -> {
        String dataset = datascanSpec.getDataset();
        String table = datascanSpec.getTable();
        String column = datascanSpec.getColumn();
        if (dataset != null && table != null && column != null) {
          return isFieldMatch.apply(dataset, metadataObject.dataset_name)
            && isFieldMatch.apply(table, metadataObject.table_name)
            && isFieldMatch.apply(column, metadataObject.column_name);
        } else if (dataset != null && table != null) {
          return isFieldMatch.apply(dataset, metadataObject.dataset_name)
            && isFieldMatch.apply(table, metadataObject.table_name);
        } else if (dataset != null) {
           return isFieldMatch.apply(dataset, metadataObject.dataset_name);
        } else {
           return Boolean.TRUE;
        }
      };
    List<Map<String, String>> results = new ArrayList<>();
    int limit = spec.getLimit();
    while (iter.hasNext()) {
      if (limit > 0 && results.size() >= limit) {
        iter.close();
        break;
      }

      VersionedMetadataObject m = iter.next();
      if (!isSpecEmptyOrMatch.apply(spec, m)) {
        continue;
      }
      HashMap<String, String> result = new HashMap<>();
      result.put("dataset", m.dataset_name);
      result.put("table", m.table_name);
      result.put("column", m.column_name);
      results.add(result);
    }
    return results;
  }

  @BodyParser.Of(BodyParser.TolerantText.class)
  public Result searchDatasetNames(Request req) throws IOException {
    String json = req.body().asText();
    DataScanSpec spec = DataScanSpec.fromJson(json, Type.SEARCH);
    String errorString = spec.checkRequiredFields();
    if (errorString != null) {
      return badRequest(errorString);
    }
    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    Boolean startsWith = spec.getBegins_with();
    List<String> terms = spec.getTerm().stream().map(String::toLowerCase).collect(toList());
    List<Map<String, String>> results = new ArrayList<>();
    try (ClosableIterator<VersionedMetadataObject> iter =
      new VersionedMetadataObject()
        .searchDatasetNames(context, context.getCurrentMetadataVersion(), terms, startsWith)
        .setBatch(true)
        .closeableIterator()) {
      results = this.iteratorToMap(iter, spec);
    } catch (Exception e) {
      e.printStackTrace();
      return internalServerError();
    }
    return ok(Json.toJson(results));
  }
}