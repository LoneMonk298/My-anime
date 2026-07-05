<template>
  <section class="admin-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Resources</p>
        <h1>资源管理</h1>
        <span>用于管理番剧资料、网盘链接、分类、提取码和下载权限。</span>
      </div>
      <el-button type="primary" disabled>新增资源</el-button>
    </div>

    <el-alert
      title="当前为路由骨架页，资源表结构和接口会在下一步补齐。"
      type="info"
      :closable="false"
      show-icon
    />

    <div class="toolbar">
      <el-input placeholder="搜索资源标题" disabled />
      <el-select placeholder="资源类型" disabled>
        <el-option label="番剧" value="anime" />
      </el-select>
      <el-select placeholder="状态" disabled>
        <el-option label="启用" value="enabled" />
      </el-select>
    </div>

    <el-table :data="[]" border>
      <el-table-column prop="title" label="资源标题" min-width="220" />
      <el-table-column prop="category" label="分类" width="140" />
      <el-table-column prop="type" label="类型" width="120" />
      <el-table-column prop="needLogin" label="下载权限" width="140" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right" />
    </el-table>
  </section>
</template>

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

.toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 320px) 180px 160px;
  gap: 12px;
}
</style>
