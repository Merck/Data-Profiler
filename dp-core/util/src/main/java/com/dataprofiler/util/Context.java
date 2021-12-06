package com.dataprofiler.util;

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

import static org.apache.hadoop.mapreduce.Job.getInstance;

import com.dataprofiler.util.objects.MetadataVersionObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.BatchDeleter;
import org.apache.accumulo.core.client.BatchScanner;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.IteratorSetting.Column;
import org.apache.accumulo.core.client.NamespaceExistsException;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.admin.NamespaceOperations;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat;
import org.apache.accumulo.core.client.mapreduce.lib.impl.InputConfigurator;
import org.apache.accumulo.core.client.mapreduce.lib.impl.OutputConfigurator;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.util.Pair;
import org.apache.accumulo.minicluster.MiniAccumuloCluster;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;

/**
 * A context object represents a connection to the Data Profiler backend and all of the associated
 * config.
 */
public class Context implements Serializable {
  protected boolean neverConnect =
      false; // this is used when we want to only use Spark - this is kind of a hack, but
  // this makes
  // things fail early
  protected Config config;
  protected ObjectMapper mapper = new ObjectMapper();
  protected Connector connector;
  protected Authorizations authorizations;
  protected Configuration hadoopConfiguration;
  protected MiniAccumuloCluster accumulo;

  protected MetadataVersionObject defaultVersion;

  private static final TypeReference<ArrayList<String>> typeReference =
      new TypeReference<ArrayList<String>>() {};

  public Context(Context context, Authorizations authorizations) {
    initFromContext(context);
    this.authorizations = authorizations;
  }

  public Context(Context context) throws BasicAccumuloException {
    initFromContext(context);
    refreshAuthorizations();
  }

  public Context(String[] argv) throws BasicAccumuloException, IOException {
    config = new Config();
    config.parse(argv);
  }

  public Context(Config config) throws BasicAccumuloException {
    this.config = config;
  }

  public Context() throws BasicAccumuloException, IOException {
    config = new Config();
    config.parse(new String[0]);
  }

  private void initFromContext(Context context) {
    this.config = context.config;
    this.mapper = context.mapper;
    this.connector = context.connector;
  }

  public void createAllAccumuloTables() throws BasicAccumuloException {
    connect();
    NamespaceOperations nops = connector.namespaceOperations();
    TableOperations tops = connector.tableOperations();

    Set<String> namespaces = new HashSet<>();
    for (String table : config.getAllTables()) {
      String[] ns = table.split("\\.");
      if (ns.length > 0 && !namespaces.contains(ns[0])) {
        try {
          nops.create(ns[0]);
        } catch (AccumuloException | AccumuloSecurityException e) {
          throw new BasicAccumuloException("Error creating namespace " + e);
        } catch (NamespaceExistsException e) {
          e.printStackTrace();
        }

        namespaces.add(ns[0]);
      }

      try {
        tops.create(table);
      } catch (AccumuloException | AccumuloSecurityException e) {
        throw new BasicAccumuloException("Error creating table " + e);
      } catch (TableExistsException e) {
        e.printStackTrace();
      }
    }
  }

  public void connect() throws BasicAccumuloException {
    if (neverConnect) {
      throw new BasicAccumuloException("Never connect was set - yet we are connecting!");
    }
    if (connector != null) {
      return;
    }

    ZooKeeperInstance zkInstance =
        new ZooKeeperInstance(config.accumuloInstance, config.zookeepers);

    try {
      connector =
          zkInstance.getConnector(config.accumuloUser, new PasswordToken(config.accumuloPassword));
      refreshAuthorizations();
    } catch (AccumuloException e) {
      throw new BasicAccumuloException("Unable to create connection to Accumulo");
    } catch (AccumuloSecurityException e) {
      throw new BasicAccumuloException("Authentication error while trying to connect to Accumulo");
    }
  }

  public void refreshAuthorizations() throws BasicAccumuloException {
    connect();
    try {
      authorizations = connector.securityOperations().getUserAuthorizations(config.accumuloUser);
    } catch (AccumuloException | AccumuloSecurityException e) {
      throw new BasicAccumuloException("Failed to refresh authorizations " + e);
    }
  }

  public void setAuthorizationsFromJson(String authorizationsAsJson)
      throws BasicAccumuloException, IOException {
    connect();
    ArrayList<String> stringAuthorizations = mapper.readValue(authorizationsAsJson, typeReference);
    List<byte[]> byteAuthorizations =
        stringAuthorizations.stream()
            .map(a -> a.getBytes(StandardCharsets.UTF_8))
            .collect(Collectors.toList());

    authorizations = new Authorizations(byteAuthorizations);
  }

  public Scanner createScanner(String accumuloTable) throws BasicAccumuloException {
    connect();
    try {
      return connector.createScanner(accumuloTable, authorizations);
    } catch (TableNotFoundException e) {
      throw new BasicAccumuloException(String.format("Table '%s' does not exist", accumuloTable));
    }
  }

