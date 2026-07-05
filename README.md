# Anime Record Site

二次元记录站，基于 Vue 3、Spring Boot 和 MySQL。当前版本定位为动漫/番剧/漫画/游戏内容站，保留原 HTML 视觉方向，并提供后台文章管理、分类管理、封面上传、发布/下线和前台展示能力。

## Project Structure

```text
.
├─ ai-vue/        Vue 3 frontend and admin console
├─ ai-server/     Spring Boot backend API
├─ database/      MySQL initialization scripts
└─ deploy/        Nginx and deployment notes
```

## Local Development

Initialize MySQL:

```bash
mysql -u root -p < database/init.sql
```

Run backend:

```bash
cd ai-server
mvn spring-boot:run
```

Run frontend:

```bash
cd ai-vue
npm install
npm run dev
```

Build checks:

```bash
cd ai-server && mvn clean test
cd ai-vue && npm run build
```

## Main Routes

- Public site: `/anime`
- Admin login: `/login`
- Admin articles: `/backend/articles`
- Article API: `/api/article/...`
- Upload API: `/api/file/upload`

## Database

The fresh schema uses:

- `article`
- `article_category`
- `file_resource`
- `sys_user`
- `password_reset_code`

Default seed data is included in [database/init.sql](database/init.sql).
