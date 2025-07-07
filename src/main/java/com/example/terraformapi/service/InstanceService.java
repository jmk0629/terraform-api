package com.example.terraformapi.service;

import com.example.terraformapi.dto.InstanceRequest;
import com.example.terraformapi.model.OpenStackInstance;
import com.example.terraformapi.repository.InstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
public class InstanceService {

    private static final Logger log = LoggerFactory.getLogger(InstanceService.class);
    
    private final InstanceRepository instanceRepository;
    private final TerraformService terraformService;

    public InstanceService(InstanceRepository instanceRepository, TerraformService terraformService) {
        this.instanceRepository = instanceRepository;
        this.terraformService = terraformService;
    }

    public List<OpenStackInstance> getAllInstances() {
        return instanceRepository.findAll();
    }

    public Optional<OpenStackInstance> getInstanceById(Long id) {
        return instanceRepository.findById(id);
    }

    @Transactional
    public OpenStackInstance createInstance(InstanceRequest request) {
        OpenStackInstance instance = new OpenStackInstance();
        instance.setInstanceName(request.getInstanceName());
        instance.setImageId(request.getImageId());
        instance.setFlavorId(request.getFlavorId());
        instance.setNetworkName(request.getNetworkName());
        instance.setKeyPair(request.getKeyPair());
        instance.setStatus(OpenStackInstance.InstanceStatus.PENDING);

        // DB에 저장
        instance = instanceRepository.save(instance);

        boolean gitSuccess = true;
        // 동적으로 tfvars 파일 생성 및 git push
        try {
            updateTfvarsAndPush(instance);
            log.info("Git push 완료, Terraform Cloud 동기화 대기 중...");
            Thread.sleep(3000); // 3초 대기 (더 긴 대기시간)
        } catch (Exception e) {
            gitSuccess = false;
            log.error("terraform.tfvars git 반영 실패, git 없이 바로 API 실행", e);
        }

        try {
            // git 성공 또는 실패(즉시 API) 모두 여기서 Run 생성
            String runId = terraformService.createRun(instance);
            instance.setTerraformRunId(runId);
            instance.setStatus(OpenStackInstance.InstanceStatus.CREATING);
            instanceRepository.save(instance);
            log.info("인스턴스 생성 시작: {}", instance.getInstanceName());
        } catch (Exception e) {
            instance.setStatus(OpenStackInstance.InstanceStatus.ERROR);
            instanceRepository.save(instance);
            log.error("인스턴스 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("인스턴스 생성 실패", e);
        }

        return instance;
    }

    @Transactional
    public void deleteInstance(Long id) {
        Optional<OpenStackInstance> instanceOpt = instanceRepository.findById(id);
        if (instanceOpt.isPresent()) {
            OpenStackInstance instance = instanceOpt.get();
            
            if (instance.getTerraformRunId() != null) {
                try {
                    // Terraform에서 리소스 삭제
                    terraformService.destroyRun(instance.getTerraformRunId());
                    instance.setStatus(OpenStackInstance.InstanceStatus.DELETING);
                    instanceRepository.save(instance);
                    
                    log.info("인스턴스 삭제 시작: {}", instance.getInstanceName());
                } catch (Exception e) {
                    log.error("인스턴스 삭제 실패: {}", e.getMessage(), e);
                    throw new RuntimeException("인스턴스 삭제 실패", e);
                }
            } else {
                instanceRepository.delete(instance);
            }
        }
    }

    @Transactional
    public void updateInstanceStatus(Long id, OpenStackInstance.InstanceStatus status) {
        Optional<OpenStackInstance> instanceOpt = instanceRepository.findById(id);
        if (instanceOpt.isPresent()) {
            OpenStackInstance instance = instanceOpt.get();
            instance.setStatus(status);
            instanceRepository.save(instance);
        }
    }

    private String generateTerraformConfig(OpenStackInstance instance) {
        return """
            terraform {
              required_providers {
                openstack = {
                  source = \"terraform-provider-openstack/openstack\"
                  version = \"~> 1.54.1\"
                }
              }
            }
            
            provider \"openstack\" {
              # auth_url    = \"http://192.168.33.8:5000/v3\"  # 기존 값 주석 처리
              auth_url    = \"http://mstsc.3hs.co.kr:50000/v3\"
              region      = \"RegionOne\"
              domain_name = \"default\"
              tenant_id   = \"656472d18ce84b95b16ee41bc6a36aac\"
              user_name   = \"admin\"
              password    = \"3hspassw0rd\"
            }
            
            resource \"openstack_compute_instance_v2\" \"test_vm\" {
              name            = \"%s\"
              image_id        = \"%s\"
              flavor_id       = \"%s\"
              key_pair        = \"%s\"
              security_groups = [\"default\"]
            
              network {
                name = \"%s\"
              }
            }
            
            output \"instance_id\" {
              value = openstack_compute_instance_v2.test_vm.id
            }
            
            output \"instance_name\" {
              value = openstack_compute_instance_v2.test_vm.name
            }
            """.formatted(
                instance.getInstanceName(),
                instance.getImageId(),
                instance.getFlavorId(),
                instance.getKeyPair(),
                instance.getNetworkName()
            );
    }

    /**
     * OpenStackInstance 정보를 기반으로 terraform.tfvars 파일을 GitHub 저장소에 생성/커밋/푸시합니다.
     */
    private void updateTfvarsAndPush(OpenStackInstance instance) throws Exception {
        String tfvarsContent = String.format(
                "instance_name = \"%s\"\n" +
                "image_id = \"%s\"\n" +
                "flavor_id = \"%s\"\n" +
                "network_name = \"%s\"\n" +
                "key_pair = \"%s\"\n",
                instance.getInstanceName(),
                instance.getImageId(),
                instance.getFlavorId(),
                instance.getNetworkName(),
                instance.getKeyPair()
        );
        String repoPath = "D:/terraformAPI/test-openstack";
        Path tfvarsPath = Paths.get(repoPath, "terraform.tfvars");
        Files.write(tfvarsPath, tfvarsContent.getBytes());

        // git add/commit/push가 모두 끝난 후에만 다음 단계로 진행
        runCommand(repoPath, "git", "add", "terraform.tfvars");
        runCommand(repoPath, "git", "commit", "-m", "자동 생성: 사용자 입력값 반영");
        runCommand(repoPath, "git", "push", "origin", "main");
        log.info("terraform.tfvars 파일이 GitHub에 반영되었습니다. (push 완료 후 다음 단계 진행)");
    }

    private void runCommand(String workingDir, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new java.io.File(workingDir));
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("명령 실행 실패: " + String.join(" ", command));
        }
    }

    /**
     * OpenStack 서버 ID를 사용하여 Terraform API로 서버를 삭제합니다.
     */
    public void deleteInstanceByServerId(String serverId) {
        log.info("Terraform API를 통한 서버 삭제 시작: {}", serverId);
        
        try {
            // Terraform Cloud API를 통해 destroy run 생성
            String destroyRunId = terraformService.createDestroyRun(serverId);
            log.info("Terraform destroy run 생성 완료: {}", destroyRunId);
            
            // DB에서 해당 서버 정보도 삭제 (instanceId가 일치하는 경우)
            List<OpenStackInstance> instances = instanceRepository.findAll();
            for (OpenStackInstance instance : instances) {
                if (serverId.equals(instance.getInstanceId())) {
                    instance.setStatus(OpenStackInstance.InstanceStatus.DELETING);
                    instanceRepository.save(instance);
                    log.info("DB에서 인스턴스 상태를 DELETING으로 변경: {}", instance.getInstanceName());
                    break;
                }
            }
            
            log.info("Terraform API를 통한 서버 삭제 요청 완료: {}", serverId);
            
        } catch (Exception e) {
            log.error("Terraform API를 통한 서버 삭제 실패: {}", serverId, e);
            throw new RuntimeException("Terraform API를 통한 서버 삭제 실패: " + e.getMessage(), e);
        }
    }


} 