  public BatchScanner createBatchScanner(String accumuloTable) throws BasicAccumuloException {
    connect();
    try {
      return connector.createBatchScanner(
          accumuloTable, authorizations, config.accumuloScannerThreads);
    } catch (TableNotFoundException e) {
      throw new BasicAccumuloException(String.format("Table '%s' does not exist", accumuloTable));
    }
  }

  public BatchWriter createBatchWriter(
      String accumuloTable, Long maxMem, Integer maxThreads, Integer maxLatency)
      throws BasicAccumuloException {
    connect();
    BatchWriterConfig batchWriterConfig = new BatchWriterConfig();
    batchWriterConfig.setMaxMemory(maxMem);
    batchWriterConfig.setMaxWriteThreads(maxThreads);
    batchWriterConfig.setMaxLatency(maxLatency, TimeUnit.SECONDS);

    try {
      return connector.createBatchWriter(accumuloTable, batchWriterConfig);
    } catch (TableNotFoundException e) {
      throw new BasicAccumuloException(e.toString());
    }
  }

  public BatchWriter createBatchWriter(String accumuloTable) throws BasicAccumuloException {
    connect();
    return createBatchWriter(
        accumuloTable,
        Const.DEFAULT_MAX_MEM,
        Const.DEFAULT_MAX_WRITE_THREADS,
        Const.DEFAULT_MAX_LATENCY);
  }

  public BatchDeleter createBatchDeleter(String accumuloTable) throws BasicAccumuloException {
    connect();
    return createBatchDeleter(
        accumuloTable,
        Const.DEFAULT_MAX_MEM,
        Const.DEFAULT_MAX_WRITE_THREADS,
        Const.DEFAULT_MAX_LATENCY);
  }

  public BatchDeleter createBatchDeleter(
      String accumuloTable, Long maxMem, Integer maxThreads, Integer maxLatency)
      throws BasicAccumuloException {
    connect();
    BatchWriterConfig batchWriterConfig = new BatchWriterConfig();
    batchWriterConfig.setMaxMemory(maxMem);
    batchWriterConfig.setMaxWriteThreads(maxThreads);
    batchWriterConfig.setMaxLatency(maxLatency, TimeUnit.SECONDS);
    try {
      return connector.createBatchDeleter(
          accumuloTable, authorizations, maxThreads, batchWriterConfig);
    } catch (TableNotFoundException e) {
      throw new BasicAccumuloException(e.toString());
    }
  }

  public void applyInputConfiguration(
      Configuration configuration,
      String table,
      List<Text> colFams,
      List<Column> columns,
      List<Range> ranges,
      List<IteratorSetting> iterators)
      throws BasicAccumuloException {
    connect();
    Class<?> CLASS = AccumuloInputFormat.class;
    Config config = getConfig();

    InputConfigurator.setZooKeeperInstance(
        CLASS,
        configuration,
        new ClientConfiguration()
            .withInstance(config.accumuloInstance)
            .withZkHosts(config.zookeepers));

    try {
      InputConfigurator.setConnectorInfo(
          CLASS, configuration, config.accumuloUser, new PasswordToken(config.accumuloPassword));
    } catch (AccumuloSecurityException e) {
      throw new BasicAccumuloException(e.toString());
    }

    InputConfigurator.setScanAuthorizations(CLASS, configuration, getAuthorizations());

    InputConfigurator.setInputTableName(CLASS, configuration, table);

    if (ranges.size() > 0) {
      InputConfigurator.setRanges(CLASS, configuration, ranges);
    } else {
      InputConfigurator.setRanges(
          CLASS, configuration, Collections.singleton(new Range(Const.LOW_BYTE, Const.HIGH_BYTE)));
    }

    ArrayList<Pair<Text, Text>> cols = new ArrayList<>();

    for (Text c : colFams) {
      cols.add(new Pair<Text, Text>(c, null));
    }

    for (Column c : columns) {
      cols.add(new Pair<Text, Text>(c.getColumnFamily(), c.getColumnQualifier()));
    }

    if (!cols.isEmpty()) {
      InputConfigurator.fetchColumns(CLASS, configuration, cols);
    }

    for (IteratorSetting i : iterators) {
      InputConfigurator.addIterator(CLASS, configuration, i);
    }

    InputConfigurator.setBatchScan(CLASS, configuration, true);
  }

  public void applyOutputConfiguration(Configuration configuration) throws BasicAccumuloException {
    connect();
    Class<?> CLASS = AccumuloOutputFormat.class;
    Config config = getConfig();

    OutputConfigurator.setZooKeeperInstance(
        CLASS,
        configuration,
        new ClientConfiguration()
            .withInstance(config.accumuloInstance)
            .withZkHosts(config.zookeepers));

    try {
      OutputConfigurator.setConnectorInfo(
          CLASS, configuration, config.accumuloUser, new PasswordToken(config.accumuloPassword));
    } catch (AccumuloSecurityException e) {
      throw new BasicAccumuloException(e.toString());
    }

    OutputConfigurator.setCreateTables(CLASS, configuration, true);
  }

