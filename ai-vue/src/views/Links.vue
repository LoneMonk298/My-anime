<template>
  <div class="links-page">
    <div class="link-bg" aria-hidden="true">
      <i v-for="n in 5" :key="n"></i>
    </div>

    <aside class="link-sidebar" aria-label="友链分类">
      <h2><span></span>网址导航</h2>
      <button
        v-for="group in filteredGroups"
        :key="group.key"
        type="button"
        :class="{ active: activeGroup === group.key }"
        @click="scrollToGroup(group.key)"
      >
        {{ group.title }}
      </button>
      <button class="apply-btn" type="button" @click="applyVisible = true">申请入驻</button>
    </aside>

    <main class="links-main">
      <section class="links-hero">
        <button class="back-home" type="button" @click="router.push('/')">返回首页</button>
        <h1>网址导航 · 友情链接</h1>
        <div class="tag-row">
          <button
            v-for="tag in tags"
            :key="tag"
            type="button"
            :class="{ active: activeTag === tag }"
            @click="changeTag(tag)"
          >
            {{ tag }}
          </button>
        </div>
        <form class="search-box" @submit.prevent="fetchLinks">
          <input v-model.trim="keyword" type="search" placeholder="站内搜索" />
          <button type="submit">搜索</button>
        </form>
      </section>

      <section
        v-for="group in filteredGroups"
        :id="`link-${group.key}`"
        :key="group.key"
        class="link-section"
      >
        <header>
          <h2>{{ group.title }}</h2>
          <span>({{ group.items.length }})</span>
        </header>

        <div class="link-list">
          <article v-for="item in group.items" :key="item.name" class="link-card">
            <img :src="item.icon" :alt="item.name" @error="handleIconError" />
            <div>
              <h3>{{ item.name }}</h3>
              <p>{{ item.desc }}</p>
              <small>访问：{{ item.visits }} 次 <b>{{ renderRating(item.rating) }}</b></small>
            </div>
            <div class="link-actions">
              <button type="button" @click="selectedLink = item">详情</button>
              <a :href="item.url" target="_blank" rel="noreferrer" @click.prevent="openLink(item)">直达</a>
            </div>
          </article>
        </div>
      </section>

      <el-empty v-if="!loading && !filteredGroups.length" description="没有找到相关友链" />
    </main>

    <div v-if="selectedLink" class="link-modal" @click.self="selectedLink = null">
      <article>
        <button class="close-modal" type="button" @click="selectedLink = null">×</button>
        <img :src="selectedLink.icon" :alt="selectedLink.name" @error="handleIconError" />
        <h2>{{ selectedLink.name }}</h2>
        <p>{{ selectedLink.desc }}</p>
        <dl>
          <dt>分类</dt>
          <dd>{{ selectedLink.group }}</dd>
          <dt>评分</dt>
          <dd>{{ renderRating(selectedLink.rating) }}</dd>
          <dt>访问</dt>
          <dd>{{ selectedLink.visits }} 次</dd>
        </dl>
        <a :href="selectedLink.url" target="_blank" rel="noreferrer" @click.prevent="openLink(selectedLink)">打开网站</a>
      </article>
    </div>

    <div v-if="applyVisible" class="apply-modal" @click.self="applyVisible = false">
      <form class="apply-card" @submit.prevent="submitApply">
        <header>
          <h2><span>✎</span>申请入驻导航</h2>
          <button type="button" @click="applyVisible = false">×</button>
        </header>
        <p class="apply-tip">填写您的网站信息，审核通过后将展示在导航页中，请勿提交违规内容</p>
        <div class="apply-grid">
          <label>
            网站名称（必填）
            <input v-model.trim="applyForm.name" required placeholder="请输入网站名称" />
          </label>
          <label>
            网站地址（必填）
            <input v-model.trim="applyForm.url" required placeholder="https://..." />
          </label>
        </div>
        <label>
          网站简介
          <input v-model.trim="applyForm.description" placeholder="一句话介绍网站" />
        </label>
        <label>
          网站类别
          <select v-model="applyForm.category">
            <option v-for="category in categories" :key="category" :value="category">{{ category }}</option>
          </select>
        </label>
        <label>
          LOGO 图像
          <div class="logo-picker" @click="logoInput?.click()">
            <img v-if="logoPreview" :src="logoPreview" alt="LOGO 预览" />
            <span v-else>＋</span>
          </div>
          <input ref="logoInput" class="file-input" type="file" accept="image/*" @change="handleLogoPick" />
          <small>请选择正方形 LOGO 图像，支持 jpg/png/gif 格式。公开申请暂不上传文件，审核时可在后台补图。</small>
        </label>
        <footer>
          <button type="submit" :disabled="submitting">✔ 确认提交</button>
        </footer>
      </form>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { applyFriendLink, getFriendLinks, recordFriendLinkVisit } from '@/api/frontend'
