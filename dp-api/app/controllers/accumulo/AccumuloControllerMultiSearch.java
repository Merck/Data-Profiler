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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.iterators.ClosableIterator;
import com.dataprofiler.util.objects.ColumnCountIndexObject;
import com.dataprofiler.util.objects.DataScanSpec;
import com.dataprofiler.util.objects.VersionedDataScanSpec;
import com.dataprofiler.util.objects.DataScanSpec.Type;
import com.dataprofiler.util.objects.VersionedDatasetMetadata;
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
import java.util.Set;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import java.util.stream.Stream;
import static java.util.stream.Collectors.toList;

@Authenticated
@AccumuloUserContext
public class AccumuloControllerMultiSearch extends Controller {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private static final ObjectMapper mapper = new ObjectMapper();
  private final Config config;
  private final AccumuloHelper accumulo;

  @Inject
  public AccumuloControllerMultiSearch(Config config) throws IOException {
    this.config = config;
    this.accumulo = new AccumuloHelper();
  }

  private static JsonNode buildResultElement(ColumnCountIndexObject index) {
    ObjectNode element = mapper.createObjectNode();
    element.put("dataset", index.getDataset());
    element.put("table", index.getTable());
    element.put("tableId", index.getTableId());
    element.put("column", index.getColumn());
    element.set("value", mapper.createArrayNode().add(index.getValue()));
    element.set("count", mapper.createArrayNode().add(index.getCount()));
    return element;
  }

  public static <T> List<T> merge(List<T> list1, List<T> list2) {
    return Stream.concat(list1.stream(), list2.stream()).collect(toList());
  }

  public static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {
    Iterator<String> fieldNames = updateNode.fieldNames();

    while (fieldNames.hasNext()) {
      String fieldName = fieldNames.next();

      JsonNode jsonNode = mainNode.get(fieldName);

      // If field exists and is an embedded object
      if (jsonNode != null && jsonNode.isObject()) {
        merge(jsonNode, updateNode.get(fieldName));
      } else if (jsonNode != null && jsonNode.isArray()) {
        ((ArrayNode) jsonNode).add(updateNode.get(fieldName).get(0));
      }
    }
    return mainNode;
  }

  private Map<String, Map<String, ColumnCountIndexObject>> groupTermsByLevel(
      Map<String, Map<String, ColumnCountIndexObject>> termLevelCounts,
      VersionedDataScanSpec.Level level) {
    Map<String, Map<String, ColumnCountIndexObject>> result = new HashMap<>();

    termLevelCounts.forEach(
        (k, v) -> {
          Map<String, ColumnCountIndexObject> grouping = new HashMap<>();

          v.values()
              .forEach(
                  c -> {
                    String groupLevel = c.getDataset();
                    if (level == VersionedDataScanSpec.Level.DATASET) {
                      groupLevel = c.getDataset();
                      c.setTableId(null);
                      c.setTable(null);
                      c.setColumn(null);
                    } else if (level == VersionedDataScanSpec.Level.TABLE) {
                      groupLevel = c.getDataset() + c.getTable();
                      c.setColumn(null);
                    } else if (level == VersionedDataScanSpec.Level.COLUMN) {
                      groupLevel = c.getDataset() + c.getTable() + c.getColumn();
                    }

                    if (grouping.containsKey(groupLevel)) {
                      ColumnCountIndexObject index = grouping.get(groupLevel);
                      index.setCount(c.getCount() + index.getCount());
                    } else {
                      grouping.put(groupLevel, c);
                    }
                  });
          result.put(k, grouping);
        });

    return result;
  }

  private Set<String> findResultsAtLevel(
      Map<String, Map<String, ColumnCountIndexObject>> termLevelCounts,
      List<String> origTerms,
      DataScanSpec.Level groupingLevel) {

    Set<String> curr_intersection = new HashSet<>();
    Set<String> prev_intersection = new HashSet<>();

    boolean first = true;
    List<String> exactTerms = origTerms.subList(0, origTerms.size() - 1);
    for (String term : exactTerms) {
      Map<String, ColumnCountIndexObject> levels = termLevelCounts.get(term);
      if (first) {
        levels.values().forEach(v -> prev_intersection.add(buildGroupingLevel(v, groupingLevel)));
        first = false;
      } else {
        levels.values().forEach(v -> curr_intersection.add(buildGroupingLevel(v, groupingLevel)));
        prev_intersection.retainAll(curr_intersection);
      }
    }

    return prev_intersection;
  }

