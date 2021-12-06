package com.dataprofiler;

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

import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.objects.DataScanSpec;
import com.dataprofiler.util.objects.DownloadSpec;
import com.dataprofiler.util.objects.VersionedDatasetMetadata.MissingMetadataException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.log4j.Logger;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;

public class CsvExport {

  private DPSparkContext context;
  private SparkSession spark;
  private SparkAccumuloIO io;
  private String outputPath;
  private static final Logger logger = Logger.getLogger(CsvExport.class);

  public static void main(String[] args)
      throws IOException, BasicAccumuloException, MissingMetadataException {
    CsvExport ce = new CsvExport();
    ce.execute(args);
  }

  public void execute(String[] args)
      throws IOException, BasicAccumuloException, MissingMetadataException {
    JobExecutorConfig config = new JobExecutorConfig();

    if (!config.parse(args) || config.fname == null) {
      System.err.println("Usage: <main_class> [options] --fname " + "download_file <output_path>");
      System.exit(1);
    }

    context = new DPSparkContext(config, "CsvExport");

    DownloadSpec d = null;

    String json = new String(Files.readAllBytes(Paths.get(config.fname)));
    try {
      d = DownloadSpec.fromJson(json);
    } catch (Exception e) {
      System.err.println("Failed to read in Download spec: " + e);
      System.exit(1);
    }

    outputPath = context.getConfig().parameters.get(0);

    spark = context.createSparkSession();
    io = new SparkAccumuloIO(context);

    int index = -1;
    for (DataScanSpec ds : d.getDownloads()) {
      // We are going to use the index as an id for the files output and we expect this to be
      // iterpreted by external tools (order in JSON arrays is supposed to be preserved, so this
      // should be safe). But in order for this to make sense, we are going to use the position
      // in the JSON array, so we increment this way so that the indexes are used even if we
      // hit a continue later down because the dataset is empty or something.
      index++;

      ds.setReturnFullValues(true);
      Dataset<Row> rows = TableExporter.exportDataScanSpec(context, spark, io, ds);
      if (rows == null) {
        logger.warn("Dataset was empty - skipping: " + ds);
        continue;
      }

      rows.printSchema();
      String path = String.format("%s/%d", outputPath, index);

      rows.write().mode(SaveMode.Overwrite).option("header", "true").csv(path);
    }
  }
}
