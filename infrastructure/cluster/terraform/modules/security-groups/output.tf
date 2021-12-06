output "frontend_security_group_id" {
  description = "The ID of the frontend security group"
  value       = module.frontend_sg.this_security_group_id
}

output "frontend_security_group_name" {
  description = "The name of the frontend security group"
  value       = module.frontend_sg.this_security_group_name
}

output "backend_security_group_id" {
  description = "The ID of the backend security group"
  value       = module.backend_sg.this_security_group_id
}

output "backend_security_group_name" {
  description = "The name of the backend security group"
  value       = module.backend_sg.this_security_group_name
}

output "dns_security_group_id" {
  description = "The ID of the DNS security group"
  value       = module.dns_sg.this_security_group_id
}

output "dns_security_group_name" {
  description = "The name of the DNS security group"
  value       = module.dns_sg.this_security_group_name
}


output "dataprofiler_kube_ng_security_group_id" {
  description = "The ID of the dataprofiler_kube_ng security group"
  value       = aws_security_group.dataprofiler_kube_ng_sg.id
}

output "dataprofiler_kube_ng_security_group_name" {
  description = "The name of the dataprofiler_kube_ng security group"
  value       = aws_security_group.dataprofiler_kube_ng_sg.name
}


output "dataprofiler_lb_ingress_public_security_group_id" {
  description = "The ID of the dataprofiler_lb_ingress_public security group"
  value       = aws_security_group.dataprofiler_lb_ingress_public_sg.id
}

output "dataprofiler_lb_ingress_public_security_group_name" {
  description = "The name of the dataprofiler_lb_ingress_public security group"
  value       = aws_security_group.dataprofiler_lb_ingress_public_sg.name
}

output "dataprofiler_lb_ingress_private_security_group_id" {
  description = "The ID of the dataprofiler_lb_ingress_private security group"
  value       = aws_security_group.dataprofiler_lb_ingress_private.id
}

output "dataprofiler_lb_ingress_private_security_group_name" {
  description = "The name of the dataprofiler_lb_ingress_private security group"
  value       = aws_security_group.dataprofiler_lb_ingress_private.name
}
