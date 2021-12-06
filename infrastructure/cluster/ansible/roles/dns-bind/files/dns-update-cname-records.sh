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

ttl=3600
domain='dataprofiler.com'
authority_dns=''
authority_dns_backup=''

#log_file=/var/log/dns_update_cnames.log

log_info() {
  printf '%s %s' "$(date)" "$1" &>> "$log_file"
}

retrieve_cname_for_host() {
  local host=$1
  output=$(dig CNAME +short $host @$authority_dns | head -n1)
  echo $output
}

send_zone_update() {
  local host=$1
  local cname=$2

  echo "
		zone ${domain}
		update delete ${host} A
		update add ${host} ${ttl} CNAME ${cname}
		;show
		send
    "| nsupdate -v
}

report_status() {
  local line='--------------------------------------------------------------------'
  local red='\033[0;31m'
  local green='\033[0;32m'
  local none='\033[0m'
  if [[ $2 == 0 ]]; then
    printf "%s %s${green} OK $none\n" "$1" "${line:${#1}}"
  else
    printf "%s %s${red} ERROR $none\n" "$1" "${line:${#1}}"
  fi
}

declare -a external_records
external_records=(secrets.dataprofiler.com container-registry.dataprofiler.com)

#log_info "updating external CNAME records:"
#log_info "${external_records[@]}"

for host in "${external_records[@]}"; do

  # get CNAME from authority
  cname=$(retrieve_cname_for_host $host)
  cname_result=$?

  # send update request
  update=$(send_zone_update $host $cname)
  update_result=$?

  [[ $update_result -eq 0 ]] && report_status $host $update_result || report_status $host $update_result

done