import { resolveFileUrl } from '@/utils/fileUrl'

const router = useRouter()
const keyword = ref('')
const activeTag = ref('常用')
const activeGroup = ref('anime')
const selectedLink = ref(null)
const applyVisible = ref(false)
const submitting = ref(false)
const loading = ref(false)
const links = ref([])
const logoInput = ref(null)
const logoPreview = ref('')

const tags = ['常用', '工具', '社区', '灵感']
const categories = ['动漫', '博客', '娱乐', '导航', '工具', '公益']
const fallbackIcon = '/anime-assets/liuhuaa.ico'
const sampleLinks = [
  {
    id: 'sample-1',
    name: '哔哩哔哩',
    url: 'https://www.bilibili.com/',
    description: '综合视频社区，番剧、动画、游戏与生活内容都很全。',
    category: '动漫',
    tag: '常用',
    logoUrl: 'https://www.bilibili.com/favicon.ico',
    visits: 32,
    rating: 5,
  },
  {
    id: 'sample-2',
    name: '稀饭动漫',
    url: 'https://xifan.moe',
    description: '动漫资源与追番入口，适合收藏备用。',
    category: '动漫',
    tag: '常用',
    logoUrl: '/anime-assets/liuhuaa.ico',
    visits: 18,
    rating: 4,
  },
  {
    id: 'sample-3',
    name: '槿篱之家',
    url: 'https://blog.jinlizhijia.online/',
    description: '游戏盒与博客综合网站。',
    category: '博客',
    tag: '社区',
    logoUrl: '/anime-assets/liuhuaa.ico',
    visits: 21,
    rating: 4,
  },
  {
    id: 'sample-4',
    name: 'TinyPNG',
    url: 'https://tinypng.com/',
    description: '图片压缩工具，上传封面前可以先压缩。',
    category: '工具',
    tag: '工具',
    logoUrl: 'https://tinypng.com/images/apple-touch-icon.png',
    visits: 12,
    rating: 4,
  },
  {
    id: 'sample-5',
    name: 'Pixiv',
    url: 'https://www.pixiv.net/',
    description: '插画、角色与视觉灵感收集地。',
    category: '灵感',
    tag: '灵感',
    logoUrl: 'https://www.pixiv.net/favicon.ico',
    visits: 25,
    rating: 5,
  },
]

const applyForm = reactive({
  name: '',
  url: '',
  description: '',
  category: '娱乐',
})

const filteredGroups = computed(() => {
  const map = new Map()
  links.value.forEach((item) => {
    const category = item.category || '娱乐'
    if (!map.has(category)) {
      map.set(category, {
        key: category,
        title: category,
        items: [],
      })
    }
    map.get(category).items.push({
      ...item,
      desc: item.description,
      icon: resolveFileUrl(item.logoUrl || fallbackIcon),
      group: category,
    })
  })
  return Array.from(map.values())
})

