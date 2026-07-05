package com.example.aiserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_resource")
public class FileResource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String originalName;
    private String storedName;
    private String filePath;
    private String fileUrl;
    private String mimeType;
    private Long fileSize;
    private String businessType;
    private String businessId;
    private String businessField;
    private Long uploaderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
