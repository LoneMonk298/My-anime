<template>
  <div class="rich-text-editor">
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
  </div>
</template>

<script setup>
import '@wangeditor/editor/dist/css/style.css'
import { ElMessage } from 'element-plus'
import { computed, onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { Editor as WangEditor, Toolbar as WangToolbar } from '@wangeditor/editor-for-vue'
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
const editorHeight = computed(() => props.height)

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
