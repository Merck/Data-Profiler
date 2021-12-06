variable "region" {
  description = "AWS Region"
  type        = string
  default     = "us-east-1"
}

variable "availability_zone" {
  description = "AWS Availability zone"
  type        = string
  default     = "us-east-1d"
}

variable "tags" {
  description = "Tags to apply to resources created by EC2 module"
  type        = map(string)
  default = {
  }
}