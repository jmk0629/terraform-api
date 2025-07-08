terraform {
  required_providers {
    openstack = {
      source  = "terraform-provider-openstack/openstack"
      version = ">= 1.0.0"
    }
  }
}



provider "openstack" {
  #auth_url    = "http://192.168.33.8:5000/v3"
  auth_url    = "http://mstsc.3hs.co.kr:50000/v3"
  region      = "regionOne"
  domain_name = "default"
  tenant_id   = "656472d18ce84b95b16ee41bc6a36aac"
  user_name   = "admin"
  password    = "3hspassw0rd"
}

resource "openstack_compute_instance_v2" "test_vm" {
  name            = var.instance_name
  image_id        = var.image_id
  flavor_id       = var.flavor_id
  key_pair        = var.key_pair
  security_groups = ["default"]

  network {
    name = var.network_name
  }
}
