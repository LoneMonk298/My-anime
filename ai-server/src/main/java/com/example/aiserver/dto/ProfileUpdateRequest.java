package com.example.aiserver.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String username;
    private String nickname;
    private String email;
    private Integer age;
    private String gender;
    private String avatar;
}
