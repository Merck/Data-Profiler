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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * represents a slack message attachment
 *
 * @see "https://api.slack.com/methods/chat.postMessage#arg_attachments"
 *     <pre>
 *           attachments: [
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
 *               },
 *             ]
 *           }
 * </pre>
 */
public class Attachment {
  private final String text;
  private final String color;
  private final String fallback;
  private final List<Field> fields;

  protected Attachment(AttachmentBuilder builder) {
    super();
    this.text = builder.text;
    this.color = builder.color;
    this.fallback = builder.fallback;
    this.fields = builder.fields;
  }

  public static AttachmentBuilder builder() {
    return new AttachmentBuilder();
  }

  public String getText() {
    return text;
  }

  public String getColor() {
    return color;
  }

  public String getFallback() {
    return fallback;
  }

  public List<Field> getFields() {
    return fields;
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

  public static class AttachmentBuilder {
    private String text;
    private String color;
    private String fallback;
    private List<Field> fields;

    AttachmentBuilder() {
      super();
    }

    public AttachmentBuilder text(String text) {
      this.text = text;
      return this;
    }

    public AttachmentBuilder color(String color) {
      this.color = color;
      return this;
    }

    public AttachmentBuilder fallback(String fallback) {
      this.fallback = fallback;
      return this;
    }

    public AttachmentBuilder fields(List<Field> fields) {
      this.fields = fields;
      return this;
    }

    public AttachmentBuilder addField(Field field) {
      if (isNull(field)) {
        return this;
      }

      if (isNull(fields)) {
        fields = new ArrayList<>();
      }

      fields.add(field);
      return this;
    }

    public Attachment build() {
      return new Attachment(this);
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
