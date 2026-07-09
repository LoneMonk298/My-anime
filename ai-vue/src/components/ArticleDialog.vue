<template>
  <el-dialog
    v-model="dialogVisible"
    :title="isEdit ? '编辑记录文章' : '新增记录文章'"
    width="980px"
    destroy-on-close
    @close="handleClose"
  >
    <el-tabs v-model="activeTab" class="article-editor-tabs">
      <el-tab-pane label="编辑" name="edit">
        <el-form ref="formRef" :model="formData" :rules="rules" label-width="96px">
          <el-form-item label="文章标题" prop="title">
            <el-input
              v-model="formData.title"
              placeholder="例如：重温《葬送的芙莉莲》第 14 集"
              maxlength="200"
              show-word-limit
              clearable
            />
          </el-form-item>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="文章分类" prop="categoryId">
                <el-select v-model="formData.categoryId" placeholder="请选择分类" filterable class="full-select">
                  <el-option
                    v-for="item in props.categories"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="作者">
                <el-input v-model="formData.authorName" placeholder="默认 ZG" clearable />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="文章摘要" prop="summary">
            <el-input
              v-model="formData.summary"
              type="textarea"
              placeholder="写一段会出现在卡片上的短评或导语"
              maxlength="1000"
              :rows="3"
              show-word-limit
              clearable
            />
          </el-form-item>

          <el-form-item label="标签" prop="tags">
            <el-select
              v-model="formData.tagArray"
              placeholder="选择或输入标签"
              multiple
              filterable
              allow-create
              class="full-select"
            >
              <el-option v-for="item in commonTags" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>

          <el-form-item label="封面图片">
            <div class="cover-upload">
              <el-upload
                action="#"
                :before-upload="beforeUpload"
                :http-request="handleUpload"
                :show-file-list="false"
                accept="image/*"
              >
                <div v-if="!imgUrl" class="cover-placeholder">
                  <span>点击上传封面</span>
                  <small>建议 16:9，单张不超过 5MB</small>
                </div>
                <img v-else :src="imgUrl" alt="文章封面预览" class="cover-image" @error="handlePreviewError" />
              </el-upload>

              <div class="cover-actions">
                <el-button v-if="imgUrl" type="danger" size="small" plain @click.stop="removeCover">
                  删除封面
                </el-button>
                <p v-if="uploadError" class="upload-error">{{ uploadError }}</p>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="文章内容" prop="content">
            <RichTextEditor
              v-model="formData.content"
              :max-char-count="12000"
              height="420px"
              placeholder="写下剧情记录、观后感、设定整理或补番笔记"
              @change="handleContentChange"
            />
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="预览" name="preview">
        <article class="article-preview">
          <img v-if="previewImage" :src="previewImage" alt="封面预览" @error="handlePreviewError" />
          <div class="preview-head">
            <span>{{ previewCategory }}</span>
            <h1>{{ formData.title || '未命名记录文章' }}</h1>
            <p v-if="formData.summary">{{ formData.summary }}</p>
            <small>{{ formData.authorName || 'ZG' }} · {{ statusText }}</small>
          </div>
          <section class="preview-body" v-html="renderPreviewContent(formData.content)"></section>
        </article>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button @click="activeTab = activeTab === 'preview' ? 'edit' : 'preview'">
        {{ activeTab === 'preview' ? '返回编辑' : '预览效果' }}
      </el-button>
      <el-button @click="handleClose">取消</el-button>
      <el-button :loading="loading" @click="handleSubmit(0)">保存草稿</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit(1)">保存并发布</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { addArticle, updateArticle, uploadFile } from '@/api/admin'
import RichTextEditor from '@/components/RichTextEditor.vue'
import { getArticleCover, resolveFileUrl } from '@/utils/fileUrl'
import { renderContent } from '@/utils/markdown'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  categories: {
    type: Array,
    default: () => [],
  },
  article: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['update:modelValue', 'success'])

const emptyForm = () => ({
  id: '',
  title: '',
  content: '',
  coverImg: '',
  categoryId: props.categories[0]?.value || '',
  summary: '',
  tags: '',
  tagArray: [],
  authorName: 'ZG',
  status: 0,
})

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const formRef = ref()
const formData = reactive(emptyForm())
const imgUrl = ref('')
const uploadError = ref('')
const activeTab = ref('edit')
const loading = ref(false)

const isEdit = computed(() => Boolean(props.article?.id))
const previewImage = computed(() => imgUrl.value || resolveFileUrl(getArticleCover(formData), ''))
const previewCategory = computed(() => {
  return props.categories.find((item) => item.value === formData.categoryId)?.label || '番剧'
})
const statusText = computed(() => (Number(formData.status) === 1 ? '已发布' : '草稿'))

const rules = {
  title: [
    { required: true, message: '请输入文章标题', trigger: 'blur' },
    { max: 200, message: '文章标题最多 200 个字符', trigger: 'blur' },
  ],
  categoryId: [{ required: true, message: '请选择文章分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入文章内容', trigger: 'change' }],
}

