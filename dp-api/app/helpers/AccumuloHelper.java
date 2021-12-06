/**
*  Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
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
**/
package helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dataprofiler.util.BasicAccumuloException;
import com.dataprofiler.util.Context;
import com.dataprofiler.util.objects.*;
import objects.ColumnCollection;
import org.apache.log4j.Logger;
import play.cache.SyncCacheApi;

import javax.inject.Inject;
import java.util.*;

public class AccumuloHelper {

  private static final Logger logger = Logger.getLogger(AccumuloHelper.class);

  private static final ObjectMapper mapper = new ObjectMapper();

  @Inject
  public AccumuloHelper() {}

  public static String decodeForwardSlash(String source) {
    return source.replaceAll("%2[Ff]", "/");
  }

  public List<ColumnCollection.ColumnInfo> pickerColumnSearch(
      Context context,
      MetadataVersionObject version,
      String sessionId,
      SyncCacheApi cache,
      String dataset,
      Integer limit,
      String searchTerm,
      Boolean sortAsc,
      Boolean sortName)
      throws Exception {

    String cacheKey =
        dataset != null
            ? String.format("%s:%s:%s", version.getId(), sessionId, dataset)
            : String.format("%s:%s:\uD83E\uDD2E", version.getId(), sessionId);

    ColumnCollection result =
        cache.getOrElseUpdate(
            cacheKey,
            () -> {
              logger.info("Caching picker Column Search for " + cacheKey);
              ColumnCollection ret = new ColumnCollection();

              ObjectScannerIterable<VersionedMetadataObject> scanner;
              if (dataset == null) {
                scanner = new VersionedMetadataObject().scan(context, version);
              } else {
                scanner =
                    new VersionedMetadataObject()
                        .scanAllLevelsForDataset(context, version, dataset);
              }
              scanner.setBatch(true);

              for (VersionedMetadataObject entry : scanner) {
                if (entry.getMetadata_level() == 2) {
                  String datasetVal = entry.getDataset_name();
                  String tableVal = entry.getTable_name();
                  String columnVal = entry.getColumn_name();
                  if (dataset != null && dataset.equals(datasetVal)) {
                    ret.add(datasetVal, tableVal, columnVal);
                  } else {
                    ret.add(datasetVal, tableVal, columnVal);
                  }
                }
              }

              return ret;
            },
            300);

    return result.prepareReturn(dataset, sortAsc, sortName, searchTerm).top(limit);
  }

  public Set<APIToken> apiKeys(Context context) {
    ObjectScannerIterable<APIToken> list = APIToken.list(context);
    Set<APIToken> keys = new HashSet<>();
    Iterator<APIToken> iter = list.iterator();
    while (iter.hasNext()) {
      keys.add(iter.next());
    }
    return keys;
  }

  public APIToken newApiKey(Context context, String username) {
    try {
      APIToken newToken = APIToken.createToken(username);
      newToken.put(context);
      return newToken;
    } catch (Exception e) {
      logger.error("Unable to create API Token", e);
      return null;
    }
  }

  public Set<ColumnEntityObject> columnEntities(Context context) {
    ObjectScannerIterable<ColumnEntityObject> list = ColumnEntityObject.list(context);
    Set<ColumnEntityObject> entities = new HashSet<>();
    Iterator<ColumnEntityObject> iter = list.iterator();
    while (iter.hasNext()) {
      entities.add(iter.next());
    }
    return entities;
  }

  public ColumnEntityObject newColumnEntity(
      Context context,
      String created_by,
      String title,
      ArrayList<String> column_names,
      String grouping_logic) {
    try {
      ColumnEntityObject ceo =
          new ColumnEntityObject(created_by, title, column_names, grouping_logic);
      ceo.put(context);
      return ceo;
    } catch (Exception e) {
      logger.error("Unable to create Column Entity", e);
      return null;
    }
  }

  public Boolean deleteColumnEntity(Context context, String id) {
    try {
      return ColumnEntityObject.deleteById(context, id);
    } catch (Exception e) {
      logger.error("Error deleting Column Entity", e);
      return false;
    }
  }

  public Set<ElementAlias> columnAliases(Context context) {
    ObjectScannerIterable<ElementAlias> list = new ElementAlias().scanColumns(context);
    Set<ElementAlias> objs = new HashSet<>();
    Iterator<ElementAlias> iter = list.iterator();
    while (iter.hasNext()) {
      objs.add(iter.next());
    }
    return objs;
  }

  public ElementAlias newColumnAlias(
      Context context,
      String dataset,
      String table,
      String column,
      String alias,
      String createdBy) {
    try {
      ElementAlias ea =
          new ElementAlias(
              ElementAlias.AliasType.COLUMN.toString(), dataset, table, column, alias, createdBy);
      ea.put(context);
      return ea;
    } catch (Exception e) {
      logger.error("Unable to create Custom Column Name", e);
      return null;
    }
  }

  public Boolean deleteColumnAlias(Context context, String dataset, String table, String column) {
    try {
      return ElementAlias.deleteColumnAlias(context, dataset, table, column);
    } catch (Exception e) {
      logger.error("Error deleting Custom Column Name", e);
      return false;
    }
  }

  public Boolean deleteApiKey(Context context, String token) {
    try {
      return APIToken.deleteByToken(context, token);
    } catch (BasicAccumuloException e) {
      logger.error("Error deleting token: ", e);
      return false;
    }
  }

  public class DatasetSampleObject {

    /** The count of the samples */
    public Long ct;

    /** A unique array of concept friendly names */
    public HashSet<String> cn = new HashSet<>();

    public DatasetSampleObject() {}
  }
}
