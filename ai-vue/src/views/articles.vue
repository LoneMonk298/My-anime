<template>
  <div class="article-admin-page">
    <PageHead page-title="记录站内容管理">
      <template #buttons>
        <el-button v-if="activeTab === 'articles'" type="primary" @click="handleEdit({})">新增文章</el-button>
        <el-button v-else type="primary" @click="handleCategoryEdit({})">新增分类</el-button>
      </template>
    </PageHead>

    <el-tabs v-model="activeTab" class="admin-tabs">
      <el-tab-pane label="文章管理" name="articles">
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
          <el-table-column label="分类" width="150">
            <template #default="{ row }">
              {{ categoryMap[row.categoryId] || row.categoryId || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="authorName" label="作者" width="130" />
          <el-table-column prop="readCount" label="阅读量" width="100" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTypeMap[row.status] || 'info'" effect="light">
                {{ statusMap[row.status] || '未知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="170">
            <template #default="{ row }">
              {{ formatDate(row.updatedAt || row.publishedAt || row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="300" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="handleEdit(row)">编辑</el-button>
              <el-button v-if="row.status === 1" text type="success" @click="openFrontend(row)">前台查看</el-button>
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
      </el-tab-pane>

      <el-tab-pane label="分类管理" name="categories">
        <el-table v-loading="categoryLoading" :data="categoryRows" class="article-table">
          <el-table-column prop="name" label="分类名称" min-width="180" />
          <el-table-column prop="code" label="标识" min-width="160" />
          <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip />
          <el-table-column prop="sortOrder" label="排序" width="90" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="handleCategoryEdit(row)">编辑</el-button>
              <el-button text type="danger" @click="handleCategoryDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <ArticleDialog
      v-model:model-value="dialogVisible"
      :article="currentArticle"
      :categories="categories"
      @success="handleSubmit"
    />

    <el-dialog v-model="categoryDialogVisible" :title="categoryForm.id ? '编辑分类' : '新增分类'" width="520px">
      <el-form ref="categoryFormRef" :model="categoryForm" :rules="categoryRules" label-width="86px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="例如：番剧" clearable />
        </el-form-item>
        <el-form-item label="分类标识" prop="code">
          <el-input v-model="categoryForm.code" placeholder="例如：bangumi" clearable />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="categoryForm.description" type="textarea" :rows="3" placeholder="用于后台识别分类用途" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="categoryForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="categorySaving" @click="submitCategory">保存分类</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { dayjs, ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { Timer } from '@element-plus/icons-vue'
import {
  addCategory,
  articleList,
  categoryTree,
  changeArticleStatus,
  deleteArticle,
  deleteCategory,
  getArticleDetail,
  updateCategory,
} from '@/api/admin'
import ArticleDialog from '@/components/ArticleDialog.vue'
import PageHead from '@/components/PageHead.vue'
import Tabelserch from '@/components/Tabelserch.vue'

const activeTab = ref('articles')
const categoryMap = reactive({})
const categories = ref([])
const categoryRows = ref([])
const tableData = ref([])
const dialogVisible = ref(false)
const currentArticle = ref(null)
const loading = ref(false)
const categoryLoading = ref(false)
const categorySaving = ref(false)
const categoryDialogVisible = ref(false)
const categoryFormRef = ref()
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

const categoryForm = reactive({
  id: '',
  name: '',
  code: '',
  description: '',
  sortOrder: 0,
  status: 1,
})

const categoryRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入分类标识', trigger: 'blur' }],
}

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

const loadCategories = async () => {
  categoryLoading.value = true
  try {
    const res = await categoryTree()
    const data = res?.data || res || []
    Object.keys(categoryMap).forEach((key) => delete categoryMap[key])
    categoryRows.value = data
    categories.value = data
      .filter((item) => item.status === 1)
      .map((item) => {
        const categoryName = item.categoryName || item.name
        categoryMap[item.id] = categoryName
        return {
          label: categoryName,
          value: item.id,
        }
      })
    formItem[1].options = categories.value
  } finally {
    categoryLoading.value = false
  }
}

const handleStatusChange = async (row, status) => {
  const actionText = status === 1 ? '发布' : '下线'
  await ElMessageBox.confirm(`确认${actionText}文章《${row.title}》吗？`, '确认操作', {
    confirmButtonText: `确认${actionText}`,
    cancelButtonText: '取消',
    type: status === 1 ? 'info' : 'warning',
  })

  await changeArticleStatus(row.id, { status })
  ElMessage.success(`文章已${actionText}`)
  handleSearch()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除文章《${row.title}》吗？删除后前台将不再展示。`, '删除文章', {
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

const openFrontend = (row) => {
  window.open(`/article/${row.id}`, '_blank')
}

const resetCategoryForm = () => {
  Object.assign(categoryForm, {
    id: '',
    name: '',
    code: '',
    description: '',
    sortOrder: 0,
    status: 1,
  })
}

const handleCategoryEdit = (row) => {
  resetCategoryForm()
  if (row?.id) {
    Object.assign(categoryForm, row)
  }
  categoryDialogVisible.value = true
}

const submitCategory = async () => {
  const valid = await categoryFormRef.value?.validate().catch(() => false)
  if (!valid) return

  categorySaving.value = true
  try {
    if (categoryForm.id) {
      await updateCategory(categoryForm.id, categoryForm)
      ElMessage.success('分类已更新')
    } else {
      await addCategory(categoryForm)
      ElMessage.success('分类已新增')
    }
    categoryDialogVisible.value = false
    await loadCategories()
    await handleSearch()
  } finally {
    categorySaving.value = false
  }
}

const handleCategoryDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除分类《${row.name}》吗？`, '删除分类', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await deleteCategory(row.id)
  ElMessage.success('分类已删除')
  await loadCategories()
}

onMounted(async () => {
  await loadCategories()
  handleSearch()
})
</script>

<style scoped>
.article-admin-page {
  padding: 4px 0 24px;
}

.admin-tabs {
  margin-top: 10px;
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

.pagination-info {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
