<template>
  <div class="article-admin-page">
    <PageHead page-title="记录站文章管理">
      <template #buttons>
        <el-button type="primary" @click="handleEdit({})">新增文章</el-button>
      </template>
    </PageHead>

    <Tabelserch :form-item="formItem" @search="handleSearch" />

    <el-table v-loading="loading" :data="tableData" class="article-table">
      <el-table-column label="文章标题" fixed="left" min-width="280">
        <template #default="{ row }">
          <div class="title-cell">
            <el-icon><Timer /></el-icon>
            <span>{{ row.title }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="分类" width="160">
        <template #default="{ row }">
          {{ categoryMap[row.categoryId] || row.categoryId || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="authorName" label="作者" width="150" />
      <el-table-column prop="readCount" label="阅读量" width="120" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTypeMap[row.status] || 'info'" effect="light">
            {{ statusMap[row.status] || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.updatedAt || row.publishedAt || row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button
            v-if="row.status === 0 || row.status === 2"
            text
            type="success"
            @click="handleStatusChange(row, 1)"
          >
            发布
          </el-button>
          <el-button v-if="row.status === 1" text type="warning" @click="handleStatusChange(row, 2)">
            下线
          </el-button>
          <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-info">
      <el-pagination
        v-model:current-page="pagination.currentPage"
        background
        layout="total, prev, pager, next, jumper"
        :total="pagination.total"
        :page-size="pagination.size"
        @current-change="handleChange"
      />
    </div>

    <ArticleDialog
      v-model:model-value="dialogVisible"
      :article="currentArticle"
      :categories="categories"
      @success="handleSubmit"
    />
  </div>
</template>

<script setup>
import { dayjs, ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { Timer } from '@element-plus/icons-vue'
import {
  articleList,
  categoryTree,
  changeArticleStatus,
  deleteArticle,
  getArticleDetail,
} from '@/api/admin'
import ArticleDialog from '@/components/ArticleDialog.vue'
import PageHead from '@/components/PageHead.vue'
import Tabelserch from '@/components/Tabelserch.vue'

const categoryMap = reactive({})
const categories = ref([])
const tableData = ref([])
const dialogVisible = ref(false)
const currentArticle = ref(null)
const loading = ref(false)
const lastSearchParams = ref({})

const statusMap = {
  0: '草稿',
  1: '已发布',
  2: '已下线',
}

const statusTypeMap = {
  0: 'info',
  1: 'success',
  2: 'warning',
}

const formItem = [
  {
    comp: 'input',
    prop: 'title',
    label: '文章标题',
    placeholder: '请输入文章标题',
  },
  {
    comp: 'select',
    prop: 'categoryId',
    label: '文章分类',
    placeholder: '请选择文章分类',
    options: [],
  },
  {
    comp: 'select',
    prop: 'status',
    label: '发布状态',
    placeholder: '请选择发布状态',
    options: [
      { label: '草稿', value: 0 },
      { label: '已发布', value: 1 },
      { label: '已下线', value: 2 },
    ],
  },
]

const pagination = reactive({
  currentPage: 1,
  size: 10,
  total: 0,
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

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
}

const handleChange = (page) => {
  pagination.currentPage = page
  handleSearch()
}

const handleSearch = async (formData) => {
  if (formData) {
    lastSearchParams.value = { ...formData }
    pagination.currentPage = 1
  }

  loading.value = true
  try {
    const res = await articleList(cleanParams({
      currentPage: pagination.currentPage,
      size: pagination.size,
      ...lastSearchParams.value,
    }))
    const data = res?.data || res || {}
    const records = Array.isArray(data.records) ? data.records : (Array.isArray(data) ? data : [])
    tableData.value = records
    pagination.total = Number(data.total ?? records.length)
  } finally {
    loading.value = false
  }
}

const handleStatusChange = async (row, status) => {
  const actionText = status === 1 ? '发布' : '下线'
  await ElMessageBox.confirm(`确认${actionText}文章「${row.title}」吗？`, '确认操作', {
    confirmButtonText: `确认${actionText}`,
    cancelButtonText: '取消',
    type: status === 1 ? 'info' : 'warning',
  })

  await changeArticleStatus(row.id, { status })
  ElMessage.success(`文章已${actionText}`)
  handleSearch()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除文章「${row.title}」吗？删除后前台将不再展示。`, '删除文章', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning',
  })

  await deleteArticle(row.id)
  ElMessage.success('文章已删除')
  handleSearch()
}

const handleSubmit = () => {
  dialogVisible.value = false
  currentArticle.value = null
  pagination.currentPage = 1
  handleSearch()
}

const handleEdit = async (row) => {
  if (!row.id) {
    currentArticle.value = null
    dialogVisible.value = true
    return
  }

  const res = await getArticleDetail(row.id)
  currentArticle.value = res?.data || res
  dialogVisible.value = true
}

onMounted(async () => {
  const res = await categoryTree()
  const data = res?.data || res || []

  categories.value = data.map((item) => {
    const categoryName = item.categoryName || item.name
    categoryMap[item.id] = categoryName
    return {
      label: categoryName,
      value: item.id,
    }
  })
  formItem[1].options = categories.value

  handleSearch()
})
</script>

<style scoped>
.article-admin-page {
  padding: 4px 0 24px;
}

.article-table {
  width: 100%;
  margin-top: 25px;
}

.title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
