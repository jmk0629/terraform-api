package com.example.terraformapi.service;

import com.example.terraformapi.model.OpenStackInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class TerraformService {

    @Value("${terraform.cloud.api-url}")
    private String apiUrl;

    @Value("${terraform.cloud.organization}")
    private String organization;

    @Value("${terraform.cloud.workspace-id}")
    private String workspaceId;

    @Value("${terraform.cloud.token}")
    private String token;

    private final RestTemplate restTemplate = new RestTemplate();

    public String createConfigurationVersion() {
        String url = apiUrl + "/organizations/" + organization + "/workspaces/" + workspaceId + "/configuration-versions";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/vnd.api+json"));
        headers.setBearerAuth(token);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("data", Map.of(
            "type", "configuration-versions",
            "attributes", Map.of(
                "auto-queue-runs", false
            )
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            return (String) data.get("id");
        } catch (Exception e) {
            log.error("Configuration version 생성 실패", e);
            throw new RuntimeException("Terraform 설정 버전 생성 실패", e);
        }
    }

    public void uploadConfiguration(String configVersionId, String terraformConfig) {
        String url = apiUrl + "/configuration-versions/" + configVersionId + "/upload";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setBearerAuth(token);

        HttpEntity<byte[]> request = new HttpEntity<>(terraformConfig.getBytes(), headers);
        
        try {
            restTemplate.put(url, request);
        } catch (Exception e) {
            log.error("Terraform 설정 업로드 실패", e);
            throw new RuntimeException("Terraform 설정 업로드 실패", e);
        }
    }

    public String createRun(OpenStackInstance instance) {
        String url = apiUrl + "/runs";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/vnd.api+json"));
        headers.setBearerAuth(token);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("data", Map.of(
            "type", "runs",
            "attributes", Map.of(
                "message", "Create OpenStack instance: " + instance.getInstanceName(),
                "auto-apply", true
            ),
            "relationships", Map.of(
                "workspace", Map.of(
                    "data", Map.of(
                        "type", "workspaces",
                        "id", workspaceId
                    )
                )
            )
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            return (String) data.get("id");
        } catch (Exception e) {
            log.error("Terraform 실행 생성 실패", e);
            throw new RuntimeException("Terraform 실행 생성 실패", e);
        }
    }

    public String getRunStatus(String runId) {
        String url = apiUrl + "/runs/" + runId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<String> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            Map<String, Object> attributes = (Map<String, Object>) data.get("attributes");
            return (String) attributes.get("status");
        } catch (Exception e) {
            log.error("Terraform 실행 상태 조회 실패", e);
            throw new RuntimeException("Terraform 실행 상태 조회 실패", e);
        }
    }

    public void applyRun(String runId) {
        String url = apiUrl + "/runs/" + runId + "/actions/apply";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/vnd.api+json"));
        headers.setBearerAuth(token);

        HttpEntity<String> request = new HttpEntity<>(headers);
        
        try {
            restTemplate.postForEntity(url, request, Void.class);
        } catch (Exception e) {
            log.error("Terraform 실행 적용 실패", e);
            throw new RuntimeException("Terraform 실행 적용 실패", e);
        }
    }

    public void destroyRun(String runId) {
        String url = apiUrl + "/runs/" + runId + "/actions/discard";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/vnd.api+json"));
        headers.setBearerAuth(token);

        HttpEntity<String> request = new HttpEntity<>(headers);
        
        try {
            restTemplate.postForEntity(url, request, Void.class);
        } catch (Exception e) {
            log.error("Terraform 실행 취소 실패", e);
            throw new RuntimeException("Terraform 실행 취소 실패", e);
        }
    }

    /**
     * Terraform Cloud에서 destroy run을 생성합니다.
     */
    public String createDestroyRun(String serverId) {
        String url = apiUrl + "/runs";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/vnd.api+json"));
        headers.setBearerAuth(token);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("data", Map.of(
            "type", "runs",
            "attributes", Map.of(
                "message", "Destroy OpenStack instance: " + serverId,
                "is-destroy", true,
                "auto-apply", true
            ),
            "relationships", Map.of(
                "workspace", Map.of(
                    "data", Map.of(
                        "type", "workspaces",
                        "id", workspaceId
                    )
                )
            )
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            return (String) data.get("id");
        } catch (Exception e) {
            log.error("Terraform destroy 실행 생성 실패", e);
            throw new RuntimeException("Terraform destroy 실행 생성 실패", e);
        }
    }
} 