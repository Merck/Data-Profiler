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

import com.dataprofiler.accumulo.MiniAccumuloConfigUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.minicluster.MiniAccumuloCluster;
import org.apache.accumulo.minicluster.MiniAccumuloConfig;
import org.apache.log4j.Logger;

public class MiniAccumuloContext extends Context implements AutoCloseable {

  private static final Logger logger = Logger.getLogger(MiniAccumuloContext.class);
  private Path tempPath;
  private MiniAccumuloCluster accumulo;
  private static final String[] DEFAULT_AUTHS = new String[] {"LIST.PUBLIC_DATA", "LIST.PRIVATE_DATA"};
  private String[] rootAuths;
  private boolean randomZookeeperPort = false;

  public MiniAccumuloContext() throws BasicAccumuloException, IOException {
    super();
    this.rootAuths = DEFAULT_AUTHS;
  }

  public MiniAccumuloContext(Config config) throws BasicAccumuloException {
    super(config);
    this.rootAuths = DEFAULT_AUTHS;
  }

  public MiniAccumuloContext(Config config, String[] auths) throws BasicAccumuloException {
    super(config);
    this.rootAuths = auths;
  }

  public MiniAccumuloContext(String[] auths) throws BasicAccumuloException, IOException {
    this();
    this.rootAuths = auths;
  }

  private static void replaceInFile(String filePath, String oldString, String newString) {
    File fileToBeModified = new File(filePath);
    String oldContent = "";
    BufferedReader reader = null;
    FileWriter writer = null;
    try {
      reader = new BufferedReader(new FileReader(fileToBeModified));
      String line = reader.readLine();
      while (line != null) {
        oldContent = oldContent + line + System.lineSeparator();
        line = reader.readLine();
      }
      String newContent = oldContent.replaceAll(oldString, newString);
      writer = new FileWriter(fileToBeModified);
      writer.write(newContent);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        reader.close();
        writer.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void createMiniAccumulo() throws BasicAccumuloException {
    try {
      tempPath = Files.createTempDirectory("mini-accumulo");
      tempPath.toFile().deleteOnExit();
      logger.info(String.format("created mini accumulo in %s", tempPath.toString()));
    } catch (IOException e) {
      throw new BasicAccumuloException("Failed to create temp directory " + e);
    }

    logger.info("Setting accumulo password to " + config.accumuloPassword);
    MiniAccumuloConfig mac = new MiniAccumuloConfig(tempPath.toFile(), config.accumuloPassword);
    if (!randomZookeeperPort) {
      mac = mac.setZooKeeperPort(2181);
    }

    try {
      accumulo = new MiniAccumuloCluster(mac);
      // set classpath, needed for java 9+
      MiniAccumuloConfigUtil.setConfigClassPath(accumulo.getConfig());
      // allow connections from non-localhosts
      logger.info("Allowing zookeeper connections from all hosts");
      replaceInFile(
          tempPath.toString() + "/conf/zoo.cfg",
          "clientPortAddress=127.0.0.1",
          "clientPortAddress=0.0.0.0");
      logger.info("Starting accumulo");
      accumulo.start();
      logger.info("accumulo has been started");
    } catch (IOException | InterruptedException e) {
      throw new BasicAccumuloException("Error starting cluster " + e);
    }

    config.accumuloInstance = accumulo.getInstanceName();
    config.zookeepers = accumulo.getZooKeepers();
  }

  @Override
  public void connect() throws BasicAccumuloException {
    if (this.client != null) {
      return;
    }
    createMiniAccumulo();
    // Always run Spark locally for mini-acummulo
    config.runSparkLocally = true;
    super.connect();

    // NOTE: Setup auths for the root user, be sure to get all the potential auths in the system
    //  or the API may see the following error
    // java.lang.RuntimeException: org.apache.accumulo.core.client.AccumuloSecurityException:
    // Error BAD_AUTHORIZATIONS for user root on table curr.metadata(ID:4) -
    // The user does not have the specified authorizations assigned]
    if (this.rootAuths == null) {
      logger.warn(
          "rootAuths are null, this should not happen, they should at least be assigned a default");
      logger.warn("assigning rootAuths again...");
      this.rootAuths =
          new String[] {
            "LIST.PUBLIC_DATA",
            "LIST.PRIVATE_DATA"
          };
    }
    logger.info("\n---------using root auths - " + Arrays.toString(this.rootAuths));
    try {
      getClient()
          .securityOperations()
          .changeUserAuthorizations("root", new Authorizations(this.rootAuths));
    } catch (AccumuloException | AccumuloSecurityException e) {
      throw new BasicAccumuloException(e.toString());
    }
    refreshAuthorizations();
    createAllAccumuloTables();
  }

  public void close() throws IOException, InterruptedException {
    logger.info("Stopping accumulo...");
    accumulo.stop();
    logger.info("accumulo stopped");
    Files.walk(tempPath).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
  }

  public void serve() throws InterruptedException {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              public void run() {
                try {
                  close();
                } catch (InterruptedException | IOException e) {
                  e.printStackTrace();
                }
              }
            });
    Thread.sleep(Long.MAX_VALUE);
  }

  public Path getTempPath() {
    return Paths.get(this.tempPath.toString());
  }

  public MiniAccumuloConfig getMiniAccumuloConfig() {
    return this.accumulo.getConfig();
  }

  public boolean isRandomZookeeperPort() {
    return randomZookeeperPort;
  }

  public void setRandomZookeeperPort(boolean randomZookeeperPort) {
    this.randomZookeeperPort = randomZookeeperPort;
  }
}
