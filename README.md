# Anime Record Site

二次元记录站，基于 Vue 3、Spring Boot 和 MySQL。当前版本定位为动漫、番剧、漫画、游戏内容站，提供后台文章管理、分类管理、封面上传、发布/下线和前台展示能力。

## Project Structure

```text
.
├─ ai-vue/        Vue 3 frontend and admin console
├─ ai-server/     Spring Boot backend API
├─ database/      MySQL initialization scripts
└─ deploy/        Nginx and deployment notes
```

## Local Development

Initialize MySQL with a privileged account:

```bash
mysql -u root -p < database/init.sql
```

The script creates the `anime_record_site` database and a local development user:

```text
DB_USERNAME=ai_user
DB_PASSWORD=change_me
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

## Troubleshooting

Backend error responses include a `traceId`. Search the backend console or log file for that value to locate the exact exception and request path.

If MySQL uses a different account, set environment variables before starting the backend:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
mvn spring-boot:run
```

## Main Routes

- Public site: `/anime`
- Article detail: `/article/:id`
- Admin login: `/auth/login`
- Admin articles: `/user/articles`
- Article API: `/api/article/...`
- Public article view API: `/api/article/{id}/view`
- Upload API: `/api/file/upload`

## Database

The fresh schema uses:

- `article`
- `article_category`
- `file_resource`
- `sys_user`
- `password_reset_code`

Default seed data is included in [database/init.sql](database/init.sql).
