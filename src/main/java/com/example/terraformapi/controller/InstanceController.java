package com.example.terraformapi.controller;

import com.example.terraformapi.dto.InstanceRequest;
import com.example.terraformapi.model.OpenStackInstance;
import com.example.terraformapi.service.InstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/instances")
@RequiredArgsConstructor
public class InstanceController {

    private final InstanceService instanceService;

    @GetMapping
    public ResponseEntity<List<OpenStackInstance>> getAllInstances() {
        return ResponseEntity.ok(instanceService.getAllInstances());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OpenStackInstance> getInstanceById(@PathVariable Long id) {
        return instanceService.getInstanceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<OpenStackInstance> createInstance(@Valid @RequestBody InstanceRequest request) {
        OpenStackInstance instance = instanceService.createInstance(request);
        return ResponseEntity.ok(instance);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInstance(@PathVariable Long id) {
        instanceService.deleteInstance(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/openstack/{serverId}")
    public ResponseEntity<String> deleteInstanceByServerId(@PathVariable String serverId) {
        try {
            instanceService.deleteInstanceByServerId(serverId);
            return ResponseEntity.ok("서버 " + serverId + " 삭제 완료");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("서버 삭제 실패: " + e.getMessage());
        }
    }
} 