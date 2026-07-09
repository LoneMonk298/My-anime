<template>
  <div class="rich-text-editor">
    <div class="editor-tools">
      <el-button size="small" @click="markdownDialogVisible = true">导入 Markdown</el-button>
    </div>
    <div class="editor-container" :style="{ height: editorHeight }">
      <WangToolbar
        :editor="editorRef"
        :default-config="toolbarConfig"
        mode="default"
        class="editor-toolbar"
      />

      <WangEditor
        v-model="content"
        :default-config="editorConfig"
        mode="default"
        class="wang-editor"
        @onCreated="handleCreated"
        @onChange="handleChange"
        @onDestroyed="handleDestroyed"
      />
    </div>

    <div v-if="showWordCount" class="editor-footer">
      <div class="word-count">
        <span class="count-text">{{ currentCharCount }} / {{ maxCharCount }}</span>
        <div class="process-bar">
          <div :style="{ width: Math.min((currentCharCount / maxCharCount) * 100, 100) + '%' }"></div>
        </div>
      </div>
    </div>

    <el-dialog v-model="markdownDialogVisible" title="导入 Markdown" width="760px" append-to-body>
      <div class="markdown-importer">
        <el-input
          v-model="markdownSource"
          type="textarea"
          :rows="14"
          placeholder="粘贴 Markdown 内容，转换后会写入编辑器。"
        />
        <section class="markdown-preview">
          <p>转换预览</p>
          <div v-html="markdownPreview || '<p>暂无内容</p>'"></div>
        </section>
      </div>
      <template #footer>
        <el-button @click="markdownDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="applyMarkdown">转换并写入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import '@wangeditor/editor/dist/css/style.css'
