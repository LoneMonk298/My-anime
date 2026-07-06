-- Anime Record Site deployment SQL.
-- Charset: UTF-8
-- Default admin: admin / aqt5201314

CREATE DATABASE IF NOT EXISTS anime_record_site
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'ai_user'@'localhost' IDENTIFIED BY 'change_me';
CREATE USER IF NOT EXISTS 'ai_user'@'%' IDENTIFIED BY 'change_me';
GRANT ALL PRIVILEGES ON anime_record_site.* TO 'ai_user'@'localhost';
GRANT ALL PRIVILEGES ON anime_record_site.* TO 'ai_user'@'%';
FLUSH PRIVILEGES;

USE anime_record_site;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(80) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  nickname VARCHAR(80) DEFAULT NULL,
  avatar_url VARCHAR(500) DEFAULT NULL,
  email VARCHAR(160) DEFAULT NULL,
  phone VARCHAR(40) DEFAULT NULL,
  age INT DEFAULT NULL,
  gender VARCHAR(20) DEFAULT NULL,
  role VARCHAR(40) NOT NULL DEFAULT 'USER',
  status TINYINT NOT NULL DEFAULT 1,
  last_login_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  KEY idx_sys_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站点用户与管理员';

CREATE TABLE IF NOT EXISTS article_category (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  parent_id BIGINT UNSIGNED DEFAULT NULL,
  name VARCHAR(80) NOT NULL,
  code VARCHAR(80) NOT NULL,
  description VARCHAR(255) DEFAULT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_article_category_code (code),
  KEY idx_article_category_parent (parent_id),
  CONSTRAINT fk_article_category_parent
    FOREIGN KEY (parent_id) REFERENCES article_category(id)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章分类';

CREATE TABLE IF NOT EXISTS article (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  category_id BIGINT UNSIGNED NOT NULL,
  author_id BIGINT UNSIGNED DEFAULT NULL,
  author_name VARCHAR(80) DEFAULT NULL,
  summary VARCHAR(1000) DEFAULT NULL,
  content MEDIUMTEXT NOT NULL,
  cover_img VARCHAR(500) DEFAULT NULL,
  tags VARCHAR(500) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0 草稿, 1 已发布, 2 已下线',
  read_count INT NOT NULL DEFAULT 0,
  published_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_article_category (category_id),
  KEY idx_article_status (status),
  KEY idx_article_published_at (published_at),
  CONSTRAINT fk_article_category
    FOREIGN KEY (category_id) REFERENCES article_category(id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_article_author
    FOREIGN KEY (author_id) REFERENCES sys_user(id)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='记录站文章';

CREATE TABLE IF NOT EXISTS file_resource (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  original_name VARCHAR(255) NOT NULL,
  stored_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  file_url VARCHAR(500) DEFAULT NULL,
  mime_type VARCHAR(120) DEFAULT NULL,
  file_size BIGINT DEFAULT NULL,
  business_type VARCHAR(80) DEFAULT NULL,
  business_id VARCHAR(80) DEFAULT NULL,
  business_field VARCHAR(80) DEFAULT NULL,
  uploader_id BIGINT UNSIGNED DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_file_resource_business (business_type, business_id, business_field),
  KEY idx_file_resource_uploader (uploader_id),
  CONSTRAINT fk_file_resource_uploader
    FOREIGN KEY (uploader_id) REFERENCES sys_user(id)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上传文件';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网址导航与友情链接';

INSERT INTO sys_user (id, username, password_hash, nickname, email, role, status)
VALUES
  (1, 'admin', '$2a$10$sZBtsj4wL2tx.qLkeLg/COUJpiWIYTEKoykxMk0HL64AKnVWLagmi', '站长', 'admin@example.com', 'SUPER_ADMIN', 1)
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  nickname = VALUES(nickname),
  email = VALUES(email),
  role = VALUES(role),
  status = VALUES(status);

INSERT INTO article_category (id, parent_id, name, code, description, sort_order, status)
VALUES
  (1, NULL, '番剧', 'bangumi', '番剧记录、补番进度与单集短评', 10, 1),
  (2, NULL, '动画', 'anime', '动画资讯、观后感与设定整理', 20, 1),
  (3, NULL, '漫画', 'manga', '漫画阅读记录与角色讨论', 30, 1),
  (4, NULL, '游戏', 'game', '游戏记录、攻略与友链内容', 40, 1),
  (5, NULL, '随笔', 'essay', '站长随笔与日常记录', 50, 1)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  name = VALUES(name),
  description = VALUES(description),
  sort_order = VALUES(sort_order),
  status = VALUES(status);

INSERT INTO friend_link (id, name, url, description, category, tag, logo_url, visits, rating, status, sort_order)
VALUES
  (1, '哔哩哔哩', 'https://www.bilibili.com/', '综合视频社区，番剧、动画、游戏与生活内容都很全。', '动漫', '常用', 'https://www.bilibili.com/favicon.ico', 32, 5, 1, 10),
  (2, '稀饭动漫', 'https://xifan.moe', '动漫资源与追番入口，适合收藏备用。', '动漫', '常用', '/anime-assets/liuhuaa.ico', 18, 4, 1, 20),
  (3, '槿篱之家', 'https://blog.jinlizhijia.online/', '个人记录、开发随笔和日常整理。', '博客', '社区', '/anime-assets/liuhuaa.ico', 21, 4, 1, 30),
  (4, 'TinyPNG', 'https://tinypng.com/', '图片压缩工具，上传封面前可以先压缩。', '工具', '工具', 'https://tinypng.com/images/apple-touch-icon.png', 12, 4, 1, 40),
  (5, 'Pixiv', 'https://www.pixiv.net/', '插画、角色与视觉灵感收集地。', '灵感', '灵感', 'https://www.pixiv.net/favicon.ico', 25, 5, 1, 50)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  url = VALUES(url),
  description = VALUES(description),
  category = VALUES(category),
  tag = VALUES(tag),
  logo_url = VALUES(logo_url),
  visits = VALUES(visits),
  rating = VALUES(rating),
  status = VALUES(status),
  sort_order = VALUES(sort_order);