  private String buildGroupingLevel(ColumnCountIndexObject index, DataScanSpec.Level level) {
    String key = null;
    if (level == DataScanSpec.Level.COLUMN) {
      key = index.getDataset() + index.getTable() + index.getColumn();
    } else if (level == DataScanSpec.Level.TABLE) {
      key = index.getDataset() + index.getTable();
    }
    // Default
    if (key == null) {
      key = index.getDataset();
    }
    return key;
  }

  private void findMostGranularScope(
      List<String> origTerms,
      Map<String, Map<String, ColumnCountIndexObject>> termLevelCounts,
      DataScanSpec.Level groupingLevel) {

    // Assume column level. Traverse each value map and intersect at this level
    if (groupingLevel == DataScanSpec.Level.COLUMN) {
      Set<String> columnIntersection =
          findResultsAtLevel(termLevelCounts, origTerms, DataScanSpec.Level.COLUMN);
      if (columnIntersection.size() > 0) {
        // Filter out out negative matches not at our current level
        termLevelCounts
            .values()
            .forEach(
                e -> {
                  e.values()
                      .removeIf(
                          v -> !columnIntersection.contains(buildGroupingLevel(v, groupingLevel)));
                });
      }
    } else if (groupingLevel == DataScanSpec.Level.TABLE) {
      Set<String> tableIntersection =
          findResultsAtLevel(termLevelCounts, origTerms, DataScanSpec.Level.TABLE);
      if (tableIntersection.size() > 0) {
        // Filter out out negative matches not at our current level
        termLevelCounts
            .values()
            .forEach(
                e -> {
                  e.values()
                      .removeIf(
                          v -> !tableIntersection.contains(buildGroupingLevel(v, groupingLevel)));
                });
      }
    }
  }

  public DataScanSpec.Level determineGroupLevel(DataScanSpec spec) {
    // Override search level if a specific dataset is provided
    DataScanSpec.Level groupingLevel = spec.getGroupingLevel();

    // If a dataset or dataset:table is specified,
    if (spec.getDataset() != null) {
      spec.setLevel(VersionedDataScanSpec.Level.DATASET);
    }
    return groupingLevel;
  }

