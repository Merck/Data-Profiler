package com.dataprofiler.matomo.response;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class MatomoResponseElement {

  String label;

  @JsonProperty("nb_impressions")
  Long nbImpressions;

  @JsonProperty("nb_visits")
  Long nbVisits;

  @JsonProperty("nb_uniq_visitors")
  Long nbUniqVisitors;

  @JsonProperty("nb_events")
  Long nbEvents;

  @JsonProperty("nb_hits")
  Long nbHits;

  @JsonProperty("sum_time_spent")
  Long sumTimeSpent;

  @JsonProperty("sum_daily_nb_uniq_visitors")
  Long sumDailyNbUniqVisitors;

  @JsonProperty("nb_hits_with_time_server")
  String nbHitsWithTimeServer;

  @JsonProperty("min_time_server")
  String minTimeServer;

  @JsonProperty("max_time_server")
  String maxTimeServer;

  @JsonProperty("entry_nb_uniq_visitors")
  String entryNbUniqVisitors;

  @JsonProperty("entry_nb_visits")
  String entryNbVisits;

  @JsonProperty("entry_nb_actions")
  String entryNbActions;

  @JsonProperty("entry_sum_visit_length")
  String entrySumVisitLength;

  @JsonProperty("entry_bounce_count")
  String entryBounceCount;

  @JsonProperty("exit_nb_uniq_visitors")
  String exitNbUniqVisitors;

  @JsonProperty("exit_nb_visits")
  String exitNbVisits;

  @JsonProperty("avg_time_server")
  Float avgTimeServer;

  @JsonProperty("avg_page_load_time")
  Float avgPageLoadTime;

  @JsonProperty("avg_time_on_page")
  Long avgTimeOnPage;

  @JsonProperty("bounce_rate")
  String bounceRate;

  @JsonProperty("exit_rate")
  String exitRate;

  String url;

  @JsonProperty("Actions_PageUrl")
  String actionsPageUrl;

  String segment;

  @JsonProperty("is_summary")
  Boolean isSummary;

  public MatomoResponseElement() {
    super();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .appendSuper(super.toString())
        .append("label", label)
        .append("nbImpressions", nbImpressions)
        .append("nbVisits", nbVisits)
        .append("nbEvents", nbEvents)
        .append("nbUniqVisitors", nbUniqVisitors)
        .append("url", url)
        .append("actionsPageUrl", actionsPageUrl)
        .append("segment", segment)
        .toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }
    MatomoResponseElement rhs = (MatomoResponseElement) obj;
    return new EqualsBuilder()
        .appendSuper(super.equals(obj))
        .append(label, rhs.label)
        .append(nbImpressions, rhs.nbImpressions)
        .append(nbVisits, rhs.nbVisits)
        .append(nbEvents, rhs.nbEvents)
        .append(nbUniqVisitors, rhs.nbUniqVisitors)
        .append(nbHits, rhs.nbHits)
        .append(sumTimeSpent, rhs.sumTimeSpent)
        .append(sumDailyNbUniqVisitors, rhs.sumDailyNbUniqVisitors)
        .append(nbHitsWithTimeServer, rhs.nbHitsWithTimeServer)
        .append(minTimeServer, rhs.minTimeServer)
        .append(maxTimeServer, rhs.maxTimeServer)
        .append(entryNbUniqVisitors, rhs.entryNbUniqVisitors)
        .append(entryNbVisits, rhs.entryNbVisits)
        .append(entryNbActions, rhs.entryNbActions)
        .append(entrySumVisitLength, rhs.entrySumVisitLength)
        .append(entryBounceCount, rhs.entryBounceCount)
        .append(exitNbUniqVisitors, rhs.exitNbUniqVisitors)
        .append(exitNbVisits, rhs.exitNbVisits)
        .append(avgTimeServer, rhs.avgTimeServer)
        .append(avgPageLoadTime, rhs.avgPageLoadTime)
        .append(avgTimeOnPage, rhs.avgTimeOnPage)
        .append(bounceRate, rhs.bounceRate)
        .append(exitRate, rhs.exitRate)
        .append(url, rhs.url)
        .append(actionsPageUrl, rhs.actionsPageUrl)
        .append(segment, rhs.segment)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(44587, 51)
        .appendSuper(super.hashCode())
        .append(label)
        .append(nbImpressions)
        .append(nbVisits)
        .append(nbUniqVisitors)
        .append(url)
        .append(actionsPageUrl)
        .append(segment)
        .toHashCode();
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public Long getNbImpressions() {
    return nbImpressions;
  }

  public void setNbImpressions(Long nbImpressions) {
    this.nbImpressions = nbImpressions;
  }

  public Long getNbVisits() {
    return nbVisits;
  }

  public void setNbVisits(Long nbVisits) {
    this.nbVisits = nbVisits;
  }

  public Long getNbEvents() {
    return nbEvents;
  }

  public void setNbEvents(Long nbEvents) {
    this.nbEvents = nbEvents;
  }

  public Long getNbUniqVisitors() {
    return nbUniqVisitors;
  }

  public void setNbUniqVisitors(Long nbUniqVisitors) {
    this.nbUniqVisitors = nbUniqVisitors;
  }

  public Long getNbHits() {
    return nbHits;
  }

  public void setNbHits(Long nbHits) {
    this.nbHits = nbHits;
  }

  public Long getSumTimeSpent() {
    return sumTimeSpent;
  }

  public void setSumTimeSpent(Long sumTimeSpent) {
    this.sumTimeSpent = sumTimeSpent;
  }

  public String getNbHitsWithTimeServer() {
    return nbHitsWithTimeServer;
  }

  public void setNbHitsWithTimeServer(String nbHitsWithTimeServer) {
    this.nbHitsWithTimeServer = nbHitsWithTimeServer;
  }

  public String getMinTimeServer() {
    return minTimeServer;
  }

  public void setMinTimeServer(String minTimeServer) {
    this.minTimeServer = minTimeServer;
  }

  public String getMaxTimeServer() {
    return maxTimeServer;
  }

  public void setMaxTimeServer(String maxTimeServer) {
    this.maxTimeServer = maxTimeServer;
  }

  public String getEntryNbUniqVisitors() {
    return entryNbUniqVisitors;
  }

  public void setEntryNbUniqVisitors(String entryNbUniqVisitors) {
    this.entryNbUniqVisitors = entryNbUniqVisitors;
  }

  public String getEntryNbVisits() {
    return entryNbVisits;
  }

  public void setEntryNbVisits(String entryNbVisits) {
    this.entryNbVisits = entryNbVisits;
  }

  public String getEntryNbActions() {
    return entryNbActions;
  }

  public void setEntryNbActions(String entryNbActions) {
    this.entryNbActions = entryNbActions;
  }

  public String getEntrySumVisitLength() {
    return entrySumVisitLength;
  }

  public void setEntrySumVisitLength(String entrySumVisitLength) {
    this.entrySumVisitLength = entrySumVisitLength;
  }

  public String getEntryBounceCount() {
    return entryBounceCount;
  }

  public void setEntryBounceCount(String entryBounceCount) {
    this.entryBounceCount = entryBounceCount;
  }

  public String getExitNbUniqVisitors() {
    return exitNbUniqVisitors;
  }

  public void setExitNbUniqVisitors(String exitNbUniqVisitors) {
    this.exitNbUniqVisitors = exitNbUniqVisitors;
  }

  public String getExitNbVisits() {
    return exitNbVisits;
  }

  public void setExitNbVisits(String exitNbVisits) {
    this.exitNbVisits = exitNbVisits;
  }

  public Float getAvgTimeServer() {
    return avgTimeServer;
  }

  public void setAvgTimeServer(Float avgTimeServer) {
    this.avgTimeServer = avgTimeServer;
  }

  public Float getAvgPageLoadTime() {
    return avgPageLoadTime;
  }

  public void setAvgPageLoadTime(Float avgPageLoadTime) {
    this.avgPageLoadTime = avgPageLoadTime;
  }

  public Long getAvgTimeOnPage() {
    return avgTimeOnPage;
  }

  public void setAvgTimeOnPage(Long avgTimeOnPage) {
    this.avgTimeOnPage = avgTimeOnPage;
  }

  public String getBounceRate() {
    return bounceRate;
  }

  public void setBounceRate(String bounceRate) {
    this.bounceRate = bounceRate;
  }

  public String getExitRate() {
    return exitRate;
  }

  public void setExitRate(String exitRate) {
    this.exitRate = exitRate;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getActionsPageUrl() {
    return actionsPageUrl;
  }

  public void setActionsPageUrl(String actionsPageUrl) {
    this.actionsPageUrl = actionsPageUrl;
  }

  public String getSegment() {
    return segment;
  }

  public void setSegment(String segment) {
    this.segment = segment;
  }

  public Long getSumDailyNbUniqVisitors() {
    return sumDailyNbUniqVisitors;
  }

  public void setSumDailyNbUniqVisitors(Long sumDailyNbUniqVisitors) {
    this.sumDailyNbUniqVisitors = sumDailyNbUniqVisitors;
  }

  public Boolean getSummary() {
    return isSummary;
  }

  public void setSummary(Boolean summary) {
    isSummary = summary;
  }
}
