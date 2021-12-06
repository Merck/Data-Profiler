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
output "master_private_dns" {
  description = "Private DNS name assigned to the master instances"
  value       = module.ec2_accumulo_master.private_dns
}

output "worker_private_dns" {
  description = "List of private DNS names assigned to the worker instances"
  value       = module.ec2_accumulo_worker.private_dns
}

output "zookeeper_private_dns" {
  description = "List of private DNS names assigned to the zookeeper instances"
  value       = module.ec2_zookeeper.private_dns
}

output "master_private_ip" {
  description = "Private IP assigned to the master instances"
  value       = module.ec2_accumulo_master.private_ip
}

output "worker_private_ip" {
  description = "List of private IPs assigned to the worker instances"
  value       = module.ec2_accumulo_worker.private_ip
}

output "zookeeper_private_ip" {
  description = "List of private IPs assigned to the zookeeper instances"
  value       = module.ec2_zookeeper.private_ip
}

output "master_tags" {
  description = "Tags applied to master"
  value       = module.ec2_accumulo_master.tags
}

output "worker_tags" {
  description = "Tags applied to the workers"
  value       = module.ec2_accumulo_worker.tags
}

output "zookeeper_tags" {
  description = "Tags applied to the zookeepers"
  value       = module.ec2_zookeeper.tags
}