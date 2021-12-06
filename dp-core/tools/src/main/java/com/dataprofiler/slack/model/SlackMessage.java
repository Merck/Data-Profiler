package com.dataprofiler.slack.model;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * represents a slack message
 *
 * @see "https://api.slack.com/methods/chat.postMessage
 *     <pre>
 *    slack.send(
 *       {
 *         ...defaultSlack,
 *         text: "HR Data Loaded",
 *         attachments: [
 *           {
 *             fallback: fallback,
 *             color: "#00FF00",
 *             fields: [
 *               {
 *                 title: "Employees Loaded",
 *                 value: runInstance.numTotalLines,
 *                 short: true
 *               },
 *               {
 *                 title: "Environment",
 *                 value: process.env.ENVIRONMENT_NAME,
 *                 short: true
 *               },
 *               {
 *                 title: "Time Elapsed",
 *                 value: moment(runInstance.startTime).fromNow(true),
 *                 short: true
 *               }
 *             ]
 *           }
 *         ]
 *       },
 * </pre>
 */
public class SlackMessage {

  @JsonProperty("username")
  private final String userName;

  private final String text;

  @JsonProperty("icon_emoji")
  private final String iconEmoji;

  private final String channel;
  private final List<Attachment> attachments;

  protected SlackMessage(SlackMessageBuilder builder) {
    super();
    this.userName = builder.userName;
    this.text = builder.text;
    this.iconEmoji = builder.iconEmoji;
    this.channel = builder.channel;
    this.attachments = builder.attachments;
  }

  public static SlackMessageBuilder builder() {
    return new SlackMessageBuilder();
  }

  public String getUserName() {
    return userName;
  }

  public String getText() {
    return text;
  }

  public String getIconEmoji() {
    return iconEmoji;
  }

  public String getChannel() {
    return channel;
  }

  public List<Attachment> getAttachments() {
    return attachments;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  @Override
  public boolean equals(Object rhs) {
    return EqualsBuilder.reflectionEquals(this, rhs);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this, false);
  }

  public static class SlackMessageBuilder {
    private String userName;
    private String text;
    private String iconEmoji;
    private String channel;
    private List<Attachment> attachments;

    SlackMessageBuilder() {
      super();
    }

    public SlackMessageBuilder username(String userName) {
      this.userName = userName;
      return this;
    }

    public SlackMessageBuilder text(String text) {
      this.text = text;
      return this;
    }

    public SlackMessageBuilder icon_emoji(String iconEmoji) {
      this.iconEmoji = iconEmoji;
      return this;
    }

    public SlackMessageBuilder channel(String channel) {
      this.channel = channel;
      return this;
    }

    public SlackMessageBuilder attachments(List<Attachment> attachments) {
      this.attachments = attachments;
      return this;
    }

    public SlackMessageBuilder addAttachment(Attachment attachment) {
      if (isNull(attachment)) {
        return this;
      }

      if (isNull(attachments)) {
        attachments = new ArrayList<>();
      }

      attachments.add(attachment);
      return this;
    }

    public SlackMessage build() {
      return new SlackMessage(this);
    }

    @Override
    public String toString() {
      return ReflectionToStringBuilder.toString(this);
    }

    @Override
    public boolean equals(Object rhs) {
      return EqualsBuilder.reflectionEquals(this, rhs);
    }

    @Override
    public int hashCode() {
      return HashCodeBuilder.reflectionHashCode(this, false);
    }
  }
}
