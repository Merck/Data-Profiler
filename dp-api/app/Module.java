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
import com.google.inject.AbstractModule;

/**
 * This class is a Guice module that tells Guice how to bind several different types. This Guice
 * module is created when the Play application starts.
 *
 * <p>Play will automatically use any class called `Module` that is in the root package. You can
 * create modules in other locations by adding `play.modules.enabled` settings to the
 * `application.conf` configuration file.
 */
public class Module extends AbstractModule {

  @Override
  public void configure() {}
}
