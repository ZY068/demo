<template>
  <div class="finance-audit">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>财务审计 - 待处理报销单</span>
          <el-tag type="warning">{{ tableData.length }} 条待审计</el-tag>
        </div>
      </template>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无待审计报销单" />

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="claimNo" label="单号" width="160" />
        <el-table-column prop="title" label="报销事由" min-width="180" />
        <el-table-column label="金额(元)" width="100">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: bold">{{ formatAmount(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="expenseDate" label="消费日期" width="110" />
        <el-table-column label="风险评分" width="100">
          <template #default="{ row }">
            <div class="risk-cell" @click="showRiskDetail(row)" v-if="row.riskScore && row.riskScore > 0">
              <el-tag :type="getRiskType(row.riskScore)" effect="dark" style="cursor: pointer">
                {{ row.riskScore.toFixed(1) }}
              </el-tag>
              <el-icon v-if="row.riskScore >= 4" class="warning-icon">
                <Warning />
              </el-icon>
            </div>
            <span v-else style="color: #999">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default>
            <el-tag type="primary">待财务审计</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.riskScore && row.riskScore >= 4"
                       type="warning" size="small" @click="viewRiskAlerts(row)">
              查看预警
            </el-button>
            <el-button type="success" size="small" @click="handleAudit(row, true)">通过</el-button>
            <el-button type="danger" size="small" @click="handleAudit(row, false)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 审计对话框 -->
    <el-dialog v-model="dialogVisible" :title="auditAction ? '审计通过' : '审计驳回'" width="400px">
      <el-form>
        <el-form-item label="审计意见">
          <el-input v-model="comment" type="textarea" :rows="3" placeholder="请输入审计意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :type="auditAction ? 'success' : 'danger'" @click="submitAudit">确认</el-button>
      </template>
    </el-dialog>

    <!-- 风险详情对话框 -->
    <el-dialog v-model="riskDetailVisible" title="风险详情" width="650px">
      <div v-if="riskDetail" class="risk-detail">
        <el-alert
          v-if="riskDetail.riskScore && riskDetail.riskScore > 4"
          type="error"
          :closable="false"
          show-icon
          style="margin-bottom: 16px"
        >
          <template #title>该报销单存在较高风险，请仔细审核后再审批</template>
        </el-alert>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="风险评分">
            <el-tag :type="getRiskType(riskDetail.riskScore)" effect="dark" size="large">
              {{ riskDetail.riskScore ? riskDetail.riskScore.toFixed(1) : '0' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="风险等级">
            <el-tag :type="getRiskLevelTagType(riskDetail.riskLevel)" effect="plain">
              {{ getRiskLevelTextByLevel(riskDetail.riskLevel) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">风险预警明细</el-divider>

        <el-table :data="riskDetail.alerts" size="small" v-if="riskDetail.alerts && riskDetail.alerts.length > 0">
          <el-table-column prop="riskType" label="风险类型" width="120">
            <template #default="{ row }">{{ getRiskTypeName(row.riskType) }}</template>
          </el-table-column>
          <el-table-column prop="alertMessage" label="预警信息" min-width="200" />
          <el-table-column prop="riskLevel" label="等级" width="80">
            <template #default="{ row }">
              <el-tag :type="getLevelTagType(row.riskLevel)" size="small">
                {{ getLevelText(row.riskLevel) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-else description="暂无风险预警" :image-size="60" />
      </div>

      <template #footer>
        <el-button @click="riskDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'
import api from '../../api'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const currentRow = ref(null)
const auditAction = ref(true)
const comment = ref('')
const riskDetailVisible = ref(false)
const riskDetail = ref(null)

const formatAmount = (amount) => {
  if (!amount) return '0.00'
  return (amount / 100).toFixed(2)
}

const getRiskType = (score) => {
  if (!score || score < 1) return 'success'  // 绿色：无风险
  if (score < 2) return 'info'               // 蓝色：低风险
  if (score < 4) return 'warning'            // 黄色：中风险
  return 'danger'                             // 红色：高/严重风险
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

const getLevelTagType = (level) => {
  const types = { 1: 'success', 2: 'info', 3: 'warning', 4: 'danger' }
  return types[level] || 'info'
}

const getLevelText = (level) => {
  const texts = { 1: '低', 2: '中', 3: '高', 4: '严重' }
  return texts[level] || '未知'
}

const getRiskLevelTagType = (level) => {
  const types = {
    'LOW': 'success',
    'MEDIUM': 'info',
    'HIGH': 'warning',
    'SEVERE': 'danger'
  }
  return types[level] || 'info'
}

const getRiskLevelTextByLevel = (level) => {
  const texts = {
    'LOW': '低风险',
    'MEDIUM': '中风险',
    'HIGH': '高风险',
    'SEVERE': '严重风险'
  }
  return texts[level] || '低风险'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await api.getPendingClaims()
    if (res.code === 200) {
      tableData.value = res.data || []
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleAudit = (row, approved) => {
  currentRow.value = row
  auditAction.value = approved
  comment.value = approved ? '审计通过' : ''
  dialogVisible.value = true
}

const submitAudit = async () => {
  if (!currentRow.value) return

  try {
    const res = await api.approveClaim(currentRow.value.id, auditAction.value, comment.value)
    if (res.code === 200) {
      ElMessage.success(auditAction.value ? '审计通过，已入账' : '审计驳回')
      dialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '操作失败')
  }
}

// 显示风险详情
const showRiskDetail = async (row) => {
  try {
    const res = await api.getRiskDetail(row.id)
    if (res.code === 200) {
      riskDetail.value = res.data
      riskDetailVisible.value = true
    }
  } catch (e) {
    ElMessage.error('获取风险详情失败')
  }
}

// 查看风险预警
const viewRiskAlerts = async (row) => {
  await showRiskDetail(row)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.finance-audit { padding: 0; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.risk-cell { display: flex; align-items: center; gap: 4px; }
.warning-icon { color: #e6a23c; animation: pulse 1s infinite; }
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
.risk-detail { padding: 0 10px; }
</style>