package com.example.terraformapi.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class InstanceRequest {
    
    @NotBlank(message = "인스턴스 이름은 필수입니다")
    private String instanceName;
    
    @NotBlank(message = "이미지 ID는 필수입니다")
    private String imageId;
    
    @NotBlank(message = "Flavor ID는 필수입니다")
    private String flavorId;
    
    @NotBlank(message = "네트워크 이름은 필수입니다")
    private String networkName;
    
    @NotBlank(message = "키 페어는 필수입니다")
    private String keyPair;
} 