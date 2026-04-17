<template>
  <div class="finance-audit">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">财务审计</h2>
        <span class="pending-badge" v-if="tableData.length > 0">{{ tableData.length }} 条待审计</span>
      </div>
    </div>

    <!-- 审计卡片列表 -->
    <div v-if="!loading && tableData.length > 0" class="audit-grid">
      <div
        v-for="row in tableData"
        :key="row.id"
        class="audit-card"
        :class="{ 'high-risk': row.riskScore && row.riskScore >= 4 }"
      >
        <div class="card-left-bar" :class="row.riskScore && row.riskScore >= 4 ? 'bar-danger' : 'bar-normal'"></div>

        <div class="card-body">
          <div class="card-top">
            <div class="card-meta">
              <span class="card-no">{{ row.claimNo }}</span>
              <span class="card-date">{{ row.expenseDate }}</span>
            </div>
            <div class="card-right">
              <span class="card-amount">¥ {{ formatAmount(row.amount) }}</span>
              <span v-if="row.riskScore && row.riskScore > 0" class="risk-badge" :class="getRiskClass(row.riskScore)">
                {{ row.riskScore.toFixed(1) }}
                <el-icon v-if="row.riskScore >= 4" class="warning-pulse"><Warning /></el-icon>
              </span>
            </div>
          </div>

          <div class="card-title">{{ row.title }}</div>

          <div class="card-footer">
            <div class="card-applicant">
              <div class="avatar">{{ (row.userId || 'U').toString().charAt(0) }}</div>
              <span>申请人: {{ row.userId }}</span>
            </div>
            <div class="audit-actions">
              <el-button v-if="row.riskScore && row.riskScore >= 4" class="btn-warning" size="small" @click="showRiskDetail(row)">
                <el-icon><Warning /></el-icon> 预警
              </el-button>
              <el-button class="btn-reject" size="small" @click="handleAudit(row, false)">
                <el-icon><Close /></el-icon> 驳回
              </el-button>
              <el-button class="btn-approve" size="small" type="primary" @click="handleAudit(row, true)">
                <el-icon><Check /></el-icon> 通过
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="!loading && tableData.length === 0" class="empty-state">
      <div class="empty-icon"><el-icon><CircleCheck /></el-icon></div>
      <h3>暂无待审计报销单</h3>
      <p>所有报销单均已处理完毕</p>
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="36"><Loading /></el-icon>
    </div>

    <!-- 审计抽屉 -->
    <el-drawer v-model="dialogVisible" :title="auditAction ? '审计通过' : '审计驳回'" size="440px" direction="rtl">
      <div class="drawer-inner" v-if="currentRow">
        <div class="drawer-info">
          <div class="info-row">
            <span class="info-label">单号</span>
            <span class="info-value mono">{{ currentRow.claimNo }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">金额</span>
            <span class="info-value amount-high">¥ {{ formatAmount(currentRow.amount) }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">事由</span>
            <span class="info-value">{{ currentRow.title }}</span>
          </div>
        </div>

        <div class="comment-section">
          <label class="comment-label">审计意见</label>
          <el-input v-model="comment" type="textarea" :rows="4" :placeholder="auditAction ? '选填' : '请输入驳回原因'" />
        </div>

        <div class="drawer-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button :type="auditAction ? 'primary' : 'danger'" @click="submitAudit">
            {{ auditAction ? '确认通过' : '确认驳回' }}
          </el-button>
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
            <span class="risk-badge large" :class="getRiskClass(riskDetail.riskScore)">{{ riskDetail.riskScore?.toFixed(1) || '0' }}</span>
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
            <span class="level-tag" :class="`level-tag-${alert.riskLevel}`">{{ getLevelText(alert.riskLevel) }}</span>
          </div>
        </div>
        <el-empty v-else description="暂无风险预警" :image-size="60" />

        <div class="drawer-actions">
          <el-button @click="riskDetailVisible = false">关闭</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Check, Close, Warning, CircleCheck, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../../api'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const currentRow = ref(null)
const auditAction = ref(true)
const comment = ref('')
const riskDetailVisible = ref(false)
const riskDetail = ref(null)

const formatAmount = (amount) => (amount ? (amount / 100).toFixed(2) : '0.00')

const getRiskClass = (score) => {
  if (!score || score < 1) return 'risk-none'
  if (score < 2) return 'risk-low'
  if (score < 4) return 'risk-mid'
  if (score < 6) return 'risk-high'
  return 'risk-severe'
}

const getRiskLevelTextByLevel = (level) => ({ 'LOW': '低风险', 'MEDIUM': '中风险', 'HIGH': '高风险', 'SEVERE': '严重风险' }[level] || '低风险')
const getRiskTypeName = (type) => ({ 'WEEKEND_CLAIM': '周末报销', 'HIGH_AMOUNT': '高金额', 'HIGH_FREQUENCY': '高频报销', 'BUDGET_EXCEED': '预算超支' }[type] || type)
const getLevelText = (level) => ({ 1: '低', 2: '中', 3: '高', 4: '严重' }[level] || '未知')

const loadData = async () => {
  loading.value = true
  try {
    const res = await api.getPendingClaims()
    if (res.code === 200) tableData.value = res.data || []
  } catch {} finally { loading.value = false }
}

const handleAudit = (row, approved) => {
  currentRow.value = row
  auditAction.value = approved
  comment.value = approved ? '' : ''
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
    } else { ElMessage.error(res.msg || '操作失败') }
  } catch { ElMessage.error('操作失败') }
}

