#!/bin/bash
#
# Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
#
#	Licensed to the Apache Software Foundation (ASF) under one
#	or more contributor license agreements. See the NOTICE file
#	distributed with this work for additional information
#	regarding copyright ownership. The ASF licenses this file
#	to you under the Apache License, Version 2.0 (the
#	"License"); you may not use this file except in compliance
#	with the License. You may obtain a copy of the License at
#
#	http://www.apache.org/licenses/LICENSE-2.0
#
#
#	Unless required by applicable law or agreed to in writing,
#	software distributed under the License is distributed on an
#	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#	KIND, either express or implied. See the License for the
#	specific language governing permissions and limitations
#	under the License.
#

# Activate ROU attributes
curl localhost:8081/graphql -H 'Authorization: dp-rou-key' -H 'Content-Type: application/json' -XPOST --data '{"query":"mutation {markAttributeActive(value:\"system.admin\"){id value is_active}}"}'
curl localhost:8081/graphql -H 'Authorization: dp-rou-key' -H 'Content-Type: application/json' -XPOST --data '{"query":"mutation {markAttributeActive(value:\"LIST.PUBLIC_DATA\"){id value is_active}}"}'

# Activate ROU attributes for 'developer' user
curl localhost:8081/graphql -H 'Authorization: dp-rou-key' -H 'Content-Type: application/json' -XPOST --data '{"query":"mutation {createUpdateUser(username: \"local-developer\", attributes: [\"system.admin\",\"LIST.PUBLIC_DATA\",\"LIST.PRIVATE_DATA\"]) {id}}" }'
