<template>
  <section class="admin-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Friend Links</p>
        <h1>友链管理</h1>
        <span>审核前台申请，管理网址导航、友情链接、LOGO、排序和访问数据。</span>
      </div>
      <el-button type="primary" @click="openDialog()">新增友链</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.keyword" clearable placeholder="搜索网站名称或简介" @keyup.enter="fetchLinks" />
      <el-select v-model="query.status" clearable placeholder="状态">
        <el-option label="待审核" :value="0" />
        <el-option label="已启用" :value="1" />
        <el-option label="已拒绝/停用" :value="2" />
      </el-select>
      <el-select v-model="query.category" clearable placeholder="分类">
        <el-option v-for="category in categories" :key="category" :label="category" :value="category" />
      </el-select>
      <el-select v-model="query.tag" clearable placeholder="标签">
        <el-option v-for="tag in tags" :key="tag" :label="tag" :value="tag" />
      </el-select>
      <el-button type="primary" @click="fetchLinks">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="links" border>
      <el-table-column label="LOGO" width="86">
        <template #default="{ row }">
          <el-avatar class="logo-avatar" :src="resolveLogo(row.logoUrl)" shape="circle" @click="previewLogo(row)" />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="网站名称" min-width="150" />
      <el-table-column prop="url" label="网址" min-width="220" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="100" />
      <el-table-column prop="tag" label="标签" width="90" />
      <el-table-column prop="visits" label="访问" width="90" />
      <el-table-column prop="sortOrder" label="排序" width="90" />
      <el-table-column label="状态" width="112">
        <template #default="{ row }">
          <el-tag :type="statusMeta(row.status).type">{{ statusMeta(row.status).text }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button v-if="row.status === 0" size="small" type="success" @click="setStatus(row, 1)">通过</el-button>
          <el-button v-if="row.status === 0" size="small" type="warning" @click="setStatus(row, 2)">拒绝</el-button>
          <el-button v-if="row.status === 2" size="small" type="success" @click="setStatus(row, 1)">启用</el-button>
          <el-button v-if="row.status === 1" size="small" type="warning" @click="setStatus(row, 2)">停用</el-button>
          <el-button size="small" type="danger" @click="removeLink(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-info">
      <el-pagination
        v-model:current-page="pagination.currentPage"
        background
        layout="total, prev, pager, next"
        :page-size="pagination.size"
        :total="pagination.total"
        @current-change="fetchLinks"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑友链' : '新增友链'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="网站名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入网站名称" />
        </el-form-item>
        <el-form-item label="网站地址" prop="url">
          <el-input v-model="form.url" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="网站简介" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="一句话介绍网站" />
        </el-form-item>
        <el-form-item label="分类标签">
          <div class="inline-grid">
            <el-select
              v-model="form.category"
              filterable
              allow-create
              default-first-option
              placeholder="分类"
              @change="syncTagByCategory"
            >
              <el-option v-for="category in categories" :key="category" :label="category" :value="category" />
            </el-select>
            <el-select v-model="form.tag" placeholder="系统标签">
              <el-option v-for="tag in tags" :key="tag" :label="tag" :value="tag" />
            </el-select>
          </div>
          <p class="field-tip">“常用”由前台按访问次数自动展示；这里通常只需要确认分类，系统会自动归入工具/社区/灵感。</p>
        </el-form-item>
        <el-form-item label="LOGO">
          <div class="logo-editor">
            <el-avatar :src="resolveLogo(form.logoUrl)" :size="54" @click="previewLogo(form)" />
            <el-upload :show-file-list="false" :before-upload="beforeLogoUpload" :http-request="uploadLogo">
              <el-button>上传 LOGO</el-button>
            </el-upload>
            <el-input v-model="form.logoUrl" placeholder="也可以直接填写图片地址" />
          </div>
        </el-form-item>
        <el-form-item label="运营数据">
          <div class="inline-grid three">
            <label class="metric-input">
              <span>访问次数</span>
              <el-input-number v-model="form.visits" :min="0" controls-position="right" />
            </label>
            <label class="metric-input">
              <span>评分</span>
              <el-rate v-model="form.rating" :max="5" />
            </label>
            <label class="metric-input">
              <span>排序值</span>
              <el-input-number v-model="form.sortOrder" controls-position="right" />
            </label>
          </div>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button :value="0">待审核</el-radio-button>
            <el-radio-button :value="1">启用</el-radio-button>
            <el-radio-button :value="2">拒绝/停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveLink">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="logoPreviewVisible" title="LOGO 预览" width="320px">
      <div class="logo-preview">
        <img :src="logoPreviewUrl" alt="LOGO 预览" />
      </div>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addFriendLink,
  changeFriendLinkStatus,
  deleteFriendLink,
  friendLinkList,
  updateFriendLink,
  uploadFile,
} from '@/api/admin'
import { resolveFileUrl } from '@/utils/fileUrl'

