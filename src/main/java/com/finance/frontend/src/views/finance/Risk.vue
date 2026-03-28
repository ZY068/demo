<template>
  <div class="risk-page">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card danger">
          <div class="stat-value">{{ stats.severe }}</div>
          <div class="stat-label">严重风险</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card warning">
          <div class="stat-value">{{ stats.high }}</div>
          <div class="stat-label">高风险</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card info">
          <div class="stat-value">{{ stats.medium }}</div>
          <div class="stat-label">中风险</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card success">
          <div class="stat-value">{{ stats.low }}</div>
          <div class="stat-label">低风险</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px">
      <template #header><span>风险预警列表</span></template>
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="riskType" label="风险类型" width="120">
          <template #default="{ row }">{{ getRiskTypeName(row.riskType) }}</template>
        </el-table-column>
        <el-table-column prop="alertMessage" label="预警信息" min-width="200" />
        <el-table-column label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.riskLevel)">{{ getLevelText(row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联报销单" width="100">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="viewClaim(row)">查看</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="预警时间" width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="deleteClaim(row)">删除报销单</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && tableData.length === 0" description="暂无风险预警" />
    </el-card>

    <!-- 报销单详情对话框 -->
    <el-dialog v-model="detailVisible" title="报销单详情" width="600px">
      <el-descriptions :column="2" border v-if="currentClaim">
        <el-descriptions-item label="单号">{{ currentClaim.claimNo }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ formatAmount(currentClaim.amount) }} 元</el-descriptions-item>
        <el-descriptions-item label="事由" :span="2">{{ currentClaim.title }}</el-descriptions-item>
        <el-descriptions-item label="消费日期">{{ currentClaim.expenseDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentClaim.status)">{{ getStatusText(currentClaim.status) }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api'

const loading = ref(false)
const tableData = ref([])
const stats = reactive({ severe: 0, high: 0, medium: 0, low: 0 })
const detailVisible = ref(false)
const currentClaim = ref(null)

const getLevelType = (level) => {
  const types = { 1: 'success', 2: 'info', 3: 'warning', 4: 'danger' }
  return types[level] || ''
}

const getLevelText = (level) => {
  const texts = { 1: '低', 2: '中', 3: '高', 4: '严重' }
  return texts[level] || ''
}

const getRiskTypeName = (type) => {
  const names = {
    'WEEKEND_CLAIM': '周末报销',
    'HIGH_AMOUNT': '高金额',
    'HIGH_FREQUENCY': '高频报销',
    'BUDGET_EXCEED': '预算超支'
  }
  return names[type] || type
}

const formatAmount = (amount) => {
  if (!amount) return '0.00'
  return (amount / 100).toFixed(2)
}

const getStatusType = (status) => {
  const types = { 0: 'info', 1: 'warning', 2: 'primary', 3: 'danger', 4: 'success', 5: 'danger' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { 0: '草稿', 1: '待经理审批', 2: '待财务审计', 3: '经理驳回', 4: '已入账', 5: '财务驳回' }
  return texts[status] || '未知'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await api.getRiskAlerts()
    if (res.code === 200) {
      tableData.value = res.data || []
      stats.severe = tableData.value.filter(r => r.riskLevel === 4).length
      stats.high = tableData.value.filter(r => r.riskLevel === 3).length
      stats.medium = tableData.value.filter(r => r.riskLevel === 2).length
      stats.low = tableData.value.filter(r => r.riskLevel === 1).length
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 查看关联报销单
const viewClaim = async (row) => {
  try {
    const res = await api.getClaim(row.claimId)
    if (res.code === 200) {
      currentClaim.value = res.data
      detailVisible.value = true
    }
  } catch (e) {
    ElMessage.error('获取报销单信息失败')
  }
}

// 删除报销单
const deleteClaim = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除该报销单吗？此操作会删除关联的风险预警记录。`,
      '删除确认',
      { type: 'warning' }
    )
    const res = await api.deleteClaim(row.claimId)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.stat-card { text-align: center; padding: 20px 0; }
.stat-value { font-size: 32px; font-weight: bold; }
.stat-label { color: #999; margin-top: 8px; }
.stat-card.danger .stat-value { color: #f56c6c; }
.stat-card.warning .stat-value { color: #e6a23c; }
.stat-card.info .stat-value { color: #409eff; }
.stat-card.success .stat-value { color: #67c23a; }
</style>