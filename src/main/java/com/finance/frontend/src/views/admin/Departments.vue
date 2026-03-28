<template>
  <div class="departments-page">
    <el-card>
      <template #header><span>部门管理</span></template>
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="deptCode" label="部门编码" width="120" />
        <el-table-column prop="deptName" label="部门名称" width="150" />
        <el-table-column label="部门经理" width="100">
          <template #default="{ row }">
            {{ row.managerId ? `ID: ${row.managerId}` : '未设置' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status ? 'success' : 'danger'">{{ row.status ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
      </el-table>
    </el-card>
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
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>