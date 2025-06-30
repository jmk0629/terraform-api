package com.example.terraformapi.repository;

import com.example.terraformapi.model.OpenStackInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstanceRepository extends JpaRepository<OpenStackInstance, Long> {
} 