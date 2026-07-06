<template>
  <div class="article-page">
    <div class="star-background" aria-hidden="true">
      <div class="hero-glow"></div>
      <i v-for="n in 6" :key="n"></i>
    </div>

    <header class="site-header">
      <div class="top-clock">
        <span>{{ currentDateText }}</span>
        <strong>{{ currentTime }}</strong>
      </div>

      <div class="nav-art" aria-hidden="true"></div>

      <nav class="main-nav" aria-label="主导航">
        <span class="nav-spacer" aria-hidden="true"></span>
        <button class="home-link" type="button" @click="router.push('/')">
          <i class="fa-solid fa-home" aria-hidden="true"></i>
          HOME
        </button>
        <button
          class="mobile-menu-toggle"
          type="button"
          :aria-expanded="mobileMenuOpen"
          aria-label="展开导航"
          @click="mobileMenuOpen = !mobileMenuOpen"
        >
          <i :class="mobileMenuOpen ? 'fa-solid fa-xmark' : 'fa-solid fa-bars'" aria-hidden="true"></i>
        </button>
        <div class="nav-links" :class="{ open: mobileMenuOpen }">
          <a href="https://www.lonemonk.xyz" target="_blank" rel="noreferrer">个人主页</a>
          <a href="https://www.bilibili.com/" target="_blank" rel="noreferrer">B站官网</a>
          <a href="https://www.pixiv.net/" target="_blank" rel="noreferrer">P站官网</a>
          <a href="https://xifan.moe" target="_blank" rel="noreferrer">稀饭动漫</a>
          <button type="button" @click="router.push('/links')">槿篱游戏</button>
          <button type="button" @click="goAdmin">管理后台</button>
        </div>
      </nav>

      <section class="latest-news">
        <span class="latest-spacer" aria-hidden="true"></span>
        <div class="latest-title">最近更新</div>
        <div class="latest-track">
          <div class="latest-marquee">
            <button v-for="item in marqueeArticles" :key="`${item.id}-a`" type="button" @click="goArticle(item.id)">
              <i class="fa-solid fa-circle-arrow-right" aria-hidden="true"></i>
              {{ item.title }}
            </button>
            <button v-for="item in marqueeArticles" :key="`${item.id}-b`" type="button" @click="goArticle(item.id)">
              <i class="fa-solid fa-circle-arrow-right" aria-hidden="true"></i>
              {{ item.title }}
            </button>
          </div>
        </div>
      </section>
    </header>

    <main class="detail-content">
      <button class="back-button" type="button" @click="router.push('/')">
        <i class="fa-solid fa-arrow-left" aria-hidden="true"></i>
        返回首页
      </button>

      <el-skeleton v-if="loading" class="detail-skeleton" :rows="12" animated />

      <section v-else-if="loadError" class="empty-state">
        <h1>记录暂时无法打开</h1>
        <p>{{ loadError }}</p>
        <button type="button" @click="fetchArticle">重新加载</button>
      </section>

      <article v-else-if="article" class="article-shell">
        <figure class="cover-wrap">
          <img :src="getImage(article)" :alt="article.title" @error="handleImageError" />
        </figure>

        <section class="article-head">
          <span class="category">{{ getCategoryName(article.categoryId) }}</span>
          <h1>{{ article.title }}</h1>
          <div class="meta-line">
            <span><i class="fa-regular fa-clock" aria-hidden="true"></i>{{ formatDate(article.publishedAt || article.updatedAt) }}</span>
            <span><i class="fa-solid fa-user-circle" aria-hidden="true"></i>{{ article.authorName || article.author || 'ZG' }}</span>
            <span v-if="article.readCount !== undefined"><i class="fa-regular fa-eye" aria-hidden="true"></i>{{ article.readCount }} reads</span>
          </div>
          <p v-if="article.summary" class="summary">{{ article.summary }}</p>
        </section>

        <section class="article-body" v-html="article.content || '<p>这篇记录暂时还没有正文。</p>'"></section>
      </article>

      <section v-if="article && (previousArticle || nextArticle)" class="neighbor-section">
        <button class="neighbor-card" type="button" :disabled="!previousArticle" @click="goArticle(previousArticle?.id)">
          <span>上一篇</span>
          <strong>{{ previousArticle?.title || '已经是第一篇记录' }}</strong>
        </button>
        <button class="neighbor-card next" type="button" :disabled="!nextArticle" @click="goArticle(nextArticle?.id)">
          <span>下一篇</span>
          <strong>{{ nextArticle?.title || '已经是最后一篇记录' }}</strong>
        </button>
      </section>

      <section v-if="relatedArticles.length" class="related-section">
        <div class="section-title">
          <span>Archive</span>
          <h2>相关推荐</h2>
        </div>
        <div class="related-grid">
          <article v-for="item in relatedArticles" :key="item.id" class="related-card" @click="goArticle(item.id)">
            <img :src="getImage(item)" :alt="item.title" @error="handleImageError" />
            <div>
              <span>{{ getCategoryName(item.categoryId) }}</span>
              <h3>{{ item.title }}</h3>
              <small>{{ formatDate(item.publishedAt || item.updatedAt) }} · {{ item.authorName || item.author || 'ZG' }}</small>
            </div>
          </article>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { dayjs } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { getArticleCategoryTree, getArticleList, getArticleView } from '@/api/frontend'
