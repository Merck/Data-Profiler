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
#!/usr/bin/env bash

if [[ "$1" == *\/* ]] || [[ "$1" == *\\* ]] || [[ "$1" == *\@* ]]
then
  echo "Cannot have slashes or @ in package name"
  exit 1
fi

if [ -z "$1" ]
then
  echo "You must pass an argument such as `yarn run add-package my-sweet-package-name`"
  exit 1
fi

LERNA_PKG="@dp-ui\/$1"
REPLACEMENT="@dp-ui\/newpackage"

cp -r ./scaffold ./packages/$1
cd ./packages/$1
sed -i '' "s/$REPLACEMENT/$LERNA_PKG/g" package.json
rm copy.sh
cd $OLDPWD
yarn run bootstrap
yarn run lerna add "@dp-ui/$1" --scope @dp-ui/parent