const showRiskDetail = async (row) => {
  try {
    const res = await api.getRiskDetail(row.id)
    if (res.code === 200) { riskDetail.value = res.data; riskDetailVisible.value = true }
  } catch { ElMessage.error('获取风险详情失败') }
}

onMounted(() => loadData())
</script>

<style scoped>
.finance-audit { padding: 0; min-height: 100%; }

.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.header-left { display: flex; align-items: center; gap: 16px; }
.page-title { font-size: 22px; font-weight: 700; color: #fff; margin: 0; }
.pending-badge {
  display: inline-flex; align-items: center; padding: 4px 14px;
  background: rgba(96,165,250,0.2); border: 1px solid rgba(96,165,250,0.3);
  border-radius: 20px; color: #60a5fa; font-size: 13px; font-weight: 600;
}

/* 审计卡片 */
.audit-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 20px; }

.audit-card {
  display: flex;
  background: rgba(255,255,255,0.08);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: 18px;
  overflow: hidden;
  transition: all 0.25s ease;
  box-shadow: 0 4px 20px rgba(31,38,135,0.15);
}

.audit-card:hover { transform: translateY(-3px); border-color: rgba(96,165,250,0.35); box-shadow: 0 12px 40px rgba(31,38,135,0.3); }
.audit-card.high-risk { border-color: rgba(248,113,113,0.3); }

