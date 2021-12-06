package com.dataprofiler.datasetproperties.provider;

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
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;

import com.dataprofiler.datasetdelta.model.CommentUUID;
import com.dataprofiler.datasetdelta.model.CompleteDelta;
import com.dataprofiler.datasetdelta.model.DatasetDelta;
import com.dataprofiler.provider.DatasetMetadataProvider;
import com.dataprofiler.provider.DatasetPropertiesException;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.CustomAnnotation;
import com.dataprofiler.util.objects.VersionedDatasetMetadata;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrphanedDeltaCommentProvider {
  private static final Logger logger = LoggerFactory.getLogger(OrphanedDeltaCommentProvider.class);

  protected Context context;
  protected DatasetMetadataProvider datasetMetadataProvider;
  protected String commentSymbol = "\u270D";

  public OrphanedDeltaCommentProvider() {
    super();
  }

  public Set<CustomAnnotation> fetchDatasetSystemComments(String dataset) {
    checkState();
    CustomAnnotation scanner = new CustomAnnotation();
    Supplier<Set<CustomAnnotation>> setComparedByUUID =
        () -> new TreeSet<>(comparing(CustomAnnotation::getUuid));
    return stream(scanner.scanHierarchyByDataset(context, dataset).spliterator(), false)
        .filter(CustomAnnotation::isSystemComment)
        .collect(toCollection(setComparedByUUID));
  }

  /**
   * @param versionedDatasetMetadata
   * @param isDryRun
   * @throws DatasetPropertiesException
   */
  public void deleteOrphanedDeltaComments(
      VersionedDatasetMetadata versionedDatasetMetadata, boolean isDryRun)
      throws DatasetPropertiesException {
    try {
      CompleteDelta completeDelta =
          datasetMetadataProvider.parseCompleteDelta(versionedDatasetMetadata);
      deleteOrphanedDeltaComments(completeDelta, isDryRun);
    } catch (IOException e) {
      throw new DatasetPropertiesException(e);
    }
  }

  /**
   * @param completeDelta
   * @param isDryRun
   */
  public void deleteOrphanedDeltaComments(CompleteDelta completeDelta, boolean isDryRun)
      throws DatasetPropertiesException {
    checkState();
    if (isNull(completeDelta) || isNull(completeDelta.getLastKnownVersions())) {
      return;
    }

    Instant start = now();

    String dataset = completeDelta.getLastKnownVersions().getDataset();
    Set<CommentUUID> commentIds =
        completeDelta.getDeltas().stream().map(DatasetDelta::getComment).collect(toSet());
    Set<CustomAnnotation> systemComments = fetchDatasetSystemComments(dataset);
    Set<CommentUUID> orphanedCommentIds = orphanedDeltaComments(commentIds, systemComments);
    Instant end = now();
    if (logger.isInfoEnabled()) {
      Duration duration = between(start, end);
      logger.info(
          format(
              "%s found %s delta comment reference(s) for dataset: %s time: %s",
              commentSymbol, commentIds.size(), dataset, duration));
      logger.info(
          format(
              "%s found %s system comment(s) for dataset: %s time: %s",
              commentSymbol, systemComments.size(), dataset, duration));
      logger.info(
          format(
              "%s found %s orphaned comment reference(s) for dataset: %s time: %s",
              commentSymbol, orphanedCommentIds.size(), dataset, duration));
    }
    Set<UUID> uniqueOrphanIds =
        orphanedCommentIds.stream().map(CommentUUID::getUuid).collect(toSet());
    if (logger.isDebugEnabled()) {
      logger.debug(format("%s %s", commentSymbol, uniqueOrphanIds));
    }

    if (!isDryRun) {
      // remove the given orphan ids
      Set<DatasetDelta> fixedDeltas =
          completeDelta.getDeltas().stream()
              .filter(Objects::nonNull)
              .filter(
                  delta -> {
                    if (isNull(delta.getComment()) || isNull(delta.getComment().getUuid())) {
                      return false;
                    }
                    UUID uuid = delta.getComment().getUuid();
                    return !uniqueOrphanIds.contains(uuid);
                  })
              .collect(toSet());
      int before = commentIds.size();
      int after = fixedDeltas.size();
      if (after < before) {
        int difference = (after - before) * -1;
        // save to db
        completeDelta.setDeltas(fixedDeltas);
        datasetMetadataProvider.replaceDeltaProperties(completeDelta);
        logger.info(
            format(
                "%s %s removed comment reference(s), %s - %s = %s comment reference(s) remaining, dataset: %s",
                commentSymbol, difference, before, difference, after, dataset));
      } else {
        logger.info(format("%s no change to deltas for dataset: %s", commentSymbol, dataset));
      }
    } else {
      logger.info(format("%s dataset: %s is dry run, skipping...", commentSymbol, dataset));
    }

    end = now();
    if (logger.isInfoEnabled()) {
      Duration duration = between(start, end);
      logger.info(
          format(
              "%s finished pruning comments for dataset: %s time: %s",
              commentSymbol, dataset, duration));
    }
  }

  /**
   * @param versionedDatasetMetadata
   * @param isDryRun
   * @throws DatasetPropertiesException
   */
  public void deleteOrphanedSystemComments(
      VersionedDatasetMetadata versionedDatasetMetadata, boolean isDryRun)
      throws DatasetPropertiesException {
    try {
      CompleteDelta completeDelta =
          datasetMetadataProvider.parseCompleteDelta(versionedDatasetMetadata);
      deleteOrphanedSystemComments(completeDelta, isDryRun);
    } catch (IOException e) {
      throw new DatasetPropertiesException(e);
    }
  }

  /**
   * @param completeDelta
   * @param isDryRun
   */
  public void deleteOrphanedSystemComments(CompleteDelta completeDelta, boolean isDryRun)
      throws DatasetPropertiesException {
    checkState();
    if (isNull(completeDelta) || isNull(completeDelta.getLastKnownVersions())) {
      return;
    }

    Instant start = now();

    String dataset = completeDelta.getLastKnownVersions().getDataset();
    Set<CommentUUID> commentIds =
        completeDelta.getDeltas().stream().map(DatasetDelta::getComment).collect(toSet());
    Set<CustomAnnotation> systemComments = fetchDatasetSystemComments(dataset);
    Set<CustomAnnotation> orphanedSystemComments =
        orphanedSystemComments(commentIds, systemComments);
    Instant end = now();
    if (logger.isInfoEnabled()) {
      Duration duration = between(start, end);
      logger.info(
          format(
              "%s found %s delta comment reference(s) for dataset: %s time: %s",
              commentSymbol, commentIds.size(), dataset, duration));
      logger.info(
          format(
              "%s found %s system comment(s) for dataset: %s time: %s",
              commentSymbol, systemComments.size(), dataset, duration));
      logger.info(
          format(
              "%s found %s orphaned system comment(s) for dataset: %s time: %s",
              commentSymbol, orphanedSystemComments.size(), dataset, duration));
    }

    if (!isDryRun) {
      // remove the given orphan ids
      int before = systemComments.size();
      int orphanedCount = orphanedSystemComments.size();
      if (orphanedCount > 0) {
        int remainder = before - orphanedCount;
        // save to db
        cleanupComments(orphanedSystemComments);
        logger.info(
            format(
                "%s %s removed system comment(s), %s - %s = %s system comment(s) remaining, dataset: %s",
                commentSymbol, orphanedCount, before, orphanedCount, remainder, dataset));
      } else {
        logger.info(
            format("%s no change to system comment(s) for dataset: %s", commentSymbol, dataset));
      }
    } else {
      logger.info(format("%s dataset: %s is dry run, skipping...", commentSymbol, dataset));
    }

    end = now();
    if (logger.isInfoEnabled()) {
      Duration duration = between(start, end);
      logger.info(
          format(
              "%s finished pruning system comments for dataset: %s time: %s",
              commentSymbol, dataset, duration));
    }
  }

  /**
   * destroy given comments
   *
   * @param comments Set of comments
   */
  public void cleanupComments(Set<CustomAnnotation> comments) throws DatasetPropertiesException {
    if (isNull(comments) || comments.isEmpty()) {
      return;
    }

    Instant start = now();
    if (logger.isDebugEnabled()) {
      logger.debug(format("%s destroying %s comment(s)", commentSymbol, comments.size()));
    }
    int destroyed = 0;
    try {
      for (CustomAnnotation comment : comments) {
        String id = comment.getUuid().toString();
        if (logger.isDebugEnabled()) {
          logger.debug(format("%s destroying %s", commentSymbol, id));
        }
        comment.destroy(context);
        destroyed++;
        if (logger.isDebugEnabled()) {
          logger.debug(format("%s destroyed %s", commentSymbol, id));
        }
      }
    } catch (BasicAccumuloException e) {
      e.printStackTrace();
      throw new DatasetPropertiesException(e);
    } finally {
      Instant end = now();
      if (logger.isInfoEnabled()) {
        Duration duration = between(start, end);
        logger.info(
            format(
                "%s finished destroying %s/%s comment(s) time: %s",
                commentSymbol, destroyed, comments.size(), duration));
      }
    }
  }

  protected Set<CommentUUID> orphanedDeltaComments(
      Set<CommentUUID> commentIds, Set<CustomAnnotation> systemComments) {
    if (isNull(commentIds)
        || commentIds.isEmpty()
        || isNull(systemComments)
        || systemComments.isEmpty()) {
      return emptySet();
    }

    Set<UUID> commentUUIDSet = commentIds.stream().map(CommentUUID::getUuid).collect(toSet());
    Set<UUID> systemUUIDSet =
        systemComments.stream().map(CustomAnnotation::getUuid).collect(toSet());
    Set<UUID> orphanedCommentUUIDSet =
        commentUUIDSet.stream().filter(uuid -> !systemUUIDSet.contains(uuid)).collect(toSet());
    return commentIds.stream()
        .filter(commentUUID -> orphanedCommentUUIDSet.contains(commentUUID.getUuid()))
        .collect(toSet());
  }

  protected Set<CustomAnnotation> orphanedSystemComments(
      Set<CommentUUID> commentIds, Set<CustomAnnotation> systemComments) {
    if (isNull(commentIds) || isNull(systemComments)) {
      return emptySet();
    }

    Set<UUID> commentUUIDSet = commentIds.stream().map(CommentUUID::getUuid).collect(toSet());
    Set<UUID> systemUUIDSet =
        systemComments.stream().map(CustomAnnotation::getUuid).collect(toSet());
    Set<UUID> orphanedSystemUUIDSet =
        systemUUIDSet.stream().filter(uuid -> !commentUUIDSet.contains(uuid)).collect(toSet());
    return systemComments.stream()
        .filter(comment -> orphanedSystemUUIDSet.contains(comment.getUuid()))
        .collect(toSet());
  }

  protected void checkState() {
    if (isNull(context)) {
      throw new IllegalStateException("context is null!");
    }
  }

  public Context getContext() {
    return context;
  }

  public void setContext(Context context) {
    this.context = context;
  }

  public DatasetMetadataProvider getDatasetMetadataProvider() {
    return datasetMetadataProvider;
  }

  public void setDatasetMetadataProvider(DatasetMetadataProvider datasetMetadataProvider) {
    this.datasetMetadataProvider = datasetMetadataProvider;
  }
}
