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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * represents a slack message attachment
 *
 * @see "https://api.slack.com/methods/chat.postMessage#arg_attachments"
 *     <pre>
 *                 {
 *                   title: "Environment",
 *                   value: process.env.ENVIRONMENT_NAME,
 *                   short: true
 *                 }
 *     </pre>
 */
public class Field {
  private final String title;
  private final String value;

  @JsonProperty("short")
  private final boolean isShort;

  protected Field(FieldBuilder builder) {
    super();
    this.title = builder.title;
    this.value = builder.value;
    this.isShort = builder.isShort;
  }

  public static FieldBuilder builder() {
    return new FieldBuilder();
  }

  public String getTitle() {
    return title;
  }

  public String getValue() {
    return value;
  }

  public boolean isShort() {
    return isShort;
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

  public static class FieldBuilder {
    private String title;
    private String value;
    private boolean isShort;

    FieldBuilder() {
      super();
    }

    public FieldBuilder title(String title) {
      this.title = title;
      return this;
    }

    public FieldBuilder value(String value) {
      this.value = value;
      return this;
    }

    public FieldBuilder isShort(boolean isShort) {
      this.isShort = isShort;
      return this;
    }

    public Field build() {
      return new Field(this);
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
