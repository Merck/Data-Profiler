package com.dataprofiler.datasetperformance.cli;

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

import static com.dataprofiler.provider.MemoryUsageReportProvider.memoryUsage;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import com.dataprofiler.datasetperformance.DatasetPerformanceException;
import com.dataprofiler.datasetperformance.DatasetPerformanceProvider;
import com.dataprofiler.datasetperformance.PerformanceGenerationTask;
import com.dataprofiler.datasetperformance.PerformanceSaveTask;
import com.dataprofiler.datasetperformance.config.DatasetPerformanceConfig;
import com.dataprofiler.datasetperformance.model.DailyStat;
import com.dataprofiler.datasetperformance.model.DatasetPerformance;
import com.dataprofiler.datasetperformance.model.status.PerformanceTaskStatus;
import com.dataprofiler.datasetperformance.spec.DatasetPerformanceSpec;
import com.dataprofiler.matomo.MatomoException;
import com.dataprofiler.matomo.MatomoHelper;
import com.dataprofiler.matomo.MatomoPerformanceProvider;
import com.dataprofiler.matomo.response.MatomoResponseElement;
import com.dataprofiler.provider.DatasetMetadataProvider;
import com.dataprofiler.rulesofuse.RulesOfUseException;
import com.dataprofiler.rulesofuse.RulesOfUseHelper;
import com.dataprofiler.rulesofuse.response.RouUserData;
import com.dataprofiler.slack.SlackApiException;
import com.dataprofiler.slack.model.Attachment;
import com.dataprofiler.slack.model.Attachment.AttachmentBuilder;
import com.dataprofiler.slack.model.Field;
import com.dataprofiler.slack.model.SlackMessage;
import com.dataprofiler.slack.model.SlackMessage.SlackMessageBuilder;
import com.dataprofiler.slack.provider.SlackProvider;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.VersionedDatasetMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * cli to kick off the performance processing for all datasets configure max threads to process the
 * datasets concurrently - one thread per dataset
 */
public class DatasetPerformanceCli {
  private static final Logger logger = LoggerFactory.getLogger(DatasetPerformanceCli.class);
  // careful not to DOS ROU when changing threads
  // if you do change MAX_THREADS, tune the resources section in the helm chart
  private static final int MAX_THREADS = 12;

  private DatasetPerformanceConfig config;
  private DatasetPerformanceSpec spec;
  private Context context;
  private DatasetPerformanceProvider dataPerformanceProvider;
  private DatasetMetadataProvider datasetMetadataProvider;
  private MatomoPerformanceProvider matomoPerformanceProvider;
  private VersionedDatasetMetadata metadata;
  private ObjectMapper objectMapper;
  private ExecutorService executorService;
  private RulesOfUseHelper rouHelper;
  private MatomoHelper matomoHelper;
  private RouUserData contractors;
  private Map<String, List<MatomoResponseElement>> matomoCache;
  private Duration totalDuration = Duration.ZERO;
  private SlackProvider slackProvider;

  public DatasetPerformanceCli() {
    super();
  }

  public static void main(String[] args) {
    try {
      DatasetPerformanceCli exporter = new DatasetPerformanceCli();
      exporter.execute(args);
    } catch (DatasetPerformanceException e) {
      e.printStackTrace();
      logger.warn(e.toString());
      System.exit(1);
    }
    System.exit(0);
  }

