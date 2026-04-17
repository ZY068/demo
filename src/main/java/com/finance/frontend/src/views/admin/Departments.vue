<template>
  <div class="departments-page">
    <div class="page-header">
      <h2 class="page-title">部门管理</h2>
    </div>
    <div class="glass-card">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="deptCode" label="部门编码" width="160" />
        <el-table-column prop="deptName" label="部门名称" min-width="200" />
        <el-table-column label="部门经理" width="140">
          <template #default="{ row }">
            <span class="manager-id" v-if="row.managerId">ID: {{ row.managerId }}</span>
            <span class="no-manager" v-else>未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="status-pill" :class="row.status ? 'enabled' : 'disabled'">
              {{ row.status ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api'

const loading = ref(false)
const tableData = ref([])

const loadData = async () => {
  loading.value = true
  try {
    const res = await api.getDepartments()
    if (res.code === 200) tableData.value = res.data || []
  } catch {} finally { loading.value = false }
}

onMounted(() => loadData())
</script>

<style scoped>
.departments-page { padding: 0; min-height: 100%; }
.page-header { margin-bottom: 24px; }
.page-title { font-size: 22px; font-weight: 700; color: #fff; margin: 0; }
.glass-card {
  background: rgba(255,255,255,0.08); backdrop-filter: blur(14px);
  border: 1px solid rgba(255,255,255,0.15); border-radius: 18px; overflow: hidden;
}
.manager-id { font-size: 13px; color: rgba(255,255,255,0.7); }
.no-manager { font-size: 13px; color: rgba(255,255,255,0.35); font-style: italic; }
.status-pill {
  display: inline-flex; align-items: center; padding: 3px 10px;
  border-radius: 20px; font-size: 12px; font-weight: 600;
}
.status-pill.enabled { background: rgba(52,211,153,0.2); color: #34d399; border: 1px solid rgba(52,211,153,0.3); }
.status-pill.disabled { background: rgba(255,255,255,0.08); color: rgba(255,255,255,0.35); border: 1px solid rgba(255,255,255,0.15); }
</style>
