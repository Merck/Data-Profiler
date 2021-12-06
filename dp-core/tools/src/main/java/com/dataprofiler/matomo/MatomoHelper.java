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

import static java.lang.String.format;

import com.dataprofiler.matomo.response.MatomoResponseElement;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

public class MatomoHelper {

  private static final Logger logger = Logger.getLogger(MatomoHelper.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  static {
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  private final String apiUrl;
  private final String apiKey;
  private final String apiIdSite;
  private final String uiIdSite;

  public MatomoHelper(String matomoApiKey, String apiIdSite, String uiIdSite) {
    super();
    this.apiUrl = "https://admin.dataprofiler.com/analytics/index.php";
    this.apiKey = matomoApiKey;
    this.apiIdSite = apiIdSite;
    this.uiIdSite = uiIdSite;
  }

  public JsonNode matomoCall(String date, String method, String period, String site)
      throws MatomoException {

    String idSite = site == "api" ? this.apiIdSite : this.uiIdSite;

    if (logger.isDebugEnabled()) {
      logger.debug(format("Matomo idSite:%s date:%s", idSite, date));
    }

    try {
      HttpResponse<com.mashape.unirest.http.JsonNode> jsonResponse =
          Unirest.get(apiUrl)
              //              .header("Accept", "application/json")
              //              .header("Content-Type", "application/json")
              .queryString("module", "API")
              .queryString("token_auth", this.apiKey)
              .queryString("format", "json")
              .queryString("idSite", idSite)
              .queryString("method", method)
              .queryString("period", period)
              .queryString("date", date)
              .queryString("flat", "1")
              .queryString("filter_limit", "-1")
              .asJson();
      String responseJSONString = jsonResponse.getBody().toString();
      return mapper.readValue(responseJSONString, JsonNode.class);
    } catch (Exception e) {
      logger.error("Failed to query Matomo");
      logger.error(e.getMessage());
      throw new MatomoException();
    }
  }

  protected static String formatToMatomoDate(LocalDate localDate) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    return localDate.format(formatter);
  }

  public List<MatomoResponseElement> allChartDataElements(LocalDate localDate)
      throws MatomoException {
    String formattedDate = formatToMatomoDate(localDate);
    return allChartDataElements(formattedDate);
  }

  public List<MatomoResponseElement> allChartDataElements(String day) throws MatomoException {
    JsonNode jsonNode = matomoCall(day, "Actions.getPageUrls", "day", "api");
    try {
      return mapper.readValue(
          jsonNode.toString(), new TypeReference<List<MatomoResponseElement>>() {});
    } catch (IOException e) {
      throw new MatomoException(e);
    }
  }

  public Map<String, List<MatomoResponseElement>> allChartDataElementsByWeek()
      throws MatomoException {
    JsonNode jsonNode = matomoCall("last26", "Actions.getPageUrls", "week", "api");
    try {
      return mapper.readValue(
          jsonNode.toString(), new TypeReference<Map<String, List<MatomoResponseElement>>>() {});
    } catch (IOException e) {
      throw new MatomoException(e);
    }
  }

  public List<MatomoResponseElement> allSearchAppearanceElements(
      LocalDate localDate, LocalDate startDate) throws MatomoException {
    String formattedDate = formatToMatomoDate(localDate);
    String formattedStartDate = formatToMatomoDate(startDate);
    return allSearchAppearanceElements(formattedDate, formattedStartDate);
  }

  public List<MatomoResponseElement> allSearchAppearanceElements(String day, String startDay)
      throws MatomoException {
    String dateRange = startDay + "," + day;
    JsonNode jsonNode = matomoCall(dateRange, "Contents.getContentPieces", "range", "ui");
    try {
      return mapper.readValue(
          jsonNode.toString(), new TypeReference<List<MatomoResponseElement>>() {});
    } catch (IOException e) {
      throw new MatomoException(e);
    }
  }

  public List<MatomoResponseElement> allDownloadElements(LocalDate localDate, LocalDate startDate)
      throws MatomoException {
    String formattedDate = formatToMatomoDate(localDate);
    String formattedStartDate = formatToMatomoDate(startDate);
    return allDownloadElements(formattedDate, formattedStartDate);
  }

  public List<MatomoResponseElement> allDownloadElements(String day, String startDay)
      throws MatomoException {
    String dateRange = startDay + "," + day;
    JsonNode jsonNode = matomoCall(dateRange, "Events.getCategory", "range", "ui");
    try {
      return mapper.readValue(
          jsonNode.toString(), new TypeReference<List<MatomoResponseElement>>() {});
    } catch (IOException e) {
      throw new MatomoException(e);
    }
  }
}
