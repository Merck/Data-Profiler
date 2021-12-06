package com.dataprofiler.bulkIngest;

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

import com.beust.jcommander.Parameter;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Config;
import com.dataprofiler.util.Context;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;

/***
 * AccumuloBulkIngest takes a directory on HDFS output by the TableLoader in
 * split ingest mode and does Accumulo Bulk Ingest for all of the files in that
 * directory.
 */
public class AccumuloBulkIngest {
  private static final Logger logger = Logger.getLogger(AccumuloBulkIngest.class);
  private FileSystem fs;

  public static void main(String[] args)
      throws BasicAccumuloException, IOException, URISyntaxException {
    ABIConfig config = new ABIConfig();
    if (!config.parse(args)) {
      config.printHelp();
      logger.info("AccumuloBulkIngest --input-dir <path>");
      System.exit(1);
    }
    Context context = new Context(config);
    AccumuloBulkIngest bulkIngest = new AccumuloBulkIngest();
    bulkIngest.ingest(context, config);
  }

  // Here is the basic layout that we are expecting. Top level is a uuid, next
  // level is the name of the accumulo table, next level is just
  // dataprofiler tables but named by a uuid with rfiles.
  //
  // ubuntu@dataprofiler-converged-cluster-accumulo-worker-1:~$ /opt/hadoop/current/bin/hdfs dfs -ls
  // /loader/8647bdaa-674e-4fe6-bf99-214263f06f0a/
  // Found 5 items
  // drwxr-xr-x - spark spark 0 2020-05-19 01:52
  // /loader/8647bdaa-674e-4fe6-bf99-214263f06f0a/curr.columnCounts
  // drwxr-xr-x - spark spark 0 2020-05-19 01:52
  // /loader/8647bdaa-674e-4fe6-bf99-214263f06f0a/curr.datawaveRows
  // drwxr-xr-x - spark spark 0 2020-05-19 01:52
  // /loader/8647bdaa-674e-4fe6-bf99-214263f06f0a/curr.index
  // drwxr-xr-x - spark spark 0 2020-05-19 01:52
  // /loader/8647bdaa-674e-4fe6-bf99-214263f06f0a/curr.metadata
  // drwxr-xr-x - spark spark 0 2020-05-19 01:52
  // /loader/8647bdaa-674e-4fe6-bf99-214263f06f0a/curr.samples
  // ubuntu@dataprofiler-converged-cluster-accumulo-worker-1:~$ /opt/hadoop/current/bin/hdfs dfs -ls
  // /loader/8647bdaa-674e-4fe6-bf99-214263f06f0a/curr.datawaveRows/
  // Found 2 items
  // drwxr-xr-x - spark spark 0 2020-05-19 01:52
  // /loader/8647bdaa-674e-4fe6-bf99-214263f06f0a/curr.datawaveRows/58942319-bc65-4678-abc7-18e1e9f0dc97
  // drwxr-xr-x - spark spark 0 2020-05-19 01:52
  // /loader/8647bdaa-674e-4fe6-bf99-214263f06f0a/curr.datawaveRows/e8872a4a-d30a-49bb-bb40-b95dcd46ba70
  // ubuntu@dataprofiler-converged-cluster-accumulo-worker-1:~$ /opt/hadoop/current/bin/hdfs dfs -ls
  // /loader/8647bdaa-674e-4fe6-bf99-214263f06f0a/curr.datawaveRows/58942319-bc65-4678-abc7-18e1e9f0dc97
  // Found 2 items
  // -rw-r--r-- 3 spark spark 0 2020-05-19 01:52
  // /loader/8647bdaa-674e-4fe6-bf99-214263f06f0a/curr.datawaveRows/58942319-bc65-4678-abc7-18e1e9f0dc97/_SUCCESS
  // -rw-r--r-- 3 spark spark 309 2020-05-19 01:52
  // /loader/8647bdaa-674e-4fe6-bf99-214263f06f0a/curr.datawaveRows/58942319-bc65-4678-abc7-18e1e9f0dc97/part-r-00000.rf
  public void ingest(Context context, ABIConfig config)
      throws IOException, BasicAccumuloException, URISyntaxException {
    final String dir = config.inputDir;

    TableOperations tops = context.getConnector().tableOperations();

    if (context.getConfig().runSparkLocally) {
      fs = FileSystem.get(context.createHadoopConfigurationForLocal());
    } else {
      fs = FileSystem.get(context.createHadoopConfigurationFromConfig());
    }

    Path failDir = new Path(dir + "/fail");
    logger.info("Trying to make failure directory " + failDir);
    fs.mkdirs(failDir);

    FileStatus[] tableFileStatus = fs.listStatus(new Path(dir), (p) -> !p.getName().equals("fail"));
    for (FileStatus lfs : tableFileStatus) logger.info(lfs);

    List<Path> accumuloTables =
        java.util.Arrays.asList(tableFileStatus).stream()
            .filter(t -> t.isDirectory())
            .map(t -> t.getPath())
            .collect(Collectors.toList());

    logger.info("Accumulo tables found: ");
    for (Path p : accumuloTables) logger.info(p);

    for (Path accumuloTablePath : accumuloTables) {
      String accumuloTableName = accumuloTablePath.getName();
      List<Path> tablePaths =
          java.util.Arrays.asList(fs.listStatus(accumuloTablePath)).stream()
              .map(f -> f.getPath())
              .collect(Collectors.toList());

      logger.info("Looping over table paths...");
      for (Path table : tablePaths) {
        logger.info("`- Table path: " + table);
        try {
          tops.importDirectory(accumuloTableName, table.toString(), failDir.toString(), true);
        } catch (Exception e) {
          logger.error("Failed to import table: " + table.toString());
          throw new BasicAccumuloException(e.toString());
        }
        // Throw exception if failures directory contains files
        if (fs.listFiles(failDir, true).hasNext()) {
          throw new IllegalStateException(
              "Bulk import failed!  Found files that failed to import "
                  + "in failures directory: "
                  + failDir);
        }
      }
    }

    if (config.cleanup) {
      fs.delete(new Path(dir), true);
    }
  }

  public static class ABIConfig extends Config {
    @Parameter(
        names = "--input-dir",
        description = "HDFS path to bulk load into Accumulo",
        required = true)
    public String inputDir;

    @Parameter(
        names = "--cleanup",
        description = "Clean up the import HDFS path upon success",
        arity = 1)
    public boolean cleanup = false;
  }
}
