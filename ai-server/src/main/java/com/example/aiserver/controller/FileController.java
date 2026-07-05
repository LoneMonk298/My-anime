package com.example.aiserver.controller;

import com.example.aiserver.common.ApiResult;
import com.example.aiserver.common.BusinessException;
import com.example.aiserver.entity.FileResource;
import com.example.aiserver.mapper.FileResourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FileController {
    private final FileResourceMapper fileResourceMapper;

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    @Value("${app.file.public-prefix}")
    private String publicPrefix;

    @PostMapping("/file/upload")
    public ApiResult<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam String businessType,
            @RequestParam(required = false) String businessId,
            @RequestParam(required = false) String businessField
    ) {
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }

        String storedName = UUID.randomUUID() + extension;
        String datePath = LocalDate.now().toString().replace("-", "/");
        Path targetDir = Path.of(uploadDir, datePath);
        Path targetFile = targetDir.resolve(storedName);

        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile);
        } catch (IOException ex) {
            throw new BusinessException("文件保存失败");
        }

        String normalizedBusinessId = StringUtils.hasText(businessId) ? businessId : UUID.randomUUID().toString();
        String normalizedBusinessField = StringUtils.hasText(businessField) ? businessField : businessType;
        String filePath = publicPrefix + "/" + datePath + "/" + storedName;
        String apiFileUrl = "/api" + filePath;
        FileResource resource = new FileResource();
        resource.setOriginalName(originalName);
        resource.setStoredName(storedName);
        resource.setFilePath(filePath);
        resource.setFileUrl(apiFileUrl);
        resource.setMimeType(file.getContentType());
        resource.setFileSize(file.getSize());
        resource.setBusinessType(businessType);
        resource.setBusinessId(normalizedBusinessId);
        resource.setBusinessField(normalizedBusinessField);
        fileResourceMapper.insert(resource);

        return ApiResult.success(Map.of(
                "id", resource.getId(),
                "filePath", filePath,
                "fileUrl", apiFileUrl,
                "url", apiFileUrl,
                "originalName", originalName
        ));
    }
}
