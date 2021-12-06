package com.dataprofiler.datasetperformance;

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
import static java.time.Duration.between;
import static java.time.Instant.now;

import com.dataprofiler.datasetperformance.model.DailyStat;
import com.dataprofiler.datasetperformance.model.DatasetPerformance;
import com.dataprofiler.datasetperformance.model.TrendStat;
import com.dataprofiler.matomo.MatomoPerformanceProvider;
import com.dataprofiler.matomo.response.MatomoResponseElement;
import com.dataprofiler.provider.DatasetMetadataProvider;
import com.dataprofiler.rulesofuse.response.RouUserData;
import com.dataprofiler.util.objects.VersionedDatasetMetadata;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 1 task/thread per dataset */
public class PerformanceGenerationTask implements Callable<DatasetPerformance> {
  private static final Logger logger = LoggerFactory.getLogger(PerformanceGenerationTask.class);
  private static final String trackingStart = "2021-05-01"; // month which tracking was enabled

  private final String dataset;
  private final int daysAgo;
  private final DatasetPerformanceProvider dataPerformanceProvider;
  private final DatasetMetadataProvider datasetMetadataProvider;
  private final MatomoPerformanceProvider matomoPerformanceProvider;
  private final RouUserData contractors;
  private final Map<String, List<MatomoResponseElement>> matomoCache;

  public PerformanceGenerationTask(
      DatasetPerformanceProvider dataPerformanceProvider,
      DatasetMetadataProvider datasetMetadataProvider,
      MatomoPerformanceProvider matomoPerformanceProvider,
      RouUserData contractors,
      Map<String, List<MatomoResponseElement>> matomoCache,
      String dataset,
      int daysAgo) {
    this.dataPerformanceProvider = dataPerformanceProvider;
    this.datasetMetadataProvider = datasetMetadataProvider;
    this.matomoPerformanceProvider = matomoPerformanceProvider;
    this.contractors = contractors;
    this.matomoCache = matomoCache;
    this.dataset = dataset;
    this.daysAgo = daysAgo;
  }

  @Override
  public DatasetPerformance call() throws DatasetPerformanceException {
    return executeDataset(dataset);
  }

  protected DatasetPerformance executeDataset(String dataset) throws DatasetPerformanceException {
    if (logger.isDebugEnabled()) {
      logger.debug(format("building performance numbers for %s", dataset));
    }

    Instant start = now();

    VersionedDatasetMetadata metadata = datasetMetadataProvider.fetchDatasetMetadata(dataset);
    DatasetPerformance performance =
        dataPerformanceProvider.calculateRouPerformance(
            metadata, contractors.getData().getImmutableUsersWithAttribute());
    TrendStat searchAppearance = new TrendStat();
    TrendStat downloads = new TrendStat();
    ZoneOffset zone = ZoneId.systemDefault().getRules().getOffset(now());
    LocalDateTime now = LocalDateTime.now(zone);
    LocalDate today = now.toLocalDate();
    LocalDate yesterday = now.minusDays(1).toLocalDate();
    List<DailyStat> chartDataStats = new ArrayList<>();

    for (Entry<String, List<MatomoResponseElement>> entry : matomoCache.entrySet()) {
      String k = entry.getKey();
      DailyStat dailyStat = buildDailyStatByWeek(k);
      chartDataStats.add(dailyStat);
    }

    // from today
    downloads.setAllTime(fetchAllDownloads(today, LocalDate.parse(trackingStart)));
    downloads.setLastSevenFromToday(fetchAllDownloads(today, today.minusDays(7)));

    searchAppearance.setAllTime(fetchAllSearchAppearances(today, LocalDate.parse(trackingStart)));
    searchAppearance.setLastSevenFromToday(fetchAllSearchAppearances(today, today.minusDays(7)));

    // from yesterday
    downloads.setLastSevenFromYesterday(fetchAllDownloads(yesterday, yesterday.minusDays(7)));

    searchAppearance.setLastSevenFromYesterday(
        fetchAllSearchAppearances(yesterday, yesterday.minusDays(7)));

    performance.setDataset(dataset);
    performance.setChartData(chartDataStats);
    performance.setSearchAppearanceData(searchAppearance);
    performance.setDownloadData(downloads);

    // NOTE: daysAgo param is no longer utilized here
    // TODO: unit test for findByDataset/calculateDailyStat

    if (logger.isDebugEnabled()) {
      logger.debug(format("dataset: %s performance: %s", dataset, performance));
    }

    // write data to dataset properties
    //    boolean success = updateDatasetProperties(performance, metadata);

    Instant end = now();
    if (logger.isInfoEnabled()) {
      Duration duration = between(start, end);
      logger.info(
          format("all performance metrics generated for dataset: %s time: %s", dataset, duration));
    }

    return performance;
  }

