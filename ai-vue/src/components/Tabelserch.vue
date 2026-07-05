<template>
  <el-form ref="formRef" :model="formData" class="table-search">
    <el-row :gutter="24">
      <el-col v-for="item in formItemWithCol" :key="item.prop" v-bind="item.col">
        <el-form-item :label="item.label" :prop="item.prop">
          <component
            :is="isComp(item.comp)"
            v-model="formData[item.prop]"
            :clearable="item.clearable !== false"
            :placeholder="item.placeholder"
          >
            <template v-if="item.comp === 'select'">
              <el-option label="全部" value="" />
              <el-option
                v-for="opt in item.options"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </template>
          </component>
        </el-form-item>
      </el-col>
    </el-row>
    <el-row class="search-actions">
      <el-button type="primary" @click="submitForm">查询</el-button>
      <el-button @click="resetForm">重置</el-button>
    </el-row>
  </el-form>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'

const props = defineProps({
  formItem: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['search'])
const formRef = ref(null)
const formData = reactive({})

const formItemWithCol = computed(() => {
  return props.formItem.map((item) => ({
    ...item,
    col: item.col || {
      xs: 24,
      sm: 12,
      md: 8,
      lg: 6,
      xl: 4,
    },
  }))
})

const syncFields = () => {
  props.formItem.forEach((item) => {
    if (!(item.prop in formData)) {
      formData[item.prop] = item.defaultValue ?? ''
    }
  })
}

const isComp = (comp) => ({
  input: 'elInput',
  select: 'elSelect',
}[comp])

const submitForm = () => {
  emit('search', { ...formData })
}

const resetForm = () => {
  Object.keys(formData).forEach((key) => {
    formData[key] = ''
  })
  formRef.value?.resetFields()
  emit('search', { ...formData })
}

watch(() => props.formItem, syncFields, { immediate: true, deep: true })
</script>

<style scoped>
.table-search {
  margin-top: 4px;
}

.search-actions {
  gap: 10px;
}
</style>
