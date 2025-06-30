package com.example.terraformapi.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "openstack_instances")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenStackInstance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String instanceName;
    
    @Column(nullable = false)
    private String imageId;
    
    @Column(nullable = false)
    private String flavorId;
    
    @Column(nullable = false)
    private String networkName;
    
    @Column(nullable = false)
    private String keyPair;
    
    @Enumerated(EnumType.STRING)
    private InstanceStatus status = InstanceStatus.PENDING;
    
    private String terraformRunId;
    
    private String instanceId;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum InstanceStatus {
        PENDING, CREATING, RUNNING, STOPPED, DELETING, ERROR
    }
} 