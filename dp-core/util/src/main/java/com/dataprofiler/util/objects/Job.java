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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;

/**
 * Job represents a data upload. download, or other job. For historical purposes, all of the data
 * for the upload is stored directly in this entry. For download jobs, there is a separate entry
 * (DownloadSpec) that holds the rest of the data.
 *
 * <p>At some point, this should probably be all split apart for the different job types. But it's
 * not a huge deal.
 */
public class Job extends AccumuloObject<Job> {
  public static final int NEW = 0;
  public static final int IN_PROGRESS = 6;
  public static final int COMPLETE = 7;
  public static final int CANCELLED = 8;
  public static final int ERROR = 9;
  private static final String[] FRIENDLY_STATUS_MESSAGES =
      new String[] {
        "New",
        "",
        "",
        "",
        "",
        "",
        "IN_PROGRESS",
        "Complete",
        "Cancelled",
        "Error loading data",
        "",
        ""
      };
  private static final int MAX_SCAN_FOR_RUNNABLE_JOB = 100000;
  private static final TypeReference<Job> staticTypeReference = new TypeReference<Job>() {};
  // These are shared by upload and download
  private String jobId;
  private Type type = Type.UPLOAD;
  private String datasetName;
  private int status;
  private String statusMessage;
  private long submissionDateTime;
  private String creatingUser;
  // These are only for upload
  private String visibilities;
  // These are only for upload of CSV files.
  private String delimiter;

  private String escape;
  private String quote;
  private String charset;
  private String s3Path;
  private boolean deleteBeforeReload = false;
  private boolean deleteDatasetBeforeReload = false;
  private Map<String, String> columnProperties = new HashMap<>();
  private Map<String, String> tableProperties = new HashMap<>();
  private Map<String, String> datasetProperties = new HashMap<>();

  public Job() {
    super(Const.ACCUMULO_DATA_LOAD_JOBS_TABLE_ENV_KEY);
    this.submissionDateTime = System.currentTimeMillis();
  }

  public Job(
      String jobId,
      String datasetName,
      String visibilities,
      String s3Path,
      int status,
      String creatingUser) {
    super(Const.ACCUMULO_DATA_LOAD_JOBS_TABLE_ENV_KEY);
    init(jobId, datasetName, visibilities, s3Path, status, creatingUser);
    this.submissionDateTime = System.currentTimeMillis();
  }

  public Job(
      String jobId,
      String datasetName,
      String visibilities,
      String s3Path,
      int status,
      String creatingUser,
      long submissionDateTime) {
    super(Const.ACCUMULO_DATA_LOAD_JOBS_TABLE_ENV_KEY);
    init(jobId, datasetName, visibilities, s3Path, status, creatingUser);
    this.submissionDateTime = submissionDateTime;
  }

  public static Job fromJson(String json) throws IOException {
    return mapper.readValue(json, staticTypeReference);
  }

  public static Job fetchByJobId(Context context, String jobId) {
    Job job = new Job();
    job.setJobId(jobId);
    return job.fetch(context, job.createAccumuloKey());
  }

  public boolean isDeleteDatasetBeforeReload() {
    return deleteDatasetBeforeReload;
  }

  public void setDeleteDatasetBeforeReload(boolean deleteDatasetBeforeReload) {
    this.deleteDatasetBeforeReload = deleteDatasetBeforeReload;
  }

  public String getEscape() {
    return escape;
  }

  public void setEscape(String escape) {
    this.escape = escape;
  }

  public String getQuote() {
    return quote;
  }

  public void setQuote(String quote) {
    this.quote = quote;
  }

  private void init(
      String jobId,
      String datasetName,
      String visibilities,
      String s3Path,
      int status,
      String creatingUser) {
    this.jobId = jobId;
    this.datasetName = datasetName;
    this.visibilities = visibilities;
    this.s3Path = s3Path;
    setStatus(status);
    this.creatingUser = creatingUser;
  }

  public void checkRequiredFields() throws InvalidDataFormat {
    if (jobId == null || status < 0) {
      throw new InvalidDataFormat("Malformed job");
    }

    if (creatingUser == null) {
      creatingUser = "unknown";
    }

    if (type == Type.UPLOAD && (datasetName == null || visibilities == null || s3Path == null)) {
      throw new InvalidDataFormat("Malformed upload job");
    }

    setStatus(status);
  }

  @Override
  public Job fromEntry(Entry<Key, Value> entry) {
    String value = entry.getValue().toString();
    visibility = entry.getKey().getColumnVisibility().toString();

    if (value.length() > 0 && value.startsWith("{")) {
      Job job;
      try {
        job = fromJson(value);
      } catch (IOException e) {
        throw new InvalidDataFormat(e.toString());
      }
      job.checkRequiredFields();

      job.updatePropertiesFromEntry(entry);
      return job;
    } else {
      String[] values = entry.getValue().toString().split("\u0000");

      if (values.length == 5) {
        // This is only for older entrys that don't have a submissionDateTime, so make certain that
        // is not a valid date.
        Job job =
            new Job(
                entry.getKey().getRow().toString(),
                values[0],
                values[1],
                values[3],
                Integer.parseInt(values[4]),
                "unknown",
                -1);
        job.updatePropertiesFromEntry(entry);

        return job;
      } else {
        Job job =
            new Job(
                entry.getKey().getRow().toString(),
                values[0],
                values[1],
                values[3],
                Integer.parseInt(values[4]),
                "unknown",
                Long.parseLong(values[5]));
        job.updatePropertiesFromEntry(entry);

        return job;
      }
    }
  }

