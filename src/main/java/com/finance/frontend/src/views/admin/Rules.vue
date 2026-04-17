<template>
  <div class="rules-page">
    <div class="page-header">
      <h2 class="page-title">规则配置</h2>
    </div>
    <div class="glass-card">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="ruleCode" label="规则编码" width="160" />
        <el-table-column prop="ruleName" label="规则名称" width="180" />
        <el-table-column prop="ruleType" label="规则类型" width="140" />
        <el-table-column prop="description" label="描述" min-width="220" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="status-pill" :class="row.enabled ? 'enabled' : 'disabled'">
              {{ row.enabled ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="toggleRule(row)" />
          </template>
        </el-table-column>
      </el-table>
    </div>
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
  } catch {} finally { loading.value = false }
}

const toggleRule = async (row) => {
  ElMessage.success(`规则「${row.ruleName}」已${row.enabled ? '启用' : '禁用'}`)
}

onMounted(() => loadData())
</script>

<style scoped>
.rules-page { padding: 0; min-height: 100%; }
.page-header { margin-bottom: 24px; }
.page-title { font-size: 22px; font-weight: 700; color: #fff; margin: 0; }
.glass-card {
  background: rgba(255,255,255,0.08); backdrop-filter: blur(14px);
  border: 1px solid rgba(255,255,255,0.15); border-radius: 18px; overflow: hidden;
}
.status-pill {
  display: inline-flex; align-items: center; padding: 3px 10px;
  border-radius: 20px; font-size: 12px; font-weight: 600;
}
.status-pill.enabled { background: rgba(52,211,153,0.2); color: #34d399; border: 1px solid rgba(52,211,153,0.3); }
.status-pill.disabled { background: rgba(255,255,255,0.08); color: rgba(255,255,255,0.4); border: 1px solid rgba(255,255,255,0.15); }
</style>
