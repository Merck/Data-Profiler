package com.dataprofiler.datasetquality.provider;

/*-
 * 
 * dataprofiler-tools
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

import static java.lang.String.format;
import static java.lang.String.join;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import com.dataprofiler.datasetquality.MultisearchException;
import com.dataprofiler.datasetquality.model.multisearch.ColumnCountResult;
import com.dataprofiler.datasetquality.model.multisearch.GroupedColumnCountResult;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.iterators.ClosableIterator;
import com.dataprofiler.util.objects.ColumnCountIndexObject;
import com.dataprofiler.util.objects.DataScanSpec;
import com.dataprofiler.util.objects.VersionedDataScanSpec;
import com.dataprofiler.util.objects.VersionedDatasetMetadata.MissingMetadataException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultisearchProvider {
  private static final Logger logger = LoggerFactory.getLogger(MultisearchProvider.class);
  private static final String SEARCH_SYMBOL = "∈";

  protected Context context;

  public MultisearchProvider() {
    super();
  }

  public MultisearchProvider(Context context) {
    this();
    this.context = context;
  }

  /**
   * @param dataScanSpec
   * @return
   * @throws Exception
   */
  public List<GroupedColumnCountResult> multiSearch(DataScanSpec dataScanSpec)
      throws MultisearchException {
    ensureInitialized();
    if (isNull(dataScanSpec)) {
      throw new IllegalArgumentException("datascan spec is null!");
    }

    String validationErrors = dataScanSpec.checkRequiredMultiSearchFields();
    if (nonNull(validationErrors)) {
      throw new IllegalArgumentException(validationErrors);
    }

    Instant totalStart = now();
    try {
      VersionedDataScanSpec spec = new VersionedDataScanSpec(context, dataScanSpec);
      final int limit = spec.getLimit();
      VersionedDataScanSpec.Level groupingLevel = determineGroupLevel(spec);
      Map<String, Map<String, ColumnCountIndexObject>> termLevelCounts = new HashMap<>();
      Set<String> intersectionPrev = new HashSet<>();
      List<String> originalTerms =
          new ArrayList<>(spec.getTerm().subList(0, spec.getTerm().size() - 1));
      //      List<String> originalTerms =
      //          new LinkedHashSet<>(spec.getTerm().subList(0, spec.getTerm().size() - 1))
      //              .stream().map(el -> el.toLowerCase()).collect(toList());
      final int numTerms = spec.getTerm().size();

      for (int i = 0; i < numTerms; i++) {
        Instant start = now();
        // Make all but the final term an exact match
        if (i < numTerms - 1) {
          spec.setBegins_with(false);
        } else {
          // Honor the provided parameter if this is not a multi-search.
          if (numTerms > 1) {
            spec.setBegins_with(true);
          }
        }
        boolean isFirstIteration = i == 0;
        queryFirstTerm(spec, limit, termLevelCounts, intersectionPrev, isFirstIteration);
        Instant end = now();
        if (logger.isTraceEnabled()) {
          Duration duration = between(start, end);
          logger.trace(format("%s spec: %s time: %s", SEARCH_SYMBOL, spec, duration));
        }
        // After searching remove the first term
        spec.getTerm().remove(0);
      }

      final String finalLogMsg =
          "%s dataset: %s, terms: %s, count: %s, grouped results: %s, total time: %s";
      if (intersectionPrev.size() == 0) {
        Instant totalEnd = now();
        if (logger.isInfoEnabled()) {
          Duration duration = between(totalStart, totalEnd);
          logger.info(
              format(
                  finalLogMsg,
                  SEARCH_SYMBOL,
                  spec.getDataset(),
                  originalTerms,
                  0,
                  emptyList(),
                  duration));
        }
        return emptyList();
      }

      // We've seen the last term
      termLevelCounts.forEach(
          (k, v) -> v.keySet().removeIf(kInner -> !intersectionPrev.contains(kInner)));

      // This function updates termLevelCounts based on requested grouping Level.
      if (originalTerms.size() >= 2) {
        findMostGranularScope(originalTerms, termLevelCounts, groupingLevel);
      }

      Map<String, Map<String, ColumnCountIndexObject>> groupings =
          groupTermsByLevel(termLevelCounts, groupingLevel);

      if (logger.isDebugEnabled()) {
        logger.debug(
            format(
                "%s dataset: %s groupings: %s",
                SEARCH_SYMBOL, spec.getDataset(), groupings.keySet().size()));
      }
      List<GroupedColumnCountResult> groupedColumnCountResults =
          groupResults(originalTerms, numTerms, groupings);

      Instant totalEnd = now();
      if (logger.isInfoEnabled()) {
        Duration duration = between(totalStart, totalEnd);
        final long totalCount =
            groupedColumnCountResults.stream().mapToLong(el -> el.getCount()).sum();
        final long numResultElements = groupedColumnCountResults.size();
        logger.info(
            format(
                finalLogMsg,
                SEARCH_SYMBOL,
                spec.getDataset(),
                originalTerms,
                totalCount,
                numResultElements,
                duration));
      }
      return groupedColumnCountResults;
    } catch (MissingMetadataException e) {
      throw new MultisearchException(e);
    }
  }

  /**
   * query the first term in the spec, add to term level counts
   *
   * @param spec
   * @param limit
   * @param termLevelCounts
   * @param intersectionPrev
   * @throws MultisearchException
   */
  protected void queryFirstTerm(
      VersionedDataScanSpec spec,
      int limit,
      Map<String, Map<String, ColumnCountIndexObject>> termLevelCounts,
      Set<String> intersectionPrev,
      boolean isFirstIteration)
      throws MultisearchException {
    Set<String> intersectionCurr = new HashSet<>();
    List<ColumnCountIndexObject> results = new ArrayList<>();
    try (ClosableIterator<ColumnCountIndexObject> iter =
        new ColumnCountIndexObject().find(context, spec).closeableIterator()) {
      if (logger.isDebugEnabled()) {
        logger.debug(format("%s results size:%s, limit: %s", SEARCH_SYMBOL, results.size(), limit));
      }
      while (iter.hasNext() && results.size() < limit) {
        ColumnCountIndexObject indexObj = iter.next();
        if (logger.isDebugEnabled()) {
          logger.debug(
              format("%s firstIteration: %s, obj: %s", SEARCH_SYMBOL, isFirstIteration, indexObj));
        }
        String level = indexObj.getDataset();
        if (spec.getLevel() == VersionedDataScanSpec.Level.DATASET) {
          level = indexObj.getDataset() + indexObj.getTable();
        } else if (spec.getLevel() == VersionedDataScanSpec.Level.TABLE) {
          level = indexObj.getDataset() + indexObj.getTable() + indexObj.getColumn();
        }
        boolean updateTermCounts = false;
        // Don't check the first iteration
        if (isFirstIteration) {
          results.add(indexObj);
          intersectionCurr.add(level);
          updateTermCounts = true;
        } else if (intersectionPrev.contains(level)) {
          results.add(indexObj);
          intersectionCurr.add(level);
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
      }
    } catch (Exception e) {
      throw new MultisearchException(e);
    }
    // Clear the previous intersection and populate with current values
    intersectionPrev.clear();
    intersectionPrev.addAll(intersectionCurr);
    if (logger.isTraceEnabled()) {
      logger.trace(
          format(
              "%s intersectionPrev: %s termLevelCounts: %s",
              SEARCH_SYMBOL, intersectionPrev, termLevelCounts));
    }
  }

  protected List<GroupedColumnCountResult> groupResults(
      List<String> originalTerms,
      final int numTerms,
      Map<String, Map<String, ColumnCountIndexObject>> groupings) {

    if (logger.isTraceEnabled()) {
      logger.trace("original terms: " + originalTerms);
      logger.trace("groupings " + groupings);
    }
    // Create all possible search permutations
    List<String> searchPermutations =
        groupings.keySet().stream()
            .filter(p -> !originalTerms.contains(p))
            .map(lt -> join("", originalTerms) + lt)
            .collect(toList());

    if (logger.isTraceEnabled()) {
      logger.trace("search permutations: " + searchPermutations);
    }
    Map<String, ColumnCountResult> merged = new HashMap<>();

    // Merge search permutation results and update stats within elements on collisions
    originalTerms.forEach(
        (searchTerm) -> {
          Map<String, ColumnCountIndexObject> indexLevels = groupings.get(searchTerm);
          indexLevels.forEach(
              (level, index) -> {
                searchPermutations.forEach(
                    last -> {
                      merged.merge(
                          last + level, new ColumnCountResult(index), ColumnCountResult::merge);
                    });
              });
          // Remove the first n-1 search terms
          groupings.remove(searchTerm);
        });

    if (logger.isTraceEnabled()) {
      logger.trace("merged " + merged);
    }
    // Everything left over (due to the last search term) gets included....
    String prefix = join("", originalTerms);
    groupings.forEach(
        (term, v) -> {
          v.forEach(
              (level, index) ->
                  merged.merge(
                      prefix + term + level,
                      new ColumnCountResult(index),
                      ColumnCountResult::merge));
        });

    if (logger.isTraceEnabled()) {
      logger.trace("groupings: " + groupings);
    }

    // Collect search permutation mappings
    Map<String, List<ColumnCountResult>> termGrouping = new HashMap<>();
    merged.forEach(
        (termAndLevel, v) -> {
          List<String> values = v.getValues();
          List<String> valuesLower = values.stream().map(String::toLowerCase).collect(toList());
          termGrouping.merge(join("||", valuesLower), singletonList(v), this::merge);
        });

    termGrouping.entrySet().removeIf(e -> e.getKey().split("\\|\\|").length != numTerms);

    if (logger.isTraceEnabled()) {
      logger.trace("term groupings " + termGrouping);
    }

    List<GroupedColumnCountResult> groupedResults = new ArrayList<>();
    termGrouping.forEach(
        (k, v) -> {
          GroupedColumnCountResult groupedResult = new GroupedColumnCountResult();
          List<String> values = new ArrayList<>();
          asList(k.split("\\|\\|")).forEach(values::add);
          groupedResult.setValues(values);
          long total = v.stream().flatMap(el -> el.getCounts().stream()).mapToLong(i -> i).sum();
          groupedResult.setCount(total);
          List<ColumnCountResult> elements = groupedResult.getElements();
          if (isNull(elements)) {
            elements = new ArrayList<>();
          }
          elements.addAll(v);
          groupedResult.setElements(elements);
          groupedResults.add(groupedResult);
        });

    if (logger.isTraceEnabled()) {
      logger.trace("group results " + groupedResults);
    }
    return groupedResults;
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

  private DataScanSpec.Level determineGroupLevel(DataScanSpec spec) {
    // Override search level if a specific dataset is provided
    DataScanSpec.Level groupingLevel = spec.getGroupingLevel();

    // If a dataset or dataset:table is specified,
    if (spec.getDataset() != null) {
      spec.setLevel(VersionedDataScanSpec.Level.DATASET);
    }
    return groupingLevel;
  }

  private Set<String> findResultsAtLevel(
      Map<String, Map<String, ColumnCountIndexObject>> termLevelCounts,
      List<String> origTerms,
      DataScanSpec.Level groupingLevel) {

    Set<String> currIntersection = new HashSet<>();
    Set<String> prevIntersection = new HashSet<>();

    boolean first = true;
    List<String> exactTerms = origTerms.subList(0, origTerms.size() - 1);
    for (String term : exactTerms) {
      Map<String, ColumnCountIndexObject> levels = termLevelCounts.get(term);
      if (first) {
        levels.values().forEach(v -> prevIntersection.add(buildGroupingLevel(v, groupingLevel)));
        first = false;
      } else {
        levels.values().forEach(v -> currIntersection.add(buildGroupingLevel(v, groupingLevel)));
        prevIntersection.retainAll(currIntersection);
      }
    }

    return prevIntersection;
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

  public <T> List<T> merge(List<T> list1, List<T> list2) {
    return concat(list1.stream(), list2.stream()).collect(toList());
  }

  protected void ensureInitialized() {
    if (isNull(context)) {
      throw new IllegalStateException("context is null");
    }
  }

  public Context getContext() {
    return context;
  }

  public void setContext(Context context) {
    this.context = context;
  }
}
