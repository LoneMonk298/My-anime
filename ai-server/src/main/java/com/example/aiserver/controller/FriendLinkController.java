package com.example.aiserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aiserver.common.ApiResult;
import com.example.aiserver.common.BusinessException;
import com.example.aiserver.dto.FriendLinkStatusRequest;
import com.example.aiserver.entity.FriendLink;
import com.example.aiserver.mapper.FriendLinkMapper;
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
import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class FriendLinkController {
    private static final Set<Integer> LINK_STATUSES = Set.of(0, 1, 2);
    private static final Set<String> LINK_TAGS = Set.of("常用", "工具", "社区", "灵感");

    private final FriendLinkMapper friendLinkMapper;

    @GetMapping("/link/page")
    public ApiResult<Page<FriendLink>> page(
            @RequestParam(defaultValue = "1") long currentPage,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Integer status
    ) {
        long safeCurrentPage = Math.max(1, currentPage);
        long safeSize = Math.min(Math.max(1, size), 100);
        LambdaQueryWrapper<FriendLink> wrapper = new LambdaQueryWrapper<FriendLink>()
                .and(StringUtils.hasText(keyword), query -> query
                        .like(FriendLink::getName, keyword)
                        .or()
                        .like(FriendLink::getDescription, keyword))
                .eq(StringUtils.hasText(category), FriendLink::getCategory, category)
                .eq(StringUtils.hasText(tag), FriendLink::getTag, tag)
                .eq(status != null, FriendLink::getStatus, status)
                .orderByAsc(FriendLink::getSortOrder)
                .orderByDesc(FriendLink::getUpdatedAt);
        return ApiResult.success(friendLinkMapper.selectPage(new Page<>(safeCurrentPage, safeSize), wrapper));
    }

    @GetMapping("/link/enabled")
    public ApiResult<List<FriendLink>> enabledLinks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag
    ) {
        LambdaQueryWrapper<FriendLink> wrapper = new LambdaQueryWrapper<FriendLink>()
                .eq(FriendLink::getStatus, 1)
                .and(StringUtils.hasText(keyword), query -> query
                        .like(FriendLink::getName, keyword)
                        .or()
                        .like(FriendLink::getDescription, keyword))
                .eq(StringUtils.hasText(tag) && !"常用".equals(tag), FriendLink::getTag, tag)
                .orderByAsc(FriendLink::getSortOrder)
                .orderByDesc(FriendLink::getUpdatedAt);
        return ApiResult.success(friendLinkMapper.selectList(wrapper));
    }

    @PostMapping("/link/apply")
    public ApiResult<FriendLink> apply(@RequestBody FriendLink link) {
        normalize(link, true);
        link.setStatus(0);
        friendLinkMapper.insert(link);
        return ApiResult.success(link);
    }

    @PostMapping("/link")
    public ApiResult<FriendLink> add(@RequestBody FriendLink link) {
        normalize(link, false);
        friendLinkMapper.insert(link);
        return ApiResult.success(link);
    }

    @PutMapping("/link/{id}")
    public ApiResult<FriendLink> update(@PathVariable Long id, @RequestBody FriendLink link) {
        requireLink(id);
        normalize(link, false);
        link.setId(id);
        link.setUpdatedAt(LocalDateTime.now());
        friendLinkMapper.updateById(link);
        return ApiResult.success(friendLinkMapper.selectById(id));
    }

    @PutMapping("/link/{id}/status")
    public ApiResult<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody FriendLinkStatusRequest request) {
        requireLink(id);
        validateStatus(request.getStatus());
        FriendLink link = new FriendLink();
        link.setId(id);
        link.setStatus(request.getStatus());
        link.setUpdatedAt(LocalDateTime.now());
        friendLinkMapper.updateById(link);
        return ApiResult.success();
    }

    @PostMapping("/link/{id}/visit")
    public ApiResult<Void> recordVisit(@PathVariable Long id) {
        FriendLink existing = requireLink(id);
        if (existing.getStatus() == null || existing.getStatus() != 1) {
            throw new BusinessException("友链尚未启用");
        }
        FriendLink link = new FriendLink();
        link.setId(id);
        link.setVisits((existing.getVisits() == null ? 0 : existing.getVisits()) + 1);
        link.setUpdatedAt(LocalDateTime.now());
        friendLinkMapper.updateById(link);
        return ApiResult.success();
    }

    @DeleteMapping("/link/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        requireLink(id);
        friendLinkMapper.deleteById(id);
        return ApiResult.success();
    }

    private void normalize(FriendLink link, boolean applying) {
        if (!StringUtils.hasText(link.getName())) {
            throw new BusinessException("网站名称不能为空");
        }
        if (!StringUtils.hasText(link.getUrl())) {
            throw new BusinessException("网站地址不能为空");
        }
        if (!link.getUrl().startsWith("http://") && !link.getUrl().startsWith("https://")) {
            throw new BusinessException("网站地址必须以 http:// 或 https:// 开头");
        }
        if (!StringUtils.hasText(link.getDescription())) {
            link.setDescription("这个网站暂时没有简介。");
        }
        if (!StringUtils.hasText(link.getCategory())) {
            link.setCategory("娱乐");
        }
        if (!StringUtils.hasText(link.getTag()) || !LINK_TAGS.contains(link.getTag())) {
            link.setTag("常用");
        }
        if (!StringUtils.hasText(link.getLogoUrl())) {
            link.setLogoUrl("/anime-assets/liuhuaa.ico");
        }
        if (link.getVisits() == null) {
            link.setVisits(0);
        }
        if (link.getRating() == null) {
            link.setRating(4);
        }
        if (link.getSortOrder() == null) {
            link.setSortOrder(0);
        }
        if (!applying) {
            if (link.getStatus() == null) {
                link.setStatus(1);
            }
            validateStatus(link.getStatus());
        }
    }

    private void validateStatus(Integer status) {
        if (status == null || !LINK_STATUSES.contains(status)) {
            throw new BusinessException("友链状态不正确");
        }
    }

    private FriendLink requireLink(Long id) {
        FriendLink link = friendLinkMapper.selectById(id);
        if (link == null) {
            throw new BusinessException("友链不存在");
        }
        return link;
    }
}
