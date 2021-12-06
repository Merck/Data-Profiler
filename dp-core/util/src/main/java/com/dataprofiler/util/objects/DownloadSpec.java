package com.dataprofiler.util.objects;

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

import com.dataprofiler.util.Const;
import com.dataprofiler.util.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;

public class DownloadSpec extends AccumuloObject<DownloadSpec> {
  private String jobId;
  private List<DataScanSpec> downloads = new ArrayList<>();

  private static final TypeReference<DownloadSpec> staticTypeReference =
      new TypeReference<DownloadSpec>() {};

  public DownloadSpec() {
    super(Const.ACCUMULO_DATA_LOAD_JOBS_TABLE_ENV_KEY);
  }

  @Override
  public ObjectScannerIterable<DownloadSpec> scan(Context context) {
    return super.scan(context).fetchColumnFamily(Const.COL_FAM_DOWNLOAD_SPEC);
  }

  @Override
  public DownloadSpec fromEntry(Entry<Key, Value> entry) {
    DownloadSpec ds = null;
    try {
      ds = fromJson(entry.getValue().toString());
    } catch (IOException e) {
      throw new InvalidDataFormat(e.toString());
    }

    Key key = entry.getKey();
    ds.jobId = key.getRow().toString();

    ds.updatePropertiesFromEntry(entry);

    return ds;
  }

  public static DownloadSpec fetchByJobId(Context context, String jobId) {
    DownloadSpec ds = new DownloadSpec();
    ds.setJobId(jobId);
    return ds.fetch(context, ds.createAccumuloKey());
  }

  /**
   * Key for DownloadSpec consists of the following:
   *
   * <p>RowID - jobId
   *
   * <p>ColFam - COL_FAM_DOWNLOAD_SPEC
   *
   * <p>ColQual - ""
   *
   * @return Key
   * @throws InvalidDataFormat
   */
  @Override
  public Key createAccumuloKey() throws InvalidDataFormat {
    return new Key(jobId, Const.COL_FAM_DOWNLOAD_SPEC, "", visibility);
  }

  public String toJson() throws JsonProcessingException {
    return mapper.writeValueAsString(this);
  }

  public static DownloadSpec fromJson(String json) throws IOException {
    DownloadSpec ds = mapper.readValue(json, staticTypeReference);
    for (DataScanSpec spec : ds.downloads) {
      spec.upgradeFilters();
    }

    return ds;
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public List<DataScanSpec> getDownloads() {
    return downloads;
  }

  public void setDownloads(List<DataScanSpec> downloads) {
    this.downloads = downloads;
  }

  @Override
  public String toString() {
    return "DownloadSpec{" + "downloads=" + downloads + '}';
  }
}
