package com.example.aiserver.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetCodeRequest {
    @Email
    @NotBlank
    private String email;
}
