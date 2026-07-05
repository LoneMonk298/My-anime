# Anime Record Site Backend

Spring Boot backend for the anime record site.

## Stack

- Spring Boot 3
- MyBatis-Plus
- MySQL
- JWT authentication

## Run Locally

Import the database:

```bash
mysql -u root -p < ../database/init.sql
```

Start the application:

```bash
mvn spring-boot:run
```

The default configuration reads database and upload settings from environment variables when provided. See `src/main/resources/application.yml`.

## Main APIs

- `POST /api/user/login`
- `POST /api/user/register`
- `POST /api/user/logout`
- `GET /api/article/category/tree`
- `POST /api/article/category`
- `GET /api/article/page`
- `POST /api/article`
- `GET /api/article/{id}`
- `PUT /api/article/{id}`
- `PUT /api/article/{id}/status`
- `DELETE /api/article/{id}`
- `POST /api/file/upload`
