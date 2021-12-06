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
module "frontend_sg" {
  source = "terraform-aws-modules/security-group/aws"
  version = "3.18.0"

  name        = "dataprofiler-frontend-sg"
  description = "Frontend security group for cluster: dataprofiler"
  tags        = var.tags
  vpc_id      = var.vpc_id

  ingress_cidr_blocks = ["0.0.0.0/0"]

  ingress_rules = ["ssh-tcp"]
  egress_rules  = ["all-all"]
}

module "backend_sg" {
  source = "terraform-aws-modules/security-group/aws"
  version = "3.18.0"

  name        = "dataprofiler-backend-sg"
  description = "Backend security group for cluster: dataprofiler"
  tags        = var.tags
  vpc_id      = var.vpc_id

  ingress_with_self = [
    {
      rule = "all-all"
    }
  ]
  ingress_with_source_security_group_id = [
    {
      rule                     = "all-all"
      source_security_group_id = module.frontend_sg.this_security_group_id
    }
  ]
  egress_rules = ["all-all"]
}

module "dns_sg" {
  source = "terraform-aws-modules/security-group/aws"
  version = "3.18.0"

  name        = "dataprofiler-dns-sg"
  description = "DNS security group for cluster: dataprofiler"
  tags        = var.tags
  vpc_id      = var.vpc_id

  ingress_cidr_blocks = ["0.0.0.0/0"]

  ingress_rules = ["dns-tcp", "dns-udp"]
}

# Dataprofiler Kube SG must have aws_security_group_rules to deal with cyclic dependencies in other
# Security Groups

resource "aws_security_group" "dataprofiler_kube_ng_sg" {
  name        = "dataprofiler-kube-ng-sg"
  description = "Kubernetes node group security group"
  vpc_id      = var.vpc_id

  tags = merge(
    tomap({"Name" = "dataprofiler-kube-node-group-sg"})
  , var.tags)

}

resource "aws_security_group_rule" "egress" {
  security_group_id = aws_security_group.dataprofiler_kube_ng_sg.id
  type              = "egress"
  description       = ""
  from_port         = 0
  to_port           = 65535
  protocol          = "all"
  cidr_blocks       = ["0.0.0.0/0"]
  ipv6_cidr_blocks  = ["::/0"]
}

resource "aws_security_group_rule" "ingress_all" {
  security_group_id = aws_security_group.dataprofiler_kube_ng_sg.id
  type              = "ingress"
  description       = ""
  from_port         = 0
  to_port           = 65535
  protocol          = "all"
  cidr_blocks       = ["0.0.0.0/0"]
  ipv6_cidr_blocks  = ["::/0"]
}

resource "aws_security_group_rule" "icmp_ingress" {
  security_group_id = aws_security_group.dataprofiler_kube_ng_sg.id
  type              = "ingress"
  description       = ""
  from_port         = -1
  to_port           = -1
  protocol          = "icmp"
  cidr_blocks       = ["0.0.0.0/0"]
  ipv6_cidr_blocks  = ["::/0"]
}

resource "aws_security_group_rule" "ssh_ingress" {
  security_group_id = aws_security_group.dataprofiler_kube_ng_sg.id
  type              = "ingress"
  description       = ""
  from_port         = 22
  to_port           = 22
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  ipv6_cidr_blocks  = ["::/0"]
}

resource "aws_security_group_rule" "s3_proxy_ingress" {
  type              = "ingress"
  description       = "s3proxy"
  security_group_id = aws_security_group.dataprofiler_kube_ng_sg.id
  from_port         = 30400
  to_port           = 30400
  cidr_blocks       = []
  ipv6_cidr_blocks  = []
  protocol          = "tcp"
}

resource "aws_security_group_rule" "ingress_to_dataprofiler_lb_ingress_private" {
  type                     = "ingress"
  security_group_id        = aws_security_group.dataprofiler_kube_ng_sg.id
  from_port                = 0
  to_port                  = 65535
  protocol                 = "-1"
  source_security_group_id = aws_security_group.dataprofiler_lb_ingress_private.id
}

resource "aws_security_group_rule" "ingress_to_dataprofiler_lb_ingress_public" {
  type                     = "ingress"
  security_group_id        = aws_security_group.dataprofiler_kube_ng_sg.id
  from_port                = 0
  to_port                  = 65535
  protocol                 = "-1"
  source_security_group_id = aws_security_group.dataprofiler_lb_ingress_public_sg.id
}

resource "aws_security_group_rule" "ingress_with_self" {
  type                     = "ingress"
  security_group_id        = aws_security_group.dataprofiler_kube_ng_sg.id
  from_port                = 0
  to_port                  = 65535
  protocol                 = "all"
  source_security_group_id = aws_security_group.dataprofiler_kube_ng_sg.id
}


resource "aws_security_group" "dataprofiler_lb_ingress_public_sg" {
  name        = "dataprofiler-lb-ingress-public"
  description = "Dataprofiler SG - For load balancers only - Allows all HTTP/S traffic (for all users)"
  vpc_id      = var.vpc_id

  ingress {
    from_port        = 443
    to_port          = 443
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  ingress {
    from_port        = 80
    to_port          = 80
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(
    tomap({"Name" = "dataprofiler-lb-ingress-public"})
  , var.tags)

}


resource "aws_security_group" "dataprofiler_lb_ingress_private" {
  name        = "dataprofiler-lb-ingress-private"
  description = "Dataprofiler SG - For load balancers only - Allows all HTTP/S traffic (only from within kube cluster)"
  vpc_id      = var.vpc_id

  ingress {
    description      = ""
    from_port        = 443
    to_port          = 443
    protocol         = "tcp"
    cidr_blocks      = []
    ipv6_cidr_blocks = []
    security_groups  = [aws_security_group.dataprofiler_kube_ng_sg.id]
  }

  ingress {
    description      = ""
    from_port        = 80
    to_port          = 80
    protocol         = "tcp"
    cidr_blocks      = []
    ipv6_cidr_blocks = []
    security_groups  = [aws_security_group.dataprofiler_kube_ng_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(
    tomap({"Name" = "dataprofiler-lb-ingress-private"})
  , var.tags)

}