import { ElMessage } from 'element-plus'
import { computed, onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { Editor as WangEditor, Toolbar as WangToolbar } from '@wangeditor/editor-for-vue'
import { renderMarkdown } from '@/utils/markdown'
import { uploadFile } from '@/api/admin'
import { resolveFileUrl } from '@/utils/fileUrl'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  placeholder: {
    type: String,
    default: '请输入内容',
  },
  height: {
    type: String,
    default: '500px',
  },
  showWordCount: {
    type: Boolean,
    default: true,
  },
  maxCharCount: {
    type: Number,
    default: 5000,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue', 'change', 'create'])

const editorRef = shallowRef(null)
const content = ref(props.modelValue)
const markdownDialogVisible = ref(false)
const markdownSource = ref('')
const editorHeight = computed(() => props.height)

const markdownPreview = computed(() => {
  if (!markdownSource.value.trim()) return ''
  return renderMarkdown(markdownSource.value)
})

const currentCharCount = computed(() => {
  if (!content.value) return 0
  return editorRef.value?.getText()?.trim().length || 0
})

const toolbarConfig = {
  toolbarKeys: [
    'bold',
    'italic',
    'underline',
    'through',
    'color',
    'bgColor',
    'clearStyle',
    '|',
    'fontSize',
    'fontFamily',
    '|',
    'justifyLeft',
    'justifyCenter',
    'justifyRight',
    'justifyJustify',
    '|',
    'lineHeight',
    '|',
    'insertLink',
    'insertImage',
    '|',
    'bulletedList',
    'numberedList',
    '|',
    'blockquote',
    'codeBlock',
    '|',
    'undo',
    'redo',
  ],
}

const editorConfig = computed(() => ({
  placeholder: props.placeholder,
  readOnly: props.disabled,
  maxLength: props.maxCharCount,
  MENU_CONF: {
    uploadImage: {
      customUpload: async (file, insertFn) => {
        try {
          const fileRes = await uploadFile(file, {
            businessType: 'ARTICLE',
            businessId: crypto.randomUUID(),
            businessField: 'content',
          })
          const data = fileRes?.data || {}
          const filePath = data.filePath || data.fileUrl || data.url
          if (!filePath) {
            ElMessage.error('图片上传失败')
            return
          }
          insertFn(resolveFileUrl(filePath))
        } catch (error) {
          ElMessage.error(`图片上传失败：${error.message || '请稍后重试'}`)
        }
      },
      maxFileSize: 5 * 1024 * 1024,
      maxNumberOfFiles: 10,
    },
    insertLink: {
      checkLink: () => true,
    },
  },
}))

const handleCreated = (editor) => {
  editorRef.value = editor
  emit('create', editor)
}

const handleChange = (editor) => {
  const html = editor.getHtml()
  content.value = html
  emit('update:modelValue', html)
  emit('change', html)
}

const handleDestroyed = () => {
  editorRef.value = null
}

const applyMarkdown = () => {
  if (!markdownSource.value.trim()) {
    ElMessage.warning('请先粘贴 Markdown 内容')
    return
  }
  const html = renderMarkdown(markdownSource.value)
  content.value = html
  editorRef.value?.setHtml(html)
  emit('update:modelValue', html)
  emit('change', html)
  markdownDialogVisible.value = false
  ElMessage.success('Markdown 已转换为富文本')
}

watch(
  () => props.modelValue,
  (newValue) => {
    if (newValue === content.value) return
    content.value = newValue || ''
    if (editorRef.value) {
      editorRef.value.setHtml(content.value)
    }
  },
)

onBeforeUnmount(() => {
  if (editorRef.value) {
    editorRef.value.destroy()
    editorRef.value = null
  }
})

defineExpose({
  getEditor: () => editorRef.value,
  getContent: () => content.value,
  setContent: (html) => {
    content.value = html || ''
    editorRef.value?.setHtml(content.value)
  },
  clear: () => {
    content.value = ''
    editorRef.value?.clear()
  },
  getText: () => editorRef.value?.getText() || '',
})
</script>

<style lang="scss" scoped>
.rich-text-editor {
  width: 100%;
  overflow: hidden;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
}

.editor-tools {
  display: flex;
  justify-content: flex-end;
  padding: 8px 10px;
  border-bottom: 1px solid #e4e7ed;
  background: #f8fafc;
}

.editor-container {
  display: flex;
  flex-direction: column;
}

.editor-toolbar {
  flex-shrink: 0;
  border-bottom: 1px solid #e4e7ed;
}

.wang-editor {
  flex: 1;
  min-height: 300px;
}

.editor-footer {
  padding: 12px 16px;
  border-top: 1px solid #e4e7ed;
  background: #fafafa;
}

.word-count {
  display: flex;
  gap: 12px;
  align-items: center;
}

.count-text {
  color: #606266;
  font-size: 13px;
}

.process-bar {
  flex: 1;
  height: 6px;
  overflow: hidden;
  border-radius: 3px;
  background: #e4e7ed;

  div {
    height: 100%;
    border-radius: 3px;
    background: #409eff;
    transition: width 0.3s ease;
  }
}

.markdown-importer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 14px;
}

.markdown-preview {
  min-height: 320px;
  max-height: 420px;
  overflow: auto;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #f8fafc;
  color: #334155;

  > p {
    margin: 0 0 10px;
    color: #64748b;
    font-size: 12px;
    font-weight: 900;
  }

  :deep(h1),
  :deep(h2),
  :deep(h3) {
    margin: 0.8em 0 0.4em;
    color: #111827;
  }

  :deep(p),
  :deep(li) {
    line-height: 1.8;
  }

  :deep(pre) {
    overflow: auto;
    padding: 10px;
    border-radius: 6px;
    background: #0f172a;
    color: #e2e8f0;
  }
}

@media (max-width: 760px) {
  .markdown-importer {
    grid-template-columns: 1fr;
  }
}
</style>

<style lang="scss">
.wang-editor {
  .w-e-text-container {
    p {
      margin: 0 !important;
      padding: 0 !important;
      line-height: 1.75 !important;
    }

    p:first-child:empty::before {
      content: none;
    }
  }
}
</style>
