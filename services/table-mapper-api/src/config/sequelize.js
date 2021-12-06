/*
  Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
 
 	Licensed to the Apache Software Foundation (ASF) under one
 	or more contributor license agreements. See the NOTICE file
 	distributed with this work for additional information
 	regarding copyright ownership. The ASF licenses this file
 	to you under the Apache License, Version 2.0 (the
 	"License"); you may not use this file except in compliance
 	with the License. You may obtain a copy of the License at
 
 	http://www.apache.org/licenses/LICENSE-2.0
 
 
 	Unless required by applicable law or agreed to in writing,
 	software distributed under the License is distributed on an
 	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 	KIND, either express or implied. See the License for the
 	specific language governing permissions and limitations
 	under the License.
*/
module.exports = {
  username: "postgres",
  password: "",
  database: "tables",
  host: process.env["TABLES_DB"] || "db",
  dialect: "postgres",
  logging: process.env.NODE_ENV === "development",
  pool: {
    max: process.env.DATABASE_NUM_POOL_CONNS || 5,
    idle: process.env.DATABASE_POOL_IDLE_MILLI || 10000,
    evict: process.env.DATABASE_POOL_EVICT_MILLI || 10000
  }
};
