package com.dataprofiler.datasetquality.cli;

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
import static java.nio.file.Files.readAllBytes;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import com.dataprofiler.datasetquality.DatasetQualityException;
import com.dataprofiler.datasetquality.QualityTask;
import com.dataprofiler.datasetquality.config.DatasetQualityConfig;
import com.dataprofiler.datasetquality.model.status.QualityTaskStatus;
import com.dataprofiler.datasetquality.provider.DatasetQualityProvider;
import com.dataprofiler.datasetquality.provider.MultisearchProvider;
import com.dataprofiler.datasetquality.spec.DatasetQualitySpec;
import com.dataprofiler.provider.ColumnMetadataProvider;
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
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasetQualityCli {
  private static final Logger logger = LoggerFactory.getLogger(DatasetQualityCli.class);
  private static final int MAX_THREADS = 12;

  private DatasetQualityConfig config;
  private DatasetQualitySpec spec;
  private Context context;
  private ExecutorService executorService;
  private ObjectMapper objectMapper;
  private DatasetMetadataProvider datasetMetadataProvider;
  private DatasetQualityProvider datasetQualityProvider;
  private Duration totalDuration;
  private SlackProvider slackProvider;

  public static void main(String[] args) {
    try {
      DatasetQualityCli exporter = new DatasetQualityCli();
      exporter.execute(args);
    } catch (DatasetQualityException e) {
      e.printStackTrace();
      logger.warn(e.toString());
      System.exit(1);
    }
    System.exit(0);
  }

  public void execute(String[] args) throws DatasetQualityException {
    try {
      init(args);
      sendStatusToSlack(executeSpec(spec));
      logger.info(memoryUsage());
    } catch (SlackApiException e) {
      e.printStackTrace();
      throw new DatasetQualityException(e);
    } finally {
      if (nonNull(executorService)) {
        executorService.shutdown();
      }
    }
  }

  protected List<QualityTaskStatus> executeSpec(DatasetQualitySpec spec)
      throws DatasetQualityException {
    if (!checkInitialized()) {
      throw new IllegalStateException();
    }
    Instant start = now();
    List<QualityTaskStatus> status = executeTasks(buildTasks(spec));
    Instant end = now();
    if (logger.isInfoEnabled()) {
      totalDuration = between(start, end);
      logger.info(format("spec executed time: %s", totalDuration));
    }
    return status;
  }

  protected List<QualityTask> buildTasks(DatasetQualitySpec spec) {
    //    List<VersionedMetadataObject> datasets =
    // datasetMetadataProvider.fetchVersionMetadataForAllDatasets();
    Set<String> datasets =
        spec.isAllDatasets() ? datasetMetadataProvider.fetchAllDatasets() : spec.getDatasets();
    return datasets.stream()
        .map(
            dataset ->
                new QualityTask(
                    datasetMetadataProvider, datasetQualityProvider, objectMapper, dataset))
        .collect(toList());
  }

  protected List<QualityTaskStatus> executeTasks(List<QualityTask> qualityTasks)
      throws DatasetQualityException {
    List<Future<QualityTaskStatus>> results = Collections.emptyList();
    List<QualityTaskStatus> status = new ArrayList<>(results.size() * 2);
    long successCount = 0;
    try {
      results = executorService.invokeAll(qualityTasks);
      successCount = 0;
      for (Future<QualityTaskStatus> future : results) {
        QualityTaskStatus currentStatus = future.get();
        status.add(currentStatus);
        if (currentStatus.isSuccess()) {
          successCount++;
        }
      }
    } catch (ExecutionException | InterruptedException e) {
      e.printStackTrace();
      throw new DatasetQualityException(e);
    } finally {
      logger.info(format("%s / %s successfully executed", successCount, results.size()));
    }
    return status;
  }

  public void init(String[] args) throws DatasetQualityException {
    try {
      logger.debug("initializing...");
      config = parseJobArgs(args);
      spec = readSpec(config);
      if (logger.isDebugEnabled()) {
        logger.debug("config: " + config);
        logger.debug("spec: " + spec.toString());
      }
      context = new Context(config);
      if (logger.isDebugEnabled()) {
        logger.debug("context: " + context);
      }
      objectMapper = new ObjectMapper();
      datasetMetadataProvider = new DatasetMetadataProvider();
      datasetMetadataProvider.setObjectMapper(objectMapper);
      datasetMetadataProvider.setContext(context);
      MultisearchProvider multisearchProvider = new MultisearchProvider();
      multisearchProvider.setContext(context);
      datasetQualityProvider = new DatasetQualityProvider();
      datasetQualityProvider.setContext(context);
      datasetQualityProvider.setMultisearchProvider(multisearchProvider);
      datasetQualityProvider.setColumnMetadataProvider(new ColumnMetadataProvider(context));
      datasetQualityProvider.setTableMetadataProvider(new TableMetadataProvider(context));
      datasetQualityProvider.setDatasetMetadataProvider(new DatasetMetadataProvider(context));
      executorService = Executors.newFixedThreadPool(MAX_THREADS);
      slackProvider = new SlackProvider(config.slackWebhook);
      slackProvider.setObjectMapper(objectMapper);
    } catch (Exception e) {
      throw new DatasetQualityException(e);
    }
  }

  protected DatasetQualitySpec readSpec(DatasetQualityConfig config) throws IOException {
    logger.info("fname:" + config.fname);
    DatasetQualitySpec spec = null;
    if (config.fname != null) {
      String json = new String(readAllBytes(Paths.get(config.fname)));
      spec = DatasetQualitySpec.fromJson(json);
    }
    logger.info("loaded spec:" + spec);
    return spec;
  }

  protected DatasetQualityConfig parseJobArgs(String[] args)
      throws IOException, DatasetQualityException {
    logger.debug("creating and verifying job config");
    DatasetQualityConfig config = new DatasetQualityConfig();
    if (!config.parse(args)) {
      String err = "Usage: <main_class> [options] --fname " + "dataset-quality-spec.json ";
      throw new DatasetQualityException(err);
    }
    logger.debug("loaded config: " + config);
    return config;
  }

  protected boolean checkInitialized() {
    return nonNull(config)
        && nonNull(context)
        && nonNull(executorService)
        && nonNull(objectMapper)
        && nonNull(datasetMetadataProvider)
        && nonNull(datasetMetadataProvider.getContext())
        && nonNull(datasetMetadataProvider.getObjectMapper())
        && nonNull(datasetQualityProvider)
        && nonNull(datasetQualityProvider.getMultisearchProvider())
        && nonNull(datasetQualityProvider.getContext())
        && nonNull(datasetQualityProvider.getColumnMetadataProvider())
        && nonNull(datasetQualityProvider.getTableMetadataProvider())
        && nonNull(datasetQualityProvider.getDatasetMetadataProvider())
        && nonNull(slackProvider)
        && nonNull(slackProvider.getObjectMapper());
  }

  protected void sendStatusToSlack(List<QualityTaskStatus> status) throws SlackApiException {
    slackProvider.sendMessage(statusToSlackMessage(status));
  }

  protected SlackMessage statusToSlackMessage(List<QualityTaskStatus> status) {
    SlackMessageBuilder slackMessageBuilder =
        SlackMessage.builder()
            .channel("#dataprofiler-loading")
            .username("Nightly Job")
            .icon_emoji(":crescent_moon:");
    AttachmentBuilder attachmentBuilder =
        Attachment.builder().text("Dataset Quality").color("#00FF00");

    int datasetsProcessed = 0;
    int successCount = 0;
    int totalQualities = 0;
    for (QualityTaskStatus qualityTaskStatus : status) {
      datasetsProcessed++;
      if (qualityTaskStatus.isSuccess()) {
        successCount++;
      }
      totalQualities += qualityTaskStatus.getQualitiesUpdated();
    }

    attachmentBuilder.addField(
        Field.builder()
            .isShort(true)
            .title("Qualities Loaded")
            .value(Integer.toString(totalQualities))
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
            .value(format("%.3f", (float) (totalDuration.getSeconds() / datasetsProcessed)))
            .build());
    attachmentBuilder.addField(
        Field.builder()
            .isShort(true)
            .title("Datasets processed")
            .value(format("%s", datasetsProcessed))
            .build());
    attachmentBuilder.addField(
        Field.builder()
            .isShort(true)
            .title("Datasets with processing failures")
            .value(format("%s", (datasetsProcessed - successCount)))
            .build());
    attachmentBuilder.addField(
        Field.builder().isShort(true).title("Memory Usage").value(memoryUsage()).build());
    slackMessageBuilder.addAttachment(attachmentBuilder.build());
    return slackMessageBuilder.build();
  }
}
