-- MySQL initialization script for Anime Record Site.
-- Fresh schema for the anime-site rebuild.

CREATE DATABASE IF NOT EXISTS anime_record_site
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Site users and administrators';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Article category tree';

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
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0 draft, 1 published, 2 offline',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Anime record articles';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Uploaded files';

INSERT INTO sys_user (id, username, password_hash, nickname, email, phone, age, gender, role, status)
VALUES
  (1, 'admin', '$2a$10$demo-password-hash-replace-in-backend', '站长', 'admin@example.com', '13800000000', NULL, 'unknown', 'SUPER_ADMIN', 1)
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
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
  name = VALUES(name),
  description = VALUES(description),
  sort_order = VALUES(sort_order),
  status = VALUES(status);

INSERT INTO article (
  id, title, category_id, author_id, author_name, summary, content, cover_img, tags, status, read_count, published_at
)
VALUES
  (
    1,
    'RE：从零开始的异世界生活',
    1,
    1,
    'ZG',
    '记录一次补番时的剧情回顾和角色印象。',
    '<p>这里可以写番剧记录、观后感、角色讨论和截图整理。</p>',
    '/anime-assets/cover-rezero.jpg',
    '番剧,补番,异世界',
    1,
    0,
    NOW()
  ),
  (
    2,
    '中二病也要谈恋爱！',
    1,
    1,
    'ZG',
    '关于青春、中二和恋爱的轻松记录。',
    '<p>这里可以写单集短评、台词摘录和角色关系。</p>',
    '/anime-assets/cover-chunibyo.jpg',
    '番剧,恋爱,青春',
    1,
    0,
    NOW()
  )
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  category_id = VALUES(category_id),
  summary = VALUES(summary),
  content = VALUES(content),
  cover_img = VALUES(cover_img),
  tags = VALUES(tags),
  status = VALUES(status);
