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
import com.dataprofiler.util.objects.DataScanSpec;
import com.dataprofiler.util.objects.DatawaveRowObject;
import com.dataprofiler.util.objects.VersionedDataScanSpec;
import com.dataprofiler.util.objects.VersionedDatasetMetadata.MissingMetadataException;
import java.io.IOException;
import org.apache.hadoop.mapreduce.JobContext;

public class RowInputFormat extends DataProfilerAccumuloInput {

  @Override
  public void applyJobConfiguration(JobContext jobContext, Context context)
      throws BasicAccumuloException, IOException {

    String dataScanSpec = jobContext.getConfiguration().get("DataProfiler.dataScanSpec");
    VersionedDataScanSpec ds = null;
    try {
      ds = new VersionedDataScanSpec(context, DataScanSpec.fromJson(dataScanSpec));
    } catch (MissingMetadataException e) {
      throw new RuntimeException(e.toString());
    }

    new DatawaveRowObject()
        .find(context, ds)
        .applyInputConfiguration(jobContext.getConfiguration());
  }
}
