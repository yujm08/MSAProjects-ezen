package com.example.userservice.dto;

import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class UserDto {
    private String email;
    private String name;
    private String pwd;
    private String userId;
    private Date createdAt;
    private String encryptedPwd;

    // 추가: 역할 정보 (예: ["ROLE_USER", "ROLE_ADMIN"])
    private List<String> roles;
}
