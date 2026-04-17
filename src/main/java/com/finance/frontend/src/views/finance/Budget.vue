<template>
  <div class="finance-budget">
    <div class="page-header">
      <h2 class="page-title">预算管理</h2>
    </div>

    <!-- 预算说明条 -->
    <div class="budget-tips">
      <el-icon><InfoFilled /></el-icon>
      <span><b>预算总额</b>：部门全年报销额度上限　<b>已使用</b>：审批通过已入账金额　<b>冻结</b>：待审批中的金额</span>
    </div>

    <!-- 预算卡片列表 -->
    <div class="budget-grid" v-if="!loading && tableData.length > 0">
      <div v-for="row in tableData" :key="row.id" class="budget-card">
        <div class="budget-top">
          <div class="dept-info">
            <div class="dept-name">{{ row.deptName }}</div>
            <div class="dept-code">{{ row.deptCode }}</div>
          </div>
          <el-button type="primary" size="small" link class="edit-btn" @click="handleEdit(row)">调整预算</el-button>
        </div>

        <div class="budget-amounts">
          <div class="amount-item">
            <span class="amount-label">预算总额</span>
            <span class="amount-value blue">¥ {{ formatAmount(row.budgetAmount) }}</span>
          </div>
          <div class="amount-item">
            <span class="amount-label">已使用</span>
            <span class="amount-value orange">¥ {{ formatAmount(row.usedAmount) }}</span>
          </div>
          <div class="amount-item">
            <span class="amount-label">冻结</span>
            <span class="amount-value gray">¥ {{ formatAmount(row.frozenAmount) }}</span>
          </div>
          <div class="amount-item">
            <span class="amount-label">可用余额</span>
            <span class="amount-value green">¥ {{ formatAmount(row.availableAmount) }}</span>
          </div>
        </div>

        <div class="progress-section">
          <div class="progress-header">
            <span class="progress-label">使用率</span>
            <span class="progress-pct" :class="getRateClass(row)">{{ calcUsageRate(row) }}%</span>
          </div>
          <div class="progress-track">
            <div class="progress-fill" :style="{ width: calcUsageRate(row) + '%', background: getProgressGradient(row) }"></div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="36"><Loading /></el-icon>
    </div>

    <!-- 调整预算抽屉 -->
    <el-drawer v-model="dialogVisible" title="调整部门预算" size="440px" direction="rtl">
      <div class="drawer-inner" v-if="form">
        <el-form :model="form" label-position="top">
          <el-form-item label="部门名称">
            <el-input v-model="form.deptName" disabled />
          </el-form-item>
          <el-form-item label="预算年度">
            <el-input v-model="form.budgetYear" disabled />
          </el-form-item>
          <el-form-item label="预算总额（元）" prop="budgetAmount">
            <el-input-number v-model="form.budgetAmount" :precision="2" :min="0" :step="10000" style="width: 100%" />
          </el-form-item>
        </el-form>
        <div class="drawer-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">保存</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { InfoFilled, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../../api'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const form = reactive({ deptId: null, deptName: '', budgetYear: new Date().getFullYear(), budgetAmount: 0, usedAmount: 0, frozenAmount: 0 })

const formatAmount = (amount) => (amount !== null && amount !== undefined) ? Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '0.00'

const calcUsageRate = (row) => row.budgetAmount ? Math.round((row.usedAmount / row.budgetAmount) * 100) : 0

const getRateClass = (row) => {
  const r = calcUsageRate(row)
  if (r >= 90) return 'rate-danger'
  if (r >= 70) return 'rate-warning'
  return 'rate-ok'
}

const getProgressGradient = (row) => {
  const r = calcUsageRate(row)
  if (r >= 90) return 'linear-gradient(90deg, rgba(248,113,113,0.8), rgba(248,113,113,0.6))'
  if (r >= 70) return 'linear-gradient(90deg, rgba(251,191,36,0.8), rgba(251,191,36,0.6))'
  return 'linear-gradient(90deg, rgba(52,211,153,0.8), rgba(52,211,153,0.6))'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await api.getDepartments()
    if (res.code === 200 && res.data) {
      const budgetPromises = res.data.map(async (dept) => {
        try {
          const bRes = await api.getDepartmentBudget(dept.id, new Date().getFullYear())
          if (bRes.code === 200 && bRes.data) {
            return { ...dept, budgetAmount: bRes.data.budgetAmount || 0, usedAmount: bRes.data.usedAmount || 0, frozenAmount: bRes.data.frozenAmount || 0, availableAmount: Math.max(0, (bRes.data.budgetAmount || 0) - (bRes.data.usedAmount || 0) - (bRes.data.frozenAmount || 0)) }
          }
        } catch {}
        return { ...dept, budgetAmount: 0, usedAmount: 0, frozenAmount: 0, availableAmount: 0 }
      })
      tableData.value = await Promise.all(budgetPromises)
    }
  } catch { ElMessage.error('加载预算数据失败') } finally { loading.value = false }
}

