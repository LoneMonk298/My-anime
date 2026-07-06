package com.example.aiserver.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FriendLinkSchemaInitializer {
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS friend_link (
                  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                  name VARCHAR(120) NOT NULL,
                  url VARCHAR(500) NOT NULL,
                  description VARCHAR(500) DEFAULT NULL,
                  category VARCHAR(60) NOT NULL DEFAULT '娱乐',
                  tag VARCHAR(40) NOT NULL DEFAULT '常用',
                  logo_url VARCHAR(500) DEFAULT NULL,
                  visits INT NOT NULL DEFAULT 0,
                  rating INT NOT NULL DEFAULT 4,
                  status TINYINT NOT NULL DEFAULT 1 COMMENT '0待审核 1启用 2停用',
                  sort_order INT NOT NULL DEFAULT 0,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  deleted TINYINT NOT NULL DEFAULT 0,
                  PRIMARY KEY (id),
                  KEY idx_friend_link_status (status, tag, category),
                  KEY idx_friend_link_sort (sort_order, updated_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网址导航与友情链接'
                """);

        jdbcTemplate.update("""
                INSERT IGNORE INTO friend_link
                (id, name, url, description, category, tag, logo_url, visits, rating, status, sort_order)
                VALUES
                (1, '哔哩哔哩', 'https://www.bilibili.com/', '综合视频社区，番剧、动画、游戏与生活内容都很全。', '动漫', '常用', 'https://www.bilibili.com/favicon.ico', 32, 5, 1, 10),
                (2, '稀饭动漫', 'https://xifan.moe', '动漫资源与追番入口，适合收藏备用。', '动漫', '常用', '/anime-assets/liuhuaa.ico', 18, 4, 1, 20),
                (3, '槿篱之家', 'https://blog.jinlizhijia.online/', '个人记录、开发随笔和日常整理。', '博客', '社区', '/anime-assets/liuhuaa.ico', 21, 4, 1, 30),
                (4, 'TinyPNG', 'https://tinypng.com/', '图片压缩工具，上传封面前可以先压缩。', '工具', '工具', 'https://tinypng.com/images/apple-touch-icon.png', 12, 4, 1, 40),
                (5, 'Pixiv', 'https://www.pixiv.net/', '插画、角色与视觉灵感收集地。', '灵感', '灵感', 'https://www.pixiv.net/favicon.ico', 25, 5, 1, 50)
                """);
    }
}
