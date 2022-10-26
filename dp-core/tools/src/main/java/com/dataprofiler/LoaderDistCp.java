// distcp has some things (like a default ctor) that are package private...
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

import com.beust.jcommander.Parameter;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Config;
import com.dataprofiler.util.Context;
import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.tools.DistCp;
import org.apache.hadoop.tools.DistCpOptions;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoaderDistCp extends DistCp {

  public static class LoaderDistCpConfig extends Config {
    @Parameter(names = "--src")
    public String src;

    @Parameter(names = "--dest")
    public String dest;
  }

  public static final Logger LOG = LoggerFactory.getLogger(LoaderDistCp.class);

  public LoaderDistCp(Configuration configuration, DistCpOptions inputOptions) throws Exception {
    super(configuration, inputOptions);
  }

  @Override
  public int run(String[] argv) {
    try {
      Job j = this.execute();
      if (!j.waitForCompletion(true)) {
        LOG.warn("Waiting for dist cp job to finish was unsuccessful.");
        return 127;
      }
    } catch (Exception e) {
      LOG.error("Failed to create LoaderDistCp instance.", e);
      return -1;
    }
    return 0;
  }

  public static void main(String[] args) {
    LoaderDistCpConfig config = new LoaderDistCpConfig();
    try {
      if (!config.parse(args)) {
        System.exit(127);
      }
    } catch (IOException e) {
      LOG.error("Error parsing CLI arguments.", e);
      System.exit(127);
    }

    DistCpOptions distCpOpts = 
        new DistCpOptions
            .Builder(java.util.Collections.singletonList(new Path(config.src)),
                new Path(config.dest))
            .build();

    Context context;
    try {
      context = new Context(config);
    } catch (BasicAccumuloException e) {
      LOG.error("Failed to create a context from the CLI configuration.", e);
      return;
    }
    Configuration hadoopConfig = context.createHadoopConfigurationFromConfig();
    // hadoopConfig.set("distcp.copy.listing.class", "org.apache.hadoop.tools.SimpleCopyListing");

    LOG.info("Input paths: {}", distCpOpts.getSourcePaths());

    try {
      System.exit(ToolRunner.run(new LoaderDistCp(hadoopConfig, distCpOpts), args));
    } catch (Exception e) {
      LOG.error("OOPS", e);
    }
  }
}
