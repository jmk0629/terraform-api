output "instance_id" {
  value = openstack_compute_instance_v2.test_vm.id
}

output "instance_name" {
  value = openstack_compute_instance_v2.test_vm.name
}
