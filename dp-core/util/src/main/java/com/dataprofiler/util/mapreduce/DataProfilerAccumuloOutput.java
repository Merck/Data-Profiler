package com.dataprofiler.util.mapreduce;

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

import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Context;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.accumulo.core.client.AccumuloClient;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.security.SecurityErrorCode;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.ClientProperty;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.TabletId;
import org.apache.accumulo.hadoop.mapreduce.AccumuloOutputFormat;
import org.apache.accumulo.hadoopImpl.mapreduce.lib.OutputConfigurator;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.OutputCommitter;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.log4j.Logger;

public abstract class DataProfilerAccumuloOutput extends OutputFormat<Text, Text> {

  private static final Class<?> CLASS = AccumuloOutputFormat.class;
  protected static final Logger log = Logger.getLogger(CLASS);
  protected static ObjectMapper mapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public abstract Mutation convertValue(String value) throws IOException;

  public abstract String getTableName(Context context);

  @Override
  public RecordWriter<Text, Text> getRecordWriter(TaskAttemptContext context)
      throws IOException, InterruptedException {
    try {
      return new AccumuloRecordWriter(context);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void checkOutputSpecs(JobContext jobContext) throws IOException, InterruptedException {
    Context context;
    try {
      context = new Context();
      context.applyOutputConfiguration(jobContext.getConfiguration());
    } catch (Exception e) {
      throw new IOException(e.toString());
    }

    Properties props = OutputConfigurator.getClientProperties(CLASS, jobContext.getConfiguration());

    if (props.isEmpty()) {
      throw new IOException("Connector info has not been set.");
    }

    String principal = props.getProperty(ClientProperty.AUTH_PRINCIPAL.getKey());
    String token = props.getProperty(ClientProperty.AUTH_TOKEN.getKey());
    AccumuloClient client = OutputConfigurator.createClient(CLASS, jobContext.getConfiguration());

    try {
      if (!client.securityOperations().authenticateUser(principal, new PasswordToken(token))){
        throw new IOException("Unable to authenticate user");
      }
    } catch (AccumuloException e) {
      throw new IOException(e);
    } catch (AccumuloSecurityException e) {
      throw new IOException(e);
    }
  }

  @Override
  public OutputCommitter getOutputCommitter(TaskAttemptContext taskAttemptContext) {
    return new NullOutputFormat<String, String>().getOutputCommitter(taskAttemptContext);
  }

  protected class AccumuloRecordWriter extends RecordWriter<Text, Text> {

    private final String tableName;
    private final BatchWriter bw;
    private final AccumuloClient client;
    private long mutCount = 0;
    private final long valCount = 0;

    protected AccumuloRecordWriter(TaskAttemptContext taskAttemptContext)
        throws AccumuloException, AccumuloSecurityException, IOException, TableNotFoundException {

      Context context;
      try {
        context = new Context();
      } catch (BasicAccumuloException e) {
        throw new IOException(e.toString());
      }

      this.tableName = getTableName(context);

      if (tableName == null) {
        throw new TableNotFoundException(null, "No table name provided", null);
      }
  
      this.client = OutputConfigurator.createClient(CLASS, taskAttemptContext.getConfiguration());

      this.bw =
          client.createBatchWriter(
              tableName,
              OutputConfigurator.getBatchWriterOptions(
                  CLASS, taskAttemptContext.getConfiguration()));
    }

    @Override
    public void write(Text key, Text value) throws IOException, InterruptedException {
      ++mutCount;

      try {
        bw.addMutation(convertValue(value.toString()));
      } catch (MutationsRejectedException e) {
        throw new IOException(e);
      }
    }

    @Override
    public void close(TaskAttemptContext taskAttemptContext)
        throws IOException, InterruptedException {
      log.debug("mutations written: " + mutCount + ", values written: " + valCount);

      try {
        bw.close();
      } catch (MutationsRejectedException e) {
        if (e.getSecurityErrorCodes().size() >= 0) {
          HashMap<String, Set<SecurityErrorCode>> tables = new HashMap<>();
          for (Map.Entry<TabletId, Set<SecurityErrorCode>> ke :
              e.getSecurityErrorCodes().entrySet()) {
            Set<SecurityErrorCode> secCodes = tables.get(ke.getKey().getTableId().toString());
            if (secCodes == null) {
              secCodes = new HashSet<>();
              tables.put(ke.getKey().getTableId().toString(), secCodes);
            }
            secCodes.addAll(ke.getValue());
          }

          log.error("Not authorized to write to tables : " + tables);
        }

        if (e.getConstraintViolationSummaries().size() > 0) {
          log.error("Constraint violations : " + e.getConstraintViolationSummaries().size());
        }
        throw new IOException(e);
      }
    }
  }
}
