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
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import org.apache.accumulo.core.client.mapreduce.InputFormatBase;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.util.format.DefaultFormatter;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.log4j.Level;

public abstract class DataProfilerAccumuloInput extends InputFormatBase<Key, Value> {

  @Override
  public List<InputSplit> getSplits(JobContext jobContext) throws IOException {
    Context context;
    String authorizations = jobContext.getConfiguration().get("DataProfiler.authorizationsAsJson");
    try {
      context = new Context();
      if (authorizations != null) {
        context.setAuthorizationsFromJson(authorizations);
      }

      applyJobConfiguration(jobContext, context);
    } catch (Exception e) {
      throw new IOException(e.toString());
    }

    return super.getSplits(jobContext);
  }

  @Override
  public RecordReader<Key, Value> createRecordReader(InputSplit split, TaskAttemptContext context)
      throws IOException, InterruptedException {
    log.setLevel(getLogLevel(context));

    // Override the log level from the configuration as if the InputSplit has one it's the more
    // correct one to use.
    if (split instanceof org.apache.accumulo.core.client.mapreduce.RangeInputSplit) {
      org.apache.accumulo.core.client.mapreduce.RangeInputSplit accSplit =
          (org.apache.accumulo.core.client.mapreduce.RangeInputSplit) split;
      Level level = accSplit.getLogLevel();
      if (null != level) {
        log.setLevel(level);
      }
    } else {
      throw new IllegalArgumentException("No RecordReader for " + split.getClass().toString());
    }

    return new RecordReaderBase<Key, Value>() {
      @Override
      public boolean nextKeyValue() throws IOException, InterruptedException {
        if (scannerIterator.hasNext()) {
          ++numKeysRead;
          Entry<Key, Value> entry = scannerIterator.next();
          currentKey = entry.getKey();
          currentK = entry.getKey();
          currentV = entry.getValue();
          if (log.isTraceEnabled())
            log.trace("Processing key/value pair: " + DefaultFormatter.formatEntry(entry, true));
          return true;
        }
        return false;
      }
    };
  }

  public abstract void applyJobConfiguration(JobContext jobContext, Context context)
      throws BasicAccumuloException, IOException;
}
