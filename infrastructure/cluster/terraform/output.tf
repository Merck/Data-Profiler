#
# Security group output
#

output "frontend_security_group_id" {
  description = "The ID of the frontend security group"
  value       = module.security_group.frontend_security_group_id
}

output "frontend_security_group_name" {
  description = "The name of the frontend security group"
  value       = module.security_group.frontend_security_group_name
}

output "backend_security_group_id" {
  description = "The ID of the backend security group"
  value       = module.security_group.backend_security_group_id
}

output "backend_security_group_name" {
  description = "The name of the backend security group"
  value       = module.security_group.backend_security_group_name
}

output "dns_security_group_name" {
  description = "The name of the DNS security group"
  value       = module.security_group.dns_security_group_name
}

output "dns_security_group_id" {
  description = "The ID of the DNS security group"
  value       = module.security_group.dns_security_group_id
}


#
# Load balancer output
#

# output "public_lb_id" {
#   description = "The ID and ARN of the load balancer"
#   value       = module.load_balancer.public_lb_id
# }

# output "public_lb_dns_name" {
#   description = "The DNS name of the load balancer"
#   value       = module.load_balancer.public_lb_dns_name
# }

#
# Bastion output
#

output "bastion_private_dns" {
  description = "Private DNS name assigned to the bastion instances"
  value       = module.bastion.private_dns
}

output "bastion_private_ip" {
  description = "Private IP assigned to the bastion instances"
  value       = module.bastion.private_ip
}

output "bastion_tags" {
  description = "Tags applied to the bastion hosts"
  value       = module.bastion.tags
}


#
# DNS output
#

output "dns_private_dns" {
  description = "Private DNS name assigned to the primary DNS instance"
  value       = module.dns.private_dns
}

output "dns_private_ip" {
  description = "Private IP assigned to the primary DNS instance"
  value       = module.dns.private_ip
}

output "dns_tags" {
  description = "Tags applied to the DNS hosts"
  value       = module.dns.tags
}


#
# Data-processing output
#

output "data_processing_private_dns" {
  description = "Private DNS name assigned to the data-processing instances"
  value       = module.data_processing.private_dns
}

output "data_processing_private_ip" {
  description = "Private IP assigned to the data-processing instances"
  value       = module.data_processing.private_ip
}

output "data_processing_tags" {
  description = "Tags applied to the data-processing hosts"
  value       = module.data_processing.tags
}


#
# Spark output
#

output "spark_master_private_dns" {
  description = "Private DNS name assigned to the Spark master instances"
  value       = module.spark_cluster.master_private_dns
}

output "spark_worker_private_dns" {
  description = "List of private DNS names assigned to the Spark worker instances"
  value       = module.spark_cluster.worker_private_dns
}

output "master_private_ip" {
  description = "Private IP assigned to the Spark master instances"
  value       = module.spark_cluster.master_private_ip
}

output "spark_worker_private_ip" {
  description = "List of private IPs assigned to the Spark worker instances"
  value       = module.spark_cluster.worker_private_ip
}

output "spark_master_tags" {
  description = "Tags applied to Spark master"
  value       = module.spark_cluster.master_tags
}

output "spark_worker_tags" {
  description = "Tags applied to the Spark workers"
  value       = module.spark_cluster.worker_tags
}


#
# Accumulo output
#

output "accumulo_master_private_dns" {
  description = "Private DNS name assigned to the Accumulo master instances"
  value       = module.accumulo_cluster.master_private_dns
}

output "accumulo_worker_private_dns" {
  description = "List of private DNS names assigned to the Accumulo worker instances"
  value       = module.accumulo_cluster.worker_private_dns
}

output "zookeeper_private_dns" {
  description = "List of private DNS names assigned to the zookeeper instances"
  value       = module.accumulo_cluster.zookeeper_private_dns
}

output "accumulo_master_private_ip" {
  description = "Private IP assigned to the Accumulo master instances"
  value       = module.accumulo_cluster.master_private_ip
}

output "accumulo_worker_private_ip" {
  description = "List of private IPs assigned to the Accumulo worker instances"
  value       = module.accumulo_cluster.worker_private_ip
}

output "zookeeper_private_ip" {
  description = "List of private IPs assigned to the zookeeper instances"
  value       = module.accumulo_cluster.zookeeper_private_ip
}

output "accumulo_master_tags" {
  description = "Tags applied to Accumulo master"
  value       = module.accumulo_cluster.master_tags
}

output "accumulo_worker_tags" {
  description = "Tags applied to the Accumulo workers"
  value       = module.accumulo_cluster.worker_tags
}

output "zookeeper_tags" {
  description = "Tags applied to the zookeepers"
  value       = module.accumulo_cluster.zookeeper_tags
}
