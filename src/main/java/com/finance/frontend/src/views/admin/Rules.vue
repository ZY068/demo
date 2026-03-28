<template>
  <div class="rules-page">
    <el-card>
      <template #header><span>规则配置</span></template>
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="ruleCode" label="规则编码" width="140" />
        <el-table-column prop="ruleName" label="规则名称" width="150" />
        <el-table-column prop="ruleType" label="规则类型" width="120" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="toggleRule(row)" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api'

const loading = ref(false)
const tableData = ref([])

const loadData = async () => {
  loading.value = true
  try {
    const res = await api.getRules()
    if (res.code === 200) tableData.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const toggleRule = async (row) => {
  ElMessage.success(`规则 ${row.ruleName} 已${row.enabled ? '启用' : '禁用'}`)
}

onMounted(() => {
  loadData()
})
</script>