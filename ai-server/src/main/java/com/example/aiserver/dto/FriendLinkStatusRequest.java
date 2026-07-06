package com.example.aiserver.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FriendLinkStatusRequest {
    @NotNull(message = "状态不能为空")
    private Integer status;
}
