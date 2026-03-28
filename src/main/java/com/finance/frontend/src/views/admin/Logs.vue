<template>
  <div class="logs-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>审计日志</span>
          <el-button type="primary" size="small" @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="operatorId" label="操作人ID" width="100" />
        <el-table-column label="操作类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getActionType(row.actionType)">{{ row.actionType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作描述" min-width="250" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" width="130" />
        <el-table-column prop="createdAt" label="操作时间" width="160" />
        <el-table-column label="详情" width="80">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="showDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="dialogVisible" title="操作详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="操作人ID">{{ currentLog?.operatorId }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ currentLog?.actionType }}</el-descriptions-item>
        <el-descriptions-item label="操作描述">{{ currentLog?.action }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog?.ipAddress }}</el-descriptions-item>
        <el-descriptions-item label="设备信息">{{ currentLog?.deviceInfo }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ currentLog?.createdAt }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>变更数据</el-divider>

      <el-row :gutter="20">
        <el-col :span="12">
          <h4>变更前</h4>
          <pre class="json-box">{{ formatJson(currentLog?.beforeValue) }}</pre>
        </el-col>
        <el-col :span="12">
          <h4>变更后</h4>
          <pre class="json-box">{{ formatJson(currentLog?.afterValue) }}</pre>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const currentLog = ref(null)

const getActionType = (type) => {
  const types = {
    SUBMIT: 'primary',
    APPROVE: 'success',
    REJECT: 'danger',
    AUDIT: 'warning',
    MODIFY: 'info',
    DELETE: 'danger'
  }
  return types[type] || ''
}

const formatJson = (str) => {
  if (!str) return '无'
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await api.getAuditLogs({})
    if (res.code === 200) tableData.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const showDetail = (row) => {
  currentLog.value = row
  dialogVisible.value = true
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.json-box {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 300px;
  overflow: auto;
}
</style>