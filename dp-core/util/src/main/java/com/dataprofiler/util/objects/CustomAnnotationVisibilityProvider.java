package com.dataprofiler.util.objects;

/*-
 * 
 * dataprofiler-util
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

import com.dataprofiler.util.Context;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomAnnotationVisibilityProvider {
  private static final Logger logger =
      LoggerFactory.getLogger(CustomAnnotationVisibilityProvider.class);

  public String lookupVisibilityExpressionMetadata(Context context, String dataset) {
    VersionedDatasetMetadata metadata =
        new VersionedMetadataObject().allMetadataForDataset(context, dataset);
    return metadata.getDatasetMetadata().getVisibility();
  }

  public String lookupVisibilityExpressionMetadata(
      Context context, MetadataVersionObject version, String dataset) {
    VersionedDatasetMetadata metadata =
        new VersionedMetadataObject().allMetadataForDataset(context, version, dataset);
    return metadata.getDatasetMetadata().getVisibility();
  }

  public String lookupVisibilityExpressionMetadata(Context context, String dataset, String table) {
    VersionedTableMetadata metadata =
        new VersionedMetadataObject().allMetadataForTable(context, dataset, table);
    return metadata.getTable().getVisibility();
  }

  public String lookupVisibilityExpressionMetadata(
      Context context, MetadataVersionObject version, String dataset, String table) {
    VersionedTableMetadata metadata =
        new VersionedMetadataObject().allMetadataForTable(context, version, dataset, table);
    return metadata.getTable().getVisibility();
  }

  public String lookupVisibilityExpressionMetadata(
      Context context, MetadataVersionObject version, String dataset, String table, String column) {
    if (logger.isDebugEnabled()) {
      logger.debug(
          format(
              "looking up visibility for dataset: %s table: %s column: %s",
              dataset, table, column));
    }
    VersionedTableMetadata metadata =
        new VersionedMetadataObject().allMetadataForTable(context, version, dataset, table);
    return findColumnVisibilityExpression(metadata, dataset, table, column);
  }

  public String lookupVisibilityExpressionMetadata(
      Context context, String dataset, String table, String column) {
    if (logger.isDebugEnabled()) {
      logger.debug(
          format(
              "looking up visibility for dataset: %s table: %s column: %s",
              dataset, table, column));
    }
    VersionedTableMetadata metadata =
        new VersionedMetadataObject().allMetadataForTable(context, dataset, table);
    return findColumnVisibilityExpression(metadata, dataset, table, column);
  }

  protected String findColumnVisibilityExpression(
      VersionedTableMetadata metadata, String dataset, String table, String column) {
    logger.debug(format("metadata: %s", metadata.getVersion()));
    Map<String, VersionedMetadataObject> cols = metadata.getColumns();
    if (logger.isDebugEnabled()) {
      logger.debug(format("found %s visibilities", cols.entrySet().size()));
      cols.entrySet()
          .forEach(
              entry -> {
                String msg =
                    format(
                        "found column: %s viz: %s",
                        entry.getKey(), entry.getValue().getVisibility());
                logger.debug(msg);
              });
    }
    if (cols.containsKey(column)) {
      VersionedMetadataObject columnMetadata = cols.get(column);
      return columnMetadata.getVisibility();
    } else {
      throw new IllegalStateException(
          format(
              "visibility expression was not found for dataset: %s table: %s column: %s",
              dataset, table, column));
    }
  }
}
