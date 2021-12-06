package com.dataprofiler.util.objects;

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

import com.dataprofiler.util.Context;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomAnnotationMock {

  public static CustomAnnotation scannerWithMock() {
    return customAnnotationWithMockedVisibility();
  }

  public static CustomAnnotation customAnnotationWithMockedVisibility() {
    String viz = "LIST.PUBLIC_DATA";
    CustomAnnotationVisibilityProvider visibilityProvider =
        mock(CustomAnnotationVisibilityProvider.class);
    when(visibilityProvider.lookupVisibilityExpressionMetadata(
            any(Context.class), any(String.class)))
        .thenReturn(viz);
    when(visibilityProvider.lookupVisibilityExpressionMetadata(
            any(Context.class), any(String.class), any(String.class)))
        .thenReturn(viz);
    when(visibilityProvider.lookupVisibilityExpressionMetadata(
            any(Context.class), any(String.class), any(String.class), any(String.class)))
        .thenReturn(viz);
    CustomAnnotation annotation = new CustomAnnotation();
    annotation.setVisibilityProvider(visibilityProvider);
    return annotation;
  }
}
