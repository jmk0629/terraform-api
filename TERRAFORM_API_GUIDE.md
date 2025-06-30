# Terraform Cloud API를 활용한 tf 파일 업로드 가이드

## 📋 개요

Terraform Cloud API를 통해 tf 파일을 업로드하고 OpenStack 가상서버를 관리하는 방법을 설명합니다.

## 🔑 사전 준비

### 1. Terraform Cloud API 토큰 생성

1. Terraform Cloud에 로그인
2. User Settings → Tokens → Create API token
3. 토큰 생성 후 안전한 곳에 보관

### 2. Organization 및 Workspace 정보 확인

- **Organization**: `3hs-openstack`
- **Workspace ID**: `prj-2NDi4xHWXQ2ms2pz`

## 🚀 API 활용 방법

### 1. Configuration Version 생성

먼저 Terraform 설정을 업로드할 Configuration Version을 생성합니다.

```bash
curl -X POST \
  https://app.terraform.io/api/v2/workspaces/YOUR_WORKSPACE_ID/configuration-versions \
  -H "Authorization: Bearer YOUR_TERRAFORM_TOKEN" \
  -H "Content-Type: application/vnd.api+json" \
  -d '{
    "data": {
      "type": "configuration-versions",
      "attributes": {
        "auto-queue-runs": false
      }
    }
  }'
```

응답 예시:
```json
{
  "data": {
    "id": "cv-xxxxxxxxx",
    "type": "configuration-versions",
    "attributes": {
      "upload-url": "https://archivist.terraform.io/v1/object/..."
    }
  }
}
```

### 2. Terraform 설정 파일 업로드

생성된 `upload-url`을 사용하여 tf 파일을 업로드합니다.

```bash
curl -X PUT \
  "UPLOAD_URL_FROM_PREVIOUS_RESPONSE" \
  -H "Content-Type: application/octet-stream" \
  --data-binary @main.tf
```

### 3. Run 생성

Configuration Version이 업로드되면 Run을 생성합니다.

```bash
curl -X POST \
  https://app.terraform.io/api/v2/runs \
  -H "Authorization: Bearer YOUR_TERRAFORM_TOKEN" \
  -H "Content-Type: application/vnd.api+json" \
  -d '{
    "data": {
      "type": "runs",
      "attributes": {
        "message": "Create OpenStack instance",
        "auto-apply": true
      },
      "relationships": {
        "workspace": {
          "data": {
            "type": "workspaces",
            "id": "prj-2NDi4xHWXQ2ms2pz"
          }
        }
      }
    }
  }'
```

### 4. Run 상태 확인

Run의 진행 상황을 확인합니다.

```bash
curl -X GET \
  https://app.terraform.io/api/v2/runs/RUN_ID \
  -H "Authorization: Bearer YOUR_TERRAFORM_TOKEN"
```

## 📁 tf 파일 예시

### 기본 OpenStack 인스턴스 생성

```hcl
terraform {
  required_providers {
    openstack = {
      source = "terraform-provider-openstack/openstack"
      version = "~> 1.54.1"
    }
  }
}

provider "openstack" {
  auth_url    = "http://192.168.33.8:5000/v3"
  region      = "RegionOne"
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

variable "instance_name" {
  description = "Name of the instance"
  type        = string
  default     = "terraform-vm"
}

variable "image_id" {
  description = "ID of the image"
  type        = string
  default     = "8ed84cb4-0533-4efc-a4bc-5bbd3dc2a787"
}

variable "flavor_id" {
  description = "ID of the flavor"
  type        = string
  default     = "ddbc4ef2-4575-44d7-92fc-c7afc41ed4f9"
}

variable "network_name" {
  description = "Name of the network"
  type        = string
  default     = "test"
}

variable "key_pair" {
  description = "Name of the key pair"
  type        = string
  default     = "test"
}

output "instance_id" {
  value = openstack_compute_instance_v2.test_vm.id
}

output "instance_name" {
  value = openstack_compute_instance_v2.test_vm.name
}
```

## 🔧 Spring Boot에서의 구현

### TerraformService 클래스

```java
@Service
public class TerraformService {
    
    @Value("${terraform.cloud.api-url}")
    private String apiUrl;
    
    @Value("${terraform.cloud.workspace-id}")
    private String workspaceId;
    
    @Value("${terraform.cloud.token}")
    private String token;
    
    public String createConfigurationVersion() {
        String url = apiUrl + "/workspaces/" + workspaceId + "/configuration-versions";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("data", Map.of(
            "type", "configuration-versions",
            "attributes", Map.of(
                "auto-queue-runs", false
            )
        ));
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return (String) data.get("id");
    }
    
    public void uploadConfiguration(String configVersionId, String terraformConfig) {
        String url = apiUrl + "/configuration-versions/" + configVersionId + "/upload";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setBearerAuth(token);
        
        HttpEntity<byte[]> request = new HttpEntity<>(terraformConfig.getBytes(), headers);
        restTemplate.put(url, request);
    }
    
    public String createRun(OpenStackInstance instance) {
        String url = apiUrl + "/runs";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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
        
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return (String) data.get("id");
    }
}
```

## 📊 Run 상태 값

Terraform Cloud Run의 상태는 다음과 같습니다:

- `pending` - 대기 중
- `planning` - 계획 수립 중
- `planned` - 계획 완료
- `applying` - 적용 중
- `applied` - 적용 완료
- `errored` - 오류 발생
- `canceled` - 취소됨
- `discarded` - 폐기됨

## 🔍 상태 모니터링

### Run 상태 확인

```java
public String getRunStatus(String runId) {
    String url = apiUrl + "/runs/" + runId;
    
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    
    HttpEntity<String> request = new HttpEntity<>(headers);
    
    ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    Map<String, Object> attributes = (Map<String, Object>) data.get("attributes");
    return (String) attributes.get("status");
}
```

### 주기적 상태 확인

```java
@Scheduled(fixedDelay = 30000) // 30초마다
public void checkRunStatus() {
    List<OpenStackInstance> instances = instanceRepository.findByStatus(InstanceStatus.CREATING);
    
    for (OpenStackInstance instance : instances) {
        String status = terraformService.getRunStatus(instance.getTerraformRunId());
        
        switch (status) {
            case "applied":
                instance.setStatus(InstanceStatus.RUNNING);
                break;
            case "errored":
                instance.setStatus(InstanceStatus.ERROR);
                break;
            case "canceled":
            case "discarded":
                instance.setStatus(InstanceStatus.ERROR);
                break;
        }
        
        instanceRepository.save(instance);
    }
}
```

## ⚠️ 주의사항

1. **API 토큰 보안**: API 토큰을 소스 코드에 하드코딩하지 마세요.
2. **Rate Limiting**: Terraform Cloud API는 요청 제한이 있습니다.
3. **에러 처리**: 네트워크 오류나 API 오류에 대한 적절한 처리가 필요합니다.
4. **상태 동기화**: Run 상태와 로컬 데이터베이스 상태를 동기화해야 합니다.

## 🔗 참고 자료

- [Terraform Cloud API Documentation](https://www.terraform.io/cloud-docs/api-docs)
- [OpenStack Provider Documentation](https://registry.terraform.io/providers/terraform-provider-openstack/openstack/latest/docs)
- [Spring Boot REST API Guide](https://spring.io/guides/gs/rest-service/) 