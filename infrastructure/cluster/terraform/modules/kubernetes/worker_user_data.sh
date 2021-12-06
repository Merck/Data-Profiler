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

sudo apt update
# sudo apt upgrade
sudo apt install chrony -y && chronyc activity
sudo apt install python3-pip -y
sudo pip3 install --user awscli
sudo ~/.local/bin/aws configure <<EOF


us-east-1
json
EOF
chronyc sources
chronyc tracking

# mount our extra volume
sudo apt install xfsprogs -y
lsblk
sudo file -s /dev/nvme1n1
sudo mkfs -t xfs /dev/nvme1n1
sudo mkdir /data
sudo mount /dev/nvme1n1 /data

# Note: checkout user data output in cloud-init-output.log 
# sudo cat /var/log/cloud-init-output.log 
# sudo ~/.local/bin/aws ec2 describe-iam-instance-profile-associations