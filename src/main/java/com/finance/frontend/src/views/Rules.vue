<template>
  <div class="rules-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>报销政策规则</span>
          <el-button type="primary" @click="reloadRules">
            <el-icon><Refresh /></el-icon> 重载规则
          </el-button>
        </div>
      </template>

      <el-table :data="ruleList" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="ruleCode" label="规则编码" width="160" />
        <el-table-column prop="ruleName" label="规则名称" width="180" />
        <el-table-column prop="ruleType" label="规则类型" width="140">
          <template #default="{ row }">
            <el-tag>{{ getTypeText(row.ruleType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="规则描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="priority" label="优先级" width="80">
          <template #default="{ row }">{{ row.priority }}</template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="80">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="toggleRule(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" size="small" link>编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>费用类别</span>
        </div>
      </template>
      <el-table :data="categoryList" stripe style="width: 100%">
        <el-table-column prop="code" label="类别编码" width="140" />
        <el-table-column prop="name" label="类别名称" width="140" />
        <el-table-column prop="maxAmountPerClaim" label="单次上限(元)" width="140">
          <template #default="{ row }">{{ row.maxAmountPerClaim || '无限制' }}</template>
        </el-table-column>
        <el-table-column prop="requireInvoice" label="必须发票">
          <template #default="{ row }">
            <el-tag :type="row.requireInvoice ? 'danger' : 'success'">{{ row.requireInvoice ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status ? 'success' : 'info'">{{ row.status ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const loading = ref(false)
const ruleList = ref([])
const categoryList = ref([])

const getTypeText = (type) => {
  const texts = {
    'AMOUNT_LIMIT': '金额限制',
    'CATEGORY_LIMIT': '类别限制',
    'TIME_LIMIT': '时间限制',
    'BUDGET_CONTROL': '预算控制'
  }
  return texts[type] || type
}

const loadRules = async () => {
  loading.value = true
  try {
    const res = await api.getRules()
    if (res.code === 200 && res.data) {
      ruleList.value = res.data
    }
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    const res = await api.getCategories()
    if (res.code === 200 && res.data) {
      categoryList.value = res.data
    }
  } catch {}
}

const reloadRules = async () => {
  try {
    await api.reloadRules()
    ElMessage.success('规则已重载')
  } catch {
    ElMessage.error('重载失败')
  }
}

const toggleRule = async (rule) => {
  ElMessage.info(`规则 ${rule.ruleName} 已${rule.enabled ? '启用' : '禁用'}`)
}

onMounted(() => {
  loadRules()
  loadCategories()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