  protected DailyStat buildDailyStat(LocalDate currentDate) throws DatasetPerformanceException {
    Instant start = now();
    List<MatomoResponseElement> dailyMatomoElements =
        matomoPerformanceProvider.fetchChartData(currentDate);

    List<MatomoResponseElement> dailyDatasetMatomoElements =
        matomoPerformanceProvider.filterActionPageByDataset(dailyMatomoElements, dataset);

    if (logger.isDebugEnabled()) {
      logger.debug(
          format("found chart data matomo elements for dataset:%s date:%s", dataset, currentDate));
    }

    if (logger.isTraceEnabled()) {
      dailyDatasetMatomoElements.forEach(el -> logger.trace(el.toString()));
    }

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    String formatDateTime = currentDate.format(formatter);
    long sum =
        dailyDatasetMatomoElements.stream()
            .filter(el -> Objects.nonNull(el.getNbUniqVisitors()))
            .mapToLong(MatomoResponseElement::getNbUniqVisitors)
            .sum();
    DailyStat dailyStat = new DailyStat();
    dailyStat.setDay(formatDateTime);
    dailyStat.setViews(sum);

    Instant end = now();
    if (logger.isDebugEnabled()) {
      Duration duration = between(start, end);
      logger.debug(format("%s date:%s views:%s time:%s", dataset, formatDateTime, sum, duration));
    }
    return dailyStat;
  }

  protected DailyStat buildDailyStatByWeek(String week) {
    Instant start = now();
    List<MatomoResponseElement> dailyMatomoElements = matomoCache.get(week);

    List<MatomoResponseElement> dailyDatasetMatomoElements =
        matomoPerformanceProvider.filterActionPageByDataset(dailyMatomoElements, dataset);

    if (logger.isDebugEnabled()) {
      logger.debug(
          format("found chart data matomo elements for dataset:%s week:%s", dataset, week));
    }

    if (logger.isTraceEnabled()) {
      dailyDatasetMatomoElements.forEach(el -> logger.trace(el.toString()));
    }

    long sum =
        dailyDatasetMatomoElements.stream()
            .filter(el -> Objects.nonNull(el.getSumDailyNbUniqVisitors()))
            .mapToLong(MatomoResponseElement::getSumDailyNbUniqVisitors)
            .sum();
    DailyStat dailyStat = new DailyStat();
    dailyStat.setDay(week);
    dailyStat.setViews(sum);

    Instant end = now();
    if (logger.isDebugEnabled()) {
      Duration duration = between(start, end);
      logger.debug(format("%s week:%s views:%s time:%s", dataset, week, sum, duration));
    }
    return dailyStat;
  }

  protected long fetchAllSearchAppearances(LocalDate currentDate, LocalDate startDate)
      throws DatasetPerformanceException {
    List<MatomoResponseElement> dailyMatomoElements =
        matomoPerformanceProvider.fetchSearchAppearances(currentDate, startDate);

    List<MatomoResponseElement> dailyDatasetMatomoElements =
        matomoPerformanceProvider.filterLabelByDataset(dailyMatomoElements, dataset);

    if (logger.isInfoEnabled()) {
      logger.info(
          format(
              "found search appearance matomo elements for dataset:%s date:%s",
              dataset, currentDate));
    }

    if (logger.isTraceEnabled()) {
      dailyDatasetMatomoElements.forEach(el -> logger.trace(el.toString()));
    }

    return dailyDatasetMatomoElements.stream()
        .mapToLong(MatomoResponseElement::getNbImpressions)
        .sum();
  }

  protected long fetchAllDownloads(LocalDate currentDate, LocalDate startDate)
      throws DatasetPerformanceException {
    List<MatomoResponseElement> dailyMatomoElements =
        matomoPerformanceProvider.fetchDownloads(currentDate, startDate);

    List<MatomoResponseElement> dailyDatasetMatomoElements =
        matomoPerformanceProvider.filterLabelByDataset(dailyMatomoElements, dataset);

    if (logger.isInfoEnabled()) {
      logger.info(
          format("found download matomo elements for dataset:%s date:%s", dataset, currentDate));
    }

    if (logger.isTraceEnabled()) {
      dailyDatasetMatomoElements.forEach(el -> logger.trace(el.toString()));
    }

    return dailyDatasetMatomoElements.stream()
        .filter(el -> Objects.nonNull(el.getNbEvents()))
        .mapToLong(MatomoResponseElement::getNbEvents)
        .sum();
  }
}
