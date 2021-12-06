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

set -euo pipefail

# The intent of this entrypoint is to override the regular docker dns resolution with a special dnsmasq container

# This dig command works since the container is started with regular docker DNS before we mangle it up
DNS_SERVER=$(dig +short dnsmasq)
REPLACE="nameserver $DNS_SERVER"

# You can't just edit-in-place since /etc/resolv.conf can't disturb the inode
cp /etc/resolv.conf /tmp/resolv.conf
sed -i "s/^nameserver.*$/$REPLACE/g" /tmp/resolv.conf
cp /tmp/resolv.conf /etc/resolv.conf

# If you want to echo the local docker address (which is pretty unhelpful to a layperson) uncomment the next line
# echo "*** DNS Server for this container is at $REPLACE ***"

# Continue on as normal
exec "$@"