  public void init(String[] args) throws DatasetPerformanceException {
    try {
      logger.debug("initializing...");
      config = parseJobArgs(args);
      spec = readSpec(config);
      if (logger.isDebugEnabled()) {
        logger.debug("config: " + config);
        logger.debug("spec: " + spec.toString());
      }
      //      context = new DPSparkContext(config, "Dataset Performance Job");
      context = new Context(config);
      if (logger.isDebugEnabled()) {
        logger.debug("context: " + context);
      }
      objectMapper = new ObjectMapper();
      datasetMetadataProvider = new DatasetMetadataProvider();
      datasetMetadataProvider.setContext(context);
      datasetMetadataProvider.setObjectMapper(objectMapper);
      dataPerformanceProvider = new DatasetPerformanceProvider();
      String rouApiUrl = config.rulesOfUseApiPath;
      String rouApiKey = config.rulesOfUseApiKey;
      rouHelper = new RulesOfUseHelper(rouApiUrl, rouApiKey);
      String matomoApiKey = config.matomoApiKey;
      String analyticsApiSiteId = config.analyticsApiSiteId;
      String analyticsUiSiteId = config.analyticsUiSiteId;
      matomoHelper = new MatomoHelper(matomoApiKey, analyticsApiSiteId, analyticsUiSiteId);
      dataPerformanceProvider.setRouHelper(rouHelper);
      dataPerformanceProvider.setMatomoHelper(matomoHelper);
      matomoPerformanceProvider = new MatomoPerformanceProvider();
      matomoPerformanceProvider.setMatomoHelper(matomoHelper);
      executorService = Executors.newFixedThreadPool(MAX_THREADS);
      slackProvider = new SlackProvider(config.slackWebhook);
      slackProvider.setObjectMapper(objectMapper);
    } catch (Exception e) {
      throw new DatasetPerformanceException(e);
    }
  }

  public void execute(String[] args) throws DatasetPerformanceException {
    try {
      init(args);
      sendStatusToSlack(executeSpec(spec));
      logger.info(memoryUsage());
    } catch (SlackApiException e) {
      throw new DatasetPerformanceException(e);
    } finally {
      if (nonNull(executorService)) {
        executorService.shutdown();
      }
    }
  }

  protected List<PerformanceTaskStatus> executeSpec(DatasetPerformanceSpec spec)
      throws DatasetPerformanceException {
    if (!checkInitialized()) {
      throw new IllegalStateException();
    }

    Instant start = now();
    List<PerformanceTaskStatus> status = executeTasks(buildTasks(spec));
    Instant end = now();
    if (logger.isInfoEnabled()) {
      totalDuration = between(start, end);
      logger.info(format("spec executed time: %s", totalDuration));
    }
    return status;
  }

  protected List<PerformanceGenerationTask> buildTasks(DatasetPerformanceSpec spec)
      throws DatasetPerformanceException {
    // build contractor name cache

    initContractorCache();

    // build matomo cache
    initMatomoCache();

    final int daysAgo = spec.getDaysAgo() > 0 ? spec.getDaysAgo() : 14;
    Set<String> datasets =
        spec.isAllDatasets() ? datasetMetadataProvider.fetchAllDatasets() : spec.getDatasets();
    return datasets.stream()
        .map(dataset -> buildGenerationTask(dataset, daysAgo))
        .collect(toList());
  }

  protected PerformanceGenerationTask buildGenerationTask(String dataset, int daysAgo) {
    return new PerformanceGenerationTask(
        dataPerformanceProvider,
        datasetMetadataProvider,
        matomoPerformanceProvider,
        contractors,
        matomoCache,
        dataset,
        daysAgo);
  }

  protected PerformanceSaveTask buildSaveTask(DatasetPerformance performance) {
    return new PerformanceSaveTask(datasetMetadataProvider, objectMapper, performance);
  }

  protected List<PerformanceTaskStatus> executeTasks(
      List<PerformanceGenerationTask> performanceTasks) throws DatasetPerformanceException {
    if (isNull(performanceTasks)) {
      return emptyList();
    }
    long successCount = 0;
    try {
      List<PerformanceTaskStatus> status = executeSaveTasks(generateToSaveTasks(performanceTasks));
      successCount = status.stream().filter(PerformanceTaskStatus::isSuccess).count();
      return status;
    } catch (Exception e) {
      throw new DatasetPerformanceException(e);
    } finally {
      logger.info(format("%s / %s successfully executed", successCount, performanceTasks.size()));
    }
  }

