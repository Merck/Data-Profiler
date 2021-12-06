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

variable "vpc_id" {
  description = "ID of the VPC"
  type        = string
}

variable "worker_instance_ids" {
  description = "A list of instance IDs for kubernetes workers"
  type        = list(string)
}

variable "vpc_security_group_ids_ingress_private" {
  description = "A list of security group IDs to associate with the private ingress"
  type        = list(string)
}

variable "vpc_security_group_ids_ingress_public" {
  description = "A list of security group IDs to associate with the public ingress"
  type        = list(string)
}

variable "certificate_arn" {
  description = "ARN for the SSL certificate"
  type        = string
}

variable "tags" {
  description = "Tags to apply to resources created by the load-balancers module"
  type        = map(string)
}
