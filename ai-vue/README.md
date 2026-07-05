# Anime Record Site Frontend

Vue 3 frontend for the anime record site. It contains the public `/anime` page and the admin console for article/category management.

## Stack

- Vue 3
- Vite
- Vue Router
- Pinia
- Element Plus
- Axios
- WangEditor

## Scripts

```bash
npm install
npm run dev
npm run build
```

The Vite dev server proxies `/api` to the Spring Boot backend configured in [vite.config.js](vite.config.js).

## Key Paths

- `src/views/anime.vue`: public anime record page
- `src/views/articles.vue`: admin article management
- `src/components/ArticleDialog.vue`: article editor dialog
- `src/api/admin.js`: admin API wrapper
- `src/api/frontend.js`: public API wrapper
- `public/anime-assets/`: static visual assets
