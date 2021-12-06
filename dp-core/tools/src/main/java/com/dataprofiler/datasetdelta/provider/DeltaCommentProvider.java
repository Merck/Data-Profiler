package com.dataprofiler.datasetdelta.provider;

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

import static com.dataprofiler.datasetdelta.model.DeltaEnum.COLUMN_ADDED;
import static com.dataprofiler.datasetdelta.model.DeltaEnum.COLUMN_REMOVED;
import static com.dataprofiler.datasetdelta.model.DeltaEnum.NO_OP;
import static com.dataprofiler.datasetdelta.model.DeltaEnum.TABLE_ADDED;
import static com.dataprofiler.datasetdelta.model.DeltaEnum.TABLE_REMOVED;
import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toSet;

import com.dataprofiler.datasetdelta.model.CommentUUID;
import com.dataprofiler.datasetdelta.model.DatasetDelta;
import com.dataprofiler.datasetdelta.model.DeltaEnum;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.NumberUtil;
import com.dataprofiler.util.objects.CustomAnnotation;
import com.dataprofiler.util.objects.CustomAnnotation.AnnotationType;
import com.dataprofiler.util.objects.MetadataVersionObject;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeltaCommentProvider {
  private static final Logger logger = LoggerFactory.getLogger(DeltaCommentProvider.class);

  protected Context context;
  protected String commentSymbol = "\u270D";
  protected static final Set<DeltaEnum> IGNORE_DELTAS = new HashSet<>();

  static {
    IGNORE_DELTAS.add(NO_OP);
    //    IGNORE_DELTAS.add(COLUMN_VALUES_DECREASED);
    //    IGNORE_DELTAS.add(COLUMN_VALUES_INCREASED);
    //    IGNORE_DELTAS.add(DATASET_VALUES_DECREASED);
    //    IGNORE_DELTAS.add(DATASET_VALUES_INCREASED);
  }

  public DeltaCommentProvider() {
    super();
  }

  /**
   * create, save and link comments for the given DatasetDeltas, if the delta has a null comment
   *
   * @throws IllegalStateException if context is not set for this provider
   * @throws IllegalArgumentException complete delta or deltas are null
   * @param deltas Set<DatasetDelta> with DatasetDeltas
   * @return Set<DatasetDelta> with DatasetDelta set containing linked comment ids
   */
  public Set<DatasetDelta> saveDeltaComments(final Set<DatasetDelta> deltas) {
    if (isNull(context) || isNull(deltas)) {
      throw new IllegalStateException(
          "context or deltas were null, cannot store comments for deltas");
    }

    Instant start = now();

    if (deltas.isEmpty()) {
      return deltas;
    }

    String dataset = deltas.stream().findFirst().get().getDataset();
    if (logger.isInfoEnabled()) {
      logger.info(
          format("%s saving %s delta comments for %s", commentSymbol, deltas.size(), dataset));
    }

    Set<CustomAnnotation> savedComments = new HashSet<>();
    Set<DatasetDelta> validDeltas = filterForValid(deltas);
    Set<DatasetDelta> linkedDeltas = new HashSet<>();
    int errorCount = 0;
    int successCount = 0;
    int alreadyLinkedCount = 0;
    // save comment if it was a valid enum
    for (DatasetDelta delta : validDeltas) {
      try {
        if (delta.hasLinkedComment()) {
          alreadyLinkedCount++;
          linkedDeltas.add(delta);
          continue;
        }
        CustomAnnotation comment = buildComment(delta);
        if (isNull(comment) || isNull(comment.getNote()) || comment.getNote().isEmpty()) {
          logger.warn(format("empty comment for dataset %s, skipping...", delta.getDataset()));
          continue;
        }
        if (logger.isInfoEnabled()) {
          logger.info(
              format(
                  "%s saving dataset: %s table: %s column: %s comment: %s",
                  commentSymbol,
                  dataset,
                  comment.getTable(),
                  comment.getColumn(),
                  comment.getNote()));
        }
        DeltaEnum deltaEnum = delta.getDeltaEnum();
        MetadataVersionObject version =
            deltaEnum.isAddedEnum()
                ? new MetadataVersionObject(delta.getTargetVersion())
                : new MetadataVersionObject(delta.getFromVersion());
        comment.put(context, version);
        savedComments.add(comment);
        UUID uuid = comment.getUuid();
        // link the id in the delta
        delta.setComment(new CommentUUID(uuid));
        if (logger.isTraceEnabled()) {
          logger.trace(
              format("%s saved comment %s linked to delta %s", commentSymbol, uuid, delta));
        }
        linkedDeltas.add(delta);
        successCount++;
      } catch (Exception e) {
        errorCount++;
        e.printStackTrace();
        logger.warn(
            format(
                "%s error saving comment for dataset: %s, skipping",
                commentSymbol, delta.getDataset()));
        //        // back out of the previously saved comments
        //        silentlyCleanupComments(savedComments);
        //        throw new DatasetDeltaException(e);
      }
    }

    Instant end = now();
    if (logger.isInfoEnabled()) {
      Duration duration = between(start, end);
      logger.info(
          format(
              "%s finished saving comment(s) %s success %s failed %s already linked for dataset: %s time: %s",
              commentSymbol, successCount, errorCount, alreadyLinkedCount, dataset, duration));
    }
    return linkedDeltas;
  }

  /**
   * destroy given comments, ignore any errors
   *
   * @param comments Set of comments
   */
  protected void silentlyCleanupComments(Set<CustomAnnotation> comments) {
    comments.forEach(
        comment -> {
          try {
            String id = comment.getUuid().toString();
            logger.info(format("%s destroying %s", commentSymbol, id));
            comment.destroy(context);
            logger.info(format("%s destroyed %s", commentSymbol, id));
          } catch (BasicAccumuloException basicAccumuloException) {
            basicAccumuloException.printStackTrace();
          }
        });
  }

  /**
   * build up comment per enum type
   *
   * @param delta DatasetDelta to render into a text comment
   * @return comment custom to the given dataset, ready to be saved
   */
  protected CustomAnnotation buildComment(DatasetDelta delta) {
    DeltaEnum deltaEnum = delta.getDeltaEnum();
    CustomAnnotation comment = new CustomAnnotation();

    if (logger.isDebugEnabled()) {
      logger.debug(format("building comment for %s", deltaEnum));
    }
    switch (deltaEnum) {
      case DATASET_RENAMED:
      case DATASET_VALUES_DECREASED:
      case DATASET_VALUES_INCREASED:
        comment.setDataset(delta.getDataset());
        comment.setAnnotationType(AnnotationType.DATASET);
        break;
      case TABLE_ADDED:
      case TABLE_REMOVED:
      case TABLE_VALUES_DECREASED:
      case TABLE_VALUES_INCREASED:
        comment.setDataset(delta.getDataset());
        comment.setTable(delta.getTable());
        comment.setAnnotationType(AnnotationType.TABLE);
        break;
      case COLUMN_ADDED:
      case COLUMN_REMOVED:
      case COLUMN_VALUES_DECREASED:
      case COLUMN_VALUES_INCREASED:
        comment.setDataset(delta.getDataset());
        comment.setTable(delta.getTable());
        comment.setColumn(delta.getColumn());
        comment.setAnnotationType(AnnotationType.COLUMN);
        break;
      case NO_OP:
        comment = null;
        break;
      default:
        logger.info("cannot build comment for unknown delta type, skipping");
        comment = null;
        break;
    }

    if (nonNull(comment)) {
      comment.setNote(buildNote(delta));
      comment.setCreatedBy(CustomAnnotation.SYSTEM_COMMENT_AUTHOR);
      comment.setDataTour(false);
    }
    return comment;
  }

  public String buildNote(DatasetDelta delta) {
    if (isNull(delta) || isNull(delta.getDeltaEnum())) {
      return "";
    }
    DeltaEnum deltaEnum = delta.getDeltaEnum();

    if (deltaEnum.equals(COLUMN_REMOVED)) {
      return format(
          "Column %s removed from table %s in dataset %s",
          delta.getValueFrom(), delta.getTable(), delta.getDataset());
    } else if (deltaEnum.equals(COLUMN_ADDED)) {
      return format(
          "Column %s added to table %s in dataset %s",
          delta.getValueTo(), delta.getTable(), delta.getDataset());
    } else if (deltaEnum.equals(TABLE_REMOVED)) {
      return format("Table %s removed from dataset %s", delta.getValueFrom(), delta.getDataset());
    } else if (deltaEnum.equals(TABLE_ADDED)) {
      return format("Table %s added to dataset %s", delta.getValueTo(), delta.getDataset());
    }

    // values changed / catch all
    String deltaMsg = delta.getDeltaEnum().toString();
    String action = upperFirst(deltaMsg.replace('_', ' ').toLowerCase());
    String valueFrom = delta.getValueFrom();
    String valueTo = delta.getValueTo();
    if (deltaEnum.isValueEnum()) {
      try {
        valueFrom = NumberUtil.humanReadableValue(parseLong(delta.getValueFrom()));
        valueTo = NumberUtil.humanReadableValue(parseLong(delta.getValueTo()));
      } catch (NumberFormatException nfe) {
        nfe.printStackTrace();
      }
    }
    return format("%s from %s to %s", action, valueFrom, valueTo);
  }

  protected String upperFirst(String str) {
    if (isNull(str) || str.isEmpty()) {
      return "";
    }

    if (str.length() == 1) {
      return str.toUpperCase();
    }

    return format("%s%s", str.substring(0, 1).toUpperCase(), str.substring(1));
  }

  /**
   * remove known deltas that we do not use in the ui
   *
   * @param deltas Set of deltas
   * @return Set of DatasetDelta objects not included in the ignore set
   */
  protected Set<DatasetDelta> filterForValid(Set<DatasetDelta> deltas) {
    return deltas.stream()
        .filter(el -> !IGNORE_DELTAS.contains(el.getDeltaEnum()))
        .collect(toSet());
  }

  public Context getContext() {
    return context;
  }

  public void setContext(Context context) {
    this.context = context;
  }
}
