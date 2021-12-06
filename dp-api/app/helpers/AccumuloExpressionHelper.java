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

import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.ColumnVisibility;
import org.apache.accumulo.core.security.VisibilityEvaluator;
import org.apache.accumulo.core.util.BadArgumentException;

import java.io.UnsupportedEncodingException;

public class AccumuloExpressionHelper {

  private static final String ENCODING = "UTF-8";

  /** escapeSingleString returns an escaped version of an attribute */
  public static String escapeSingleString(String input) {
    try {
      byte[] bytestring = input.getBytes(ENCODING);
      byte[] output = new VisibilityEvaluator(new Authorizations()).escape(bytestring, false);
      String ret = new String(output, ENCODING);
      return ret;
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace(System.out);
      return null;
    }
  }

  /**
   * validateExpression Callers of this method need to catch
   * org.apache.accumulo.core.util.BadArgumentException; null means it's a bad expression; true
   * means it's good
   */
  public static String validateExpression(String input) throws BadArgumentException {
    try {
      byte[] bytestring = input.getBytes(ENCODING);
      String ret = new ColumnVisibility(bytestring).toString();
      return ret;
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace(System.out);
      return null;
    }
  }
}
