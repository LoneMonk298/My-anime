package com.example.aiserver.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArticleStatusRequest {
    @NotNull
    @Min(0)
    @Max(2)
    private Integer status;
}
