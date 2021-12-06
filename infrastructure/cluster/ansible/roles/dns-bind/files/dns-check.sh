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

get_hosts_from_aws() {
  local site=$1
  hosts=$(aws ec2 describe-instances \
	  	--filters Name=tag:Name,Values=dataprofiler-${site}* \
  		--query "Reservations[*].Instances[*].{PrivateIp:PrivateIpAddress,Name:Tags[?Key=='Name']|[0].Value}" \
		--output text)
  echo "$hosts"
}

check_ipv4_forward_zone() {
  local host=$1
  local ip_version=$3
  dig -4 +short +search $host | grep -v -e '^$'
  return $?
}

check_ipv4_reverse_zone() {
  local ip=$1
  local ip_version=$3
  dig -4 +short +search -x $ip | grep -v -e '^$'
  return $?
}

report_status() {
  local line='--------------------------------------------------------------------'
  local red='\033[0;31m'
  local green='\033[0;32m'
  local none='\033[0m'
  if [[ $2 == 0 ]]; then
    printf "%s %s[ ${green} OK ${none}]\n" "$1" "${line:${#1}}"
  else
    printf "%s %s[ ${red} ERROR ${none}]\n" "$1" "${line:${#1}}"
  fi
}

[[ -z $1 ]] && echo -e "usage:\n\t dns-check.sh [ development | preview | production ]" && exit 1

site=${1:-production}

# set new line as temporary separator
PREV_IFS="$IFS"
IFS=$'\n'

# inventory_hostname -> private_ip
declare -a aws_cluster_info
aws_cluster_info=("$(get_hosts_from_aws $site)")

# return IFS to default
#IFS="${PREV_IFS}"

# get cluster instances from aws
declare -A cluster_hosts
cluster_hosts=()

for l in ${aws_cluster_info[@]}; do
  ll=$(echo "$l" | tr -s ' ' | awk '{$1=$1;print}')
  hostname=$(echo "$ll" | awk '{ print $1 }')
  ip=$(echo "$ll" | awk '{ print $2 }')
  cluster_hosts["$hostname"]="$ip"
done

echo "**** checking ipv4 forward and reverse lookups"

for h in "${!cluster_hosts[@]}"; do

  # aws actual ip
  aws_ip="${cluster_hosts[$h]}"

  printf "%s (%s):\n" "$h" "$aws_ip"

  # aws inventory hosts -> cluster host names
  if grep -q -e '[0-9]$' <<< "$h"; then
      hh=$(echo "$h" | sed -e "s/dataprofiler-${site}-//g" | sed -r "s/(.*)-/\1-${site}-/")
  else
      hh=$(echo "$h" | sed -e "s/dataprofiler-${site}-//g")
      hh="${hh}-${site}"
  fi

  # forward lookups
  ip=$(check_ipv4_forward_zone $hh)
  forward_result=$?
  report_status "    $hh (forward)" $forward_result

  # reverse lookup using result from above
  fqdn=$(check_ipv4_reverse_zone $ip)
  reverse_result=$?
  report_status "    $ip (reverse)" $reverse_result

  # if the forward lookup was successful, but the reverse lookup failed
  if [[ $foward_result -eq 0 && $reverse_result -ne 0 ]]; then
    echo "    **** attempting to update nameserver...."
    ssh -o "StrictHostKeyChecking=no" $ip -C "sudo dhclient"
    reverse_update=$?

    # if the nameserver update succeeded after a reverse lookup failure
    if [[ $reverse_update == 0 && $reverse_result != 0 ]]; then
      echo "    **** dynamic update succeeded for $ip"
      echo "    **** retrying reverse lookup for $ip...."
      fqdn=$(check_ipv4_reverse_zone $ip)
      reverse_recheck=$?
    else
      echo "    **** dynamic update failed for $ip"
    fi
    report_status "    $ip (reverse)" $reverse_recheck
  fi

  # result should mirror one another AND match what aws is reporting
  [[ "$hh" == "$(echo $fqdn | cut -d"." -f1)" ]] && [[ "$ip" == "$aws_ip" ]] && \
	  report_status "    synced" $? || report_status "    synced" $?

done