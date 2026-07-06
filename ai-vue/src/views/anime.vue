<template>
  <div class="anime-home">
    <div class="star-background" aria-hidden="true">
      <div class="hero-glow"></div>
      <i v-for="n in 6" :key="n"></i>
    </div>

    <transition name="loader-fade">
      <div v-if="loadingScreen" class="loader-wrapper">
        <img src="/anime-assets/ship.svg" alt="" />
      </div>
    </transition>

    <header class="site-header">
      <div class="top-clock">
        <span>{{ currentDateText }}</span>
        <strong>{{ currentTime }}</strong>
      </div>

      <div class="nav-art" aria-hidden="true"></div>

      <nav class="main-nav" aria-label="主导航">
        <span class="nav-spacer" aria-hidden="true"></span>
        <button class="home-link" type="button" @click="scrollTop">
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
          <button type="button" @click="openLinkGame">友情链接</button>
          <button type="button" @click="goAdmin">登录入口</button>
        </div>
      </nav>

      <section class="latest-news">
        <span class="latest-spacer" aria-hidden="true"></span>
        <div class="latest-title">最近更新</div>
        <div class="latest-track">
          <div class="latest-marquee">
            <button v-for="item in marqueeArticles" :key="`${item.id}-a`" type="button" @click="openArticle(item.id)">
              <i class="fa-solid fa-circle-arrow-right" aria-hidden="true"></i>
              {{ item.title }}
            </button>
            <button v-for="item in marqueeArticles" :key="`${item.id}-b`" type="button" @click="openArticle(item.id)">
              <i class="fa-solid fa-circle-arrow-right" aria-hidden="true"></i>
              {{ item.title }}
            </button>
          </div>
        </div>
      </section>
    </header>

    <main class="page-content">
      <section class="feature-row">
        <div class="main-feature">
          <el-carousel
            v-if="featuredArticles.length"
            height="500px"
            arrow="always"
            indicator-position="none"
            :interval="5200"
          >
            <el-carousel-item v-for="article in featuredArticles" :key="article.id">
              <article class="feature-card" @click="openArticle(article.id)">
                <img :src="getImage(article)" :alt="article.title" @error="handleImageError" />
                <div class="feature-bottom">
                  <span>{{ getCategoryName(article.categoryId) }}</span>
                  <h1>{{ article.title }}</h1>
                  <div class="meta-line">
                    <small>{{ formatDate(article.publishedAt || article.updatedAt) }}</small>
                    <b>ZG</b>
                  </div>
                </div>
              </article>
            </el-carousel-item>
          </el-carousel>

          <div v-else class="empty-feature">
            <h1>还没有发布文章</h1>
            <p>登录后台新增文章并发布后，这里会自动显示轮播。</p>
          </div>
        </div>

        <aside class="top-right-area">
          <div class="tab-panel">
            <div class="tab-head">
              <button
                v-for="tab in tabs"
                :key="tab.key"
                type="button"
                :class="{ active: activeTab === tab.key }"
                @click="changeTab(tab.key)"
              >
                <el-icon><component :is="tab.icon" /></el-icon>
                {{ tab.label }}
              </button>
            </div>

            <el-skeleton v-if="sideLoading" :rows="5" animated />
            <div v-else class="small-post-list">
              <button v-for="article in sideArticles" :key="article.id" type="button" @click="openArticle(article.id)">
                <img :src="getImage(article)" :alt="article.title" @error="handleImageError" />
                <span>{{ getCategoryName(article.categoryId) }}</span>
                <strong>{{ article.title }}</strong>
              </button>
              <p v-if="!sideArticles.length">暂无记录</p>
            </div>
          </div>
        </aside>
      </section>

      <section class="post-grid">
        <article v-for="article in articles" :key="article.id" class="post-card" @click="openArticle(article.id)">
          <img :src="getImage(article)" :alt="article.title" @error="handleImageError" />
          <div class="post-body">
            <span>{{ getCategoryName(article.categoryId) }}</span>
            <h2>{{ article.title }}</h2>
            <p>{{ article.summary || '这篇记录暂时没有摘要。' }}</p>
            <small>{{ formatDate(article.publishedAt || article.updatedAt) }} · ZG</small>
          </div>
        </article>
        <el-empty v-if="!articleLoading && !articles.length" description="暂无已发布文章" />
      </section>

      <div class="pager">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          background
          layout="prev, pager, next"
          :total="pagination.total"
          :page-size="pagination.size"
          @current-change="fetchArticles"
        />
      </div>
    </main>

    <footer class="site-footer" @mouseenter="startDanmaku" @mouseleave="stopDanmaku">
      <div class="footer-motto" aria-label="滚动短句">
        <span
          v-for="(item, index) in mottoChars"
          :key="`${item.char}-${index}`"
          :class="{ visible: item.visible }"
        >
          {{ item.char }}
        </span>
      </div>
      <div class="danmaku-wrap">
        <div class="danmaku-list">
          <span
            v-for="item in danmakuItems"
            :key="item.id"
            class="danmaku-item"
            :style="{
              color: item.color,
              top: item.top,
              animationDuration: `${item.duration}s`,
              animationDelay: `${item.delay}s`,
              opacity: item.leaving ? 0 : 0.9,
            }"
          >
            {{ item.text }}
          </span>
        </div>
      </div>
    </footer>

    <div v-if="confirmStep" class="confirm-overlay" @click="closeConfirm">
      <div class="confirm-box" :class="`step-${confirmStep}`" @click.stop>
        <p>{{ confirmText }}</p>
        <div class="confirm-actions" :class="{ dodge: confirmStep === 3 }">
          <button
            class="confirm-ok"
            type="button"
            :style="confirmOkStyle"
            @mouseenter="moveConfirmOk"
            @focus="moveConfirmOk"
            @click="nextConfirm"
          >
            {{ confirmOkText }}
          </button>
          <button class="confirm-cancel" type="button" @click="cancelConfirm">{{ confirmCancelText }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { dayjs } from 'element-plus'
import { useRouter } from 'vue-router'
import { Clock, DataLine, Star } from '@element-plus/icons-vue'
import { getArticleCategoryTree, getArticleList } from '@/api/frontend'
import { getArticleCover, resolveFileUrl } from '@/utils/fileUrl'

const router = useRouter()
const loadingScreen = ref(true)
const articleLoading = ref(false)
const sideLoading = ref(false)
const articles = ref([])
const sideArticles = ref([])
const categories = ref([])
const activeTab = ref('recent')
const now = ref(new Date())
const mobileMenuOpen = ref(false)
const confirmStep = ref(0)
const confirmOkOffset = reactive({ x: 0, y: 0 })
const mottoChars = ref([])
const danmakuItems = ref([])

let clockTimer = null
let mottoTimer = null
let danmakuTimer = null
let danmakuId = 0

const fallbackImages = ['/anime-assets/cover-rezero.jpg', '/anime-assets/cover-chunibyo.jpg']
const jumpUrl = 'https://blog.jinlizhijia.online/'

const filters = reactive({
  title: '',
  categoryId: '',
})

const pagination = reactive({
  currentPage: 1,
  size: 9,
  total: 0,
})

const tabs = [
  { key: 'recent', label: '最近', icon: Clock },
  { key: 'hot', label: '热门', icon: DataLine },
  { key: 'bangumi', label: '番剧', icon: Star },
]

const mottoList = [
  '保持热爱，奔赴山海',
  '星光不负赶路人',
  '努力成为更好的自己',
  '慢慢来，一切都来得及',
  '前路漫漫，亦有星光',
]

const danmakuTexts = [
  '慢慢来，一切都来得及',
  '保持热爱，奔赴山海',
  '星光不负赶路人',
  '前路漫漫，亦有星光',
  '努力成为更好的自己',
  '慢慢来，会更好',
  '你超棒的',
]

const danmakuColors = ['#64ffda', '#4fc3f7', '#81c784', '#ffb74d', '#f06292', '#ba68c8']

const currentTime = computed(() => dayjs(now.value).format('HH:mm:ss'))
const currentDateText = computed(() => {
  const weekMap = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${weekMap[now.value.getDay()]}. ${dayjs(now.value).format('M 月 D日, YYYY')}`
})

const marqueeArticles = computed(() => {
  return articles.value.length ? articles.value.slice(0, 6) : [
    { id: 'placeholder-1', title: 'RE：从零开始的异世界生活' },
    { id: 'placeholder-2', title: '中二病也要谈恋爱！' },
  ]
})

const featuredArticles = computed(() => articles.value.slice(0, 2))
const confirmText = computed(() => {
  if (confirmStep.value === 1) return '你确定要打开吗？'
  if (confirmStep.value === 2) return '你成年了吗？'
  return '你真的刚满十八岁吗？'
})
const confirmOkText = computed(() => {
  if (confirmStep.value === 2) return '是的'
  if (confirmStep.value === 3) return '包的'
  return '确定'
})
const confirmCancelText = computed(() => {
  if (confirmStep.value === 2) return '不是'
  if (confirmStep.value === 3) return '并非'
  return '取消'
})

const confirmOkStyle = computed(() => {
  if (confirmStep.value !== 3) return {}
  return {
    transform: `translate(${confirmOkOffset.x}px, ${confirmOkOffset.y}px)`,
  }
})

const cleanParams = (params) => {
  const output = { ...params }
  Object.keys(output).forEach((key) => {
    if (output[key] === '' || output[key] === null || output[key] === undefined) {
      delete output[key]
    }
  })
  return output
}

const scrollTop = () => window.scrollTo({ top: 0, behavior: 'smooth' })

const getImage = (article = {}) => {
  if (typeof article.id === 'string' && article.id.startsWith('placeholder')) {
    return fallbackImages[article.id.endsWith('2') ? 1 : 0]
  }
  const cover = getArticleCover(article)
  if (cover) return resolveFileUrl(cover)
  const index = Math.abs(Number(article.id || 0)) % fallbackImages.length
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

const fetchArticles = async () => {
  articleLoading.value = true
  try {
    const res = await getArticleList(cleanParams({
      currentPage: pagination.currentPage,
      size: pagination.size,
      status: 1,
      sortField: 'publishedAt',
      sortDirection: 'desc',
      title: filters.title.trim(),
      categoryId: filters.categoryId,
    }))
    const data = res?.data || res || {}
    articles.value = Array.isArray(data.records) ? data.records : []
    pagination.total = Number(data.total ?? articles.value.length)
    pagination.currentPage = Number(data.current ?? data.currentPage ?? pagination.currentPage)
  } catch (error) {
    console.error('Failed to load published articles:', error)
    articles.value = []
    pagination.total = 0
  } finally {
    articleLoading.value = false
  }
}

const fetchSideArticles = async () => {
  sideLoading.value = true
  try {
    const bangumiId = categories.value.find((item) => item.label.includes('番剧'))?.value
    const res = await getArticleList(cleanParams({
      currentPage: 1,
      size: 6,
      status: 1,
      sortField: activeTab.value === 'hot' ? 'readCount' : 'publishedAt',
      sortDirection: 'desc',
      categoryId: activeTab.value === 'bangumi' ? bangumiId : undefined,
    }))
    const data = res?.data || res || {}
    sideArticles.value = Array.isArray(data.records) ? data.records : []
  } catch (error) {
    console.error('Failed to load side articles:', error)
    sideArticles.value = []
  } finally {
    sideLoading.value = false
  }
}

const changeTab = (tab) => {
  activeTab.value = tab
  fetchSideArticles()
}

const openArticle = async (id) => {
  if (typeof id === 'string') return
  router.push(`/article/${id}`)
}

const goAdmin = () => {
  router.push({ path: '/auth/login', query: { redirect: '/user/dashboard', forceLogin: '1' } })
}

const animateMotto = async (text) => {
  mottoChars.value = text.split('').map((char) => ({ char, visible: false }))
  await nextTick()
  mottoChars.value.forEach((item, index) => {
    window.setTimeout(() => {
      item.visible = true
    }, index * 50)
  })
}

const fadeOutMotto = () => {
  mottoChars.value.forEach((item, index) => {
    window.setTimeout(() => {
      item.visible = false
    }, index * 30)
  })
}

const startMottoLoop = () => {
  let index = 0
  animateMotto(mottoList[index])
  mottoTimer = window.setInterval(() => {
    fadeOutMotto()
    window.setTimeout(() => {
      index = (index + 1) % mottoList.length
      animateMotto(mottoList[index])
    }, 900)
  }, 4800)
}

const createDanmaku = () => {
  const duration = Math.random() * 10 + 8
  const delay = Math.random() * 2
  const item = {
    id: danmakuId++,
    text: danmakuTexts[Math.floor(Math.random() * danmakuTexts.length)],
    color: danmakuColors[Math.floor(Math.random() * danmakuColors.length)],
    top: `${Math.floor(Math.random() * 3) * 15}%`,
    duration,
    delay,
    leaving: false,
  }
  danmakuItems.value.push(item)
  window.setTimeout(() => {
    danmakuItems.value = danmakuItems.value.filter((current) => current.id !== item.id)
  }, (duration + delay) * 1000)
}

const startDanmaku = () => {
  if (danmakuTimer) return
  createDanmaku()
  danmakuTimer = window.setInterval(createDanmaku, 1500)
}

const stopDanmaku = () => {
  if (danmakuTimer) {
    window.clearInterval(danmakuTimer)
    danmakuTimer = null
  }
  danmakuItems.value = danmakuItems.value.map((item) => ({ ...item, leaving: true }))
  window.setTimeout(() => {
    danmakuItems.value = []
  }, 650)
}

const openLinkGame = () => {
  router.push('/links')
}

const nextConfirm = () => {
  if (confirmStep.value < 3) {
    confirmStep.value += 1
    confirmOkOffset.x = 0
    confirmOkOffset.y = 0
    return
  }
  closeConfirm()
}

const moveConfirmOk = () => {
  if (confirmStep.value !== 3) return

  const candidates = [
    { x: -86, y: -34 },
    { x: 84, y: -30 },
    { x: -72, y: 36 },
    { x: 74, y: 34 },
    { x: 8, y: -48 },
  ].filter((item) => Math.abs(item.x - confirmOkOffset.x) + Math.abs(item.y - confirmOkOffset.y) > 38)

  const next = candidates[Math.floor(Math.random() * candidates.length)] || { x: 74, y: -34 }
  confirmOkOffset.x = next.x
  confirmOkOffset.y = next.y
}

const cancelConfirm = () => {
  if (confirmStep.value === 3) {
    window.location.href = jumpUrl
    return
  }
  closeConfirm()
}

const closeConfirm = () => {
  confirmStep.value = 0
  confirmOkOffset.x = 0
  confirmOkOffset.y = 0
}

onMounted(async () => {
  clockTimer = window.setInterval(() => {
    now.value = new Date()
  }, 1000)
  startMottoLoop()

  try {
    await fetchCategories()
    await Promise.allSettled([fetchArticles(), fetchSideArticles()])
  } finally {
    window.setTimeout(() => {
      loadingScreen.value = false
    }, 1000)
  }
})

onBeforeUnmount(() => {
  if (clockTimer) window.clearInterval(clockTimer)
  if (mottoTimer) window.clearInterval(mottoTimer)
  if (danmakuTimer) window.clearInterval(danmakuTimer)
})
</script>

<style scoped>
.anime-home {
  --gutter: 42px;
  --deep: #06101f;
  --nav: #233669;
  --panel: rgba(10, 25, 47, 0.5);
  --panel-strong: rgba(7, 20, 38, 0.92);
  --line: rgba(255, 255, 255, 0.1);
  --text: #e6f1ff;
  --muted: #98a9bf;
  --cyan: #64ffda;
  --blue: #0d6efd;

  position: relative;
  min-height: 100vh;
  overflow-x: hidden;
  color: var(--text);
  background:
    radial-gradient(circle at 50% 50%, #162340 0%, #0d1117 58%, #06101f 100%);
  font-family: "Microsoft YaHei", "HarmonyOS Sans SC", system-ui, sans-serif;
}

.anime-home,
.anime-home *,
.anime-home *::before,
.anime-home *::after {
  box-sizing: border-box;
}

:global(html) {
  scrollbar-color: #4a5568 #06101f;
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
  border-radius: 8px;
  border: 2px solid #06101f;
}

.anime-home::selection,
.anime-home *::selection {
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

.loader-wrapper {
  position: fixed;
  inset: 0;
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0a192f 0%, #112240 50%);
}

.loader-wrapper img {
  width: 300px;
  animation: shipBreath 1.5s ease-in-out infinite alternate;
}

.loader-fade-leave-active {
  transition: opacity 1s ease-out, visibility 1s ease-out;
}

.loader-fade-leave-to {
  opacity: 0;
  visibility: hidden;
}

.site-header,
.page-content,
.site-footer {
  position: relative;
  z-index: 1;
}

.top-clock {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  box-sizing: border-box;
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
  box-sizing: border-box;
  height: 42px;
  padding-left: 0;
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
  box-sizing: border-box;
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

.latest-marquee i {
  font-size: 14px;
}

.page-content {
  width: 100%;
  box-sizing: border-box;
  padding: 22px var(--gutter) 28px;
}

.feature-row {
  display: grid;
  grid-template-columns: minmax(0, 2.05fr) minmax(360px, 1fr);
  gap: 24px;
  margin-bottom: 24px;
}

.main-feature,
.tab-panel,
.post-card {
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  background: var(--panel);
  backdrop-filter: blur(8px);
  overflow: hidden;
}

.main-feature,
.post-card,
.small-post-list button {
  transition: border-color 0.22s ease, box-shadow 0.22s ease, transform 0.22s ease, background 0.22s ease;
}

.feature-card {
  position: relative;
  height: 100%;
  cursor: pointer;
}

.feature-card img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.95;
}

.feature-bottom {
  position: absolute;
  inset: auto 0 0;
  padding: 28px 42px;
  background: rgba(10, 25, 47, 0.42);
  backdrop-filter: blur(5px);
}

.feature-bottom span,
.post-body span,
.small-post-list span {
  color: var(--cyan);
  font-size: 14px;
  font-weight: 900;
}

.feature-bottom h1 {
  max-width: 920px;
  margin: 12px 0 16px;
  color: #fff;
  font-size: clamp(28px, 3.2vw, 40px);
  line-height: 1.15;
  font-weight: 950;
  transition: color 0.22s ease, text-shadow 0.22s ease;
}

.main-feature:hover,
.main-feature:focus-within,
.post-card:hover,
.post-card:focus-within {
  border-color: rgba(13, 110, 253, 0.65);
  box-shadow: 0 0 24px rgba(13, 110, 253, 0.18);
}

.main-feature:hover .feature-bottom h1,
.main-feature:focus-within .feature-bottom h1,
.post-card:hover .post-body h2,
.post-card:focus-within .post-body h2,
.small-post-list button:hover strong,
.small-post-list button:focus-visible strong {
  color: #339dff;
  text-shadow: 0 0 10px rgba(13, 110, 253, 0.42);
}

.meta-line {
  display: flex;
  align-items: center;
  gap: 18px;
  color: #dbeafe;
}

.meta-line b {
  font-size: 24px;
}

.empty-feature {
  display: grid;
  min-height: 500px;
  place-items: center;
  align-content: center;
  padding: 32px;
  text-align: center;
}

.tab-panel {
  height: 500px;
  padding: 10px;
}

.tab-head {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-bottom: 8px;
}

.tab-head button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 34px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(10, 25, 47, 0.4);
  color: #e6f1ff;
  cursor: pointer;
  font: inherit;
  font-weight: 850;
}

.tab-head button.active {
  background: linear-gradient(90deg, #64ffda 0%, #3b82f6 100%);
  color: #0a192f;
}

.small-post-list {
  display: grid;
  align-content: start;
  gap: 10px;
  height: 430px;
  overflow-y: auto;
}

.small-post-list button {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  gap: 4px 12px;
  align-items: center;
  min-height: 76px;
  padding: 8px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: rgba(10, 25, 47, 0.1);
  color: #fff;
  cursor: pointer;
  text-align: left;
}

.small-post-list button:hover,
.small-post-list button:focus-visible {
  outline: 0;
  background: rgba(13, 110, 253, 0.12);
  border-color: rgba(13, 110, 253, 0.45);
}

.small-post-list img {
  grid-row: span 2;
  width: 86px;
  height: 62px;
  border-radius: 4px;
  object-fit: cover;
}

.small-post-list strong {
  overflow: hidden;
  color: #fff;
  line-height: 1.35;
  text-overflow: ellipsis;
  transition: color 0.22s ease, text-shadow 0.22s ease;
}

.post-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}

.post-card {
  cursor: pointer;
}

.post-card img {
  display: block;
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.post-body {
  display: grid;
  gap: 10px;
  padding: 18px;
}

.post-body h2 {
  color: #fff;
  font-size: 22px;
  line-height: 1.35;
  transition: color 0.22s ease, text-shadow 0.22s ease;
}

.post-body p {
  display: -webkit-box;
  min-height: 48px;
  overflow: hidden;
  color: var(--muted);
  line-height: 1.65;
  overflow-wrap: anywhere;
  word-break: break-word;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.post-body small {
  color: #7f8ea4;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 26px;
}

.site-footer {
  width: 100%;
  box-sizing: border-box;
  padding: 22px 42px 0;
  background: rgba(10, 25, 47, 0.38);
}

.footer-motto {
  width: 100%;
  min-height: 74px;
  color: #64ffda;
  font-size: 40px;
  font-weight: 700;
  text-align: center;
  text-shadow:
    0 0 3px #64ffda,
    0 0 8px #64ffda,
    0 1px 2px rgba(0, 0, 0, 0.8);
  animation: breathe 4s ease-in-out infinite;
}

.footer-motto span {
  display: inline-block;
  opacity: 0;
  transform: translateY(12px);
  transition: all 0.6s cubic-bezier(0.25, 0.8, 0.5, 1);
}

.footer-motto span.visible {
  opacity: 1;
  transform: translateY(0);
}

.danmaku-wrap {
  position: relative;
  width: 100%;
  height: 50px;
  margin-top: 5px;
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.6s ease;
}

.site-footer:hover .danmaku-wrap {
  opacity: 1;
}

.danmaku-list {
  position: absolute;
  width: 100%;
  height: 100%;
}

.danmaku-item {
  position: absolute;
  left: 100%;
  white-space: nowrap;
  font-size: 16px;
  font-weight: 600;
  padding: 3px 10px;
  text-shadow: 0 0 5px currentColor;
  animation: danmakuMove linear forwards;
  transition: opacity 0.6s ease;
}

.confirm-overlay {
  position: fixed;
  inset: 0;
  z-index: 998;
  display: grid;
  place-items: center;
  background: rgba(0, 0, 0, 0.6);
}

.confirm-box {
  position: relative;
  min-width: 300px;
  padding: 28px 32px;
  overflow: hidden;
  border-radius: 16px;
  background: #0a192f;
  color: #e6f1ff;
  text-align: center;
  isolation: isolate;
  animation: confirmFadeIn 0.3s ease;
}

.confirm-box::before {
  position: absolute;
  top: -25%;
  left: -25%;
  z-index: 1;
  width: 150%;
  height: 150%;
  background-image: conic-gradient(transparent, #64ffda, #3b82f6, transparent 40%);
  content: "";
  animation: rotate 4.5s linear infinite;
}

.confirm-box::after {
  position: absolute;
  inset: 4px;
  z-index: 2;
  border-radius: 14px;
  background: #0a192f;
  content: "";
}

.confirm-box p,
.confirm-actions,
.confirm-box button {
  position: relative;
  z-index: 3;
}

.confirm-box p {
  margin-bottom: 22px;
}

.confirm-actions {
  display: flex;
  min-height: 46px;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.confirm-actions.dodge {
  min-width: 210px;
  min-height: 78px;
}

.confirm-box button {
  padding: 11px 22px;
  border: 0;
  border-radius: 8px;
  background: linear-gradient(90deg, #64ffda 0%, #3b82f6 100%);
  color: #fff;
  cursor: pointer;
  font-size: 15px;
  will-change: transform;
  transition:
    transform 0.26s cubic-bezier(0.2, 0.85, 0.25, 1),
    background 0.25s ease,
    font-weight 0.25s ease;
}

.confirm-box button:hover {
  background: linear-gradient(90deg, #3b82f6 0%, #64ffda 100%);
  font-weight: 800;
}

.confirm-box.step-2 {
  transform: translate(80px, -50px);
}

.confirm-box.step-3 {
  padding-bottom: 22px;
}

.confirm-box.step-3 .confirm-ok {
  z-index: 4;
}

@keyframes starMove {
  from { transform: rotate(0deg) scale(1); }
  to { transform: rotate(360deg) scale(1.05); }
}

@keyframes starAnimation {
  from { transform: translate(0, 0); }
  to { transform: translate(-50px, -30px); }
}

@keyframes shipBreath {
  from { opacity: 0.9; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1.05); }
}

@keyframes marquee {
  from { transform: translateX(0); }
  to { transform: translateX(-50%); }
}

@keyframes breathe {
  0%, 100% {
    text-shadow: 0 0 3px #64ffda, 0 0 8px #64ffda, 0 1px 2px rgba(0, 0, 0, 0.8);
  }
  50% {
    text-shadow: 0 0 5px #64ffda, 0 0 15px #64ffda, 0 2px 4px rgba(0, 0, 0, 0.8);
  }
}

@keyframes danmakuMove {
  from { transform: translateX(0); }
  to { transform: translateX(-120vw); }
}

@keyframes rotate {
  to { transform: rotate(360deg); }
}

@keyframes confirmFadeIn {
  from { opacity: 0; transform: translateY(18px) scale(0.96); }
  to { opacity: 1; transform: translate(0, 0); }
}

@media (max-width: 980px) {
  .anime-home {
    --gutter: 18px;
  }

  .feature-row,
  .post-grid {
    grid-template-columns: 1fr;
  }

  .page-content {
    padding: 18px;
  }
}

@media (max-width: 640px) {
  .anime-home {
    --gutter: 16px;
  }

  .top-clock {
    height: 42px;
    gap: 8px;
    padding-left: var(--gutter);
    font-size: 15px;
    white-space: nowrap;
  }

  .top-clock strong {
    padding: 3px 6px;
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
    justify-content: flex-start;
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
    gap: 0;
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

  .page-content {
    padding: 18px var(--gutter) 26px;
  }

  .feature-row {
    gap: 18px;
    margin-bottom: 20px;
  }

  .main-feature :deep(.el-carousel) {
    height: min(500px, calc(100dvh - 250px)) !important;
    min-height: 420px;
  }

  .feature-card img {
    height: 58%;
  }

  .feature-bottom {
    position: static;
    min-height: 42%;
    padding: 20px 20px 24px;
    background: rgba(7, 20, 38, 0.94);
    backdrop-filter: none;
  }

  .feature-bottom h1 {
    margin: 10px 0 12px;
    font-size: 27px;
    line-height: 1.22;
  }

  .meta-line {
    gap: 12px;
  }

  .meta-line b {
    font-size: 20px;
  }

  .empty-feature {
    min-height: 420px;
    padding: 28px 20px;
  }

  .empty-feature h1 {
    font-size: 32px;
    line-height: 1.2;
  }

  .empty-feature p {
    max-width: 18em;
    margin: 8px auto 0;
    line-height: 1.55;
  }

  .tab-panel {
    height: auto;
    padding: 10px;
  }

  .tab-head {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .tab-head button {
    min-height: 36px;
    font-size: 14px;
  }

  .small-post-list {
    max-height: 310px;
    height: auto;
  }

  .small-post-list button {
    grid-template-columns: 74px minmax(0, 1fr);
    min-height: 68px;
    padding: 8px;
  }

  .small-post-list img {
    width: 74px;
    height: 54px;
  }

  .post-grid {
    gap: 18px;
  }

  .post-card img {
    height: 180px;
  }

  .post-body {
    padding: 16px;
  }

  .post-body h2 {
    font-size: 21px;
  }

  .pager {
    overflow-x: auto;
    justify-content: flex-start;
    padding-bottom: 4px;
  }

  .site-footer {
    padding: 20px var(--gutter) 0;
  }

  .footer-motto {
    min-height: 58px;
    font-size: 25px;
  }

  .danmaku-wrap {
    display: none;
  }

  .confirm-box {
    width: calc(100vw - 36px);
    min-width: 0;
  }
}

@media (max-width: 420px) {
  .anime-home {
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

  .page-content {
    padding-top: 16px;
  }

  .main-feature :deep(.el-carousel) {
    min-height: 410px;
  }

  .feature-bottom h1 {
    font-size: 25px;
  }

  .post-card img {
    height: 170px;
  }
}
</style>