  protected List<PerformanceSaveTask> generateToSaveTasks(
      List<PerformanceGenerationTask> performanceTasks)
      throws InterruptedException, ExecutionException {
    List<Future<DatasetPerformance>> results = executorService.invokeAll(performanceTasks);
    List<DatasetPerformance> performances = new ArrayList<>(performanceTasks.size() * 2);
    List<PerformanceSaveTask> saveTasks = new ArrayList<>(performanceTasks.size() * 2);
    for (Future<DatasetPerformance> futures : results) {
      DatasetPerformance performance = futures.get();
      performances.add(performance);
    }

    // fix global top view count across all datasets and build the save task
    long topViewCount = topDatasetViewCount(performances, 8);
    for (DatasetPerformance performance : performances) {
      performance.setTopViewCountAcrossAllDatasets(topViewCount);
      saveTasks.add(buildSaveTask(performance));
    }
    return saveTasks;
  }

  protected List<PerformanceTaskStatus> executeSaveTasks(List<PerformanceSaveTask> saveTasks)
      throws InterruptedException, ExecutionException {
    List<Future<PerformanceTaskStatus>> saves = executorService.invokeAll(saveTasks);
    List<PerformanceTaskStatus> status = new ArrayList<>(saves.size() * 2);
    for (Future<PerformanceTaskStatus> save : saves) {
      PerformanceTaskStatus success = save.get();
      status.add(success);
    }
    return status;
  }

  protected long topDatasetViewCount(List<DatasetPerformance> performances, int daysAgo) {
    if (isNull(performances) || daysAgo < 1) {
      return 0;
    }

    long topViewCount = 0;
    String topDataset = "";
    for (DatasetPerformance performance : performances) {
      List<DailyStat> chartData = performance.getChartData();
      if (isNull(performance.getChartData()) || performance.getChartData().isEmpty()) {
        logger.warn(
            format(
                "chart data is empty for dataset: %s, skipping top view calculation",
                performance.getDataset()));
        continue;
      }
      String dataset = performance.getDataset();
      int daysAvailable = chartData.size();
      int days = min(daysAvailable, daysAgo);
      chartData = chartData.subList(0, days);
      if (days != daysAgo) {
        logger.warn(
            format(
                "expected chart data list size to be %s but found %s, first noticed at dataset: %s",
                daysAgo, days, dataset));
      }

      long currentViewCount = chartData.stream().mapToLong(DailyStat::getViews).sum();
      if (currentViewCount > topViewCount) {
        topViewCount = currentViewCount;
        topDataset = dataset;
      }
    }

    if (logger.isInfoEnabled()) {
      logger.info(format("found top dataset: %s with views: %s", topDataset, topViewCount));
    }
    return topViewCount;
  }

  protected void initContractorCache() throws DatasetPerformanceException {
    try {
      Instant start = now();
      contractors = rouHelper.queryUsersWithContractorAttribute();
      Instant end = now();
      if (logger.isInfoEnabled()) {
        Duration duration = between(start, end);
        logger.info(
            format(
                "initialized the contractor cache with %s elements time:%s",
                contractors.getData().getUsersWithAttribute().size(), duration));
      }

    } catch (RulesOfUseException e) {
      e.printStackTrace();
      throw new DatasetPerformanceException(e);
    }
  }

  protected void initMatomoCache() throws DatasetPerformanceException {
    try {
      Instant start = now();
      matomoCache = matomoHelper.allChartDataElementsByWeek();
      Instant end = now();
      if (logger.isInfoEnabled()) {
        Duration duration = between(start, end);
        logger.info(
            format(
                "initialized the matomo cache with %s elements time:%s",
                matomoCache.size(), duration));
      }

    } catch (MatomoException e) {
      e.printStackTrace();
      throw new DatasetPerformanceException(e);
    }
  }

  protected DatasetPerformanceSpec readSpec(DatasetPerformanceConfig config) throws IOException {
    logger.info("fname:" + config.fname);
    DatasetPerformanceSpec spec = null;
    if (config.fname != null) {
      String json = new String(Files.readAllBytes(Paths.get(config.fname)));
      spec = DatasetPerformanceSpec.fromJson(json);
    }
    logger.info("loaded spec:" + spec);
    return spec;
  }

  protected DatasetPerformanceConfig parseJobArgs(String[] args)
      throws IOException, DatasetPerformanceException {
    logger.debug("creating and verifying job config");
    DatasetPerformanceConfig config = new DatasetPerformanceConfig();
    if (!config.parse(args)) {
      String err =
          "Usage: <main_class> [options] --dataset-performance <dataset name> --all-dataset-performance";
      throw new DatasetPerformanceException(err);
    }
    logger.debug("loaded config: " + config);
    return config;
  }

