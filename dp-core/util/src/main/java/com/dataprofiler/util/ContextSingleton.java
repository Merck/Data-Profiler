package com.dataprofiler.util;

/*-
 * 
 * dataprofiler-util
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

import java.io.Serializable;

/***
 * A Singleton object that is serializable for the Contexts. This can be useful for
 * Spark to create a context per-executor (well - per-JVM).
 */
public class ContextSingleton implements Serializable {
  private transient Context context;
  private final Config config;

  public ContextSingleton(Config config) {
    this.config = config;
  }

  public ContextSingleton(Context context) {
    // This is done this way because when we are running with mini accumulo and spark is
    // running locally a) we need to not connect on context because that creates a new
    // mini accumulo and b) there is no reason to create a new context anyway because everything
    // is in the same JVM
    this.context = context;
    this.config = context.config;
  }

  public Context getContext() throws BasicAccumuloException {
    if (context != null) {
      return context;
    }

    context = new Context(config);
    return context;
  }
}
