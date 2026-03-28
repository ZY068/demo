<template>
  <div class="finance-budget">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>预算管理</span>
          <el-button type="primary" @click="loadData">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="deptName" label="部门名称" width="150" />
        <el-table-column prop="deptCode" label="部门编码" width="120" />
        <el-table-column label="预算年度" width="100">
          <template #default="{ row }">
            {{ currentYear }}
          </template>
        </el-table-column>
        <el-table-column label="预算总额(元)" width="140">
          <template #default="{ row }">
            <span style="font-weight: bold; color: #409eff">{{ formatAmount(row.budgetAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="已使用(元)" width="120">
          <template #default="{ row }">
            <span style="color: #e6a23c">{{ formatAmount(row.usedAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="冻结(元)" width="100">
          <template #default="{ row }">
            <span style="color: #909399">{{ formatAmount(row.frozenAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="可用余额(元)" width="130">
          <template #default="{ row }">
            <span style="color: #67c23a; font-weight: bold">{{ formatAmount(row.availableAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="使用率" width="150">
          <template #default="{ row }">
            <el-progress
              :percentage="calcUsageRate(row)"
              :color="getProgressColor(row)"
              :stroke-width="10"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">调整预算</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-divider content-position="left">预算说明</el-divider>
      <div class="budget-tips">
        <el-alert type="info" :closable="false">
          <template #title>
            <ul style="margin: 0; padding-left: 20px">
              <li><b>预算总额</b>：部门全年报销额度上限</li>
              <li><b>已使用</b>：审批通过已入账的报销金额</li>
              <li><b>冻结</b>：提交申请但尚未审批通过的金额</li>
              <li><b>可用余额</b>：预算总额 - 已使用 - 冻结</li>
              <li><b>使用率</b>：已使用 / 预算总额</li>
            </ul>
          </template>
        </el-alert>
      </div>
    </el-card>

    <!-- 调整预算对话框 -->
    <el-dialog v-model="dialogVisible" title="调整部门预算" width="450px">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="部门名称">
          <el-input v-model="form.deptName" disabled />
        </el-form-item>
        <el-form-item label="预算年度">
          <el-input v-model="form.budgetYear" disabled />
        </el-form-item>
        <el-form-item label="预算总额(元)" prop="budgetAmount">
          <el-input-number
            v-model="form.budgetAmount"
            :precision="2"
            :min="0"
            :step="10000"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../../api'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const formRef = ref()
const currentYear = new Date().getFullYear()

const form = reactive({
  deptId: null,
  deptName: '',
  budgetYear: currentYear,
  budgetAmount: 0,
  usedAmount: 0,
  frozenAmount: 0
})

const formatAmount = (amount) => {
  if (!amount && amount !== 0) return '0.00'
  return Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const calcUsageRate = (row) => {
  if (!row.budgetAmount || row.budgetAmount === 0) return 0
  return Math.round((row.usedAmount / row.budgetAmount) * 100)
}

const getProgressColor = (row) => {
  const rate = calcUsageRate(row)
  if (rate >= 90) return '#f56c6c'
  if (rate >= 70) return '#e6a23c'
  return '#67c23a'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await api.getDepartments()
    if (res.code === 200 && res.data) {
      const deptList = res.data

      // 获取每个部门的预算
      const budgetPromises = deptList.map(async (dept) => {
        try {
          const budgetRes = await api.getDepartmentBudget(dept.id, currentYear)
          if (budgetRes.code === 200 && budgetRes.data) {
            const budgetAmount = budgetRes.data.budgetAmount || 0
            const usedAmount = budgetRes.data.usedAmount || 0
            const frozenAmount = budgetRes.data.frozenAmount || 0
            const availableAmount = Math.max(0, budgetAmount - usedAmount - frozenAmount)
            return {
              ...dept,
              budgetAmount,
              usedAmount,
              frozenAmount,
              availableAmount
            }
          }
        } catch (e) {
          console.error(`获取部门${dept.id}预算失败`, e)
        }
        return {
          ...dept,
          budgetAmount: 0,
          usedAmount: 0,
          frozenAmount: 0,
          availableAmount: 0
        }
      })

      tableData.value = await Promise.all(budgetPromises)
    }
  } catch (e) {
    ElMessage.error('加载预算数据失败')
  } finally {
    loading.value = false
  }
}

const handleEdit = (row) => {
  form.deptId = row.id
  form.deptName = row.deptName
  form.budgetYear = currentYear
  form.budgetAmount = row.budgetAmount || 0
  form.usedAmount = row.usedAmount || 0
  form.frozenAmount = row.frozenAmount || 0
  dialogVisible.value = true
}

const submitForm = async () => {
  try {
    const res = await api.updateBudget(form.deptId, {
      budgetYear: form.budgetYear,
      budgetAmount: form.budgetAmount,
      usedAmount: form.usedAmount,
      frozenAmount: form.frozenAmount
    })
    if (res.code === 200) {
      ElMessage.success('预算保存成功')
      dialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.budget-tips {
  margin-top: 20px;
}

:deep(.el-progress) {
  width: 120px;
}
</style>
