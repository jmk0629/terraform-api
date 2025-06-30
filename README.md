# Terraform API - OpenStack VM 관리 시스템

Spring Boot를 활용하여 Terraform Cloud API를 통해 OpenStack 가상서버를 생성, 수정, 삭제할 수 있는 웹 애플리케이션입니다.

## 🚀 주요 기능

- **가상서버 생성**: 웹 인터페이스를 통해 OpenStack 가상서버 생성
- **가상서버 관리**: 생성된 서버 목록 조회 및 상태 확인
- **가상서버 삭제**: 실행 중인 서버 삭제
- **실시간 상태 업데이트**: 30초마다 자동 새로고침

## 🛠 기술 스택

- **Backend**: Spring Boot 3.2.0, Java 17
- **Frontend**: Thymeleaf, Bootstrap 5, JavaScript
- **Database**: H2 Database (In-Memory)
- **Infrastructure**: Terraform Cloud API, OpenStack

## 📋 사전 요구사항

1. **Terraform Cloud 계정 및 토큰**
   - Terraform Cloud에서 API 토큰 생성
   - Organization: `3hs-openstack`
   - Workspace ID: `prj-2NDi4xHWXQ2ms2pz`

2. **OpenStack 접근 정보**
   - OpenStack 서버: `192.168.33.8`
   - 계정: `admin`
   - 비밀번호: ``

## ⚙️ 설정

### 1. 환경 변수 설정

Terraform Cloud API 토큰을 환경 변수로 설정:

```bash
# Windows
set TF_TOKEN=your-terraform-token-here

# Linux/Mac
export TF_TOKEN=your-terraform-token-here
```

### 2. application.yml 설정

`src/main/resources/application.yml` 파일에서 다음 설정을 확인/수정:

```yaml
terraform:
  cloud:
    api-url: https://app.terraform.io/api/v2
    organization: your-organization-name
    workspace-id: your-workspace-id
    token: ${TF_TOKEN:your-terraform-token-here}

openstack:
  auth-url: http://your-openstack-server:5000/v3
  region: RegionOne
  domain-name: default
  tenant-id: your-tenant-id
  username: your-username
  password: ${OPENSTACK_PASSWORD:your-password}
```

## 🚀 실행 방법

### 1. 프로젝트 빌드

```bash
mvn clean install
```

### 2. 애플리케이션 실행

```bash
mvn spring-boot:run
```

### 3. 웹 접속

브라우저에서 `http://localhost:8081` 접속

## 📖 사용법

### 1. 서버 목록 조회
- 메인 페이지에서 생성된 모든 서버 목록을 확인할 수 있습니다.
- 서버 상태는 실시간으로 업데이트됩니다.

### 2. 새 서버 생성
1. "새 서버 생성" 버튼 클릭
2. 서버 정보 입력:
   - **서버명**: 생성할 서버의 이름
   - **이미지 ID**: 사용할 이미지 ID (기본값: Ubuntu 20.04)
   - **Flavor ID**: 서버 사양 ID (기본값: m1.small)
   - **네트워크명**: 연결할 네트워크명 (기본값: test)
   - **키 페어**: SSH 접속용 키 페어 (기본값: test)
3. "서버 생성" 버튼 클릭

### 3. 서버 삭제
- 실행 중인 서버의 "삭제" 버튼을 클릭하여 서버를 삭제할 수 있습니다.

## 🔧 API 엔드포인트

### REST API

- `GET /api/instances` - 모든 인스턴스 조회
- `GET /api/instances/{id}` - 특정 인스턴스 조회
- `POST /api/instances` - 새 인스턴스 생성
- `DELETE /api/instances/{id}` - 인스턴스 삭제

### 웹 페이지

- `GET /` - 메인 페이지 (서버 목록)
- `GET /create` - 서버 생성 페이지

## 📁 프로젝트 구조

```
src/
├── main/
│   ├── java/com/example/terraformapi/
│   │   ├── controller/
│   │   │   ├── InstanceController.java    # REST API 컨트롤러
│   │   │   └── WebController.java         # 웹 페이지 컨트롤러
│   │   ├── dto/
│   │   │   └── InstanceRequest.java       # 요청 DTO
│   │   ├── model/
│   │   │   └── OpenStackInstance.java     # 엔티티 모델
│   │   ├── repository/
│   │   │   └── InstanceRepository.java    # 데이터 접근 계층
│   │   ├── service/
│   │   │   ├── InstanceService.java       # 비즈니스 로직
│   │   │   └── TerraformService.java      # Terraform API 통신
│   │   └── TerraformApiApplication.java   # 메인 애플리케이션
│   └── resources/
│       ├── application.yml                # 설정 파일
│       └── templates/
│           ├── index.html                 # 메인 페이지
│           └── create.html                # 생성 페이지
```

## 🔍 Terraform Cloud API 활용 방법

### 1. Configuration Version 생성
```java
String configVersionId = terraformService.createConfigurationVersion();
```

### 2. Terraform 설정 업로드
```java
terraformService.uploadConfiguration(configVersionId, terraformConfig);
```

### 3. Run 생성 및 실행
```java
String runId = terraformService.createRun(instance);
```

### 4. 실행 상태 확인
```java
String status = terraformService.getRunStatus(runId);
```

## ⚠️ 주의사항

1. **Terraform Cloud 토큰**: 반드시 유효한 API 토큰을 설정해야 합니다.
2. **OpenStack 연결**: OpenStack 서버가 정상적으로 동작하고 있어야 합니다.
3. **네트워크 설정**: OpenStack에서 사용할 네트워크와 키 페어가 미리 생성되어 있어야 합니다.
4. **리소스 제한**: OpenStack 프로젝트의 리소스 제한을 확인하세요.

## 🐛 문제 해결

### 일반적인 오류

1. **Terraform API 연결 실패**
   - API 토큰이 올바른지 확인
   - Organization과 Workspace ID가 정확한지 확인

2. **OpenStack 연결 실패**
   - OpenStack 서버가 정상 동작하는지 확인
   - 인증 정보가 올바른지 확인

3. **서버 생성 실패**
   - 이미지 ID, Flavor ID, 네트워크명이 유효한지 확인
   - OpenStack 프로젝트의 리소스 제한 확인

## 📞 지원

문제가 발생하거나 추가 기능이 필요한 경우, 이슈를 등록해 주세요. 
