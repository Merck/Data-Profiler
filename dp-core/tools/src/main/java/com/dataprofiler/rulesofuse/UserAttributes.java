package com.dataprofiler.rulesofuse;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.apache.accumulo.core.security.Authorizations;

/**
 * TODO: remove this class if not used. deprecate this in favor of the
 * RulesOfUseHelper.UserAttributes
 *
 * @deprecated
 * @todo
 */
@Deprecated
public class UserAttributes {
  private static final ObjectMapper mapper = new ObjectMapper();
  private Set<String> allVisibilities = new HashSet<>();
  private Set<String> visibilitiesToReject = new HashSet<>();
  private Set<String> currentVisibilities = new HashSet<>();

  private void calculateCurrentVisibilities() {
    currentVisibilities = new HashSet<>();
    currentVisibilities.addAll(allVisibilities);
    currentVisibilities.removeAll(visibilitiesToReject);
  }

  private String setToJSON(Set<String> s) {
    try {
      return mapper.writeValueAsString(s);
    } catch (JsonProcessingException e) {
      return "";
    }
  }

  public String allAsJSONString() {
    return setToJSON(allVisibilities);
  }

  public String currentAsJSONString() {
    return setToJSON(currentVisibilities);
  }

  public Authorizations currentAsAuthorizations() {
    return new Authorizations(currentVisibilities.toArray(new String[currentVisibilities.size()]));
  }

  public Set<String> getAllVisibilities() {
    return allVisibilities;
  }

  public void setAllVisibilities(Set<String> allVisibilities) {
    this.allVisibilities = allVisibilities;
    calculateCurrentVisibilities();
  }

  public Set<String> getVisibilitiesToReject() {
    return visibilitiesToReject;
  }

  public void setVisibilitiesToReject(Set<String> visibilitiesToReject) {
    this.visibilitiesToReject = visibilitiesToReject;
    calculateCurrentVisibilities();
  }

  public boolean setVisibilitiesToReject(String visibilitiesToRejectJSONString) {
    if (visibilitiesToRejectJSONString == null) {
      return false;
    }

    TypeReference<ArrayList<String>> stringArrType = new TypeReference<ArrayList<String>>() {};
    try {
      ArrayList<String> reject = mapper.readValue(visibilitiesToRejectJSONString, stringArrType);

      setVisibilitiesToReject(new HashSet<>(reject));

      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
