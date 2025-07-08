variable "instance_name" {
  description = "Name of the virtual machine"
  type        = string
}

variable "image_id" {
  description = "Image ID to use for the instance"
  type        = string
}

variable "flavor_id" {
  description = "Flavor ID for the instance"
  type        = string
}

variable "network_name" {
  description = "Network name to attach the instance to"
  type        = string
}

variable "key_pair" {
  description = "SSH key pair name"
  type        = string
}
