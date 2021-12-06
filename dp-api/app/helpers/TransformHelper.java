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
package helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.dataprofiler.iterators.transforms.*;
import org.apache.accumulo.core.client.IteratorSetting;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class TransformHelper {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final List<Class> enabledTransforms =
      Arrays.asList(
          CaseTransformIterator.class,
          DateToYearIterator.class,
          DiseaseTransformIterator.class,
          ICD9DiagnosisTransformIterator.class,
          ICD9ICD10DiagnosisTransformIterator.class,
          RegionStateTransformIterator.class,
          GenderTransformIterator.class,
          GenericMappingIterator.class,
          StateAbbreviationTransformIterator.class,
          ZipToStateIterator.class);

  public static ArrayNode availableTransforms() throws NoSuchMethodException {
    ArrayNode ret = mapper.createArrayNode();
    for (Class transform : enabledTransforms) {
      Method method = transform.getMethod("describeTransform", null);

      Object output = null;
      try {
        output = method.invoke(null);
      } catch (java.lang.IllegalAccessException e) {
        break;
      } catch (java.lang.reflect.InvocationTargetException e) {
        break;
      }

      ret.add(mapper.valueToTree(output));
    }
    return ret;
  }

  public static IteratorSetting getIteratorSetting(JsonNode transform, Integer stackIndex) {
    IteratorSetting iterSetting = null;

    if (transform == null) {
      return iterSetting;
    }

    String id = transform.get("id").asText();
    String name = id + stackIndex;
    String klazz = "com.dataprofiler.iterators.transforms." + id;
    iterSetting = new IteratorSetting(stackIndex, name, klazz);
    HashMap<String, String> options = mapper.convertValue(transform.get("options"), HashMap.class);
    iterSetting.addOptions(options);
    if (!stackIndex.equals(40)) {
      iterSetting.addOption("stack", "true");
    }
    return iterSetting;
  }
}
