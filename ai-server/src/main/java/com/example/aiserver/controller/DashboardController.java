package com.example.aiserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiserver.common.ApiResult;
import com.example.aiserver.entity.Article;
import com.example.aiserver.entity.FileResource;
import com.example.aiserver.entity.FriendLink;
import com.example.aiserver.entity.SysUser;
import com.example.aiserver.mapper.ArticleMapper;
import com.example.aiserver.mapper.FileResourceMapper;
import com.example.aiserver.mapper.FriendLinkMapper;
import com.example.aiserver.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DashboardController {
    private final ArticleMapper articleMapper;
    private final FriendLinkMapper friendLinkMapper;
    private final FileResourceMapper fileResourceMapper;
    private final SysUserMapper userMapper;

    @GetMapping("/dashboard/summary")
    public ApiResult<Map<String, Object>> summary() {
        Long articleCount = articleMapper.selectCount(new LambdaQueryWrapper<>());
        Long publishedCount = articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1));
        Long draftCount = articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 0));
        Long pendingFriendLinkCount = friendLinkMapper.selectCount(new LambdaQueryWrapper<FriendLink>()
                .eq(FriendLink::getStatus, 0));
        Long resourceCount = fileResourceMapper.selectCount(new LambdaQueryWrapper<FileResource>()
                .eq(FileResource::getBusinessType, "RESOURCE"));
        Long userCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>());
        Long totalReadCount = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                        .select(Article::getReadCount))
                .stream()
                .mapToLong(article -> article.getReadCount() == null ? 0 : article.getReadCount())
                .sum();

        List<Map<String, Object>> recentPublished = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, 1)
                        .orderByDesc(Article::getPublishedAt)
                        .orderByDesc(Article::getUpdatedAt)
                        .last("LIMIT 6"))
                .stream()
                .map(this::toRecentArticle)
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("articleCount", safeCount(articleCount));
        data.put("publishedCount", safeCount(publishedCount));
        data.put("draftCount", safeCount(draftCount));
        data.put("pendingFriendLinkCount", safeCount(pendingFriendLinkCount));
        data.put("resourceCount", safeCount(resourceCount));
        data.put("userCount", safeCount(userCount));
        data.put("totalReadCount", totalReadCount);
        data.put("recentPublished", recentPublished);
        return ApiResult.success(data);
    }

    private long safeCount(Long count) {
        return count == null ? 0 : count;
    }

    private Map<String, Object> toRecentArticle(Article article) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", article.getId());
        item.put("title", article.getTitle());
        item.put("authorName", article.getAuthorName());
        item.put("readCount", article.getReadCount() == null ? 0 : article.getReadCount());
        item.put("publishedAt", article.getPublishedAt());
        item.put("updatedAt", article.getUpdatedAt());
        return item;
    }
}