const scrollToGroup = (key) => {
  activeGroup.value = key
  document.querySelector(`#link-${key}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const changeTag = async (tag) => {
  activeTag.value = tag
  await fetchLinks()
}

const fetchLinks = async () => {
  loading.value = true
  try {
    const res = await getFriendLinks({
      keyword: keyword.value,
      tag: activeTag.value,
    })
    const data = Array.isArray(res?.data) ? res.data : []
    links.value = data.length ? data : sampleLinks.filter((item) => activeTag.value === '常用' || item.tag === activeTag.value)
    if (filteredGroups.value.length && !filteredGroups.value.some((group) => group.key === activeGroup.value)) {
      activeGroup.value = filteredGroups.value[0].key
    }
  } catch (error) {
    console.error('Failed to load friend links:', error)
    links.value = sampleLinks.filter((item) => activeTag.value === '常用' || item.tag === activeTag.value)
  } finally {
    loading.value = false
  }
}

const submitApply = async () => {
  submitting.value = true
  try {
    await applyFriendLink({
      ...applyForm,
      tag: '常用',
    })
    ElMessage.success('申请已提交，等待站长审核')
    applyVisible.value = false
    Object.assign(applyForm, {
      name: '',
      url: '',
      description: '',
      category: '娱乐',
    })
    logoPreview.value = ''
  } catch (error) {
    ElMessage.error(error.message || '申请提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const handleLogoPick = (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    return
  }
  logoPreview.value = URL.createObjectURL(file)
}

const handleIconError = (event) => {
  event.target.src = fallbackIcon
}

const openLink = async (item) => {
  if (!item?.url) return
  window.open(item.url, '_blank', 'noopener,noreferrer')
  if (!Number.isFinite(Number(item.id))) return
  item.visits = Number(item.visits || 0) + 1
  try {
    await recordFriendLinkVisit(item.id)
  } catch (error) {
    console.warn('Failed to record friend link visit:', error)
  }
}

const renderRating = (rating = 4) => {
  const value = Math.max(0, Math.min(5, Number(rating) || 0))
  return `${'★'.repeat(value)}${'☆'.repeat(5 - value)}`
}

onMounted(fetchLinks)
</script>

<style scoped>
.links-page {
  min-height: 100vh;
  position: relative;
  display: grid;
  grid-template-columns: 180px minmax(0, 1000px);
  gap: 22px;
  justify-content: center;
  padding: 28px 26px 60px;
  color: #edf4ff;
  background:
    linear-gradient(135deg, rgba(80, 63, 91, 0.68), rgba(22, 32, 42, 0.86)),
    url('/anime-assets/background.png') center / cover fixed;
  font-family: "Microsoft YaHei", "HarmonyOS Sans SC", system-ui, sans-serif;
}

.links-page,
.links-page *,
.links-page *::before,
.links-page *::after {
  box-sizing: border-box;
}

.link-bg,
.link-bg i {
  position: fixed;
  inset: 0;
  pointer-events: none;
}

.link-bg {
  z-index: 0;
  overflow: hidden;
  background: radial-gradient(circle at 12% 22%, rgba(255, 117, 174, 0.18), transparent 28%),
    radial-gradient(circle at 85% 12%, rgba(100, 255, 218, 0.12), transparent 30%),
    rgba(7, 11, 18, 0.42);
}

.link-bg i {
  background:
    radial-gradient(1px 1px at 30px 50px, #fff, transparent),
    radial-gradient(2px 2px at 240px 120px, rgba(100, 255, 218, 0.8), transparent),
    radial-gradient(1px 1px at 420px 260px, #fff, transparent);
  background-size: 520px 360px;
  opacity: 0.25;
  animation: starDrift 24s linear infinite;
}

.link-bg i:nth-child(2) { animation-duration: 28s; opacity: 0.18; }
.link-bg i:nth-child(3) { animation-duration: 20s; opacity: 0.2; }
.link-bg i:nth-child(4) { animation-duration: 32s; opacity: 0.16; }
.link-bg i:nth-child(5) { animation-duration: 18s; opacity: 0.14; }

.link-sidebar,
.links-main {
  position: relative;
  z-index: 1;
}

.link-sidebar {
  position: sticky;
  top: 28px;
  align-self: start;
  display: grid;
  gap: 8px;
  padding: 14px 10px;
  border: 1px solid rgba(255, 255, 255, 0.11);
  border-radius: 10px;
  background: rgba(38, 41, 48, 0.72);
  backdrop-filter: blur(14px);
}

.link-sidebar h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 10px;
  padding: 0 8px;
  font-size: 16px;
}

.link-sidebar h2 span {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #6d83ff;
  box-shadow: 0 0 12px rgba(109, 131, 255, 0.8);
}

.link-sidebar button,
.tag-row button,
.back-home,
.search-box button,
.link-actions button,
.link-actions a,
.link-modal a {
  border: 1px solid rgba(130, 150, 255, 0.35);
  border-radius: 7px;
  background: rgba(65, 70, 88, 0.65);
  color: #edf4ff;
  cursor: pointer;
  font: inherit;
  font-weight: 800;
  text-decoration: none;
  transition: border-color 0.18s ease, background 0.18s ease, transform 0.18s ease;
}

.link-sidebar button {
  height: 38px;
  text-align: left;
  padding: 0 14px;
}

.link-sidebar button.active,
.tag-row button.active,
.link-actions a,
.search-box button {
  background: #6978e8;
  border-color: #89a0ff;
}

.apply-btn {
  margin-top: 14px;
  text-align: center !important;
}

.links-main {
  display: grid;
  gap: 20px;
}

.links-hero,
.link-section {
  border: 1px solid rgba(255, 255, 255, 0.11);
  border-radius: 10px;
  background: rgba(49, 51, 60, 0.72);
  box-shadow: 0 18px 80px rgba(0, 0, 0, 0.24);
  backdrop-filter: blur(16px);
}

.links-hero {
  position: relative;
  padding: 26px clamp(18px, 5vw, 52px);
  text-align: center;
}

.back-home {
  position: absolute;
  left: clamp(18px, 5vw, 52px);
  top: 28px;
  height: 34px;
  padding: 0 12px;
}

.links-hero h1 {
  margin: 0 0 18px;
  font-size: 28px;
  line-height: 1.2;
}

.tag-row {
  display: flex;
  justify-content: center;
  gap: 14px;
  margin-bottom: 18px;
}

.tag-row button {
  height: 34px;
  padding: 0 14px;
}

.search-box {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 90px;
  width: min(600px, 100%);
  height: 48px;
  margin: 0 auto;
}

.search-box input {
  min-width: 0;
  padding: 0 18px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-right: 0;
  border-radius: 9px 0 0 9px;
  outline: 0;
  background: rgba(44, 45, 54, 0.82);
  color: #fff;
  font: inherit;
  font-weight: 700;
}

.search-box button {
  border-radius: 0 9px 9px 0;
}

.link-section {
  scroll-margin-top: 18px;
  padding: 18px;
}

.link-section header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.09);
}

.link-section h2 {
  position: relative;
  margin: 0;
  padding-left: 12px;
  font-size: 20px;
}

.link-section h2::before {
  position: absolute;
  left: 0;
  top: 4px;
  width: 3px;
  height: 18px;
  border-radius: 999px;
  background: #89a0ff;
  content: "";
}

.link-section header span {
  color: #98a9bf;
  font-weight: 800;
}

.link-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.link-card {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  min-height: 86px;
  padding: 12px 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  background: rgba(38, 43, 48, 0.64);
}

.link-card img,
.link-modal img {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  background: rgba(255, 255, 255, 0.1);
}

.link-card h3 {
  margin: 0 0 4px;
  font-size: 17px;
}

.link-card p {
  margin: 0;
  color: #98a9bf;
  line-height: 1.6;
}

.link-card small {
  display: block;
  margin-top: 2px;
  color: #98a9bf;
}

.link-card b {
  color: #ffd60a;
}

.link-actions {
  display: flex;
  gap: 8px;
}

.link-actions button,
.link-actions a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 34px;
}

.link-sidebar button:hover,
.tag-row button:hover,
.back-home:hover,
.search-box button:hover,
.link-actions button:hover,
.link-actions a:hover,
.link-modal a:hover {
  transform: translateY(-1px);
  border-color: #9ee7ff;
}

.link-modal {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(0, 0, 0, 0.58);
}

.link-modal article {
  position: relative;
  width: min(420px, 100%);
  padding: 28px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 14px;
  background: rgba(32, 35, 46, 0.96);
  text-align: center;
}

.close-modal {
  position: absolute;
  top: 10px;
  right: 12px;
  border: 0;
  background: transparent;
  color: #fff;
  cursor: pointer;
  font-size: 28px;
}

.link-modal h2 {
  margin: 12px 0 8px;
}

.link-modal p {
  color: #c7d6eb;
  line-height: 1.7;
}

.link-modal dl {
  display: grid;
  grid-template-columns: 70px minmax(0, 1fr);
  gap: 8px 12px;
  margin: 18px 0;
  text-align: left;
}

.link-modal dt {
  color: #98a9bf;
}

.link-modal dd {
  margin: 0;
}

.link-modal a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 38px;
  padding: 0 18px;
  background: #6978e8;
}

.apply-modal {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: grid;
  place-items: center;
  padding: 18px;
  background: rgba(4, 6, 10, 0.62);
}

.apply-card {
  width: min(600px, 100%);
  padding: 0 16px 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  background: #303133;
  color: #e6edf7;
  box-shadow: 0 22px 80px rgba(0, 0, 0, 0.42);
}

.apply-card header {
  display: flex;
  height: 58px;
  align-items: center;
  justify-content: space-between;
}

.apply-card h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 18px;
}

.apply-card h2 span {
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border-radius: 50%;
  background: #6978e8;
  color: #fff;
  font-size: 14px;
}

.apply-card header button {
  border: 0;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  font-size: 28px;
}

.apply-tip {
  margin: 0 0 14px;
  padding: 14px 16px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  color: #f2f5fb;
  font-size: 13px;
  font-weight: 800;
}

.apply-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.apply-card label {
  display: grid;
  gap: 8px;
  margin-bottom: 14px;
  color: #aeb5c2;
  font-size: 13px;
  font-weight: 850;
}

.apply-card input,
.apply-card select {
  width: 100%;
  height: 36px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 4px;
  outline: 0;
  background: #242529;
  color: #edf4ff;
  font: inherit;
}

.logo-picker {
  display: grid;
  width: 140px;
  height: 140px;
  place-items: center;
  border: 1px dashed rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  cursor: pointer;
}

.logo-picker span {
  color: rgba(255, 255, 255, 0.18);
  font-size: 28px;
  font-weight: 300;
}

.logo-picker img {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  object-fit: cover;
}

.file-input {
  display: none;
}

.apply-card small {
  color: #aeb5c2;
  font-size: 12px;
}

.apply-card footer {
  display: flex;
  justify-content: flex-end;
}

.apply-card footer button {
  min-width: 132px;
  height: 38px;
  border: 1px solid rgba(235, 91, 167, 0.35);
  border-radius: 5px;
  background: rgba(111, 49, 82, 0.72);
  color: #ff8fd0;
  cursor: pointer;
  font: inherit;
  font-weight: 950;
}

.apply-card footer button:disabled {
  cursor: wait;
  opacity: 0.62;
}

@keyframes starDrift {
  from { transform: translate(0, 0); }
  to { transform: translate(-44px, -34px); }
}

@media (max-width: 760px) {
  .links-page {
    display: block;
    padding: 14px 14px 42px;
  }

  .link-sidebar {
    position: static;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    margin-bottom: 14px;
  }

  .link-sidebar h2 {
    grid-column: 1 / -1;
  }

  .link-sidebar button {
    height: 34px;
    padding: 0 8px;
    text-align: center;
    font-size: 13px;
  }

  .apply-btn {
    grid-column: 1 / -1;
    margin-top: 6px;
  }

  .links-hero {
    padding: 20px 16px;
  }

  .back-home {
    position: static;
    margin-bottom: 14px;
  }

  .links-hero h1 {
    font-size: 24px;
  }

  .tag-row {
    gap: 8px;
  }

  .tag-row button {
    flex: 1;
    padding: 0 8px;
  }

  .search-box {
    grid-template-columns: minmax(0, 1fr) 76px;
  }

  .link-section {
    padding: 14px;
  }

  .link-card {
    grid-template-columns: 46px minmax(0, 1fr);
    align-items: start;
  }

  .link-card img {
    width: 42px;
    height: 42px;
  }

  .link-actions {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }

  .apply-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .logo-picker {
    width: 118px;
    height: 118px;
  }
}
</style>
