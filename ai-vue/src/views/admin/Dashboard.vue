<template>
  <section class="admin-page dashboard-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Overview</p>
        <h1>仪表盘</h1>
        <span>记录站运营概览，快速查看内容、友链、资源和用户数据。</span>
      </div>
      <el-button :loading="loading" @click="fetchSummary">刷新数据</el-button>
    </div>

    <div class="metric-grid">
      <article v-for="item in metrics" :key="item.label" class="metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </article>
    </div>

    <div class="panel-grid">
      <section class="panel">
        <div class="panel-head">
          <h2>最近发布</h2>
          <span>Published</span>
        </div>
        <ul v-if="recentPublished.length" class="recent-list">
          <li v-for="article in recentPublished" :key="article.id" @click="router.push(`/article/${article.id}`)">
            <div>
              <strong>{{ article.title }}</strong>
              <span>{{ formatDate(article.publishedAt || article.updatedAt) }} · {{ article.authorName || 'ZG' }}</span>
            </div>
            <small>{{ article.readCount || 0 }} reads</small>
          </li>
        </ul>
        <el-empty v-else description="暂无已发布文章" />
      </section>

      <section class="panel">
        <div class="panel-head">
          <h2>模块入口</h2>
          <span>Console</span>
        </div>
        <div class="module-list">
          <button v-for="module in modules" :key="module.path" type="button" @click="router.push(module.path)">
            <strong>{{ module.title }}</strong>
            <span>{{ module.desc }}</span>
          </button>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { dashboardSummary } from '@/api/admin'

const router = useRouter()
const loading = ref(false)
const summary = ref({
  articleCount: 0,
  publishedCount: 0,
  draftCount: 0,
  pendingFriendLinkCount: 0,
  resourceCount: 0,
  userCount: 0,
  totalReadCount: 0,
  recentPublished: [],
})

const metrics = computed(() => [
  { label: '文章总数', value: summary.value.articleCount, hint: '后台已创建的全部记录文章' },
  { label: '已发布', value: summary.value.publishedCount, hint: '前台可见文章数量' },
  { label: '草稿数', value: summary.value.draftCount, hint: '待继续编辑或待发布' },
  { label: '友链申请', value: summary.value.pendingFriendLinkCount, hint: '等待审核的网站申请' },
  { label: '资源数', value: summary.value.resourceCount, hint: '资源模块已登记条目' },
  { label: '用户数', value: summary.value.userCount, hint: '后台与普通账号总量' },
  { label: '总阅读', value: summary.value.totalReadCount, hint: '已记录文章阅读累计' },
])

const recentPublished = computed(() => Array.isArray(summary.value.recentPublished) ? summary.value.recentPublished : [])

const modules = [
  { title: '记录文章', desc: '维护文章、分类、发布状态和封面', path: '/user/articles' },
  { title: '友链管理', desc: '审核申请、排序、分类和站点 LOGO', path: '/user/links' },
  { title: '资源管理', desc: '管理番剧资料、网盘链接和分类', path: '/user/resources' },
  { title: '用户管理', desc: '管理账号、角色和下载权限基础', path: '/user/users' },
]

const fetchSummary = async () => {
  loading.value = true
  try {
    const res = await dashboardSummary()
    summary.value = {
      ...summary.value,
      ...(res?.data || {}),
    }
  } finally {
    loading.value = false
  }
}

const formatDate = (value) => {
  if (!value) return '未记录时间'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 10)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
}

onMounted(fetchSummary)
</script>

<style lang="scss" scoped>
.admin-page {
  display: grid;
  gap: 22px;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;

  .eyebrow {
    margin-bottom: 8px;
    color: #169b86;
    font-size: 12px;
    font-weight: 900;
    text-transform: uppercase;
  }

  h1 {
    margin: 0 0 8px;
    color: #111827;
    font-size: 28px;
  }

  span {
    color: #64748b;
  }
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.metric-card,
.panel,
.module-list button {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.metric-card {
  display: grid;
  gap: 8px;
  padding: 18px;

  span,
  small {
    color: #64748b;
  }

  strong {
    color: #111827;
    font-size: 30px;
  }
}

.panel-grid {
  display: grid;
  grid-template-columns: 1fr 1.1fr;
  gap: 16px;
}

.panel {
  padding: 18px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;

  h2 {
    color: #111827;
    font-size: 18px;
  }

  span {
    color: #94a3b8;
    font-size: 12px;
    font-weight: 800;
    text-transform: uppercase;
  }
}

.recent-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;

  li {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 14px;
    padding: 12px;
    border: 1px solid #edf2f7;
    border-radius: 8px;
    cursor: pointer;
    transition: border-color 0.2s ease, transform 0.2s ease;

    &:hover {
      border-color: #64ffda;
      transform: translateY(-1px);
    }
  }

  div {
    display: grid;
    gap: 4px;
    min-width: 0;
  }

  strong {
    overflow: hidden;
    color: #111827;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span,
  small {
    color: #64748b;
    font-size: 12px;
  }
}

.module-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.module-list button {
  display: grid;
  gap: 8px;
  padding: 16px;
  color: #334155;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;

  strong {
    color: #111827;
    font-size: 16px;
  }

  span {
    color: #64748b;
    line-height: 1.5;
  }

  &:hover {
    border-color: #64ffda;
    box-shadow: 0 10px 26px rgba(15, 23, 42, 0.08);
    transform: translateY(-2px);
  }
}

@media (max-width: 1200px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .metric-grid,
  .panel-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .metric-grid,
  .panel-grid,
  .module-list {
    grid-template-columns: 1fr;
  }
}
</style>
