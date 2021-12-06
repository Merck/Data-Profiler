package com.dataprofiler.sqlsync.spec;

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

import com.dataprofiler.ColumnMetaData;
import com.dataprofiler.util.objects.DataScanSpec;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class SqlSyncSpec extends DataScanSpec {

  private List<ColumnMetaData> columnTypeOverrides;
  private String timestampFormat = "dd MMM yyyy hh:mm:ss a";
  private String dateFormat = "dd MMM yyyy";

  public SqlSyncSpec() {
    super();
  }

  public List<ColumnMetaData> getColumnTypeOverrides() {
    return columnTypeOverrides;
  }

  public void setColumnTypeOverrides(List<ColumnMetaData> columnTypeOverrides) {
    this.columnTypeOverrides = columnTypeOverrides;
  }

  public String getTimestampFormat() {
    return timestampFormat;
  }

  public void setTimestampFormat(String timestampFormat) {
    this.timestampFormat = timestampFormat;
  }

  public String getDateFormat() {
    return dateFormat;
  }

  public void setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .appendSuper(super.toString())
        .append("columnTypeOverrides", columnTypeOverrides)
        .append("timestampFormat", timestampFormat)
        .append("dateFormat", dateFormat)
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
    SqlSyncSpec rhs = (SqlSyncSpec) obj;
    return new EqualsBuilder()
        .appendSuper(super.equals(obj))
        .append(columnTypeOverrides, rhs.columnTypeOverrides)
        .append(timestampFormat, rhs.timestampFormat)
        .append(dateFormat, rhs.dateFormat)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(1087, 2293)
        .append(columnTypeOverrides)
        .append(timestampFormat)
        .append(dateFormat)
        .toHashCode();
  }
}
