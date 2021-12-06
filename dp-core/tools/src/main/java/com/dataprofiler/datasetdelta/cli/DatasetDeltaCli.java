package com.dataprofiler.datasetdelta.cli;

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
import static java.lang.String.format;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import com.dataprofiler.datasetdelta.DatasetDeltaException;
import com.dataprofiler.datasetdelta.DeltaTask;
import com.dataprofiler.datasetdelta.config.DatasetDeltaConfig;
import com.dataprofiler.datasetdelta.model.status.DeltaTaskStatus;
import com.dataprofiler.datasetdelta.provider.DatasetDeltaProvider;
import com.dataprofiler.datasetdelta.provider.DeltaCommentProvider;
import com.dataprofiler.datasetdelta.spec.DatasetDeltaSpec;
import com.dataprofiler.provider.DatasetMetadataProvider;
import com.dataprofiler.provider.TableMetadataProvider;
import com.dataprofiler.slack.SlackApiException;
import com.dataprofiler.slack.model.Attachment;
import com.dataprofiler.slack.model.Attachment.AttachmentBuilder;
import com.dataprofiler.slack.model.Field;
import com.dataprofiler.slack.model.SlackMessage;
import com.dataprofiler.slack.model.SlackMessage.SlackMessageBuilder;
import com.dataprofiler.slack.provider.SlackProvider;
import com.dataprofiler.util.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * cli to kick oof the delta processing for all datasets configure max threads to process the
 * datasets concurrently - one thread per dataset
 */
public class DatasetDeltaCli {
  private static final Logger logger = LoggerFactory.getLogger(DatasetDeltaCli.class);

  private static final int MAX_THREADS = 12;
  private DatasetDeltaConfig config;
  private DatasetDeltaSpec spec;
  private Context context;
  private DatasetDeltaProvider datasetDeltaProvider;
  private DatasetMetadataProvider datasetMetadataProvider;
  private TableMetadataProvider tableMetadataProvider;
  private ObjectMapper objectMapper;
  private ExecutorService executorService;
  private SlackProvider slackProvider;
  private Duration totalDuration = Duration.ZERO;

  public DatasetDeltaCli() {
    super();
  }

  public static void main(String[] args) {
    try {
      DatasetDeltaCli exporter = new DatasetDeltaCli();
      exporter.execute(args);
    } catch (DatasetDeltaException e) {
      e.printStackTrace();
      logger.warn(e.toString());
      System.exit(1);
    }
    System.exit(0);
  }

  public void init(String[] args) throws DatasetDeltaException {
    try {
      logger.debug("initializing...");
      config = parseJobArgs(args);
      spec = readSpec(config);
      if (logger.isDebugEnabled()) {
        logger.debug("config: " + config);
        logger.debug("spec: " + spec.toString());
      }
      //      context = new DPSparkContext(config, "Dataset Delta Job");
      context = new Context(config);
      if (logger.isDebugEnabled()) {
        logger.debug("context: " + context);
      }
      objectMapper = new ObjectMapper();
      datasetMetadataProvider = new DatasetMetadataProvider();
      datasetMetadataProvider.setContext(context);
      datasetMetadataProvider.setObjectMapper(objectMapper);
      tableMetadataProvider = new TableMetadataProvider();
      tableMetadataProvider.setContext(context);
      datasetDeltaProvider = new DatasetDeltaProvider();
      datasetDeltaProvider.setDatasetMetadataProvider(datasetMetadataProvider);
      datasetDeltaProvider.setTableMetadataProvider(tableMetadataProvider);
      DeltaCommentProvider commentProvider = new DeltaCommentProvider();
      commentProvider.setContext(context);
      datasetDeltaProvider.setCommentProvider(commentProvider);
      datasetDeltaProvider.setContext(context);
      executorService = Executors.newFixedThreadPool(MAX_THREADS);
      slackProvider = new SlackProvider(config.slackWebhook);
      slackProvider.setObjectMapper(objectMapper);
    } catch (Exception e) {
      throw new DatasetDeltaException(e);
    }
  }

  public void execute(String[] args) throws DatasetDeltaException {
    try {
      init(args);
      sendStatusToSlack(executeSpec(spec));
      logger.info(memoryUsage());
    } catch (Exception e) {
      throw new DatasetDeltaException(e);
    } finally {
      if (nonNull(executorService)) {
        executorService.shutdown();
      }
    }
  }

