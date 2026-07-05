# Anime Record Site Backend

Spring Boot backend for the anime record site.

## Stack

- Spring Boot 3
- MyBatis-Plus
- MySQL
- JWT authentication

## Run Locally

Import the database with a privileged MySQL account:

```bash
mysql -u root -p < ../database/init.sql
```

The script creates `anime_record_site` and grants the default local account:

```text
DB_USERNAME=ai_user
DB_PASSWORD=change_me
```

Start the application:

```bash
mvn spring-boot:run
```

Override database settings when needed:

```powershell
$env:DB_HOST="127.0.0.1"
$env:DB_PORT="3306"
$env:DB_NAME="anime_record_site"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
mvn spring-boot:run
```

## Observability

Every request gets an `X-Request-Id` response header and a matching `traceId` in backend logs. Error responses also include `traceId`, so frontend reports such as "服务器内部错误，请求编号：..." can be searched directly in the backend log.

Database exceptions are handled separately and return a database-specific message while logging the full stack trace.

## Main APIs

- `POST /api/user/login`
- `POST /api/user/register`
- `POST /api/user/logout`
- `GET /api/article/category/tree`
- `GET /api/article/page`
- `GET /api/article/{id}`
- `POST /api/article`
- `PUT /api/article/{id}`
- `PUT /api/article/{id}/status`
- `DELETE /api/article/{id}`
- `POST /api/file/upload`
