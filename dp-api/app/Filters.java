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
import filters.LoggingFilter;
import filters.StatsFilter;
import play.filters.cors.CORSFilter;
import play.filters.csrf.CSRFFilter;
import play.filters.headers.SecurityHeadersFilter;
import play.http.DefaultHttpFilters;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Filters extends DefaultHttpFilters {

  @Inject
  public Filters(
      CSRFFilter csrfFilter,
      LoggingFilter loggingFilter,
      SecurityHeadersFilter securityHeadersFilter,
      CORSFilter corsFilter,
      StatsFilter statsFilter) {
    super(csrfFilter, loggingFilter, securityHeadersFilter, corsFilter, statsFilter);
  }
}
