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

import com.dataprofiler.loader.config.LoaderConfig;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Config;
import com.dataprofiler.util.Context;
import java.io.IOException;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;

public class DPSparkContext extends Context {

  private String appName;
  private SparkConf sparkConfig;
  private JavaSparkContext sparkContext;

  public DPSparkContext(JavaSparkContext jsc) throws BasicAccumuloException, IOException {
    super();
    sparkContext = jsc;
    sparkConfig = sparkContext.getConf();
    appName = sparkConfig.get("spark.app.name");
    hadoopConfiguration = sparkContext.hadoopConfiguration();
  }

  public DPSparkContext(SparkContext context, Authorizations auths)
      throws BasicAccumuloException, IOException {
    super();
    sparkContext = JavaSparkContext.fromSparkContext(context);
    sparkConfig = sparkContext.getConf();
    this.authorizations = auths;
    appName = sparkConfig.get("spark.app.name");
    hadoopConfiguration = sparkContext.hadoopConfiguration();
  }

  public DPSparkContext(String appName, String[] argv) throws BasicAccumuloException, IOException {
    super(argv);
    this.appName = appName;
    sparkConnect();
  }

  public DPSparkContext(Config config, String appName, SparkConf sparkConf)
      throws BasicAccumuloException {
    super(config);
    this.sparkConfig = sparkConf;
    sparkConfig.setAppName(appName);
    this.appName = appName;
    sparkConnect();
  }

  public DPSparkContext(Config config, Authorizations auths, String appName)
      throws BasicAccumuloException {
    super(config);
    this.authorizations = auths;
    this.sparkConfig = createDefaultSparkConf(config, appName);
    this.appName = appName;
    sparkConnect();
  }

  public DPSparkContext(Config config, String appName) throws BasicAccumuloException {
    super(config);
    this.sparkConfig = createDefaultSparkConf(config, appName);
    this.appName = appName;
    sparkConnect();
  }

  public DPSparkContext(String appName) throws BasicAccumuloException, IOException {
    super();
    this.appName = appName;
    sparkConnect();
  }

  public DPSparkContext(Context context, String appName) throws BasicAccumuloException {
    super(context);
    this.appName = appName;
    sparkConnect();
  }

  public DPSparkContext(DPSparkContext context, Authorizations auths)
      throws BasicAccumuloException {
    super(context, auths);
    this.appName = context.appName;
    sparkConnect();
  }

  public static SparkConf createDefaultSparkConf(Config config, String appName)
      throws BasicAccumuloException {
    Class<?>[] classes;
    try {
      classes =
          new Class<?>[] {
            Class.forName("org.apache.accumulo.core.data.Key"),
            Class.forName("org.apache.accumulo.core.data.Value")
          };
    } catch (ClassNotFoundException e) {
      throw new BasicAccumuloException("Could not find key or value class");
    }

    SparkConf sparkConfig =
        new SparkConf()
            .setAppName(appName)
            .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
            .set("spark.sql.orc.enabled", "true")
            .registerKryoClasses(classes);

    if (config.runSparkLocally) {
      sparkConfig.setMaster("local[*]");
      sparkConfig.set("driver.memory", "4g");
      sparkConfig.set("driver.memoryOverhead", "1000");
    }

    if (config.runSparkFromPlayFramework) {
      sparkConfig.setMaster("local[*]");
      sparkConfig.set("driver.memory", "4g");
      sparkConfig.set("driver.memoryOverhead", "1000");
    }

    sparkConfig.set(
        "spark.driver.allowMultipleContexts", config.sparkAllowMultipleContexts ? "true" : "false");

    return sparkConfig;
  }

  public void sparkConnect() throws BasicAccumuloException {
    if (sparkConfig == null) {
      sparkConfig = createDefaultSparkConf(config, appName);
    }

    if (config instanceof LoaderConfig && ((LoaderConfig) config).buildHadoopConfig) {
      String fs = config.hadoopDefaultFs;
      String nn1 = config.hadoopNamenode1;
      String nn2 = config.hadoopNamenode2;

      sparkConfig.set("spark.hadoop.fs.defaultFS", getDefaultHadoopFSFromConfig());
      sparkConfig.set("spark.hadoop.dfs.nameservices", fs);
      sparkConfig.set("spark.hadoop.dfs.ha.namenodes." + fs, "nn1,nn2");
      sparkConfig.set(
          String.format("spark.hadoop.dfs.namenode.rpc-address.%s.nn1", fs),
          String.format("%s:8020", nn1));
      sparkConfig.set(
          String.format("spark.hadoop.dfs.namenode.http-address.%s.nn1", fs),
          String.format("%s:5070", nn1));
      sparkConfig.set(
          String.format("spark.hadoop.dfs.namenode.https-address.%s.nn1", fs),
          String.format("%s:5071", nn1));
      sparkConfig.set(
          String.format("spark.hadoop.dfs.namenode.rpc-address.%s.nn2", fs),
          String.format("%s:8020", nn2));
      sparkConfig.set(
          String.format("spark.hadoop.dfs.namenode.http-address.%s.nn2", fs),
          String.format("%s:5070", nn2));
      sparkConfig.set(
          String.format("spark.hadoop.dfs.namenode.https-address.%s.nn2", fs),
          String.format("%s:5071", nn2));
      sparkConfig.set(
          "spark.hadoop.dfs.namenode.shared.edits.dir",
          String.format("qjournal://%ss/%s", nn1, fs));
      sparkConfig.set(
          "spark.hadoop.dfs.client.failover.proxy.provider." + fs,
          "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider");
      sparkConfig.set("spark.hadoop.dfs.ha.fencing.methods", "shell(/usr/bin/true)");
      sparkConfig.set("spark.hadoop.dfs.ha.automatic-failover.enabled", "true");
      sparkConfig.set("spark.hadoop.fs.permissions.umask-mode", "0002");
    }

    sparkContext = new JavaSparkContext(sparkConfig);
    hadoopConfiguration = sparkContext.hadoopConfiguration();
    hadoopConfiguration.setInt("dfs.replication", 3);
  }

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public SparkConf getSparkConfig() {
    return sparkConfig;
  }

  public void setSparkConfig(SparkConf sparkConfig) {
    this.sparkConfig = sparkConfig;
  }

  public JavaSparkContext getSparkContext() {
    return sparkContext;
  }

  public void setSparkContext(JavaSparkContext sparkContext) {
    this.sparkContext = sparkContext;
  }

  public SparkSession createSparkSession() {
    SparkSession session = SparkSession.builder().appName(appName).getOrCreate();

    if (config.runSparkLocally) {
      session.sparkContext().setLogLevel("WARN");
    }

    return session;
  }
}
