<template>
  <div style="padding: 20px;">
    <el-card shadow="hover">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span><el-icon><List /></el-icon> 财务终审工作台</span>
          <div>
            <el-button @click="fetchAll" :type="!isRiskOnly ? 'primary' : ''">全部单据</el-button>
            <el-button @click="fetchRisk" :type="isRiskOnly ? 'danger' : ''">风险探针</el-button>
          </div>
        </div>
      </template>

      <el-table :data="displayList" border stripe :row-class-name="tableRowClassName">
        <el-table-column prop="expenseDate" label="消费日期" width="120" />
        <el-table-column prop="title" label="报销事由" />
        <el-table-column label="金额">
          <template #default="s">￥{{ (s.row.amount/100).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="s">
            <el-tag :type="getStatusType(s.row.status)">{{ getStatusText(s.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审计判定" width="120">
          <template #default="s">
            <span v-if="isWeekend(s.row.expenseDate)" style="color: #f56c6c; font-weight: bold;">⚠️ 异常</span>
            <span v-else style="color: #67c23a;">合规</span>
          </template>
        </el-table-column>

        <el-table-column label="审计操作" width="200">
  <template #default="scope">
    <el-button 
      size="small" 
      type="success" 
      :disabled="scope.row.status === 1"
      @click="handleAudit(scope.row, 1)">
      同意入账
    </el-button>

    <el-button 
      size="small" 
      type="danger" 
      :disabled="scope.row.status === 2"
      @click="handleAudit(scope.row, 2)">
      驳回单据
    </el-button>
  </template>
</el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const displayList = ref([])
const isRiskOnly = ref(false)

const fetchAll = async () => {
  isRiskOnly.value = false
  const res = await axios.get('http://localhost:8080/api/claims/list')
  if (res.data.code === 200) displayList.value = res.data.data
}

const fetchRisk = async () => {
  isRiskOnly.value = true
  const res = await axios.get('http://localhost:8080/api/claims/high-risk')
  if (res.data.code === 200) displayList.value = res.data.data
}

// 💡 核心修改：带备注的审计
// 💡 确保这个函数内部能正确触发页面重载
const handleAudit = (row, targetStatus) => {
  const title = targetStatus === 1 ? '确认合规' : '判定违规';
  
  ElMessageBox.prompt(`请输入审计意见：`, title, {
    confirmButtonText: '提交',
    cancelButtonText: '取消',
  }).then(async ({ value }) => {
    // 1. 发送请求
    const res = await axios.post('http://localhost:8080/api/claims/audit-v2', {
      id: row.id,
      status: targetStatus,
      comment: value
    });

    if (res.data.code === 200) {
      ElMessage.success('操作成功：单据状态已更新并存证');
      
      // 2. 💡 重点：暴力刷新！不管在哪个模式，都重新查一遍
      if (isRiskOnly.value) {
        await fetchRisk(); // 如果在风险探针模式，刷新风险列表
      } else {
        await fetchAll();  // 如果在全量模式，刷新全量列表
      }
    }
  }).catch(() => {});
}

const isWeekend = (dateStr) => {
  const day = new Date(dateStr).getDay()
  return day === 0 || day === 6
}
const tableRowClassName = ({ row }) => isWeekend(row.expenseDate) ? 'warning-row' : ''
const getStatusText = (s) => ({ 0: '待审批', 1: '已通过', 2: '已驳回' }[s] || '未知')
const getStatusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info')

onMounted(fetchAll)
</script>

<style>
.el-table .warning-row { background: #fff5f5 !important; }
</style>