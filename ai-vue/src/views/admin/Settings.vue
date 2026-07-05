<template>
  <section class="admin-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Settings</p>
        <h1>系统设置</h1>
        <span>先预留站点配置入口，后续可放公告、下载开关、注册开关和 SEO 配置。</span>
      </div>
    </div>

    <div class="setting-grid">
      <section v-for="group in groups" :key="group.title" class="setting-card">
        <h2>{{ group.title }}</h2>
        <p>{{ group.desc }}</p>
        <el-button disabled>等待接入</el-button>
      </section>
    </div>
  </section>
</template>

<script setup>
const groups = [
  { title: '站点信息', desc: '站点名称、备案信息、前台公告和默认封面。' },
  { title: '资源策略', desc: '下载是否需要登录、单日下载次数和资源展示开关。' },
  { title: '注册策略', desc: '注册开放状态、验证码策略和账号默认角色。' },
  { title: 'AI 功能', desc: '后续 AI 角色模拟对话的模型、角色和权限配置。' },
]
</script>

<style lang="scss" scoped>
.admin-page {
  display: grid;
  gap: 18px;
}

.page-head {
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

.setting-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.setting-card {
  display: grid;
  gap: 12px;
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;

  h2 {
    color: #111827;
    font-size: 18px;
  }

  p {
    color: #64748b;
    line-height: 1.7;
  }
}
</style>