  public Job createInputJob(
      String table,
      List<Text> colFams,
      List<Column> columns,
      List<Range> ranges,
      List<IteratorSetting> iterators)
      throws IOException, BasicAccumuloException {
    connect();
    Job job = getInstance(getHadoopConfiguration());

    applyInputConfiguration(job.getConfiguration(), table, colFams, columns, ranges, iterators);

    return job;
  }

  public Job createOutputJob(String table) throws IOException, BasicAccumuloException {
    connect();
    Job job = getInstance(getHadoopConfiguration());
    applyOutputConfiguration(job.getConfiguration());
    AccumuloOutputFormat.setDefaultTableName(job, table);

    return job;
  }

  public String getDefaultHadoopFSFromConfig() {
    String fs = getConfig().hadoopDefaultFs;
    return String.format("hdfs://%s/", fs);
  }

  /**
   * Create a Hadoop Configuration for the local file system. This is mainly used for local
   * development and testing
   *
   * @return
   */
  public Configuration createHadoopConfigurationForLocal() {
    Configuration config = new Configuration();
    config.set("fs.defaultFS", String.format("file://%s/", this.getConfig().loadOutputDest));
    return config;
  }

  /***
   * Create a new Hadoop Configuration entirely from our config. This differs from
   * getHadoopConfiguration - which will pull Configuration from the environment /
   * filesystem - in that this is entirely based on what is in the Data Profiler
   * Config object.
   */
  public Configuration createHadoopConfigurationFromConfig() {
    Configuration config = new Configuration();
    String fs = getConfig().hadoopDefaultFs;
    String nn1 = getConfig().hadoopNamenode1;
    String nn2 = getConfig().hadoopNamenode2;
    config.set("fs.defaultFS", getDefaultHadoopFSFromConfig());
    config.set("dfs.nameservices", fs);
    config.set("dfs.ha.namenodes." + fs, "nn1,nn2");
    config.set(String.format("dfs.namenode.rpc-address.%s.nn1", fs), String.format("%s:8020", nn1));
    config.set(
        String.format("dfs.namenode.http-address.%s.nn1", fs), String.format("%s:5070", nn1));
    config.set(
        String.format("dfs.namenode.https-address.%s.nn1", fs), String.format("%s:5071", nn1));
    config.set(String.format("dfs.namenode.rpc-address.%s.nn2", fs), String.format("%s:8020", nn2));
    config.set(
        String.format("dfs.namenode.http-address.%s.nn2", fs), String.format("%s:5070", nn2));
    config.set(
        String.format("dfs.namenode.https-address.%s.nn2", fs), String.format("%s:5071", nn2));
    config.set("dfs.namenode.shared.edits.dir", String.format("qjournal://%ss/%s", nn1, fs));
    config.set(
        "dfs.client.failover.proxy.provider." + fs,
        "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider");
    config.set("dfs.ha.fencing.methods", "shell(/usr/bin/true)");
    config.set("dfs.ha.automatic-failover.enabled", "true");
    config.set("fs.permissions.umask-mode", "0002");
    return config;
  }

  public Config getConfig() {
    return config;
  }

  public void setConfig(Config config) {
    this.config = config;
  }

  public ObjectMapper getMapper() {
    return mapper;
  }

  public void setMapper(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public Connector getConnector() throws BasicAccumuloException {
    connect();
    return connector;
  }

  public void setConnector(Connector connector) {
    this.connector = connector;
  }

  public Authorizations getAuthorizations() {
    return authorizations;
  }

  public void setAuthorizations(Authorizations authorizations) {
    this.authorizations = authorizations;
  }

  public Configuration getHadoopConfiguration() {
    return hadoopConfiguration;
  }

  public void setHadoopConfiguration(Configuration hadoopConfiguration) {
    this.hadoopConfiguration = hadoopConfiguration;
  }

  /**
   * * Get the current metadata version. Note that this makes a call into Accumulo so that it's not
   * stale. It's suggested that callers cache the return for the duration of a relatively
   * short-lived operation but not long term.
   *
   * @return current version of the metadata
   */
  public MetadataVersionObject getCurrentMetadataVersion() {
    return new MetadataVersionObject().fetchCurrentVersion(this);
  }

  public MetadataVersionObject getMetadataVersion(String version) {
    return new MetadataVersionObject().fetchVersion(this, version);
  }

  public boolean isNeverConnect() {
    return neverConnect;
  }

  public void setNeverConnect(boolean neverConnect) throws BasicAccumuloException {
    if (neverConnect == true && connector != null) {
      throw new BasicAccumuloException("Attempted to set neverConnect, but was already connected.");
    }
    this.neverConnect = neverConnect;
  }
}
