package com.example.aiserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aiserver.common.ApiResult;
import com.example.aiserver.dto.ArticleStatusRequest;
import com.example.aiserver.entity.Article;
import com.example.aiserver.entity.ArticleCategory;
import com.example.aiserver.mapper.ArticleCategoryMapper;
import com.example.aiserver.mapper.ArticleMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ArticleController {
    private final ArticleCategoryMapper categoryMapper;
    private final ArticleMapper articleMapper;

    @GetMapping("/article/category/tree")
    public ApiResult<List<Map<String, Object>>> categoryTree() {
        List<ArticleCategory> categories = categoryMapper.selectList(new LambdaQueryWrapper<ArticleCategory>()
                .eq(ArticleCategory::getStatus, 1)
                .orderByAsc(ArticleCategory::getParentId)
                .orderByAsc(ArticleCategory::getSortOrder));
        return ApiResult.success(categories.stream().map(this::toCategoryMap).collect(Collectors.toList()));
    }

    @GetMapping("/article/page")
    public ApiResult<Page<Article>> articlePage(
            @RequestParam(defaultValue = "1") long currentPage,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortDirection
    ) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .like(StringUtils.hasText(title), Article::getTitle, title)
                .eq(categoryId != null, Article::getCategoryId, categoryId)
                .eq(status != null, Article::getStatus, status);
        if ("readCount".equals(sortField)) {
            wrapper.orderBy(true, "asc".equalsIgnoreCase(sortDirection), Article::getReadCount);
        } else if ("publishedAt".equals(sortField)) {
            wrapper.orderBy(true, "asc".equalsIgnoreCase(sortDirection), Article::getPublishedAt);
        } else {
            wrapper.orderByDesc(Article::getUpdatedAt);
        }

        Page<Article> page = articleMapper.selectPage(new Page<>(currentPage, size), wrapper);
        return ApiResult.success(page);
    }

    @PostMapping("/article")
    public ApiResult<Article> addArticle(@RequestBody Article article) {
        if (!StringUtils.hasText(article.getAuthorName())) {
            article.setAuthorId(1L);
            article.setAuthorName("System Administrator");
        }
        if (article.getStatus() == null) {
            article.setStatus(0);
        }
        if (article.getReadCount() == null) {
            article.setReadCount(0);
        }
        articleMapper.insert(article);
        return ApiResult.success(article);
    }

    @GetMapping("/article/{id}")
    public ApiResult<Article> articleDetail(@PathVariable Long id) {
        return ApiResult.success(articleMapper.selectById(id));
    }

    @PutMapping("/article/{id}")
    public ApiResult<Article> updateArticle(@PathVariable Long id, @RequestBody Article article) {
        article.setId(id);
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(article);
        return ApiResult.success(articleMapper.selectById(id));
    }

    @PutMapping("/article/{id}/status")
    public ApiResult<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody ArticleStatusRequest request) {
        Article article = new Article();
        article.setId(id);
        article.setStatus(request.getStatus());
        if (request.getStatus() == 1) {
            article.setPublishedAt(LocalDateTime.now());
        }
        articleMapper.updateById(article);
        return ApiResult.success();
    }

    @DeleteMapping("/article/{id}")
    public ApiResult<Void> deleteArticle(@PathVariable Long id) {
        articleMapper.deleteById(id);
        return ApiResult.success();
    }

    private Map<String, Object> toCategoryMap(ArticleCategory category) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", category.getId());
        data.put("parentId", category.getParentId());
        data.put("name", category.getName());
        data.put("categoryName", category.getName());
        data.put("code", category.getCode());
        data.put("description", category.getDescription());
        data.put("sortOrder", category.getSortOrder());
        data.put("status", category.getStatus());
        data.put("createdAt", category.getCreatedAt());
        data.put("updatedAt", category.getUpdatedAt());
        data.put("deleted", category.getDeleted());
        return data;
    }
}
