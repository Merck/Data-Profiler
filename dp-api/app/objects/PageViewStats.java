/**
*  Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
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
**/
package objects;

import helpers.URLEncodingHelper;
import java.util.HashMap;

public class PageViewStats {
  private HashMap<String, HashMap<String, Integer>> data;

  public PageViewStats() {
    this.data = new HashMap<>();
  }

  public void add(String username, String path, Integer count) {
    if (count == null || count.equals(0)) {
      return;
    }
    String cleanPath = URLEncodingHelper.decode(path.replace("~", "/"));
    if (this.data.containsKey(username)) {
      HashMap<String, Integer> current = this.data.get(username);
      if (current.containsKey(cleanPath)) {
        current.replace(cleanPath, current.get(cleanPath) + count);
      } else {
        current.put(cleanPath, count);
      }
    } else {
      HashMap<String, Integer> initial = new HashMap<>();
      initial.put(cleanPath, count);
      this.data.put(username, initial);
    }
  }

  public HashMap<String, HashMap<String, Integer>> getData() {
    return data;
  }

  public void setData(HashMap<String, HashMap<String, Integer>> data) {
    this.data = data;
  }
}
