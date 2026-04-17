<template>
  <div class="approval-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">待审批报销</h2>
        <span class="pending-count-badge" v-if="tableData.length > 0">{{ tableData.length }} 条待处理</span>
      </div>
    </div>

    <!-- 审批卡片列表 -->
    <div v-if="!loading && tableData.length > 0" class="approval-grid">
      <div v-for="row in tableData" :key="row.id" class="approval-card">
        <div class="card-top">
          <div class="card-meta">
            <span class="card-no">{{ row.claimNo }}</span>
            <span class="card-date">{{ row.expenseDate }}</span>
          </div>
          <div class="card-amount">
            <span class="amount-symbol">¥</span>
            <span class="amount-value">{{ formatAmount(row.amount) }}</span>
          </div>
        </div>

        <div class="card-title">{{ row.title }}</div>

        <div class="card-desc" v-if="row.description">{{ row.description }}</div>

        <div class="card-bottom">
          <div class="card-applicant">
            <div class="avatar">{{ (row.userId || 'U').toString().charAt(0) }}</div>
            <span>申请人 ID: {{ row.userId }}</span>
          </div>
          <div class="card-actions">
            <el-button class="btn-reject" size="small" @click="handleApprove(row, false)">
              <el-icon><Close /></el-icon> 驳回
            </el-button>
            <el-button class="btn-approve" size="small" type="primary" @click="handleApprove(row, true)">
              <el-icon><Check /></el-icon> 通过
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && tableData.length === 0" class="empty-state">
      <div class="empty-icon">
        <el-icon><FolderOpened /></el-icon>
      </div>
      <h3>暂无待审批报销</h3>
      <p>所有报销单均已处理完毕</p>
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="36"><Loading /></el-icon>
    </div>

    <!-- 审批抽屉 -->
    <el-drawer v-model="dialogVisible" :title="approveAction ? '审批通过' : '审批驳回'" size="440px" direction="rtl">
      <div class="drawer-inner" v-if="currentRow">
        <div class="drawer-card-info">
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
          <label class="comment-label">审批意见</label>
          <el-input
            v-model="comment"
            type="textarea"
            :rows="4"
            :placeholder="approveAction ? '选填，可添加备注' : '请输入驳回原因'"
          />
        </div>

        <div class="drawer-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button :type="approveAction ? 'primary' : 'danger'" @click="submitApproval">
            {{ approveAction ? '确认通过' : '确认驳回' }}
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Check, Close, FolderOpened, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../../api'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const currentRow = ref(null)
const approveAction = ref(true)
const comment = ref('')

const formatAmount = (amount) => (amount ? (amount / 100).toFixed(2) : '0.00')