const handleEdit = (row) => {
  Object.assign(form, { deptId: row.id, deptName: row.deptName, budgetYear: new Date().getFullYear(), budgetAmount: row.budgetAmount || 0, usedAmount: row.usedAmount || 0, frozenAmount: row.frozenAmount || 0 })
  dialogVisible.value = true
}

const submitForm = async () => {
  try {
    const res = await api.updateBudget(form.deptId, { budgetYear: form.budgetYear, budgetAmount: form.budgetAmount, usedAmount: form.usedAmount, frozenAmount: form.frozenAmount })
    if (res.code === 200) { ElMessage.success('预算保存成功'); dialogVisible.value = false; loadData() }
    else ElMessage.error(res.msg || '保存失败')
  } catch { ElMessage.error('保存失败') }
}

onMounted(() => loadData())
</script>

<style scoped>
.finance-budget { padding: 0; min-height: 100%; }
.page-header { margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 700; color: #fff; margin: 0; }

.budget-tips {
  display: flex; align-items: center; gap: 10px;
  padding: 14px 18px;
  background: rgba(96,165,250,0.1);
  border: 1px solid rgba(96,165,250,0.2);
  border-radius: 12px;
  font-size: 13px;
  color: rgba(255,255,255,0.7);
  margin-bottom: 20px;
}
.budget-tips .el-icon { color: #60a5fa; flex-shrink: 0; }
.budget-tips b { color: #fff; }

.budget-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 20px; }

.budget-card {
  background: rgba(255,255,255,0.08);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: 18px;
  padding: 22px;
  display: flex; flex-direction: column; gap: 18px;
  transition: all 0.25s ease;
  box-shadow: 0 4px 20px rgba(31,38,135,0.1);
}
.budget-card:hover { transform: translateY(-2px); border-color: rgba(96,165,250,0.3); }

.budget-top { display: flex; align-items: flex-start; justify-content: space-between; }
.dept-name { font-size: 16px; font-weight: 700; color: #fff; }
.dept-code { font-size: 12px; color: rgba(255,255,255,0.4); margin-top: 2px; font-family: monospace; }
.edit-btn { background: rgba(96,165,250,0.15) !important; border: 1px solid rgba(96,165,250,0.25) !important; color: #60a5fa !important; border-radius: 8px !important; font-size: 12px !important; }

.budget-amounts { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.amount-item { display: flex; flex-direction: column; gap: 4px; }
.amount-label { font-size: 11px; color: rgba(255,255,255,0.45); text-transform: uppercase; letter-spacing: 0.5px; }
.amount-value { font-size: 16px; font-weight: 700; }
.amount-value.blue { color: #60a5fa; }
.amount-value.orange { color: #fbbf24; }
.amount-value.gray { color: rgba(255,255,255,0.5); }
.amount-value.green { color: #34d399; }

.progress-section { display: flex; flex-direction: column; gap: 8px; }
.progress-header { display: flex; justify-content: space-between; align-items: center; }
.progress-label { font-size: 12px; color: rgba(255,255,255,0.5); }
.progress-pct { font-size: 13px; font-weight: 700; }
.rate-ok { color: #34d399; }
.rate-warning { color: #fbbf24; }
.rate-danger { color: #f87171; }

.progress-track { height: 8px; background: rgba(255,255,255,0.1); border-radius: 4px; overflow: hidden; }
.progress-fill { height: 100%; border-radius: 4px; transition: width 1s ease; box-shadow: 0 0 10px rgba(52,211,153,0.4); }

.loading-state { text-align: center; padding: 60px; color: rgba(255,255,255,0.5); }

/* 抽屉 */
:deep(.el-drawer) { background: rgba(15, 10, 40, 0.97) !important; backdrop-filter: blur(20px); border-left: 1px solid rgba(255,255,255,0.12); }
:deep(.el-drawer__header) { color: #fff !important; border-bottom: 1px solid rgba(255,255,255,0.1); padding: 20px 24px; margin-bottom: 0; }
.drawer-inner { padding: 24px; color: #fff; display: flex; flex-direction: column; gap: 24px; }
:deep(.el-form-item__label) { color: rgba(255,255,255,0.8) !important; font-weight: 500; margin-bottom: 6px; }
:deep(.el-input__wrapper), :deep(.el-input-number .el-input__wrapper) { background: rgba(255,255,255,0.1) !important; box-shadow: 0 0 0 1px rgba(255,255,255,0.2) inset !important; border-radius: 10px !important; }
:deep(.el-input__inner) { color: #fff !important; }
.drawer-footer { display: flex; gap: 12px; justify-content: flex-end; padding-top: 16px; border-top: 1px solid rgba(255,255,255,0.1); }
</style>