  public String toJson() throws IOException {
    return createAccumuloValue().toString();
  }

  /**
   * Key for Jobs consists of the following:
   *
   * <p>* RowID - jobId * ColFam - COL_FAM_JOB * ColQual - ""
   *
   * @return Key
   * @throws InvalidDataFormat
   */
  @Override
  public Key createAccumuloKey() throws InvalidDataFormat {
    return new Key(jobId, Const.COL_FAM_JOB, "", visibility);
  }

  @Override
  public ObjectScannerIterable<Job> scan(Context context) {
    return super.scan(context).fetchColumnFamily(Const.COL_FAM_JOB);
  }

  public ArrayList<Job> listJobsForUser(Context context, String creatingUser, int limit) {
    TreeMap<Long, Job> jobs = new TreeMap<>();
    for (Job job : scan(context)) {
      if (creatingUser != null && job.getCreatingUser().equals(creatingUser)) {
        jobs.put(job.getSubmissionDateTime(), job);
      }
    }

    ArrayList startValues = new ArrayList<Map.Entry<Long, Job>>(limit);
    Iterator iterator = jobs.descendingKeySet().iterator();
    for (int i1 = 0; iterator.hasNext() && i1 < limit; i1++) {
      startValues.add(jobs.get(iterator.next()));
    }

    return startValues;
  }

  public ArrayList<Job> listJobs(Context context, int status, Type type, int limit) {
    ArrayList<Job> jobs = new ArrayList<>();
    for (Job job : scan(context)) {
      if (status >= 0 && job.getStatus() != status) {
        continue;
      }
      if (type != null && job.getType() != type) {
        continue;
      }
      jobs.add(job);
      if (limit > 0 && jobs.size() >= limit) {
        break;
      }
    }

    return jobs;
  }

  public ArrayList<Job> listJobs(Context context, int status, int limit) {
    return listJobs(context, status, null, limit);
  }

  public Job selectRunnableJob(Context context) {
    int i = 0;
    for (Job job : new Job().scan(context)) {
      if (job.getStatus() == Job.NEW) {
        return job;
      }
      i++;
      if (i > MAX_SCAN_FOR_RUNNABLE_JOB) {
        break;
      }
    }
    return null;
  }

  public String toFriendlyString() {
    String time;
    if (submissionDateTime < 0) {
      time = "Unknown";
    } else {
      time = new Date(submissionDateTime).toString();
    }
    return String.format(
        "JobID: %s, Dataset Name: %s, Visibilities: %s, Delimiter: %s, S3 Path: %s, Status: %s Submitted: %s",
        jobId, datasetName, visibilities, delimiter, s3Path, getStatusMessage(), time);
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  public String getVisibilities() {
    return visibilities;
  }

  public void setVisibilities(String visibilities) {
    this.visibilities = visibilities;
  }

  public String getDelimiter() {
    return delimiter;
  }

  public void setDelimiter(String delimiter) {
    this.delimiter = delimiter;
  }

  public String getS3Path() {
    return s3Path;
  }

  public void setS3Path(String s3Path) {
    this.s3Path = s3Path;
  }

  @JsonIgnore
  public String getFilename() {
    // This returns the file that lands on HDFS (just the file name - not the full path)
    return s3Path.substring(s3Path.lastIndexOf("/"));
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
    this.statusMessage = FRIENDLY_STATUS_MESSAGES[this.status];
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public long getSubmissionDateTime() {
    return submissionDateTime;
  }

  public void setSubmissionDateTime(long submissionDateTime) {
    this.submissionDateTime = submissionDateTime;
  }

  @JsonIgnore
  public String getFileName() {
    return s3Path.substring(s3Path.lastIndexOf("/") + 1);
  }

  public String getCreatingUser() {
    return creatingUser;
  }

  public void setCreatingUser(String creatingUser) {
    this.creatingUser = creatingUser;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public boolean isDeleteBeforeReload() {
    return deleteBeforeReload;
  }

  public void setDeleteBeforeReload(boolean deleteBeforeReload) {
    this.deleteBeforeReload = deleteBeforeReload;
  }

  public Map<String, String> getColumnProperties() {
    return columnProperties;
  }

  public void setColumnProperties(Map<String, String> columnProperties) {
    this.columnProperties = columnProperties;
  }

  public Map<String, String> getTableProperties() {
    return tableProperties;
  }

  public void setTableProperties(Map<String, String> tableProperties) {
    this.tableProperties = tableProperties;
  }

  public Map<String, String> getDatasetProperties() {
    return datasetProperties;
  }

  public void setDatasetProperties(Map<String, String> datasetProperties) {
    this.datasetProperties = datasetProperties;
  }

  public String getCharset() {
    return charset;
  }

  public void setCharset(String charset) {
    this.charset = charset;
  }

  public enum Type {
    UPLOAD,
    DOWNLOAD,
    COMMAND,
    MAKE,
    SQL;

    private static final Map<String, Type> valueMap = new HashMap<>();

    static {
      valueMap.put("upload", UPLOAD);
      valueMap.put("download", DOWNLOAD);
      valueMap.put("command", COMMAND);
      valueMap.put("make", MAKE);
      valueMap.put("sql", SQL);
    }

    @JsonCreator
    public static Type forValue(String value) {
      return valueMap.get(value.toLowerCase());
    }

    @JsonValue
    public String toValue() {
      for (Entry<String, Type> entry : valueMap.entrySet()) {
        if (entry.getValue() == this) {
          return entry.getKey();
        }
      }

      return null;
    }
  }
}