  protected boolean checkInitialized() {
    return nonNull(config)
        && nonNull(spec)
        && nonNull(objectMapper)
        && nonNull(context)
        && nonNull(executorService)
        && nonNull(matomoPerformanceProvider)
        && nonNull(datasetMetadataProvider)
        && nonNull(datasetMetadataProvider.getContext())
        && nonNull(datasetMetadataProvider.getObjectMapper())
        && nonNull(dataPerformanceProvider)
        && nonNull(dataPerformanceProvider.getRouHelper())
        && nonNull(slackProvider)
        && nonNull(slackProvider.getObjectMapper())
        && (spec.isAllDatasets() || nonNull(spec.getDatasets()));
  }

  protected void sendStatusToSlack(List<PerformanceTaskStatus> status) throws SlackApiException {
    slackProvider.sendMessage(statusToSlackMessage(status));
  }

  protected SlackMessage statusToSlackMessage(List<PerformanceTaskStatus> status) {
    SlackMessageBuilder slackMessageBuilder =
        SlackMessage.builder()
            .channel("#dataprofiler--loading")
            .username("Nightly Job")
            .icon_emoji(":crescent_moon:");
    AttachmentBuilder attachmentBuilder =
        Attachment.builder().text("Dataset Performance").color("#00FF00");

    int totalCount = 0;
    int successCount = 0;
    long maxAllTimeSearch = 0;
    long maxChartWeeks = 0;
    long maxNumUsersWithAttribute = 0;
    long maxAllTimeDownload = 0;
    for (PerformanceTaskStatus taskStatus : status) {
      totalCount++;
      if (taskStatus.isSuccess()) {
        successCount++;
      }
      maxAllTimeSearch = max(maxAllTimeSearch, taskStatus.getAllTimeSearchTrendStat());
      maxAllTimeDownload = max(maxAllTimeDownload, taskStatus.getAllTimeDownloadTrendStat());
      maxChartWeeks = max(maxChartWeeks, taskStatus.getNumChartDays());
      maxNumUsersWithAttribute =
          max(maxNumUsersWithAttribute, taskStatus.getNumUsersWithAttribute());
    }

    attachmentBuilder.addField(
        Field.builder()
            .isShort(true)
            .title("Max Searches")
            .value(format("%,d", maxAllTimeSearch))
            .build());
    attachmentBuilder.addField(
        Field.builder()
            .isShort(true)
            .title("Max Downloads")
            .value(format("%,d", maxAllTimeDownload))
            .build());
    attachmentBuilder.addField(
        Field.builder()
            .isShort(true)
            .title("Max Chart Weeks")
            .value(format("%,d", maxChartWeeks))
            .build());
    attachmentBuilder.addField(
        Field.builder()
            .isShort(true)
            .title("Max Users With Attribute")
            .value(format("%,d", maxNumUsersWithAttribute))
            .build());
    attachmentBuilder.addField(
        Field.builder().isShort(true).title("Environment").value(config.environment).build());
    attachmentBuilder.addField(
        Field.builder()
            .isShort(true)
            .title("Time Elapsed")
            .value(format("%s", totalDuration))
            .build());
    attachmentBuilder.addField(
        Field.builder()
            .isShort(true)
            .title("Avg Seconds Per Dataset")
            .value(format("%.3f", (float) (totalDuration.getSeconds() / totalCount)))
            .build());
    attachmentBuilder.addField(
        Field.builder()
            .isShort(true)
            .title("Datasets processed")
            .value(format("%s", totalCount))
            .build());
    attachmentBuilder.addField(
        Field.builder()
            .isShort(true)
            .title("Datasets with processing failures")
            .value(format("%s", (totalCount - successCount)))
            .build());
    attachmentBuilder.addField(
        Field.builder().isShort(true).title("Memory Usage").value(memoryUsage()).build());
    slackMessageBuilder.addAttachment(attachmentBuilder.build());
    return slackMessageBuilder.build();
  }
}
