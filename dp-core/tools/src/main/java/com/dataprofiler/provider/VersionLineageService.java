package com.dataprofiler.provider;

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

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

import com.dataprofiler.util.iterators.ClosableIterator;
import com.dataprofiler.util.objects.ObjectScannerIterable;
import com.dataprofiler.util.objects.VersionedMetadataObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class VersionLineageService {
  Logger logger = LoggerFactory.getLogger(VersionLineageService.class);

  List<VersionedMetadataObject> scanLineage(ObjectScannerIterable<VersionedMetadataObject> scanner)
      throws MetadataScanException {
    List<VersionedMetadataObject> list = new ArrayList<>();
    int count = 0;
    try (ClosableIterator<VersionedMetadataObject> itr = scanner.iterator()) {
      while (itr.hasNext()) {
        VersionedMetadataObject current = itr.next();
        boolean alreadySeen =
            list.stream().anyMatch(el -> el.version_id.equals(current.version_id));
        if (!alreadySeen) {
          list.add(current);
          count++;
        }
      }
    } catch (Exception e) {
      throw new MetadataScanException("exception scanning metadata");
    }

    if (logger.isTraceEnabled()) {
      logger.trace(format("loaded %s object(s)", count));
      logger.trace(format("before sort"));
      list.stream()
          .limit(10)
          .forEach(
              el -> {
                logger.trace(
                    format(
                        "versionid: %s loadtime: %s updatetime: %s",
                        el.version_id, el.getLoad_time(), el.getUpdate_time()));
              });
    }

    list = sortVersionLineage(list);

    if (logger.isTraceEnabled()) {
      logger.trace(format("after sort"));
      list.stream()
          .limit(10)
          .forEach(
              el -> {
                logger.trace(
                    format(
                        "versionid: %s loadtime: %s updatetime: %s",
                        el.version_id, el.getLoad_time(), el.getUpdate_time()));
              });
    }

    return list;
  }

  List<VersionedMetadataObject> sortVersionLineage(
      List<VersionedMetadataObject> versionedMetadataObjects) {
    if (isNull(versionedMetadataObjects) || versionedMetadataObjects.isEmpty()) {
      return versionedMetadataObjects;
    }

    return versionedMetadataObjects.stream()
        .sorted(Comparator.comparing(VersionedMetadataObject::getUpdate_time).reversed())
        .collect(toList());
  }
}
