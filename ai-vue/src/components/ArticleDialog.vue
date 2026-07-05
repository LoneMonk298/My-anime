<template>
  <el-dialog
    v-model="dialogVisible"
    :title="isEdit ? '编辑记录文章' : '新增记录文章'"
    width="760px"
    destroy-on-close
    @close="handleClose"
  >
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="110px">
      <el-form-item label="文章标题" prop="title">
        <el-input
          v-model="formData.title"
          placeholder="例如：重温《葬送的芙莉莲》第 14 集"
          maxlength="200"
          show-word-limit
          clearable
        />
      </el-form-item>

      <el-form-item label="文章分类" prop="categoryId">
        <el-select v-model="formData.categoryId" placeholder="请选择分类" filterable>
          <el-option
            v-for="item in props.categories"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="文章摘要" prop="summary">
        <el-input
          v-model="formData.summary"
          type="textarea"
          placeholder="写一段会出现在卡片上的短评或导语"
          maxlength="1000"
          :rows="4"
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

    <section v-if="previewVisible" class="content-preview">
      <h3>内容预览</h3>
      <div v-html="formData.content"></div>
    </section>

    <template #footer>
      <el-button @click="previewVisible = !previewVisible">
        {{ previewVisible ? '关闭预览' : '预览内容' }}
      </el-button>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        {{ isEdit ? '保存修改' : '新增文章' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { addArticle, updateArticle, uploadFile } from '@/api/admin'
import RichTextEditor from '@/components/RichTextEditor.vue'
import { getArticleCover, resolveFileUrl } from '@/utils/fileUrl'

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
})

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const formRef = ref()
const formData = reactive(emptyForm())
const imgUrl = ref('')
const uploadError = ref('')
const previewVisible = ref(false)
const loading = ref(false)

const isEdit = computed(() => Boolean(props.article?.id))

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
  previewVisible.value = false
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

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  const submitData = {
    ...formData,
    tags: formData.tagArray.join(','),
  }
  delete submitData.tagArray

  try {
    if (isEdit.value) {
      await updateArticle(props.article.id, submitData)
      ElMessage.success('文章已更新')
    } else {
      await addArticle(submitData)
      ElMessage.success('文章已新增')
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

.content-preview {
  max-height: 260px;
  margin-top: 18px;
  padding: 18px;
  overflow: auto;
  border-radius: 8px;
  background: #f8fafc;

  h3 {
    margin-bottom: 12px;
    color: #1f2937;
  }
}
</style>
