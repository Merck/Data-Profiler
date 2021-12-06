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
#
# Private Load Balancer
#
resource "aws_lb" "dataprofiler_lb_ingress_public_sg" {
  name                       = "dataprofiler-k8s-all-ingress-private"
  load_balancer_type         = "application"
  security_groups            = var.vpc_security_group_ids_ingress_private
  internal                   = true
  enable_deletion_protection = true
  idle_timeout               = 4000


  tags = var.tags
}

resource "aws_lb_listener" "private_http_listener" {
  load_balancer_arn = aws_lb.dataprofiler_lb_ingress_public_sg.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.dataprofiler_lb_ingress_public_sg_target_group.arn

  }
}

resource "aws_lb_listener" "private_https_listener" {
  load_balancer_arn = aws_lb.dataprofiler_lb_ingress_public_sg.arn
  port              = "443"
  protocol          = "HTTPS"
  certificate_arn   = var.certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.dataprofiler_lb_ingress_public_sg_target_group.arn

  }
}

resource "aws_lb_target_group" "dataprofiler_lb_ingress_public_sg_target_group" {
  name                          = "dataprofiler-k8s-all-ingress-private"
  port                          = 30080
  protocol                      = "HTTP"
  load_balancing_algorithm_type = "round_robin"

  vpc_id = var.vpc_id
  tags   = var.tags
}

resource "aws_lb_target_group_attachment" "dataprofiler_lb_ingress_public_sg_target_group_attachment" {
  # This may seem ultra funky, but it is ignoring any kube machines that are currently deactivated
  for_each = toset([for id in var.worker_instance_ids : id if !contains(["i-026d8c2711882b752"], id)])

  target_group_arn = aws_lb_target_group.dataprofiler_lb_ingress_public_sg_target_group.arn
  target_id        = each.key
  port             = 30080
}


#
# Public Load Balancer
#
resource "aws_lb" "dataprofiler_k8s_all_ingress_public" {
  name                       = "dataprofiler-k8s-all-ingress-public"
  load_balancer_type         = "application"
  security_groups            = var.vpc_security_group_ids_ingress_public
  internal                   = true
  enable_deletion_protection = true
  idle_timeout               = 4000

  tags = var.tags
}

resource "aws_lb_listener" "public_http_listener" {
  load_balancer_arn = aws_lb.dataprofiler_k8s_all_ingress_public.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type = "redirect"
    redirect {
      port        = 443
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_lb_listener" "public_https_listener" {
  load_balancer_arn = aws_lb.dataprofiler_k8s_all_ingress_public.arn
  port              = "443"
  protocol          = "HTTPS"
  certificate_arn   = var.certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.dataprofiler_k8s_all_ingress_public_target_group.arn
  }
}

resource "aws_lb_target_group" "dataprofiler_k8s_all_ingress_public_target_group" {
  name                          = "dataprofiler-k8s-all-ingress-public"
  port                          = 30080
  protocol                      = "HTTP"
  load_balancing_algorithm_type = "round_robin"

  vpc_id = var.vpc_id
  tags   = var.tags
}

resource "aws_lb_target_group_attachment" "dataprofiler_k8s_all_ingress_public_target_group_attachment" {
  target_group_arn = aws_lb_target_group.dataprofiler_k8s_all_ingress_public_target_group.arn
  target_id        = each.key
  port             = 30080
}