const commonTags = [
  '番剧',
  '动画',
  '漫画',
  '游戏',
  '轻小说',
  '补番',
  '观后感',
  '角色',
  '设定',
  '音乐',
  '截图',
  '推荐',
]

const resetFormData = () => {
  Object.assign(formData, emptyForm())
  imgUrl.value = ''
  uploadError.value = ''
  activeTab.value = 'edit'
  nextTick(() => formRef.value?.clearValidate())
}

const handleClose = () => {
  resetFormData()
  emit('update:modelValue', false)
}

const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5MB = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('请上传图片格式的文件')
    return false
  }

  if (!isLt5MB) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }

  return true
}

const handleUpload = async ({ file }) => {
  uploadError.value = ''
  const fileRes = await uploadFile(file, {
    businessType: 'ARTICLE',
    businessId: formData.id || crypto.randomUUID(),
    businessField: 'cover',
  })
  const data = fileRes?.data || {}
  const filePath = data.filePath || data.url || data.fileUrl || data.path
  formData.coverImg = filePath
  imgUrl.value = resolveFileUrl(filePath)
}

const handlePreviewError = () => {
  uploadError.value = '封面预览失败，请重新上传'
}

const removeCover = () => {
  formData.coverImg = ''
  imgUrl.value = ''
  uploadError.value = ''
}

const handleContentChange = (html) => {
  formData.content = html
}

const renderPreviewContent = (content) => renderContent(content || '正文内容会显示在这里。')

const buildSubmitData = (status) => {
  const submitData = {
    ...formData,
    status,
    tags: formData.tagArray.join(','),
  }
  delete submitData.tagArray
  return submitData
}

const handleSubmit = async (status) => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    activeTab.value = 'edit'
    return
  }

  loading.value = true

  try {
    if (isEdit.value) {
      await updateArticle(props.article.id, buildSubmitData(status))
      ElMessage.success(status === 1 ? '文章已保存并发布' : '草稿已保存')
    } else {
      await addArticle(buildSubmitData(status))
      ElMessage.success(status === 1 ? '文章已新增并发布' : '草稿已新增')
    }
    emit('success')
    handleClose()
  } finally {
    loading.value = false
  }
}

const applyArticle = (article) => {
  resetFormData()
  if (!article?.id) return

  Object.assign(formData, {
    ...article,
    tagArray: article.tags ? article.tags.split(',').map((tag) => tag.trim()).filter(Boolean) : [],
    authorName: article.authorName || 'ZG',
  })
  imgUrl.value = resolveFileUrl(getArticleCover(article))
}

watch(
  () => props.article,
  (article) => {
    if (dialogVisible.value) {
      applyArticle(article)
    }
  },
  { immediate: true },
)

watch(dialogVisible, (visible) => {
  if (visible) {
    applyArticle(props.article)
  }
})

watch(
  () => props.categories,
  (categories) => {
    if (!formData.categoryId && categories.length) {
      formData.categoryId = categories[0].value
    }
  },
)
</script>

<style lang="scss" scoped>
.article-editor-tabs {
  min-height: 560px;
}

.full-select {
  width: 100%;
}

.cover-upload {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: flex-end;
}

.cover-placeholder,
.cover-image {
  width: 240px;
  height: 135px;
  border-radius: 8px;
}

.cover-placeholder {
  display: grid;
  place-items: center;
  border: 1px dashed #b6c2d2;
  background: #f8fafc;
  color: #526071;
  cursor: pointer;

  span,
  small {
    display: block;
  }

  small {
    color: #8793a3;
    font-size: 12px;
  }
}

.cover-image {
  display: block;
  object-fit: cover;
}

.cover-actions {
  display: grid;
  gap: 8px;
}

.upload-error {
  margin: 0;
  color: #f56c6c;
  font-size: 12px;
}

.article-preview {
  overflow: hidden;
  border: 1px solid #d8e1ed;
  border-radius: 8px;
  background: #071b35;
  color: #e6f1ff;

  img {
    display: block;
    width: 100%;
    height: 260px;
    object-fit: cover;
  }
}

.preview-head {
  padding: 24px 28px;
  background: rgba(10, 25, 47, 0.7);

  span {
    color: #64ffda;
    font-weight: 900;
  }

  h1 {
    margin: 10px 0;
    color: #fff;
    font-size: 30px;
    line-height: 1.25;
  }

  p {
    color: #c7d6eb;
    line-height: 1.8;
  }

  small {
    color: #8fa3bd;
  }
}

.preview-body {
  min-height: 180px;
  padding: 28px;
  color: #d8e6f7;
  font-size: 16px;
  line-height: 1.9;
  word-break: break-word;

  :deep(img) {
    max-width: 100%;
    border-radius: 6px;
  }
}
</style>
