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

import com.dataprofiler.loader.TableLoader;
import com.dataprofiler.loader.config.CsvFileParams;
import com.dataprofiler.loader.config.CsvLoaderConfig;
import com.dataprofiler.util.BasicAccumuloException;
import java.io.IOException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class CsvTestLoader {

  public static void main(String[] args)
      throws IOException, BasicAccumuloException, InvalidSchemaFormatException {
    for (String f : args) {
      System.out.println(f);
    }
    CsvLoaderConfig config = new CsvLoaderConfig();
    System.out.println("parsing args");
    System.out.println(config.parse(args));
    System.out.println(config.parameters);

    config.runSparkLocally = true;
    DPSparkContext context =
        new DPSparkContext(
            config,
            "CsvTestLoader",
            DPSparkContext.createDefaultSparkConf(config, "CsvTestLoader"));

    if (config.parameters.size() < 2) {
      System.out.println("Usage: --input-file <input file>");
    }

    config.iteratorStartIndex = 0;

    CsvFileParams f = config.toFileParams();
    System.out.println(f);
    SparkSession spark = context.createSparkSession();

    Dataset<Row> origTable = TableLoader.readCSV(spark, f);

    System.out.println(origTable.take(1));
    origTable.printSchema();
  }
}
