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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class Const {

  // dataprofiler.config accumulo getAllTables
  public static final String ACCUMULO_METADATA_TABLE_ENV_KEY = "accumuloMetadataTable";
  public static final String ACCUMULO_INDEX_TABLE_ENV_KEY = "accumuloIndexTable";
  public static final String ACCUMULO_DATA_LOAD_JOBS_TABLE_ENV_KEY = "accumuloDataLoadJobsTable";
  public static final String ACCUMULO_SAMPLES_TABLE_ENV_KEY = "accumuloSamplesTable";
  public static final String ACCUMULO_DATAWAVE_ROWS_TABLE_ENV_KEY = "accumuloDatawaveRowsTable";
  public static final String ACCUMULO_COLUMN_COUNTS_TABLE_ENV_KEY = "accumuloColumnCountsTable";
  public static final String ACCUMULO_API_TOKENS_TABLE_ENV_KEY = "accumuloAPITokensTable";
  public static final String ACCUMULO_COLUMN_ENTITIES_ENV_KEY = "accumuloColumnEntitiesTable";
  public static final String ACCUMULO_ELEMENT_ALIAS_ENV_KEY = "accumuloElementAliasesTable";
  public static final String ACCUMULO_CUSTOM_ANNOTATIONS_ENV_KEY = "accumuloCustomAnnotationsTable";

  // Accumulo Sanner/Writer Constants
  public static final Long DEFAULT_MAX_MEM = 10000000L;
  public static final Integer DEFAULT_MAX_WRITE_THREADS = 20;
  public static final Integer DEFAULT_MAX_LATENCY = 10;
  public static final Integer DEFAULT_NUM_BATCH_SCAN_THREADS = 10;

  // Accumulo Table Constants
  public static final String COL_FAM_UNVERSIONED_METADATA = "ENHANCED_METADATA";
  public static final String COL_FAM_METADATA = "VERSIONED_METADATA";
  public static final String COL_FAM_DELETED_METADATA = "VERSIONED_DELETED_METADATA";
  public static final String COL_QUAL_METADATA_DATASET = "D";
  public static final String COL_QUAL_METADATA_TABLE = "T";
  public static final String COL_QUAL_METADATA_COLUMN = "C";
  public static final String COL_FAM_METADATA_CURRENT_VERSON = "METADATA_CURRENT_VERSION";
  public static final String COL_FAM_METADATA_VERSION = "METADATA_VERSION";
  public static final String COL_FAM_SAMPLE = "SAMPLE";
  public static final String COL_FAM_DOWNLOAD_SPEC = "DOWNLOAD_SPEC";
  public static final String COL_FAM_COMMAND_SPEC = "COMMAND_SPEC";
  public static final String COL_FAM_MAKE_JOB_SPEC = "MAKE_JOB_SPEC";
  public static final String COL_FAM_SQL_QUERY_JOB_SPEC = "SQL_QUERY_JOB_SPEC";
  public static final String BACKUP_TABLE_SUFFIX = "_backup";
  public static final String COL_FAM_PROJECT_PAGE_METADATA = "PP_METADATA";

  // Row Table Constants
  public static final String COL_FAM_RAW_DATA = "RAW_DATA";
  public static final String COL_FAM_COMBINATIONS = "COMBINATIONS";
  public static final String COL_FAM_INDEX = "I";
  public static final String COL_FAM_DATA = "D";
  public static final String COL_FAM_SHARD = "SHARD";

  // Job Constants
  public static final String COL_FAM_JOB = "DataLoadJob";

  // Accumulo Data Constants
  public static final String LOW_BYTE = "\u0000";
  public static final String HIGH_BYTE = "\uffff";
  public static final String NULL = LOW_BYTE;
  public static final String DELIMITER = LOW_BYTE;
  // A lexicoded long is always 9 bytes long
  public static final int LONG_LEX_LEN = 9;
  public static final int TABLET_SIZE_MAX_BYTES = 512_000_000;
  public static final int LOADER_SAMPLE_SIZE = 1000;
  public static final int MIN_TOKEN_LENGTH = 3;

  // Accumulo Constants
  public static final String ACCUMULO_META_NS = "META";
  public static final String ACCUMULO_CONCEPT_INDEX_NS = "CONCEPT_IDX";
  public static final String ACCUMULO_RESOLUTION_NS = "RESOLUTIONS";
  public static final String ACCUMULO_DATASETS_NS = "DATASETS";
  public static final String ACCUMULO_TABLES_NS = "TABLES";
  public static final String ACCUMULO_COLUMNS_NS = "COLUMNS";

  public static final String CURRENT_METADATA_VERSION_KEY = "CURRENT_VERSION";

  public static final String INDEX_GLOBAL = "GLOBAL";
  public static final String INDEX_DATASET = "DATASET";
  public static final String INDEX_TABLE = "TABLE";
  public static final String INDEX_COLUMN = "COLUMN";
  public static final Integer INDEX_MAX_LENGTH = 1000;

  public static final String ROW_VISIBILITIES = "accumuloRowLevelVisibilities";

  // Regex for pattern name g1 = Concept name; g2 = Topic Name
  public static final String CONCEPT_NAME_PATTERN = "\\[(.*?)\\]-\\((.*?)\\).*";

  // Data Types
  /*
   * This is the deafult type and it matches the string type for Spark (all of the
   * datatypes match the spark datatypes). This used to be capitilized, which is why
   * these values stored in Accumulo are usually lowercased before used.
   */
  public static final String DATA_TYPE_STRING = "string";
  public static final Integer VALUES_PER_PAGE = 100;

  // Query Parameters
  public static final Integer QUERY_PARAM_MAX_RESULTS = 1000;

  // Metadata properties
  public static final String METADATA_PROPERTY_ORIGIN = "origin";
  public static final String MAKE_CODE = "make";
  public static final String UPLOAD_CODE = "upload";
  public static final String MIXED_CODE = "mixed";
  // Sort types
  private static final String SORT_COUNT_ASCENDING = "CNT_ASC";
  private static final String SORT_COUNT_DESCENDING = "CNT_DESC";
  private static final String SORT_VALUE_ASCENDING = "VAL_ASC";
  private static final String SORT_VALUE_DESCENDING = "VAL_DESC";

  private Const() {
    throw new AssertionError();
  }

  public enum SortOrder {
    CNT_ASC(SORT_COUNT_ASCENDING),
    CNT_DESC(SORT_COUNT_DESCENDING),
    VAL_ASC(SORT_VALUE_ASCENDING),
    VAL_DESC(SORT_VALUE_DESCENDING);
    private final String code;

    SortOrder(String code) {
      this.code = code;
    }

    public static SortOrder getEnum(String code) {
      switch (code) {
        case SORT_COUNT_ASCENDING:
          return CNT_ASC;
        case SORT_COUNT_DESCENDING:
          return CNT_DESC;
        case SORT_VALUE_ASCENDING:
          return VAL_ASC;
        case SORT_VALUE_DESCENDING:
          return VAL_DESC;
        default:
          return CNT_ASC;
      }
    }

    public String GetCode() {
      return this.code;
    }
  }

  public enum Origin {
    MAKE(MAKE_CODE),
    UPLOAD(UPLOAD_CODE),
    MIXED(MIXED_CODE);

    private final String code;

    Origin(String code) {
      this.code = code;
    }

    public static Origin getEnum(String code) {
      switch (code) {
        case MAKE_CODE:
          return MAKE;
        case MIXED_CODE:
          return MIXED;
        default:
          return UPLOAD;
      }
    }

    public static Origin getAndVerifyEnum(String code) throws Exception {
      // Error check the origin
      Set<String> originSet =
          EnumSet.allOf(Origin.class).stream().map(Origin::getCode).collect(Collectors.toSet());
      if (!originSet.contains(code.toLowerCase())) {

        String origins = originSet.stream().collect(Collectors.joining(", "));

        throw new Exception(
            String.format("Origin '%s' unknown. Possible origins: %s ", code, origins));
      }

      return getEnum(code);
    }

    public String getCode() {
      return this.code;
    }
  }

  public enum LoadType {
    LIVE,
    BULK,
    SPLIT;

    private static final Map<String, LoadType> valueMap = new HashMap<>();

    static {
      valueMap.put("live", LIVE);
      valueMap.put("bulk", BULK);
      valueMap.put("split", SPLIT);
    }

    @JsonCreator
    public static LoadType forValue(String value) {
      return valueMap.get(value.toLowerCase());
    }

    @JsonValue
    public String toValue() {
      for (Entry<String, LoadType> entry : valueMap.entrySet()) {
        if (entry.getValue() == this) {
          return entry.getKey();
        }
      }
      return null;
    }
  }
}
