package com.example.userservice.dto;

import java.util.Date;
import java.util.List;

import com.example.userservice.vo.ResponseOrder;

import lombok.Data;

@Data
public class UserDto {
    private String email;
    private String name;
    private String pwd;
    private String userId;
    private Date createdAt;
    private String encryptedPwd;

    private List<ResponseOrder> orders;
}