  @BodyParser.Of(BodyParser.TolerantText.class)
  public Result multiSearch(Request req)
      throws IOException, VersionedDatasetMetadata.MissingMetadataException {
    String json = req.body().asText();
    DataScanSpec unversionedSpec = DataScanSpec.fromJson(json, Type.SEARCH);
    String errorString = unversionedSpec.checkRequiredMultiSearchFields();
    if (errorString != null) {
      return badRequest(errorString);
    }
    Context context = (Context) req.attrs().get(RulesOfUseHelper.USER_CONTEXT_TYPED_KEY);
    VersionedDataScanSpec spec = new VersionedDataScanSpec(context, unversionedSpec);

    Integer limit = spec.getLimit();

    VersionedDataScanSpec.Level groupingLevel = determineGroupLevel(spec);

    Map<String, Map<String, ColumnCountIndexObject>> termLevelCounts = new HashMap<>();
    List<ColumnCountIndexObject> result = new ArrayList<>();
    Set<String> intersection_curr = new HashSet<>();
    Set<String> intersection_prev = new HashSet<>();

    List<String> originalTerms =
        new ArrayList<>(spec.getTerm().subList(0, spec.getTerm().size() - 1));

    int numTerms = spec.getTerm().size();
    for (int i = 0; i < numTerms; i++) {

      // Clear any previous results
      result.clear();

      // Make all but the final term an exact match
      if (i < numTerms - 1) {
        spec.setBegins_with(false);
      } else {
        // Honor the provided parameter if this is not a multi-search.
        if (numTerms > 1) {
          spec.setBegins_with(true);
        }
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

          Boolean updateTermCounts = false;

          // Don't check the first iteration
          if (i == 0) {
            result.add(indexObj);
            intersection_curr.add(level);
            updateTermCounts = true;
          } else if (intersection_prev.contains(level)) {
            result.add(indexObj);
            intersection_curr.add(level);
            updateTermCounts = true;
          }

          // Accumulate counts - only add entries that have intersected level-wise
          if (updateTermCounts) {
            // Retrieve levels for term
            String currentTerm = indexObj.getValue().toLowerCase();
            Map<String, ColumnCountIndexObject> levelCounts = new HashMap<>();
            if (termLevelCounts.containsKey(currentTerm)) {
              levelCounts = termLevelCounts.get(currentTerm);
              if (levelCounts.containsKey(level)) {
                ColumnCountIndexObject prevIndex = levelCounts.get(level);
                prevIndex.setCount(prevIndex.getCount() + indexObj.getCount());
                levelCounts.put(level, prevIndex);
              } else {
                levelCounts.put(level, indexObj);
              }
            } else {
              levelCounts.put(level, indexObj);
              termLevelCounts.put(currentTerm, levelCounts);
            }
          }
        } // end while
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

    // No results available
    if (intersection_prev.size() == 0) {
      return ok(Json.toJson(result));
    }

    // We've seen the last term
    termLevelCounts.forEach(
        (k, v) -> v.keySet().removeIf(kInner -> !intersection_prev.contains(kInner)));

    // This function updates termLevelCounts based on requested grouping Level.
    if (originalTerms.size() >= 2) {
      findMostGranularScope(originalTerms, termLevelCounts, groupingLevel);
    }

    Map<String, Map<String, ColumnCountIndexObject>> groupings =
        groupTermsByLevel(termLevelCounts, groupingLevel);

    // Create all possible search permutations
    List<String> searchPermutations =
        groupings.keySet().stream()
            .filter(p -> !originalTerms.contains(p))
            .map(lt -> String.join("", originalTerms) + lt)
            .collect(toList());

    Map<String, JsonNode> merged = new HashMap<>();

    // Merge search permutation results and update stats within elements on collisions
    originalTerms.forEach(
        (searchTerm) -> {
          Map<String, ColumnCountIndexObject> indexLevels = groupings.get(searchTerm);
          indexLevels.forEach(
              (level, index) -> {
                searchPermutations.forEach(
                    last -> {
                      merged.merge(
                          last + level, buildResultElement(index), AccumuloControllerMultiSearch::merge);
                    });
              });
          // Remove the first n-1 search terms
          groupings.remove(searchTerm);
        });

    // Everything left over (due to the last search term) gets included....
    String prefix = String.join("", originalTerms);
    groupings.forEach(
        (term, v) -> {
          v.forEach(
              (level, index) ->
                  merged.merge(
                      prefix + term + level, buildResultElement(index), AccumuloControllerMultiSearch::merge));
        });

    // Collect search permutation mappings
    Map<String, List<JsonNode>> termGrouping = new HashMap<>();

    merged.forEach(
        (termAndLevel, v) -> {
          try {
            String[] values = mapper.readValue(v.get("value").toString(), String[].class);
            List<String> valuesLower =
                Arrays.stream(values).map(String::toLowerCase).collect(toList());
            termGrouping.merge(
                String.join("||", valuesLower),
                Collections.singletonList(v),
                AccumuloControllerMultiSearch::merge);
          } catch (IOException e) {
            e.printStackTrace();
          }
        });

    termGrouping.entrySet().removeIf(e -> e.getKey().split("\\|\\|").length != numTerms);

    ArrayNode groupedResult = mapper.createArrayNode();
    termGrouping.forEach(
        (k, v) -> {
          ObjectNode valueNode = mapper.createObjectNode();
          ArrayNode values = mapper.createArrayNode();
          Arrays.asList(k.split("\\|\\|")).forEach(values::add);
          valueNode.set("value", values);
          int totalCount = 0;
          Iterator<JsonNode> itr = v.iterator();
          while (itr.hasNext()) {
            Iterator<JsonNode> numItr = itr.next().get("count").elements();
            while (numItr.hasNext()) {
              totalCount += numItr.next().asInt();
            }
          }
          valueNode.put("count", totalCount);
          valueNode.set("elements", mapper.createArrayNode().addAll(v));
          groupedResult.add(valueNode);
        });

    return ok(Json.toJson(groupedResult));
  }
}