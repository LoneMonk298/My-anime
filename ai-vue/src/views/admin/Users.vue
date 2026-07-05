<template>
  <section class="admin-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Users</p>
        <h1>用户管理</h1>
        <span>为后续登录下载、资源权限和 AI 模拟对话权限做账号基础。</span>
      </div>
      <el-button type="primary" disabled>新增用户</el-button>
    </div>

    <div class="summary-grid">
      <article v-for="item in summary" :key="item.label">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <el-table :data="[]" border>
      <el-table-column prop="username" label="用户名" min-width="180" />
      <el-table-column prop="email" label="邮箱" min-width="220" />
      <el-table-column prop="role" label="角色" width="140" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="downloadLimit" label="下载限制" width="140" />
      <el-table-column prop="createdAt" label="注册时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right" />
    </el-table>
  </section>
</template>

<script setup>
const summary = [
  { label: '总用户', value: '--' },
  { label: '普通用户', value: '--' },
  { label: '管理员', value: '--' },
  { label: '禁用账号', value: '--' },
]
</script>

<style lang="scss" scoped>
.admin-page {
  display: grid;
  gap: 18px;
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

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;

  article {
    display: grid;
    gap: 6px;
    padding: 16px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: #fff;
  }

  span {
    color: #64748b;
  }

  strong {
    color: #111827;
    font-size: 26px;
  }
}
</style>
