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
package actions;

import org.springframework.core.annotation.Order;
import play.mvc.With;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static helpers.RulesOfUseHelper.ADMIN_CAPABILITY;

@With(RequireCapabilityImpl.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Order(value = 2)
public @interface RequireCapability {
  String value() default ADMIN_CAPABILITY;
}
