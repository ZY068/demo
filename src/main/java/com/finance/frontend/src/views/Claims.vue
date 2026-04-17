<template>
  <div class="claims-page">
    <!-- 顶部统计条 -->
    <div class="stats-bar">
      <div class="stat-pill" v-for="stat in statItems" :key="stat.label">
        <div class="pill-icon" :style="{ background: stat.bg }">
          <el-icon><component :is="stat.icon" /></el-icon>
        </div>
        <div class="pill-info">
          <span class="pill-value">{{ stat.value }}</span>
          <span class="pill-label">{{ stat.label }}</span>
        </div>
      </div>
    </div>

    <!-- 搜索筛选区 -->
    <div class="filter-bar">
      <div class="filter-inputs">
        <el-select v-model="query.status" placeholder="全部状态" clearable class="filter-select">
          <el-option label="待经理审批" :value="1" />
          <el-option label="待财务审计" :value="2" />
          <el-option label="经理驳回" :value="3" />
          <el-option label="已入账" :value="4" />
          <el-option label="财务驳回" :value="5" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="搜索事由或单号" clearable class="filter-input" @keyup.enter="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <el-button type="primary" class="search-btn" @click="loadData">
        <el-icon><Search /></el-icon> 搜索
      </el-button>
    </div>

    <!-- 数据表格 -->
    <div class="glass-card">
      <el-table :data="tableData" stripe v-loading="loading" style="width:100%" row-key="id">
        <el-table-column prop="claimNo" label="单号" width="160">
          <template #default="{ row }">
            <span class="claim-no">{{ row.claimNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="报销事由" min-width="200">
          <template #default="{ row }">
            <span class="claim-title">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="userId" label="申请人" width="100" />
        <el-table-column label="金额(元)" width="130">
          <template #default="{ row }">
            <span class="amount-cell" :class="{ 'amount-high': row.amount > 5000000 }">
              ¥ {{ formatAmount(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="expenseDate" label="消费日期" width="120" />
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <span class="status-badge" :class="`status-${row.status}`">
              {{ getStatusText(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="风险分" width="110" v-if="userRole === 2">
          <template #default="{ row }">
            <span v-if="row.riskScore && row.riskScore > 0" class="risk-score-badge" :class="getRiskClass(row.riskScore)" @click="showRiskDetail(row)">
              {{ row.riskScore.toFixed(1) }}
            </span>
            <span v-else class="risk-none">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="viewDetail(row)">查看</el-button>
            <el-button v-if="userRole === 2" type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer" v-if="pagination.total > 0">
        <span class="total-count">共 {{ pagination.total }} 条记录</span>
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无报销单" />
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="报销单详情" size="480px" direction="rtl">
      <div class="drawer-content" v-if="currentRow">
        <div class="detail-header">
          <div class="detail-no">{{ currentRow.claimNo }}</div>
          <span class="status-badge" :class="`status-${currentRow.status}`">
            {{ getStatusText(currentRow.status) }}
          </span>
        </div>

        <div class="detail-section">
          <div class="detail-row">
            <span class="detail-label">申请人ID</span>
            <span class="detail-value">{{ currentRow.userId }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">报销事由</span>
            <span class="detail-value">{{ currentRow.title }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">金额</span>
            <span class="detail-value amount-high">¥ {{ formatAmount(currentRow.amount) }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">消费日期</span>
            <span class="detail-value">{{ currentRow.expenseDate }}</span>
          </div>
          <div class="detail-row" v-if="currentRow.description">
            <span class="detail-label">详细说明</span>
            <span class="detail-value">{{ currentRow.description }}</span>
          </div>
          <div class="detail-row" v-if="currentRow.riskScore">
            <span class="detail-label">风险评分</span>
            <span class="risk-score-badge" :class="getRiskClass(currentRow.riskScore)">
              {{ currentRow.riskScore.toFixed(1) }} — {{ getRiskLevelText(currentRow.riskScore) }}
            </span>
          </div>
          <div class="detail-row" v-if="currentRow.auditComment">
            <span class="detail-label">审计意见</span>
            <span class="detail-value">{{ currentRow.auditComment }}</span>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 风险详情抽屉 -->
    <el-drawer v-model="riskDetailVisible" title="风险详情" size="520px" direction="rtl">
      <div class="risk-drawer" v-if="riskDetail">
        <el-alert v-if="riskDetail.riskScore && riskDetail.riskScore > 4" type="error" :closable="false" show-icon style="margin-bottom: 20px">
          <template #title>该报销单存在较高风险，请仔细审核后再审批</template>
        </el-alert>

        <div class="detail-section">
          <div class="detail-row">
            <span class="detail-label">风险评分</span>
            <span class="risk-score-badge large" :class="getRiskClass(riskDetail.riskScore)">
              {{ riskDetail.riskScore?.toFixed(1) || '0' }}
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">风险等级</span>
            <span class="detail-value">{{ getRiskLevelTextByLevel(riskDetail.riskLevel) }}</span>
          </div>
        </div>

        <div class="alerts-title">风险预警明细</div>
        <div v-if="riskDetail.alerts && riskDetail.alerts.length > 0" class="alerts-list">
          <div v-for="alert in riskDetail.alerts" :key="alert.id" class="alert-item">
            <div class="alert-indicator" :class="`level-${alert.riskLevel}`"></div>
            <div class="alert-info">
              <div class="alert-type">{{ getRiskTypeName(alert.riskType) }}</div>
              <div class="alert-msg">{{ alert.alertMessage }}</div>
            </div>
            <span class="alert-level" :class="`level-tag-${alert.riskLevel}`">{{ getLevelText(alert.riskLevel) }}</span>
          </div>
        </div>
        <el-empty v-else description="暂无风险预警" :image-size="60" />

        <div class="drawer-actions">
          <el-button @click="riskDetailVisible = false">关闭</el-button>
          <el-button type="primary" @click="handleReDetect">重新检测</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 右下角新建按钮 -->
    <el-button v-if="canCreate" class="fab-btn" type="primary" @click="$router.push('/claims/create')">
      <el-icon><Plus /></el-icon>
    </el-button>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { Plus, Search, Document, Clock, Check, Close } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const loading = ref(false)
const tableData = ref([])
const detailVisible = ref(false)
const currentRow = ref(null)
const riskDetailVisible = ref(false)
const riskDetail = ref(null)
const reDetecting = ref(false)

const query = reactive({ status: null, keyword: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })

const statItems = computed(() => [
  { label: '报销单总量', value: stats.total, icon: 'Document', bg: 'rgba(96,165,250,0.3)' },
  { label: '待审批', value: stats.pending, icon: 'Clock', bg: 'rgba(251,191,36,0.3)' },
  { label: '已驳回', value: stats.rejected, icon: 'Close', bg: 'rgba(248,113,113,0.3)' },
  { label: '已入账', value: stats.approved, icon: 'Check', bg: 'rgba(52,211,153,0.3)' },
])

const stats = reactive({ total: 0, pending: 0, rejected: 0, approved: 0 })

const userRole = computed(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) return JSON.parse(userStr).role
  return null
})

const canCreate = computed(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const role = JSON.parse(userStr).role
    return role === 0 || role === 1
  }
  return false
})

const formatAmount = (amount) => (amount ? (amount / 100).toFixed(2) : '0.00')

const getStatusText = (status) => {
  const t = { 0: '草稿', 1: '待经理审批', 2: '待财务审计', 3: '经理驳回', 4: '已入账', 5: '财务驳回' }
  return t[status] || '未知'
}

const getRiskClass = (score) => {
  if (!score || score < 1) return 'risk-none'
  if (score < 2) return 'risk-low'
  if (score < 4) return 'risk-mid'
  if (score < 6) return 'risk-high'
  return 'risk-severe'
}

const getRiskLevelText = (score) => {
  if (!score || score < 1) return '无风险'
  if (score < 2) return '低风险'
  if (score < 4) return '中风险'
  if (score < 6) return '高风险'
  return '严重风险'
}

const getRiskLevelTextByLevel = (level) => {
  const t = { 'LOW': '低风险', 'MEDIUM': '中风险', 'HIGH': '高风险', 'SEVERE': '严重风险' }
  return t[level] || '低风险'
}

const getRiskTypeName = (type) => {
  const t = { 'WEEKEND_CLAIM': '周末报销', 'HIGH_AMOUNT': '高金额', 'HIGH_FREQUENCY': '高频报销', 'BUDGET_EXCEED': '预算超支' }
  return t[type] || type
}

const getLevelText = (level) => ({ 1: '低', 2: '中', 3: '高', 4: '严重' }[level] || '未知')

const loadData = async () => {
  loading.value = true
  try {
    const res = await api.getClaims()
    if (res.code === 200 && res.data) {
      let data = [...res.data]
      if (query.status !== null && query.status !== '') data = data.filter(d => d.status === query.status)
      if (query.keyword) data = data.filter(d => (d.title?.includes(query.keyword)) || (d.claimNo?.includes(query.keyword)))
      tableData.value = data
      pagination.total = data.length
      stats.total = data.length
      stats.pending = data.filter(d => d.status === 1 || d.status === 2).length
      stats.rejected = data.filter(d => d.status === 3 || d.status === 5).length
      stats.approved = data.filter(d => d.status === 4).length
    }
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

const viewDetail = (row) => { currentRow.value = row; detailVisible.value = true }

const showRiskDetail = async (row) => {
  try {
    const res = await api.getRiskDetail(row.id)
    if (res.code === 200) { riskDetail.value = res.data; riskDetailVisible.value = true }
  } catch { ElMessage.error('获取风险详情失败') }
}

const handleReDetect = async () => {
  if (!riskDetail.value) return
  reDetecting.value = true
  try {
    const res = await api.reDetectRisk(riskDetail.value.claimId)
    if (res.code === 200) { ElMessage.success('重新检测完成'); riskDetail.value = res.data; loadData() }
  } catch { ElMessage.error('重新检测失败') }
  finally { reDetecting.value = false }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除报销单 ${row.claimNo} 吗？此操作会留下审计痕迹。`, '删除确认', { type: 'warning' })
    const res = await api.deleteClaim(row.id)
    if (res.code === 200) { ElMessage.success('删除成功'); loadData() }
    else ElMessage.error(res.msg || '删除失败')
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

onMounted(() => loadData())
</script>

<style scoped>
.claims-page { padding: 0; min-height: 100%; }

/* 统计条 */
.stats-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.stat-pill {
  flex: 1;
  min-width: 140px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(31, 38, 135, 0.15);
  transition: transform 0.2s ease;
}

.stat-pill:hover { transform: translateY(-2px); }

.pill-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  flex-shrink: 0;
}

.pill-info { display: flex; flex-direction: column; }
.pill-value { font-size: 22px; font-weight: 700; color: #fff; line-height: 1.2; }
.pill-label { font-size: 12px; color: rgba(255,255,255,0.65); margin-top: 2px; }

/* 搜索条 */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 14px;
  margin-bottom: 20px;
}

.filter-inputs { display: flex; gap: 12px; flex: 1; }

:deep(.filter-select .el-input__wrapper),
:deep(.filter-input .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.1) !important;
  box-shadow: 0 0 0 1px rgba(255,255,255,0.2) inset !important;
  border-radius: 10px;
}

:deep(.filter-select .el-input__inner),
:deep(.filter-input .el-input__inner) {
  color: #fff !important;
}

:deep(.filter-select .el-input__prefix .el-icon),
:deep(.filter-input .el-input__prefix .el-icon) {
  color: rgba(255,255,255,0.6) !important;
}

.search-btn {
  background: rgba(96, 165, 250, 0.4) !important;
  border: 1px solid rgba(96, 165, 250, 0.3) !important;
  color: #fff !important;
  border-radius: 10px;
  white-space: nowrap;
}

/* 玻璃表格卡 */
.glass-card {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 16px;
  padding: 0;
  overflow: hidden;
}

.claim-no { font-family: monospace; color: rgba(255,255,255,0.8); font-size: 13px; }
.claim-title { color: #fff; font-weight: 500; }

.amount-cell { color: #a5b4fc; font-weight: 600; }
.amount-cell.amount-high { color: #fca5a5; }

/* 状态徽章 */
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid transparent;
}

.status-0 { background: rgba(255,255,255,0.1); color: rgba(255,255,255,0.7); border-color: rgba(255,255,255,0.2); }
.status-1 { background: rgba(251,191,36,0.2); color: #fbbf24; border-color: rgba(251,191,36,0.3); }
.status-2 { background: rgba(96,165,250,0.2); color: #60a5fa; border-color: rgba(96,165,250,0.3); }
.status-3 { background: rgba(248,113,113,0.2); color: #f87171; border-color: rgba(248,113,113,0.3); }
.status-4 { background: rgba(52,211,153,0.2); color: #34d399; border-color: rgba(52,211,153,0.3); }
.status-5 { background: rgba(248,113,113,0.2); color: #f87171; border-color: rgba(248,113,113,0.3); }

/* 风险分 */
.risk-score-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 24px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  border: 1px solid transparent;
}

.risk-score-badge.large { width: auto; padding: 0 12px; height: 30px; font-size: 14px; }

.risk-none { color: rgba(255,255,255,0.3); }
.risk-low { background: rgba(52,211,153,0.2); color: #34d399; border-color: rgba(52,211,153,0.3); }
.risk-mid { background: rgba(96,165,250,0.2); color: #60a5fa; border-color: rgba(96,165,250,0.3); }
.risk-high { background: rgba(251,191,36,0.2); color: #fbbf24; border-color: rgba(251,191,36,0.3); }
.risk-severe { background: rgba(248,113,113,0.2); color: #f87171; border-color: rgba(248,113,113,0.3); animation: pulse 1.5s infinite; }

@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.7; } }

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-top: 1px solid rgba(255,255,255,0.1);
}

.total-count { color: rgba(255,255,255,0.5); font-size: 13px; }

/* 抽屉 */
:deep(.el-drawer) {
  background: rgba(20, 10, 50, 0.95) !important;
  backdrop-filter: blur(20px);
  border-left: 1px solid rgba(255,255,255,0.15);
}

:deep(.el-drawer__header) {
  color: #fff !important;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  padding: 20px 24px;
  margin-bottom: 0;
}

:deep(.el-drawer__body) { padding: 0; }

.drawer-content { padding: 24px; color: #fff; }

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.detail-no {
  font-family: monospace;
  font-size: 16px;
  color: rgba(255,255,255,0.7);
}

.detail-section { display: flex; flex-direction: column; gap: 2px; }

.detail-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}

.detail-label {
  font-size: 12px;
  color: rgba(255,255,255,0.45);
  min-width: 80px;
  flex-shrink: 0;
  padding-top: 1px;
}

.detail-value { color: #fff; font-size: 14px; line-height: 1.5; }

.alerts-title {
  font-size: 13px;
  color: rgba(255,255,255,0.5);
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 20px 0 12px;
}

.alerts-list { display: flex; flex-direction: column; gap: 10px; }

.alert-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(255,255,255,0.06);
  border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.1);
}

.alert-indicator {
  width: 4px;
  height: 36px;
  border-radius: 2px;
  flex-shrink: 0;
  margin-top: 2px;
}

.level-1 .alert-indicator, .level-tag-1 { background: #34d399; }
.level-2 .alert-indicator, .level-tag-2 { background: #60a5fa; }
.level-3 .alert-indicator, .level-tag-3 { background: #fbbf24; }
.level-4 .alert-indicator, .level-tag-4 { background: #f87171; }

.alert-info { flex: 1; }
.alert-type { font-size: 13px; color: #fff; font-weight: 600; margin-bottom: 4px; }
.alert-msg { font-size: 12px; color: rgba(255,255,255,0.6); }

.alert-level {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.level-tag-1 { background: rgba(52,211,153,0.2); color: #34d399; }
.level-tag-2 { background: rgba(96,165,250,0.2); color: #60a5fa; }
.level-tag-3 { background: rgba(251,191,36,0.2); color: #fbbf24; }
.level-tag-4 { background: rgba(248,113,113,0.2); color: #f87171; }

.drawer-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding: 20px 0 0;
  border-top: 1px solid rgba(255,255,255,0.1);
  margin-top: 20px;
}

/* 右下角悬浮按钮 */
.fab-btn {
  position: fixed !important;
  right: 32px;
  bottom: 32px;
  width: 56px;
  height: 56px;
  border-radius: 50% !important;
  background: linear-gradient(135deg, rgba(96,165,250,0.9), rgba(99,102,241,0.9)) !important;
  border: none !important;
  box-shadow: 0 8px 24px rgba(99,102,241,0.5) !important;
  font-size: 24px;
  color: #fff !important;
  z-index: 100;
  transition: all 0.3s ease !important;
}

.fab-btn:hover {
  transform: scale(1.1) !important;
  box-shadow: 0 12px 36px rgba(99,102,241,0.7) !important;
}
</style>