const loadData = async () => {
  loading.value = true
  try {
    const res = await api.getPendingClaims()
    if (res.code === 200) tableData.value = res.data || []
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

const handleApprove = (row, approved) => {
  currentRow.value = row
  approveAction.value = approved
  comment.value = approved ? '' : ''
  dialogVisible.value = true
}

const submitApproval = async () => {
  if (!currentRow.value) return
  try {
    const res = await api.approveClaim(currentRow.value.id, approveAction.value, comment.value)
    if (res.code === 200) {
      ElMessage.success(approveAction.value ? '审批通过，已提交财务审计' : '审批驳回')
      dialogVisible.value = false
      loadData()
    } else { ElMessage.error(res.msg || '操作失败') }
  } catch { ElMessage.error('操作失败') }
}

onMounted(() => loadData())
</script>

<style scoped>
.approval-page { padding: 0; min-height: 100%; }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.header-left { display: flex; align-items: center; gap: 16px; }

.page-title { font-size: 22px; font-weight: 700; color: #fff; margin: 0; }

.pending-count-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 14px;
  background: rgba(251,191,36,0.2);
  border: 1px solid rgba(251,191,36,0.3);
  border-radius: 20px;
  color: #fbbf24;
  font-size: 13px;
  font-weight: 600;
}

/* 审批卡片网格 */
.approval-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

.approval-card {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 18px;
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition: all 0.25s ease;
  box-shadow: 0 4px 20px rgba(31, 38, 135, 0.15);
}

.approval-card:hover {
  transform: translateY(-3px);
  border-color: rgba(96, 165, 250, 0.35);
  box-shadow: 0 12px 40px rgba(31, 38, 135, 0.3), 0 0 20px rgba(96, 165, 250, 0.1);
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.card-meta { display: flex; flex-direction: column; gap: 4px; }

.card-no {
  font-family: monospace;
  font-size: 13px;
  color: rgba(255,255,255,0.6);
}

.card-date { font-size: 12px; color: rgba(255,255,255,0.4); }

.card-amount {
  text-align: right;
}

.amount-symbol { font-size: 14px; color: #f87171; }
.amount-value { font-size: 22px; font-weight: 800; color: #f87171; }

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  line-height: 1.4;
}

.card-desc {
  font-size: 13px;
  color: rgba(255,255,255,0.55);
  line-height: 1.5;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 14px;
  border-top: 1px solid rgba(255,255,255,0.1);
  margin-top: auto;
}

.card-applicant {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: rgba(255,255,255,0.6);
}

.avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: rgba(96,165,250,0.3);
  border: 1px solid rgba(96,165,250,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #60a5fa;
  font-weight: 700;
}

.card-actions { display: flex; gap: 8px; }

.btn-approve {
  background: rgba(52,211,153,0.25) !important;
  border: 1px solid rgba(52,211,153,0.4) !important;
  color: #34d399 !important;
  border-radius: 10px !important;
}

.btn-approve:hover {
  background: rgba(52,211,153,0.45) !important;
  box-shadow: 0 0 12px rgba(52,211,153,0.3) !important;
}

.btn-reject {
  background: rgba(248,113,113,0.15) !important;
  border: 1px solid rgba(248,113,113,0.3) !important;
  color: #f87171 !important;
  border-radius: 10px !important;
}

.btn-reject:hover {
  background: rgba(248,113,113,0.3) !important;
  box-shadow: 0 0 12px rgba(248,113,113,0.3) !important;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 40px;
  background: rgba(255,255,255,0.06);
  border-radius: 20px;
  border: 1px solid rgba(255,255,255,0.1);
}

.empty-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: rgba(255,255,255,0.08);
  border: 1px solid rgba(255,255,255,0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  font-size: 32px;
  color: rgba(255,255,255,0.3);
}

.empty-state h3 { color: #fff; font-size: 18px; margin-bottom: 8px; }
.empty-state p { color: rgba(255,255,255,0.45); font-size: 14px; }

.loading-state {
  text-align: center;
  padding: 60px;
  color: rgba(255,255,255,0.5);
}

/* 抽屉 */
:deep(.el-drawer) {
  background: rgba(15, 10, 40, 0.97) !important;
  backdrop-filter: blur(20px);
  border-left: 1px solid rgba(255,255,255,0.12);
}

:deep(.el-drawer__header) {
  color: #fff !important;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  padding: 20px 24px;
  margin-bottom: 0;
}

.drawer-inner { padding: 24px; color: #fff; display: flex; flex-direction: column; gap: 24px; }

.drawer-card-info {
  background: rgba(255,255,255,0.06);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border: 1px solid rgba(255,255,255,0.1);
}

.info-row { display: flex; justify-content: space-between; align-items: center; }
.info-label { font-size: 13px; color: rgba(255,255,255,0.45); }
.info-value { font-size: 14px; color: #fff; }
.info-value.mono { font-family: monospace; color: rgba(255,255,255,0.7); }
.info-value.amount-high { color: #f87171; font-weight: 700; font-size: 16px; }

.comment-section { display: flex; flex-direction: column; gap: 10px; }

.comment-label { font-size: 13px; color: rgba(255,255,255,0.6); }

.drawer-footer { display: flex; gap: 12px; justify-content: flex-end; padding-top: 8px; border-top: 1px solid rgba(255,255,255,0.1); }
</style>
