package com.dataprofiler.matomo;

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

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

import com.dataprofiler.datasetperformance.DatasetPerformanceException;
import com.dataprofiler.matomo.response.MatomoResponseElement;
import com.dataprofiler.util.URLEncodingHelper;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Logger;

public class MatomoPerformanceProvider {
  private static final Logger logger = Logger.getLogger(MatomoPerformanceProvider.class);
  private MatomoHelper matomoHelper;

  public List<MatomoResponseElement> fetchChartData(LocalDate localDate)
      throws DatasetPerformanceException {
    try {
      List<MatomoResponseElement> matomoAllElements = matomoHelper.allChartDataElements(localDate);
      if (logger.isTraceEnabled()) {
        logger.trace("\nAll chart data matomo elements");
        matomoAllElements.forEach(logger::trace);
      }
      return matomoAllElements;
    } catch (MatomoException me) {
      throw new DatasetPerformanceException(me);
    }
  }

  public List<MatomoResponseElement> fetchSearchAppearances(
      LocalDate localDate, LocalDate startDate) throws DatasetPerformanceException {
    try {
      List<MatomoResponseElement> matomoAllElements =
          matomoHelper.allSearchAppearanceElements(localDate, startDate);
      if (logger.isTraceEnabled()) {
        logger.trace("\nAll search appearances matomo elements");
        matomoAllElements.forEach(logger::trace);
      }
      return matomoAllElements;
    } catch (MatomoException me) {
      throw new DatasetPerformanceException(me);
    }
  }

  public List<MatomoResponseElement> fetchDownloads(LocalDate localDate, LocalDate startDate)
      throws DatasetPerformanceException {
    try {
      List<MatomoResponseElement> matomoAllElements =
          matomoHelper.allDownloadElements(localDate, startDate);
      if (logger.isTraceEnabled()) {
        logger.trace("\nAll download matomo elements");
        matomoAllElements.forEach(logger::trace);
      }
      return matomoAllElements;
    } catch (MatomoException me) {
      throw new DatasetPerformanceException(me);
    }
  }

  public List<MatomoResponseElement> filterActionPageByDataset(
      List<MatomoResponseElement> matomoAllElements, String dataset) {
    if (isNull(dataset) || dataset.isEmpty()) {
      return Collections.emptyList();
    }
    String encodedDataset = URLEncodingHelper.encode(dataset);
    String metadataPrefix = "\\/metadata/";
    String v1TablesPrefix = "v1\\/tables\\/";
    String v1ColsPrefix = "v1\\/columns\\/";
    String colcountsPrefix = "colcounts?dataset=";

    List<MatomoResponseElement> matches =
        matomoAllElements.stream()
            .filter(
                el -> {
                  String actionsPage = el.getActionsPageUrl();
                  // looks for at least these routes
                  // /metadata/:dataset
                  // /v1/tables/:dataset
                  // /v1/columns/:dataset/
                  // colcounts?dataset=:dataset&
                  boolean hasActionPageMatch =
                      !isNull(actionsPage)
                          && (actionsPage.endsWith(metadataPrefix + encodedDataset)
                              || actionsPage.endsWith(v1TablesPrefix + encodedDataset)
                              || actionsPage.contains(v1ColsPrefix + encodedDataset + "\\/")
                              || actionsPage.contains(colcountsPrefix + encodedDataset + "&"));
                  String url = el.getUrl();
                  boolean hasUrlMatch = !isNull(url) && url.contains(encodedDataset);
                  String segment = el.getSegment();
                  boolean hasSegmentMatch = !isNull(segment) && segment.contains(encodedDataset);
                  return hasActionPageMatch || hasUrlMatch || hasSegmentMatch;
                })
            .collect(toList());
    return matches;
  }

  public List<MatomoResponseElement> filterLabelByDataset(
      List<MatomoResponseElement> matomoAllElements, String dataset) {
    if (isNull(dataset) || dataset.isEmpty()) {
      return Collections.emptyList();
    }

    List<MatomoResponseElement> matches =
        matomoAllElements.stream()
            .filter(
                el -> {
                  String label = el.getLabel();
                  boolean hasLabelMatch =
                      !isNull(label)
                          && ((label.equals(dataset + " - Dataset Search Appearance"))
                              || (label.equals("Dataset Download - " + dataset)));
                  return hasLabelMatch;
                })
            .collect(toList());
    return matches;
  }

  public MatomoHelper getMatomoHelper() {
    return matomoHelper;
  }

  public void setMatomoHelper(MatomoHelper matomoHelper) {
    this.matomoHelper = matomoHelper;
  }
}