  protected List<DeltaTaskStatus> executeSpec(DatasetDeltaSpec spec) throws DatasetDeltaException {
    if (!checkInitialized()) {
      throw new IllegalStateException();
    }

    Instant start = now();
    //    List<MetadataVersionObject> globalVersions =
    //        datasetMetadataProvider.fetchGlobalVersionLineage();
    //    Instant end = now();
    //    if (logger.isInfoEnabled()) {
    //      int limit = 5;
    //      Duration duration = between(start, end);
    //      logger.info(
    //          format(
    //              "last %s out of %s global versions time: %s",
    //              limit, globalVersions.size(), duration));
    //      globalVersions.stream().limit(limit).forEach(logger::info);
    //    }
    //    start = now();
    List<DeltaTaskStatus> status = executeTasks(buildTasks(spec));
    Instant end = now();
    if (logger.isInfoEnabled()) {
      totalDuration = between(start, end);
      logger.info(format("spec executed time: %s", totalDuration));
    }
    return status;
  }

  protected List<DeltaTask> buildTasks(DatasetDeltaSpec spec) {
    Set<String> datasets =
        spec.isAllDatasets() ? datasetMetadataProvider.fetchAllDatasets() : spec.getDatasets();
    return datasets.stream()
        .map(
            dataset ->
                new DeltaTask(datasetDeltaProvider, datasetMetadataProvider, objectMapper, dataset))
        .collect(toList());
  }

  protected List<DeltaTaskStatus> executeTasks(List<DeltaTask> deltaTasks)
      throws DatasetDeltaException {
    try {
      List<Future<DeltaTaskStatus>> results = executorService.invokeAll(deltaTasks);
      List<DeltaTaskStatus> status =
          results.stream()
              .map(
                  future -> {
                    try {
                      return future.get();
                    } catch (Exception e) {
                      e.printStackTrace();
                    }
                    return new DeltaTaskStatus().setSuccess(false);
                  })
              .collect(toList());

      long successCount = status.stream().filter(DeltaTaskStatus::isSuccess).count();
      logger.info(format("%s / %s successfully executed", successCount, results.size()));
      return status;
    } catch (InterruptedException e) {
      e.printStackTrace();
      throw new DatasetDeltaException(e);
    }
  }

  protected DatasetDeltaSpec readSpec(DatasetDeltaConfig config) throws IOException {
    logger.info("fname:" + config.fname);
    DatasetDeltaSpec spec = null;
    if (config.fname != null) {
      String json = new String(Files.readAllBytes(Paths.get(config.fname)));
      spec = DatasetDeltaSpec.fromJson(json);
    }
    logger.info("loaded spec:" + spec);
    return spec;
  }

  protected DatasetDeltaConfig parseJobArgs(String[] args)
      throws IOException, DatasetDeltaException {
    logger.info("creating and verifying job config");
    DatasetDeltaConfig config = new DatasetDeltaConfig();
    if (!config.parse(args)) {
      String err =
          "Usage: <main_class> [options] --dataset-performance <dataset name> --all-dataset-performance";
      throw new DatasetDeltaException(err);
    }
    logger.info("loaded config: " + config);
    return config;
  }

  protected boolean checkInitialized() {
    return nonNull(config)
        && nonNull(spec)
        && nonNull(objectMapper)
        && nonNull(context)
        && nonNull(executorService)
        && nonNull(datasetMetadataProvider)
        && nonNull(datasetMetadataProvider.getContext())
        && nonNull(datasetMetadataProvider.getObjectMapper())
        && nonNull(datasetDeltaProvider)
        && nonNull(datasetDeltaProvider.getDatasetMetadataProvider())
        && nonNull(datasetDeltaProvider.getTableMetadataProvider())
        && nonNull(datasetDeltaProvider.getCommentProvider())
        && nonNull(slackProvider)
        && nonNull(slackProvider.getObjectMapper())
        && (spec.isAllDatasets() || !isNull(spec.getDatasets()));
  }

  protected void sendStatusToSlack(List<DeltaTaskStatus> status) throws SlackApiException {
    slackProvider.sendMessage(statusToSlackMessage(status));
  }

  protected SlackMessage statusToSlackMessage(List<DeltaTaskStatus> status) {
    SlackMessageBuilder slackMessageBuilder =
        SlackMessage.builder()
            .channel("#data-profiler-loading")
            .username("Nightly Job")
            .icon_emoji(":crescent_moon:");
    AttachmentBuilder attachmentBuilder =
        Attachment.builder().text("Dataset Deltas").color("#00FF00");
    int totalCount = 0;
    int successCount = 0;
    int totalDeltas = 0;
    //    Duration totalDuration = Duration.ZERO;
    for (DeltaTaskStatus deltaTaskStatus : status) {
      totalCount++;
      if (deltaTaskStatus.isSuccess()) {
        successCount++;
      }
      totalDeltas += deltaTaskStatus.getDeltasUpdated();
      //      totalDuration = totalDuration.plus(deltaTaskStatus.getDuration());
    }

    if (successCount != totalCount) {
      // there was a failure
      attachmentBuilder.color("#FF3333");
    }

    attachmentBuilder.addField(
        Field.builder()
            .isShort(true)
            .title("Deltas Loaded")
            .value(Integer.toString(totalDeltas))
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
