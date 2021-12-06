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
#!/bin/bash

tag_and_push() { 
    COMPONENT=$(echo $1 | awk -F "/" '{print $2}')
    BUILD_PATH=$(dirname $1)
    docker build --no-cache --pull -t "container-registry.com/$COMPONENT:latest" $BUILD_PATH
    docker push "container-registry..com/$COMPONENT:latest"
};
export -f tag_and_push; 

echo "docker" | docker login -u "docker" --password-stdin container-registry.com 
find . -name 'Dockerfile' -exec bash -c 'tag_and_push "$0"' {} \;