.card-left-bar { width: 5px; flex-shrink: 0; }
.bar-normal { background: rgba(96,165,250,0.5); }
.bar-danger { background: #f87171; box-shadow: 0 0 12px rgba(248,113,113,0.4); }

.card-body { flex: 1; padding: 20px; display: flex; flex-direction: column; gap: 12px; }

.card-top { display: flex; justify-content: space-between; align-items: flex-start; }
.card-meta { display: flex; flex-direction: column; gap: 4px; }
.card-no { font-family: monospace; font-size: 13px; color: rgba(255,255,255,0.6); }
.card-date { font-size: 12px; color: rgba(255,255,255,0.4); }

.card-right { display: flex; flex-direction: column; align-items: flex-end; gap: 6px; }
.card-amount { font-size: 20px; font-weight: 800; color: #f87171; }

.risk-badge {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 3px 10px; border-radius: 6px; font-size: 12px; font-weight: 700;
  border: 1px solid transparent;
}
.risk-badge.large { padding: 4px 12px; font-size: 14px; }
.risk-none { background: rgba(255,255,255,0.08); color: rgba(255,255,255,0.4); }
.risk-low { background: rgba(52,211,153,0.2); color: #34d399; border-color: rgba(52,211,153,0.3); }
.risk-mid { background: rgba(96,165,250,0.2); color: #60a5fa; border-color: rgba(96,165,250,0.3); }
.risk-high { background: rgba(251,191,36,0.2); color: #fbbf24; border-color: rgba(251,191,36,0.3); }
.risk-severe { background: rgba(248,113,113,0.2); color: #f87171; border-color: rgba(248,113,113,0.3); }

.warning-pulse { animation: blink 1s infinite; }
@keyframes blink { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }

.card-title { font-size: 15px; font-weight: 600; color: #fff; }

.card-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding-top: 12px; border-top: 1px solid rgba(255,255,255,0.1); margin-top: auto;
}

.card-applicant { display: flex; align-items: center; gap: 8px; font-size: 13px; color: rgba(255,255,255,0.6); }
.avatar {
  width: 30px; height: 30px; border-radius: 50%;
  background: rgba(96,165,250,0.3); border: 1px solid rgba(96,165,250,0.4);
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; color: #60a5fa; font-weight: 700;
}

.audit-actions { display: flex; gap: 8px; }

.btn-approve {
  background: rgba(52,211,153,0.25) !important; border: 1px solid rgba(52,211,153,0.4) !important;
  color: #34d399 !important; border-radius: 10px !important;
}
.btn-approve:hover { background: rgba(52,211,153,0.45) !important; box-shadow: 0 0 12px rgba(52,211,153,0.3) !important; }

.btn-reject {
  background: rgba(248,113,113,0.15) !important; border: 1px solid rgba(248,113,113,0.3) !important;
  color: #f87171 !important; border-radius: 10px !important;
}
.btn-reject:hover { background: rgba(248,113,113,0.3) !important; box-shadow: 0 0 12px rgba(248,113,113,0.3) !important; }

.btn-warning {
  background: rgba(251,191,36,0.15) !important; border: 1px solid rgba(251,191,36,0.3) !important;
  color: #fbbf24 !important; border-radius: 10px !important;
}

/* 空/加载状态 */
.empty-state { text-align: center; padding: 80px 40px; background: rgba(255,255,255,0.06); border-radius: 20px; border: 1px solid rgba(255,255,255,0.1); }
.empty-icon { width: 72px; height: 72px; border-radius: 50%; background: rgba(255,255,255,0.08); border: 1px solid rgba(255,255,255,0.15); display: flex; align-items: center; justify-content: center; margin: 0 auto 20px; font-size: 32px; color: rgba(52,211,153,0.5); }
.empty-state h3 { color: #fff; font-size: 18px; margin-bottom: 8px; }
.empty-state p { color: rgba(255,255,255,0.45); font-size: 14px; }
.loading-state { text-align: center; padding: 60px; color: rgba(255,255,255,0.5); }

/* 抽屉 */
:deep(.el-drawer) { background: rgba(15, 10, 40, 0.97) !important; backdrop-filter: blur(20px); border-left: 1px solid rgba(255,255,255,0.12); }
:deep(.el-drawer__header) { color: #fff !important; border-bottom: 1px solid rgba(255,255,255,0.1); padding: 20px 24px; margin-bottom: 0; }
:deep(.el-drawer__body) { padding: 0; }

.drawer-inner { padding: 24px; color: #fff; display: flex; flex-direction: column; gap: 24px; }
.drawer-info { background: rgba(255,255,255,0.06); border-radius: 12px; padding: 16px; display: flex; flex-direction: column; gap: 12px; border: 1px solid rgba(255,255,255,0.1); }
.info-row { display: flex; justify-content: space-between; align-items: center; }
.info-label { font-size: 13px; color: rgba(255,255,255,0.45); }
.info-value { font-size: 14px; color: #fff; }
.info-value.mono { font-family: monospace; color: rgba(255,255,255,0.7); }
.info-value.amount-high { color: #f87171; font-weight: 700; font-size: 16px; }

.comment-section { display: flex; flex-direction: column; gap: 10px; }
.comment-label { font-size: 13px; color: rgba(255,255,255,0.6); }
.drawer-footer { display: flex; gap: 12px; justify-content: flex-end; padding-top: 8px; border-top: 1px solid rgba(255,255,255,0.1); }

.risk-drawer { padding: 24px; color: #fff; }
.detail-section { display: flex; flex-direction: column; gap: 2px; }
.detail-row { display: flex; align-items: center; gap: 16px; padding: 12px 0; border-bottom: 1px solid rgba(255,255,255,0.08); }
.detail-label { font-size: 12px; color: rgba(255,255,255,0.45); min-width: 80px; }
.detail-value { color: #fff; font-size: 14px; }

.alerts-title { font-size: 13px; color: rgba(255,255,255,0.5); text-transform: uppercase; letter-spacing: 1px; padding: 20px 0 12px; }
.alerts-list { display: flex; flex-direction: column; gap: 10px; }
.alert-item { display: flex; align-items: flex-start; gap: 12px; padding: 12px 16px; background: rgba(255,255,255,0.06); border-radius: 10px; border: 1px solid rgba(255,255,255,0.1); }
.alert-indicator { width: 4px; height: 36px; border-radius: 2px; flex-shrink: 0; margin-top: 2px; }
.level-1 .alert-indicator, .level-tag-1 { background: #34d399; }
.level-2 .alert-indicator, .level-tag-2 { background: #60a5fa; }
.level-3 .alert-indicator, .level-tag-3 { background: #fbbf24; }
.level-4 .alert-indicator, .level-tag-4 { background: #f87171; }
.alert-info { flex: 1; }
.alert-type { font-size: 13px; color: #fff; font-weight: 600; margin-bottom: 4px; }
.alert-msg { font-size: 12px; color: rgba(255,255,255,0.6); }
.level-tag { padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; flex-shrink: 0; }
.level-tag-1 { background: rgba(52,211,153,0.2); color: #34d399; }
.level-tag-2 { background: rgba(96,165,250,0.2); color: #60a5fa; }
.level-tag-3 { background: rgba(251,191,36,0.2); color: #fbbf24; }
.level-tag-4 { background: rgba(248,113,113,0.2); color: #f87171; }

.drawer-actions { display: flex; gap: 12px; justify-content: flex-end; padding: 20px 0 0; border-top: 1px solid rgba(255,255,255,0.1); margin-top: 20px; }
</style>
