package com.dataprofiler.rulesofuse.response;

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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class RouUserWithAttribute {

  protected Set<RouUser> usersWithAttribute;

  public RouUserWithAttribute() {
    super();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("usersWithAttribute", usersWithAttribute).toString();
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
    RouUserWithAttribute rhs = (RouUserWithAttribute) obj;
    return new EqualsBuilder().append(usersWithAttribute, rhs.usersWithAttribute).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(7, 89).append(usersWithAttribute).toHashCode();
  }

  public Set<RouUser> getImmutableUsersWithAttribute() {
    return Collections.unmodifiableSet(usersWithAttribute);
  }

  public Set<RouUser> getCowWithAttribute() {
    return new CopyOnWriteArraySet<>(usersWithAttribute);
  }

  public Set<RouUser> getUsersWithAttribute() {
    return usersWithAttribute;
  }

  public void setUsersWithAttribute(Set<RouUser> usersWithAttribute) {
    this.usersWithAttribute = usersWithAttribute;
  }
}