const tags = ['常用', '工具', '社区', '灵感']
const categories = ['动漫', '博客', '娱乐', '导航', '工具', '公益']
const links = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const logoPreviewVisible = ref(false)
const logoPreviewUrl = ref('')
const formRef = ref()

const query = reactive({
  keyword: '',
  status: '',
  category: '',
  tag: '',
})

const pagination = reactive({
  currentPage: 1,
  size: 10,
  total: 0,
})

const emptyForm = () => ({
  id: null,
  name: '',
  url: '',
  description: '',
  category: '娱乐',
  tag: '',
  logoUrl: '/anime-assets/liuhuaa.ico',
  visits: 0,
  rating: 4,
  status: 1,
  sortOrder: 0,
})

const form = reactive(emptyForm())

const rules = {
  name: [{ required: true, message: '请输入网站名称', trigger: 'blur' }],
  url: [{ required: true, message: '请输入网站地址', trigger: 'blur' }],
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

const fetchLinks = async () => {
  loading.value = true
  try {
    const res = await friendLinkList(cleanParams({
      currentPage: pagination.currentPage,
      size: pagination.size,
      keyword: query.keyword,
      status: query.status,
      category: query.category,
      tag: query.tag,
    }))
    const data = res?.data || {}
    links.value = Array.isArray(data.records) ? data.records : []
    pagination.total = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  Object.assign(form, emptyForm(), row || {})
  if (!form.tag || form.tag === '常用') {
    form.tag = inferTagByCategory(form.category)
  }
  dialogVisible.value = true
}

const saveLink = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form }
    if (payload.id) {
      await updateFriendLink(payload.id, payload)
    } else {
      await addFriendLink(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchLinks()
  } finally {
    saving.value = false
  }
}

const setStatus = async (row, status) => {
  await changeFriendLinkStatus(row.id, { status })
  ElMessage.success('状态已更新')
  fetchLinks()
}

const removeLink = async (row) => {
  await ElMessageBox.confirm(`确认删除友链「${row.name}」？`, '删除确认', { type: 'warning' })
  await deleteFriendLink(row.id)
  ElMessage.success('删除成功')
  fetchLinks()
}

const beforeLogoUpload = (file) => {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请上传图片文件')
    return false
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error('LOGO 大小不能超过 5MB')
    return false
  }
  return true
}

const uploadLogo = async ({ file }) => {
  const res = await uploadFile(file, {
    businessType: 'LINK',
    businessId: form.id || crypto.randomUUID(),
    businessField: 'logo',
  })
  const data = res?.data || {}
  form.logoUrl = data.filePath || data.url || data.fileUrl
}

const resolveLogo = (url) => resolveFileUrl(url || '/anime-assets/liuhuaa.ico')

const inferTagByCategory = (category = '') => {
  if (category.includes('工具')) return '工具'
  if (category.includes('灵感') || category.includes('设计') || category.includes('插画')) return '灵感'
  return '社区'
}

const syncTagByCategory = () => {
  form.tag = inferTagByCategory(form.category)
}

const previewLogo = (row) => {
  logoPreviewUrl.value = resolveLogo(row.logoUrl)
  logoPreviewVisible.value = true
}

const statusMeta = (status) => {
  if (status === 0) return { text: '待审核', type: 'info' }
  if (status === 1) return { text: '已启用', type: 'success' }
  return { text: '已拒绝/停用', type: 'warning' }
}

onMounted(fetchLinks)
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

.toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 320px) 150px 150px 150px 86px;
  gap: 12px;
}

.logo-avatar {
  cursor: zoom-in;
}

.inline-grid {
  display: grid;
  width: 100%;
  grid-template-columns: minmax(0, 1fr) 160px;
  gap: 10px;

  &.three {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

.field-tip {
  grid-column: 1 / -1;
  margin: 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.metric-input {
  display: grid;
  gap: 6px;
  min-width: 0;

  span {
    color: #64748b;
    font-size: 12px;
    font-weight: 800;
  }
}

.logo-editor {
  display: grid;
  width: 100%;
  grid-template-columns: 54px 110px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.logo-preview {
  display: grid;
  place-items: center;

  img {
    width: 180px;
    height: 180px;
    border-radius: 24px;
    object-fit: cover;
    background: #f1f5f9;
  }
}

@media (max-width: 1100px) {
  .toolbar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .toolbar,
  .inline-grid,
  .inline-grid.three,
  .logo-editor {
    grid-template-columns: 1fr;
  }
}
</style>
