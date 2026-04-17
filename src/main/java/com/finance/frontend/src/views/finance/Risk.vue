<template>
  <div class="risk-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">风险预警</h2>
    </div>

    <!-- 四个等级指标卡 -->
    <div class="risk-stats">
      <div class="risk-stat-card severe" @click="filterByLevel(4)">
        <div class="stat-icon"><el-icon><WarningFilled /></el-icon></div>
        <div class="stat-body">
          <span class="stat-value">{{ stats.severe }}</span>
          <span class="stat-label">严重风险</span>
        </div>
      </div>
      <div class="risk-stat-card high" @click="filterByLevel(3)">
        <div class="stat-icon"><el-icon><WarnTriangleFilled /></el-icon></div>
        <div class="stat-body">
          <span class="stat-value">{{ stats.high }}</span>
          <span class="stat-label">高风险</span>
        </div>
      </div>
      <div class="risk-stat-card medium" @click="filterByLevel(2)">
        <div class="stat-icon"><el-icon><InfoFilled /></el-icon></div>
        <div class="stat-body">
          <span class="stat-value">{{ stats.medium }}</span>
          <span class="stat-label">中风险</span>
        </div>
      </div>
      <div class="risk-stat-card low" @click="filterByLevel(1)">
        <div class="stat-icon"><el-icon><CircleCheckFilled /></el-icon></div>
        <div class="stat-body">
          <span class="stat-value">{{ stats.low }}</span>
          <span class="stat-label">低风险</span>
        </div>
      </div>
    </div>

    <!-- 风险预警列表 -->
    <div class="glass-card">
      <div class="card-header-row">
        <span class="card-title">风险预警列表</span>
        <span class="alert-count" v-if="filteredData.length > 0">{{ filteredData.length }} 条预警</span>
      </div>

      <div v-if="!loading && filteredData.length > 0" class="risk-list">
        <div v-for="row in filteredData" :key="row.id" class="risk-item">
          <div class="risk-indicator" :class="`level-${row.riskLevel}`"></div>
          <div class="risk-content">
            <div class="risk-top">
              <span class="risk-type">{{ getRiskTypeName(row.riskType) }}</span>
              <span class="risk-level-badge" :class="`level-badge-${row.riskLevel}`">{{ getLevelText(row.riskLevel) }}</span>
            </div>
            <div class="risk-message">{{ row.alertMessage }}</div>
            <div class="risk-meta">
              <span class="risk-time">{{ row.createdAt }}</span>
            </div>
          </div>
          <div class="risk-actions">
            <el-button type="primary" size="small" link @click="viewClaim(row)">查看报销单</el-button>
            <el-button type="danger" size="small" link @click="deleteClaim(row)">删除</el-button>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && filteredData.length === 0" description="暂无风险预警" />
    </div>

    <!-- 报销单详情抽屉 -->
    <el-drawer v-model="detailVisible" title="报销单详情" size="460px" direction="rtl">
      <div class="drawer-inner" v-if="currentClaim">
        <div class="claim-header">
          <span class="claim-no">{{ currentClaim.claimNo }}</span>
          <span class="status-badge" :class="`status-${currentClaim.status}`">{{ getStatusText(currentClaim.status) }}</span>
        </div>
        <div class="detail-section">
          <div class="detail-row">
            <span class="detail-label">金额</span>
            <span class="detail-value amount-high">¥ {{ formatAmount(currentClaim.amount) }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">事由</span>
            <span class="detail-value">{{ currentClaim.title }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">消费日期</span>
            <span class="detail-value">{{ currentClaim.expenseDate }}</span>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { WarningFilled, WarnTriangleFilled, InfoFilled, CircleCheckFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api'

const loading = ref(false)
const tableData = ref([])
const stats = reactive({ severe: 0, high: 0, medium: 0, low: 0 })
const detailVisible = ref(false)
const currentClaim = ref(null)
const activeLevel = ref(null)

const filteredData = computed(() => {
  if (!activeLevel.value) return tableData.value
  return tableData.value.filter(r => r.riskLevel === activeLevel.value)
})

const formatAmount = (amount) => (amount ? (amount / 100).toFixed(2) : '0.00')
const getRiskTypeName = (type) => ({ 'WEEKEND_CLAIM': '周末报销', 'HIGH_AMOUNT': '高金额', 'HIGH_FREQUENCY': '高频报销', 'BUDGET_EXCEED': '预算超支' }[type] || type)
const getLevelText = (level) => ({ 1: '低', 2: '中', 3: '高', 4: '严重' }[level] || '未知')
const getStatusText = (status) => ({ 0: '草稿', 1: '待经理审批', 2: '待财务审计', 3: '经理驳回', 4: '已入账', 5: '财务驳回' }[status] || '未知')

const filterByLevel = (level) => { activeLevel.value = activeLevel.value === level ? null : level }

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
  } catch {} finally { loading.value = false }
}

const viewClaim = async (row) => {
  try {
    const res = await api.getClaim(row.claimId)
    if (res.code === 200) { currentClaim.value = res.data; detailVisible.value = true }
  } catch { ElMessage.error('获取报销单信息失败') }
}

const deleteClaim = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除该报销单吗？此操作会删除关联的风险预警记录。`, '删除确认', { type: 'warning' })
    const res = await api.deleteClaim(row.claimId)
    if (res.code === 200) { ElMessage.success('删除成功'); loadData() }
    else ElMessage.error(res.msg || '删除失败')
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

onMounted(() => loadData())
</script>

<style scoped>
.risk-page { padding: 0; min-height: 100%; }

.page-header { margin-bottom: 24px; }
.page-title { font-size: 22px; font-weight: 700; color: #fff; margin: 0; }

/* 风险等级指标卡 */
.risk-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }

.risk-stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 16px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.25s ease;
  background: rgba(255,255,255,0.08);
  backdrop-filter: blur(12px);
  box-shadow: 0 4px 16px rgba(31,38,135,0.12);
}

.risk-stat-card:hover { transform: translateY(-3px); box-shadow: 0 8px 30px rgba(31,38,135,0.25); }

.risk-stat-card.severe { border-color: rgba(248,113,113,0.3); background: rgba(248,113,113,0.12); }
.risk-stat-card.high { border-color: rgba(251,191,36,0.3); background: rgba(251,191,36,0.1); }
.risk-stat-card.medium { border-color: rgba(96,165,250,0.3); background: rgba(96,165,250,0.1); }
.risk-stat-card.low { border-color: rgba(52,211,153,0.3); background: rgba(52,211,153,0.1); }

.stat-icon { font-size: 28px; flex-shrink: 0; }
.severe .stat-icon { color: #f87171; }
.high .stat-icon { color: #fbbf24; }
.medium .stat-icon { color: #60a5fa; }
.low .stat-icon { color: #34d399; }

.stat-body { display: flex; flex-direction: column; }
.stat-value { font-size: 28px; font-weight: 800; color: #fff; line-height: 1.1; }
.stat-label { font-size: 13px; color: rgba(255,255,255,0.6); margin-top: 2px; }

/* 玻璃卡片 */
.glass-card {
  background: rgba(255,255,255,0.08);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: 18px;
  overflow: hidden;
}

.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.card-title { font-size: 15px; font-weight: 600; color: #fff; }
.alert-count {
  font-size: 12px;
  padding: 2px 10px;
  background: rgba(248,113,113,0.2);
  border: 1px solid rgba(248,113,113,0.3);
  border-radius: 20px;
  color: #f87171;
}

/* 风险列表 */
.risk-list { display: flex; flex-direction: column; }

.risk-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 18px 24px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  transition: background 0.2s;
}

.risk-item:last-child { border-bottom: none; }
.risk-item:hover { background: rgba(255,255,255,0.04); }

.risk-indicator { width: 4px; height: 60px; border-radius: 2px; flex-shrink: 0; margin-top: 4px; }
.level-1, .level-badge-1 { background: #34d399; }
.level-2, .level-badge-2 { background: #60a5fa; }
.level-3, .level-badge-3 { background: #fbbf24; }
.level-4, .level-badge-4 { background: #f87171; }

.risk-content { flex: 1; display: flex; flex-direction: column; gap: 6px; }
.risk-top { display: flex; align-items: center; gap: 10px; }

.risk-type { font-size: 14px; font-weight: 600; color: #fff; }
.risk-level-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.level-badge-1 { background: rgba(52,211,153,0.2); color: #34d399; }
.level-badge-2 { background: rgba(96,165,250,0.2); color: #60a5fa; }
.level-badge-3 { background: rgba(251,191,36,0.2); color: #fbbf24; }
.level-badge-4 { background: rgba(248,113,113,0.2); color: #f87171; }

.risk-message { font-size: 13px; color: rgba(255,255,255,0.65); line-height: 1.5; }
.risk-meta { display: flex; gap: 12px; }
.risk-time { font-size: 12px; color: rgba(255,255,255,0.35); }

.risk-actions { display: flex; flex-direction: column; gap: 4px; align-items: flex-end; flex-shrink: 0; }

/* 抽屉 */
:deep(.el-drawer) { background: rgba(15, 10, 40, 0.97) !important; backdrop-filter: blur(20px); border-left: 1px solid rgba(255,255,255,0.12); }
:deep(.el-drawer__header) { color: #fff !important; border-bottom: 1px solid rgba(255,255,255,0.1); padding: 20px 24px; margin-bottom: 0; }
:deep(.el-drawer__body) { padding: 0; }

.drawer-inner { padding: 24px; color: #fff; }

.claim-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.claim-no { font-family: monospace; font-size: 15px; color: rgba(255,255,255,0.7); }

.status-badge {
  display: inline-flex; align-items: center; padding: 3px 10px;
  border-radius: 20px; font-size: 12px; font-weight: 500; border: 1px solid transparent;
}
.status-0 { background: rgba(255,255,255,0.1); color: rgba(255,255,255,0.7); border-color: rgba(255,255,255,0.2); }
.status-1 { background: rgba(251,191,36,0.2); color: #fbbf24; border-color: rgba(251,191,36,0.3); }
.status-2 { background: rgba(96,165,250,0.2); color: #60a5fa; border-color: rgba(96,165,250,0.3); }
.status-3, .status-5 { background: rgba(248,113,113,0.2); color: #f87171; border-color: rgba(248,113,113,0.3); }
.status-4 { background: rgba(52,211,153,0.2); color: #34d399; border-color: rgba(52,211,153,0.3); }

.detail-section { display: flex; flex-direction: column; gap: 2px; }
.detail-row { display: flex; gap: 16px; padding: 12px 0; border-bottom: 1px solid rgba(255,255,255,0.08); }
.detail-label { font-size: 12px; color: rgba(255,255,255,0.45); min-width: 70px; padding-top: 1px; }
.detail-value { font-size: 14px; color: #fff; line-height: 1.5; }
.amount-high { color: #f87171; font-weight: 700; }

@media (max-width: 768px) {
  .risk-stats { grid-template-columns: repeat(2, 1fr); }
}
</style>
