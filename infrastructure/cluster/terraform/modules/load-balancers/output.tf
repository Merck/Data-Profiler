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
output "public_lb_id" {
  description = "The ID and ARN of the public load balancer"
  value       = aws_lb.k8s_all_ingress_public.id
}

output "public_lb_dns_name" {
  description = "The DNS name of the public load balancer"
  value       = aws_lb.k8s_all_ingress_public.id
}


output "private_lb_id" {
  description = "The ID and ARN of the private load balancer"
  value       = aws_lb.k8s_all_ingress_private.id
}

output "private_lb_dns_name" {
  description = "The DNS name of the private load balancer"
  value       = aws_lb.k8s_all_ingress_private.name
}
