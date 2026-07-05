# Deployment Notes

Recommended production layout:

```text
Nginx web root: /www/wwwroot/anime-record-site/web
Spring Boot jar: /www/wwwroot/anime-record-site/server/anime-record-server.jar
Uploads: /www/wwwroot/anime-record-site/uploads
MySQL database: anime_record_site
```

## 1. Prepare Database

Create a MySQL database and user, then import:

```bash
mysql -u root -p < database/init.sql
```

The initialization script includes a development `ai_user/change_me` grant. For production, create a stronger database user/password and override the runtime environment:

```text
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=anime_record_site
DB_USERNAME=anime_user
DB_PASSWORD=change_this_password
JWT_SECRET=change_this_to_a_long_random_secret
UPLOAD_DIR=/www/wwwroot/anime-record-site/uploads
LOG_FILE=/www/wwwroot/anime-record-site/logs/anime-record-server.log
```

## 2. Build Backend

```bash
cd ai-server
mvn clean package -DskipTests
```

Upload the generated jar from `ai-server/target/`.

## 3. Build Frontend

```bash
cd ai-vue
npm install
npm run build
```

Upload everything in `ai-vue/dist` to the Nginx web root.

## 4. Nginx

Use [deploy/nginx.conf](nginx.conf) as the site location snippet. It serves Vue history routes and proxies `/api` to the backend on port `8080`.

## 5. Smoke Test

- Visit `/anime`
- Log in at `/login`
- Create a category
- Create an article with a cover image
- Publish and unpublish the article
- Confirm `/api/article/page?status=1` returns only published articles
- If an API returns an error, copy the `traceId` from the JSON response or `X-Request-Id` header and search the backend log.