import { getArticleCover, resolveFileUrl } from '@/utils/fileUrl'

const route = useRoute()
const router = useRouter()

const article = ref(null)
const previousArticle = ref(null)
const nextArticle = ref(null)
const relatedArticles = ref([])
const categories = ref([])
const loading = ref(false)
const loadError = ref('')
const now = ref(new Date())
const mobileMenuOpen = ref(false)
let clockTimer = null

const fallbackImages = ['/anime-assets/cover-rezero.jpg', '/anime-assets/cover-chunibyo.jpg']

const currentTime = computed(() => dayjs(now.value).format('HH:mm:ss'))
const currentDateText = computed(() => {
  const weekMap = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${weekMap[now.value.getDay()]}. ${dayjs(now.value).format('M 月 D日, YYYY')}`
})

const marqueeArticles = computed(() => {
  if (relatedArticles.value.length) return relatedArticles.value.slice(0, 6)
  return article.value ? [article.value] : [
    { id: 'placeholder-1', title: 'RE：从零开始的异世界生活' },
    { id: 'placeholder-2', title: '中二病也要谈恋爱！' },
  ]
})

const cleanParams = (params) => {
  const output = { ...params }
  Object.keys(output).forEach((key) => {
    if (output[key] === '' || output[key] === null || output[key] === undefined) delete output[key]
  })
  return output
}

const getImage = (item = {}) => {
  if (typeof item.id === 'string' && item.id.startsWith('placeholder')) {
    return fallbackImages[item.id.endsWith('2') ? 1 : 0]
  }
  const cover = getArticleCover(item)
  if (cover) return resolveFileUrl(cover)
  const index = Math.abs(Number(item.id || 0)) % fallbackImages.length
  return fallbackImages[index]
}

const handleImageError = (event) => {
  event.target.src = fallbackImages[0]
}

const getCategoryName = (categoryId) => {
  return categories.value.find((item) => item.value === categoryId)?.label || '番剧'
}

const formatDate = (date) => date ? dayjs(date).format('YYYY-MM-DD') : '未发布'

const fetchCategories = async () => {
  try {
    const res = await getArticleCategoryTree()
    const data = res?.data || res || []
    categories.value = data.map((item) => ({
      label: item.categoryName || item.name,
      value: item.id,
    }))
  } catch (error) {
    console.error('Failed to load article categories:', error)
    categories.value = []
  }
}

const fetchRelated = async () => {
  try {
    const res = await getArticleList(cleanParams({
      currentPage: 1,
      size: 4,
      status: 1,
      categoryId: article.value?.categoryId,
      sortField: 'publishedAt',
      sortDirection: 'desc',
    }))
    const data = res?.data || res || {}
    relatedArticles.value = (Array.isArray(data.records) ? data.records : [])
      .filter((item) => String(item.id) !== String(route.params.id))
      .slice(0, 3)
  } catch (error) {
    console.error('Failed to load related articles:', error)
    relatedArticles.value = []
  }
}

const fetchArticle = async () => {
  const id = route.params.id
  if (!id) return
  loading.value = true
  loadError.value = ''
  article.value = null
  previousArticle.value = null
  nextArticle.value = null
  try {
    const res = await getArticleView(id)
    const data = res?.data || res
    const currentArticle = data?.article || data
    if (!currentArticle) {
      loadError.value = '这篇记录不存在，或已经被下线。'
      return
    }
    article.value = currentArticle
    previousArticle.value = data?.previous || null
    nextArticle.value = data?.next || null
    document.title = `${currentArticle.title} - 二次元记录站`
    await fetchRelated()
  } catch (error) {
    loadError.value = error?.message || '加载失败，请稍后再试。'
  } finally {
    loading.value = false
  }
}

const goArticle = (id) => {
  if (!id || (typeof id === 'string' && id.startsWith('placeholder'))) return
  router.push(`/article/${id}`)
}

const goAdmin = () => {
  router.push({ path: '/auth/login', query: { redirect: '/user/dashboard', forceLogin: '1' } })
}

onMounted(async () => {
  clockTimer = window.setInterval(() => {
    now.value = new Date()
  }, 1000)
  await fetchCategories()
  await fetchArticle()
})

watch(() => route.params.id, async () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
  await fetchArticle()
})

onBeforeUnmount(() => {
  if (clockTimer) window.clearInterval(clockTimer)
})
</script>

<style scoped>
.article-page {
  --gutter: 42px;
  --nav: #233669;
  --panel: rgba(10, 25, 47, 0.58);
  --text: #e6f1ff;
  --muted: #98a9bf;
  --cyan: #64ffda;

  position: relative;
  min-height: 100vh;
  overflow-x: hidden;
  color: var(--text);
  background: radial-gradient(circle at 50% 50%, #162340 0%, #0d1117 58%, #06101f 100%);
  font-family: "Microsoft YaHei", "HarmonyOS Sans SC", system-ui, sans-serif;
}

.article-page,
.article-page *,
.article-page *::before,
.article-page *::after {
  box-sizing: border-box;
}

:global(body) {
  background: #06101f;
}

:global(::-webkit-scrollbar) {
  width: 10px;
  height: 10px;
}

:global(::-webkit-scrollbar-track) {
  background: #06101f;
}

:global(::-webkit-scrollbar-thumb) {
  background: #4a5568;
  border: 2px solid #06101f;
  border-radius: 8px;
}

.article-page::selection,
.article-page *::selection {
  background: #0d6efd;
  color: #64ffda;
  text-shadow: 0 0 8px rgba(100, 255, 218, 0.55);
}

.star-background,
.star-background .hero-glow,
.star-background i {
  position: fixed;
  inset: 0;
  pointer-events: none;
}

.star-background {
  z-index: 0;
  overflow: hidden;
}

.hero-glow {
  background: url('/anime-assets/hero-glow.svg') no-repeat center center / contain;
  opacity: 0.9;
  animation: starMove 80s linear infinite;
}

.star-background i {
  background:
    radial-gradient(2px 2px at 20px 30px, #fff, transparent),
    radial-gradient(2px 2px at 130px 80px, #fff, transparent),
    radial-gradient(1px 1px at 250px 120px, #64ffda, transparent),
    radial-gradient(2px 2px at 440px 180px, #fff, transparent),
    radial-gradient(1px 1px at 590px 220px, #fff, transparent);
  background-size: 600px 400px;
  opacity: 0.38;
  animation: starAnimation 20s linear infinite;
}

.star-background i:nth-child(3) { animation-duration: 18s; opacity: 0.32; }
.star-background i:nth-child(4) { animation-duration: 22s; opacity: 0.28; }
.star-background i:nth-child(5) { animation-duration: 16s; opacity: 0.22; }
.star-background i:nth-child(6) { animation-duration: 24s; opacity: 0.2; }

.site-header,
.detail-content {
  position: relative;
  z-index: 1;
}

.top-clock {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 46px;
  padding-left: var(--gutter);
  background: var(--nav);
  color: #dce9ff;
  font-size: 16px;
  font-weight: 700;
}

.top-clock strong {
  padding: 3px 6px;
  border-radius: 2px;
  background: #0d6efd;
  color: #fff;
  font-variant-numeric: tabular-nums;
}

.nav-art {
  width: 100%;
  height: 82px;
  background: url('/anime-assets/frontnav.png') center 44% / cover no-repeat;
}

.main-nav {
  display: flex;
  align-items: stretch;
  width: 100%;
  height: 42px;
  background: var(--nav);
}

.nav-links {
  display: flex;
  align-items: stretch;
}

.nav-spacer {
  display: block;
  width: var(--gutter);
  height: 100%;
  background: #1d315f;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.main-nav a,
.main-nav button,
.mobile-menu-toggle {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 112px;
  height: 100%;
  padding: 0 18px;
  border: 0;
  background: transparent;
  color: #fff;
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  font-weight: 800;
  text-decoration: none;
  line-height: 1;
  transition: background 0.18s ease, color 0.18s ease;
}

.main-nav .mobile-menu-toggle {
  display: none;
}

.main-nav i {
  margin-right: 5px;
  font-size: 14px;
}

.main-nav a:hover,
.main-nav button:hover,
.main-nav .home-link {
  background: #0d6efd;
}

.latest-news {
  display: grid;
  grid-template-columns: var(--gutter) 114px minmax(0, 1fr);
  width: 100%;
  height: 42px;
  padding-right: var(--gutter);
  overflow: hidden;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: #071b35;
}

.latest-spacer {
  display: block;
  width: var(--gutter);
  height: 100%;
  background: #06182e;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.latest-title {
  position: relative;
  display: flex;
  align-items: center;
  padding-left: 18px;
  background: #0d6efd;
  color: var(--cyan);
  font-size: 18px;
  font-weight: 950;
}

.latest-title::after {
  position: absolute;
  right: -30px;
  bottom: 0;
  width: 0;
  height: 0;
  border-bottom: 42px solid #0d6efd;
  border-right: 30px solid transparent;
  content: "";
}

.latest-track {
  min-width: 0;
  overflow: hidden;
  background: #fff;
  padding-left: 52px;
}

.latest-marquee {
  display: flex;
  width: max-content;
  height: 100%;
  align-items: center;
  gap: 28px;
  animation: marquee 20s linear infinite;
}

.latest-track:hover .latest-marquee {
  animation-play-state: paused;
}

.latest-marquee button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 0;
  background: transparent;
  color: #0d6efd;
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  font-weight: 850;
  white-space: nowrap;
}

.detail-content {
  width: 100%;
  padding: 22px var(--gutter) 54px;
}

.back-button,
.empty-state button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  margin-bottom: 18px;
  padding: 0 14px;
  border: 1px solid rgba(100, 255, 218, 0.45);
  border-radius: 4px;
  background: rgba(13, 110, 253, 0.18);
  color: #64ffda;
  cursor: pointer;
  font: inherit;
  font-weight: 850;
}

.article-shell,
.empty-state,
.detail-skeleton {
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  background: var(--panel);
  backdrop-filter: blur(8px);
}

.cover-wrap {
  width: 100%;
  height: clamp(330px, 42vw, 560px);
  margin: 0;
  overflow: hidden;
  background: #06182e;
}

.cover-wrap img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.article-head {
  padding: 34px clamp(22px, 5vw, 78px) 24px;
  background: rgba(10, 25, 47, 0.54);
}

.category,
.neighbor-card span,
.related-card span,
.section-title span {
  color: var(--cyan);
  font-size: 14px;
  font-weight: 900;
}

.article-head h1 {
  max-width: 1100px;
  margin: 12px 0 18px;
  color: #fff;
  font-size: clamp(30px, 4vw, 54px);
  line-height: 1.15;
  font-weight: 950;
}

.meta-line {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 24px;
  color: #dbeafe;
  font-size: 15px;
}

.meta-line span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.summary {
  max-width: 1080px;
  margin: 22px 0 0;
  color: #c7d6eb;
  font-size: 16px;
  line-height: 1.9;
}

.article-body {
  padding: 34px clamp(22px, 5vw, 78px) 54px;
  color: #d8e6f7;
  font-size: 17px;
  line-height: 2;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.article-body :deep(p) {
  margin: 0 0 1.25em;
}

.article-body :deep(h1),
.article-body :deep(h2),
.article-body :deep(h3) {
  color: #fff;
  line-height: 1.35;
}

.article-body :deep(img) {
  max-width: 100%;
  border-radius: 6px;
}

.article-body :deep(a) {
  color: #64ffda;
}

.empty-state {
  min-height: 360px;
  padding: 60px 24px;
  text-align: center;
}

.empty-state h1 {
  margin: 0 0 12px;
  font-size: 34px;
}

.empty-state p {
  color: var(--muted);
}

.empty-state button {
  margin: 18px auto 0;
}

.neighbor-section {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  margin-top: 20px;
}

.neighbor-card {
  display: grid;
  gap: 8px;
  min-height: 92px;
  padding: 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  background: var(--panel);
  color: #fff;
  cursor: pointer;
  font: inherit;
  text-align: left;
  transition: border-color 0.22s ease, box-shadow 0.22s ease, transform 0.22s ease;
}

.neighbor-card.next {
  text-align: right;
}

.neighbor-card strong {
  overflow: hidden;
  font-size: 20px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.neighbor-card:disabled {
  cursor: default;
  opacity: 0.48;
}

.neighbor-card:not(:disabled):hover {
  border-color: rgba(13, 110, 253, 0.65);
  box-shadow: 0 0 24px rgba(13, 110, 253, 0.18);
  transform: translateY(-2px);
}

.related-section {
  margin-top: 28px;
}

.section-title {
  margin-bottom: 16px;
}

.section-title h2 {
  margin: 6px 0 0;
  font-size: 28px;
}

.related-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.related-card {
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  background: var(--panel);
  cursor: pointer;
  transition: border-color 0.22s ease, box-shadow 0.22s ease, transform 0.22s ease;
}

.related-card:hover,
.related-card:focus-within {
  border-color: rgba(13, 110, 253, 0.65);
  box-shadow: 0 0 24px rgba(13, 110, 253, 0.18);
  transform: translateY(-2px);
}

.related-card img {
  display: block;
  width: 100%;
  height: 170px;
  object-fit: cover;
}

.related-card div {
  padding: 16px;
}

.related-card h3 {
  margin: 8px 0 12px;
  color: #fff;
  font-size: 20px;
  line-height: 1.35;
  transition: color 0.22s ease, text-shadow 0.22s ease;
}

.related-card:hover h3 {
  color: #339dff;
  text-shadow: 0 0 10px rgba(13, 110, 253, 0.42);
}

.related-card small {
  color: #7f8ea4;
}

@keyframes marquee {
  from { transform: translateX(0); }
  to { transform: translateX(-50%); }
}

@keyframes starAnimation {
  from { transform: translateY(0); }
  to { transform: translateY(-400px); }
}

@keyframes starMove {
  from { transform: translateY(0); }
  to { transform: translateY(-220px); }
}

@media (max-width: 900px) {
  .article-page {
    --gutter: 16px;
  }

  .top-clock {
    height: 42px;
    gap: 8px;
    padding-left: var(--gutter);
    font-size: 15px;
    white-space: nowrap;
  }

  .nav-art {
    height: 82px;
    background-position: center 46%;
  }

  .main-nav {
    position: relative;
    height: auto;
    min-height: 44px;
    overflow: visible;
    scrollbar-width: none;
    flex-wrap: wrap;
  }

  .main-nav::-webkit-scrollbar {
    display: none;
  }

  .nav-spacer {
    flex: 0 0 var(--gutter);
    width: var(--gutter);
    height: 44px;
  }

  .main-nav .home-link {
    flex: 0 0 44px;
    width: 44px;
    min-width: 44px;
    max-width: 44px;
    height: 44px;
    min-height: 44px;
    padding: 0;
    font-size: 0;
  }

  .main-nav .home-link i {
    margin-right: 0;
    font-size: 18px;
  }

  .main-nav .mobile-menu-toggle {
    display: inline-flex;
    position: absolute;
    left: 50%;
    top: 6px;
    z-index: 5;
    flex: 0 0 54px;
    width: 54px;
    min-width: 54px;
    max-width: 54px;
    height: 32px;
    padding: 0;
    border: 1px solid #fff;
    border-radius: 4px;
    background: #06182e;
    transform: translateX(-50%);
    box-shadow: 0 0 0 2px rgba(13, 110, 253, 0.65);
  }

  .main-nav .mobile-menu-toggle i {
    margin: 0;
    font-size: 22px;
  }

  .nav-links {
    position: static;
    z-index: 4;
    flex: 0 0 100%;
    width: 100%;
    display: none;
    padding: 18px 0 20px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.13);
    background: var(--nav);
    box-shadow: 0 16px 28px rgba(0, 0, 0, 0.28);
  }

  .nav-links.open {
    display: block;
  }

  .main-nav .nav-links a,
  .main-nav .nav-links button {
    width: 100%;
    min-width: 0;
    height: 52px;
    justify-content: flex-start;
    padding: 0 24px;
    font-size: 15px;
  }

  .main-nav i {
    margin-right: 3px;
    font-size: 12px;
  }

  .latest-news {
    grid-template-columns: var(--gutter) minmax(112px, 132px) minmax(0, 1fr);
    height: 44px;
    padding-right: 0;
  }

  .latest-title {
    padding-left: 16px;
    font-size: 18px;
  }

  .latest-title::after {
    right: -28px;
    border-bottom-width: 44px;
    border-right-width: 28px;
  }

  .latest-track {
    padding-left: 42px;
  }

  .latest-marquee {
    gap: 22px;
    animation-duration: 24s;
  }

  .latest-marquee button {
    font-size: 13px;
  }

  .detail-content {
    padding: 18px var(--gutter) 38px;
  }

  .cover-wrap {
    height: min(310px, 46dvh);
  }

  .article-head,
  .article-body {
    padding-left: 20px;
    padding-right: 20px;
  }

  .article-head {
    padding-top: 24px;
  }

  .article-head h1 {
    font-size: 32px;
    line-height: 1.2;
  }

  .summary {
    font-size: 15px;
    line-height: 1.75;
  }

  .article-body {
    padding-top: 26px;
    padding-bottom: 36px;
    font-size: 16px;
    line-height: 1.85;
  }

  .neighbor-section,
  .related-grid {
    grid-template-columns: 1fr;
  }

  .neighbor-card.next {
    text-align: left;
  }

  .neighbor-card strong {
    white-space: normal;
  }
}

@media (max-width: 420px) {
  .article-page {
    --gutter: 14px;
  }

  .top-clock {
    font-size: 14px;
  }

  .main-nav a,
  .main-nav button {
    padding: 0 6px;
  }

  .main-nav .home-link {
    flex: 0 0 44px;
    width: 44px;
    min-width: 44px;
    max-width: 44px;
    height: 44px;
    min-height: 44px;
    padding: 0;
    font-size: 0;
  }

  .nav-links {
    padding-top: 14px;
    padding-bottom: 16px;
  }

  .main-nav .nav-links a,
  .main-nav .nav-links button {
    height: 50px;
    padding-inline: 20px;
  }

  .latest-news {
    grid-template-columns: var(--gutter) 118px minmax(0, 1fr);
  }

  .latest-track {
    padding-left: 36px;
  }

  .cover-wrap {
    height: 260px;
  }

  .article-head,
  .article-body {
    padding-left: 18px;
    padding-right: 18px;
  }

  .article-head h1 {
    font-size: 28px;
  }

  .related-card img {
    height: 160px;
  }
}
</style>
