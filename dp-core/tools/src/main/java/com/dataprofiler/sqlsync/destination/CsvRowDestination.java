package com.dataprofiler.sqlsync.destination;

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
import static java.util.Objects.isNull;

import java.time.Duration;
import java.time.Instant;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvRowDestination implements RowDestination {
  private static final Logger logger = LoggerFactory.getLogger(JsonRowDestination.class);
  private static final String EXPORT_SYMBOL = "\u2192";

  SaveMode saveMode = DEFAULT_SAVE_MODE;
  String outputPath = "csv.out";
  int repartition = -1;

  public CsvRowDestination() {
    super();
  }

  public CsvRowDestination(String outputPath) {
    this(outputPath, -1);
  }

  public CsvRowDestination(String outputPath, int repartition) {
    super();
    this.outputPath = outputPath;
    this.repartition = repartition;
  }

  public void write(Dataset<Row> rows, String dest) {
    this.outputPath = dest;
    write(rows);
  }

  @Override
  public void write(Dataset<Row> rows) {
    if (isNull(this.outputPath)) {
      throw new IllegalStateException("output path is null");
    }
    if (isNull(rows) || rows.isEmpty()) {
      return;
    }
    Instant start = now();
    if (repartition > 0) {
      rows = rows.repartition(repartition);
    }
    logger.info(format("writing csv to: %s%n", outputPath));
    rows.write().mode(this.saveMode).option("header", "true").csv(this.outputPath);

    Instant end = now();
    Duration duration = between(start, end);
    if (logger.isInfoEnabled()) {
      logger.info(format("%s finished writing export time: %", EXPORT_SYMBOL, duration));
    }
  }

  public void setDest(String outputPath) {
    this.outputPath = outputPath;
  }
}
