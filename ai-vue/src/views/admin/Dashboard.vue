<template>
  <section class="admin-page dashboard-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Overview</p>
        <h1>仪表盘</h1>
        <span>记录站运营概览，后续会接入文章、资源、用户和下载数据。</span>
      </div>
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
          <h2>近期待办</h2>
          <span>Roadmap</span>
        </div>
        <ul class="task-list">
          <li v-for="task in tasks" :key="task">{{ task }}</li>
        </ul>
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
import { useRouter } from 'vue-router'

const router = useRouter()

const metrics = [
  { label: '文章总数', value: '--', hint: '待接入文章统计接口' },
  { label: '资源数量', value: '--', hint: '待新增资源表' },
  { label: '注册用户', value: '--', hint: '后续用于下载权限' },
  { label: '总阅读量', value: '--', hint: '用于观察内容热度' },
]

const tasks = [
  '完善资源管理表结构和后端 CRUD',
  '接入用户列表、禁用和角色管理',
  '补充资源下载权限和访问统计',
  '为仪表盘增加趋势图与最近动态',
]

const modules = [
  { title: '记录文章', desc: '维护文章、分类、发布状态和封面', path: '/user/articles' },
  { title: '资源管理', desc: '管理番剧资料、网盘链接和分类', path: '/user/resources' },
  { title: '用户管理', desc: '管理账号、角色和下载权限基础', path: '/user/users' },
  { title: '系统设置', desc: '站点配置、公告和后续开关项', path: '/user/settings' },
]
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
  grid-template-columns: 1fr 1.2fr;
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

.task-list {
  display: grid;
  gap: 12px;
  padding-left: 18px;
  color: #334155;
  line-height: 1.7;
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

@media (max-width: 1100px) {
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
