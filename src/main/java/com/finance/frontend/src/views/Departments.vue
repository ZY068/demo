<template>
  <div class="depts-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>部门管理</span>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-card shadow="hover" class="dept-list-card">
            <template #header>
              <span>部门列表</span>
            </template>
            <el-tree :data="deptTree" :props="{ label: 'deptName', children: 'children' }" @node-click="handleNodeClick" default-expand-all highlight-current>
              <template #default="{ node, data }">
                <span class="tree-node">
                  <el-icon><OfficeBuilding /></el-icon>
                  <span>{{ data.deptName }}</span>
                  <el-tag size="small" :type="data.status ? 'success' : 'info'" style="margin-left: 8px">
                    {{ data.status ? '正常' : '停用' }}
                  </el-tag>
                </span>
              </template>
            </el-tree>
          </el-card>
        </el-col>

        <el-col :span="16">
          <el-card shadow="hover" v-if="selectedDept">
            <template #header>
              <span>{{ selectedDept.deptName }} - 预算信息</span>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="部门编码">{{ selectedDept.deptCode }}</el-descriptions-item>
              <el-descriptions-item label="部门名称">{{ selectedDept.deptName }}</el-descriptions-item>
              <el-descriptions-item label="预算年度">{{ currentYear }}</el-descriptions-item>
              <el-descriptions-item label="预算状态">
                <el-tag :type="budgetInfo && budgetInfo.budgetAmount > 0 ? 'success' : 'info'">
                  {{ budgetInfo && budgetInfo.budgetAmount > 0 ? '已设置' : '未设置' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="预算总额" :span="2">
                <template v-if="budgetInfo">
                  <span class="budget-num">{{ formatAmount(budgetInfo.budgetAmount) }}</span> 元
                </template>
                <el-button type="primary" size="small" link v-else @click="showBudgetDialog">设置预算</el-button>
              </el-descriptions-item>
            </el-descriptions>

            <template v-if="budgetInfo && budgetInfo.budgetAmount > 0">
              <el-divider />
              <div class="budget-detail">
                <el-row :gutter="20">
                  <el-col :span="8">
                    <div class="budget-item">
                      <div class="budget-label">预算总额</div>
                      <div class="budget-value">{{ formatAmount(budgetInfo.budgetAmount) }}</div>
                    </div>
                  </el-col>
                  <el-col :span="8">
                    <div class="budget-item">
                      <div class="budget-label">已使用</div>
                      <div class="budget-value used">{{ formatAmount(budgetInfo.usedAmount) }}</div>
                    </div>
                  </el-col>
                  <el-col :span="8">
                    <div class="budget-item">
                      <div class="budget-label">可用余额</div>
                      <div class="budget-value available">{{ formatAmount(budgetInfo.availableAmount) }}</div>
                    </div>
                  </el-col>
                </el-row>
                <div class="usage-progress">
                  <span>预算使用率</span>
                  <el-progress :percentage="usageRate" :color="usageColor" />
                </div>
              </div>
            </template>
          </el-card>
          <el-card shadow="hover" v-else>
            <div class="empty-tip">
              <el-icon :size="60" color="#ccc"><OfficeBuilding /></el-icon>
              <p>请选择左侧部门查看详情</p>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <el-dialog v-model="budgetDialogVisible" title="设置部门预算" width="400px">
      <el-form ref="budgetFormRef" :model="budgetForm" label-width="100px">
        <el-form-item label="预算年度">
          <el-input-number v-model="budgetForm.budgetYear" :min="2020" :max="2030" />
        </el-form-item>
        <el-form-item label="预算金额(元)" prop="budgetAmount">
          <el-input-number v-model="budgetForm.budgetAmount" :precision="2" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="budgetDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveBudget">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { OfficeBuilding } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const deptTree = ref([])
const selectedDept = ref(null)
const budgetInfo = ref(null)
const budgetDialogVisible = ref(false)
const budgetFormRef = ref()
const currentYear = new Date().getFullYear()

const budgetForm = reactive({
  budgetYear: currentYear,
  budgetAmount: 0
})

const usageRate = computed(() => {
  if (!budgetInfo.value || !budgetInfo.value.budgetAmount) return 0
  return Math.round((budgetInfo.value.usedAmount / budgetInfo.value.budgetAmount) * 100)
})

const usageColor = computed(() => {
  const rate = usageRate.value
  if (rate < 60) return '#67c23a'
  if (rate < 85) return '#e6a23c'
  return '#f56c6c'
})

const formatAmount = (amount) => {
  if (!amount) return '0.00'
  return parseFloat(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

const loadDepartments = async () => {
  try {
    const res = await api.getDepartments()
    if (res.code === 200 && res.data) {
      deptTree.value = res.data.map(d => ({ ...d, children: [] }))
    }
  } catch {}
}

const handleNodeClick = async (data) => {
  selectedDept.value = data
  try {
    const res = await api.getDepartmentBudget(data.id, currentYear)
    if (res.code === 200 && res.data) {
      budgetInfo.value = res.data
      budgetForm.budgetAmount = res.data.budgetAmount || 0
    } else {
      budgetInfo.value = null
    }
  } catch {
    budgetInfo.value = null
  }
}

const showBudgetDialog = () => {
  budgetForm.budgetAmount = 0
  budgetDialogVisible.value = true
}

const saveBudget = async () => {
  try {
    const res = await api.updateBudget(selectedDept.value.id, {
      budgetYear: budgetForm.budgetYear,
      budgetAmount: budgetForm.budgetAmount,
      usedAmount: budgetInfo.value?.usedAmount || 0,
      frozenAmount: budgetInfo.value?.frozenAmount || 0
    })
    if (res.code === 200) {
      ElMessage.success('保存成功')
      budgetDialogVisible.value = false
      handleNodeClick(selectedDept.value)
    }
  } catch {
    ElMessage.error('保存失败')
  }
}

onMounted(() => {
  loadDepartments()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.tree-node { display: flex; align-items: center; }
.dept-list-card { min-height: 400px; }
.empty-tip { padding: 60px; text-align: center; color: #909399; }
.budget-num { font-size: 24px; font-weight: bold; color: #409eff; }
.budget-detail { margin-top: 20px; }
.budget-item { text-align: center; padding: 20px; background: #f5f7fa; border-radius: 8px; }
.budget-label { font-size: 14px; color: #909399; margin-bottom: 8px; }
.budget-value { font-size: 24px; font-weight: bold; }
.budget-value.used { color: #409eff; }
.budget-value.available { color: #67c23a; }
.usage-progress { margin-top: 20px; display: flex; align-items: center; gap: 12px; }
</style>
