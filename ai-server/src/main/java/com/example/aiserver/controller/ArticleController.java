package com.example.aiserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aiserver.common.ApiResult;
import com.example.aiserver.common.BusinessException;
import com.example.aiserver.dto.ArticleStatusRequest;
import com.example.aiserver.entity.Article;
import com.example.aiserver.entity.ArticleCategory;
import com.example.aiserver.entity.SysUser;
import com.example.aiserver.mapper.ArticleCategoryMapper;
import com.example.aiserver.mapper.ArticleMapper;
import com.example.aiserver.util.CurrentUserUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ArticleController {
    private static final Set<Integer> ARTICLE_STATUSES = Set.of(0, 1, 2);

    private final ArticleCategoryMapper categoryMapper;
    private final ArticleMapper articleMapper;
    private final CurrentUserUtil currentUserUtil;

    @GetMapping("/article/category/tree")
    public ApiResult<List<Map<String, Object>>> categoryTree(
            @RequestParam(defaultValue = "false") boolean includeDisabled
    ) {
        List<ArticleCategory> categories = categoryMapper.selectList(new LambdaQueryWrapper<ArticleCategory>()
                .eq(!includeDisabled, ArticleCategory::getStatus, 1)
                .orderByAsc(ArticleCategory::getParentId)
                .orderByAsc(ArticleCategory::getSortOrder)
                .orderByAsc(ArticleCategory::getId));
        return ApiResult.success(categories.stream().map(this::toCategoryMap).collect(Collectors.toList()));
    }

    @PostMapping("/article/category")
    public ApiResult<ArticleCategory> addCategory(@RequestBody ArticleCategory category) {
        normalizeCategory(category);
        categoryMapper.insert(category);
        return ApiResult.success(category);
    }

    @PutMapping("/article/category/{id}")
    public ApiResult<ArticleCategory> updateCategory(@PathVariable Long id, @RequestBody ArticleCategory category) {
        ArticleCategory existing = requireCategory(id);
        normalizeCategory(category);
        category.setId(existing.getId());
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(category);
        return ApiResult.success(categoryMapper.selectById(id));
    }

    @DeleteMapping("/article/category/{id}")
    public ApiResult<Void> deleteCategory(@PathVariable Long id) {
        requireCategory(id);
        Long articleCount = articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getCategoryId, id));
        if (articleCount != null && articleCount > 0) {
            throw new BusinessException("该分类下还有文章，不能删除");
        }
        categoryMapper.deleteById(id);
        return ApiResult.success();
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
        long safeCurrentPage = Math.max(1, currentPage);
        long safeSize = Math.min(Math.max(1, size), 100);
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

        Page<Article> page = articleMapper.selectPage(new Page<>(safeCurrentPage, safeSize), wrapper);
        return ApiResult.success(page);
    }

    @PostMapping("/article")
    public ApiResult<Article> addArticle(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody Article article
    ) {
        prepareArticleForSave(article, currentUserUtil.requireUser(authorization), null);
        articleMapper.insert(article);
        return ApiResult.success(article);
    }

    @GetMapping("/article/{id}")
    public ApiResult<Article> articleDetail(@PathVariable Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        return ApiResult.success(article);
    }

    @GetMapping("/article/{id}/view")
    public ApiResult<Map<String, Object>> publicArticleView(@PathVariable Long id) {
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getId, id)
                .eq(Article::getStatus, 1));
        if (article == null) {
            throw new BusinessException("文章不存在或尚未发布");
        }

        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, id)
                .setSql("read_count = read_count + 1"));
        Article refreshed = articleMapper.selectById(id);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("article", refreshed);
        data.put("previous", findNeighbor(refreshed, true));
        data.put("next", findNeighbor(refreshed, false));
        return ApiResult.success(data);
    }

    @PutMapping("/article/{id}")
    public ApiResult<Article> updateArticle(
            @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody Article article
    ) {
        Article existing = requireArticle(id);
        prepareArticleForSave(article, currentUserUtil.requireUser(authorization), existing);
        article.setId(id);
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(article);
        return ApiResult.success(articleMapper.selectById(id));
    }

    @PutMapping("/article/{id}/status")
    public ApiResult<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody ArticleStatusRequest request) {
        Article existing = requireArticle(id);
        validateStatus(request.getStatus());
        Article article = new Article();
        article.setId(id);
        article.setStatus(request.getStatus());
        article.setUpdatedAt(LocalDateTime.now());
        if (request.getStatus() == 1 && existing.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        articleMapper.updateById(article);
        return ApiResult.success();
    }

    @DeleteMapping("/article/{id}")
    public ApiResult<Void> deleteArticle(@PathVariable Long id) {
        requireArticle(id);
        articleMapper.deleteById(id);
        return ApiResult.success();
    }

    private void normalizeCategory(ArticleCategory category) {
        if (!StringUtils.hasText(category.getName())) {
            throw new BusinessException("分类名称不能为空");
        }
        if (!StringUtils.hasText(category.getCode())) {
            category.setCode("category-" + System.currentTimeMillis());
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
    }

    private void prepareArticleForSave(Article article, SysUser user, Article existing) {
        if (!StringUtils.hasText(article.getTitle())) {
            throw new BusinessException("文章标题不能为空");
        }
        if (article.getTitle().length() > 200) {
            throw new BusinessException("文章标题最多 200 个字符");
        }
        if (article.getCategoryId() == null) {
            throw new BusinessException("请选择文章分类");
        }
        ArticleCategory category = requireCategory(article.getCategoryId());
        if (category.getStatus() == null || category.getStatus() != 1) {
            throw new BusinessException("文章分类已停用，请重新选择");
        }
        if (!StringUtils.hasText(article.getContent())) {
            throw new BusinessException("文章内容不能为空");
        }
        if (article.getStatus() == null) {
            article.setStatus(0);
        }
        validateStatus(article.getStatus());
        if (article.getReadCount() == null) {
            article.setReadCount(existing == null ? 0 : existing.getReadCount());
        }
        if (!StringUtils.hasText(article.getAuthorName())) {
            article.setAuthorName(StringUtils.hasText(user.getUsername()) ? user.getUsername() : "ZG");
        }
        article.setAuthorId(user.getId());
        if (article.getStatus() == 1 && (existing == null || existing.getPublishedAt() == null)) {
            article.setPublishedAt(LocalDateTime.now());
        } else if (existing != null) {
            article.setPublishedAt(existing.getPublishedAt());
        }
    }

    private void validateStatus(Integer status) {
        if (status == null || !ARTICLE_STATUSES.contains(status)) {
            throw new BusinessException("文章状态不正确");
        }
    }

    private Article requireArticle(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        return article;
    }

    private ArticleCategory requireCategory(Long id) {
        ArticleCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        return category;
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

    private Article findNeighbor(Article article, boolean previous) {
        LocalDateTime publishedAt = article.getPublishedAt();
        if (publishedAt == null) {
            return null;
        }

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getTitle, Article::getCategoryId,
                        Article::getCoverImg, Article::getPublishedAt, Article::getAuthorName)
                .eq(Article::getStatus, 1)
                .ne(Article::getId, article.getId());

        if (previous) {
            wrapper.lt(Article::getPublishedAt, publishedAt)
                    .orderByDesc(Article::getPublishedAt);
        } else {
            wrapper.gt(Article::getPublishedAt, publishedAt)
                    .orderByAsc(Article::getPublishedAt);
        }

        wrapper.last("LIMIT 1");
        return articleMapper.selectOne(wrapper);
    }
}
