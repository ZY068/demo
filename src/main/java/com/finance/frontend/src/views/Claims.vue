<template>
  <div class="claims-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>报销单管理</span>
          <el-button type="primary" @click="$router.push('/claims/create')" v-if="canCreate">
            <el-icon><Plus /></el-icon> 新建报销单
          </el-button>
        </div>
      </template>
      <el-form inline @submit.prevent>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="待经理审批" :value="1" />
            <el-option label="待财务审计" :value="2" />
            <el-option label="经理驳回" :value="3" />
            <el-option label="已入账" :value="4" />
            <el-option label="财务驳回" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="事由/单号" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="claimNo" label="单号" width="160" />
        <el-table-column prop="title" label="报销事由" min-width="200" show-overflow-tooltip />
        <el-table-column prop="userId" label="申请人" width="100" />
        <el-table-column prop="amount" label="金额(元)" width="120">
          <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="expenseDate" label="消费日期" width="120" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="风险分" width="100" v-if="userRole === 2">
          <template #default="{ row }">
            <el-tag
              v-if="row.riskScore && row.riskScore > 0"
              :type="getRiskTagType(row.riskScore)"
              effect="dark"
              style="cursor: pointer"
              @click="showRiskDetail(row)"
            >
              {{ row.riskScore.toFixed(1) }}
            </el-tag>
            <span v-else style="color: #999">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="viewDetail(row)">查看</el-button>
            <el-button v-if="userRole === 2" type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 报销单详情对话框 -->
    <el-dialog v-model="detailVisible" title="报销单详情" width="700px">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="单号">{{ currentRow.claimNo }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ currentRow.userId }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ formatAmount(currentRow.amount) }} 元</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentRow.status)">{{ getStatusText(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="消费日期" :span="2">{{ currentRow.expenseDate }}</el-descriptions-item>
        <el-descriptions-item label="报销事由" :span="2">{{ currentRow.title }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ currentRow.description || '无' }}</el-descriptions-item>
        <el-descriptions-item label="风险评分" v-if="userRole === 2">
          <el-tag :type="getRiskTagType(currentRow.riskScore)" effect="dark">
            {{ currentRow.riskScore ? currentRow.riskScore.toFixed(1) : '0' }} - {{ getRiskLevelText(currentRow.riskScore) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审计意见">{{ currentRow.auditComment || '无' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 风险详情对话框（仅财务可见） -->
    <el-dialog v-model="riskDetailVisible" title="风险详情" width="650px" v-if="userRole === 2">
      <div v-if="riskDetail" class="risk-detail">
        <el-alert
          v-if="riskDetail.riskScore && riskDetail.riskScore > 4"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 16px"
        >
          <template #title>该报销单存在风险，请关注</template>
        </el-alert>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="风险评分">
            <el-tag :type="getRiskTagType(riskDetail.riskScore)" effect="dark" size="large">
              {{ riskDetail.riskScore ? riskDetail.riskScore.toFixed(1) : '0' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="风险等级">
            <el-tag :type="getRiskLevelTagType(riskDetail.riskLevel)" effect="plain">
              {{ getRiskLevelTextByLevel(riskDetail.riskLevel) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">风险指标</el-divider>

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
        <el-button type="primary" @click="handleReDetect" :loading="reDetecting">重新检测</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { Plus } from '@element-plus/icons-vue'
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

// 是否可以创建报销单（员工和经理可以）
const canCreate = computed(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const user = JSON.parse(userStr)
    return user.role === 0 || user.role === 1
  }
  return false
})

// 当前用户角色
const userRole = computed(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const user = JSON.parse(userStr)
    return user.role
  }
  return null
})

const formatAmount = (amount) => {
  if (!amount) return '0.00'
  return (amount / 100).toFixed(2)
}

// 状态映射
const getStatusType = (status) => {
  const types = { 0: 'info', 1: 'warning', 2: 'primary', 3: 'danger', 4: 'success', 5: 'danger' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { 0: '草稿', 1: '待经理审批', 2: '待财务审计', 3: '经理驳回', 4: '已入账', 5: '财务驳回' }
  return texts[status] || '未知'
}

// 风险评分标签类型（根据分数）
const getRiskTagType = (score) => {
  if (!score || score < 1) return 'success'  // 绿色：无风险
  if (score < 2) return 'info'               // 蓝色：低风险
  if (score < 4) return 'warning'            // 黄色：中风险
  return 'danger'                             // 红色：高/严重风险
}

const getRiskLevelText = (score) => {
  if (!score || score < 1) return '无风险'
  if (score < 2) return '低风险'
  if (score < 4) return '中风险'
  if (score < 6) return '高风险'
  return '严重风险'
}

// 风险等级标签
const getLevelTagType = (level) => {
  const types = { 1: 'success', 2: 'info', 3: 'warning', 4: 'danger' }
  return types[level] || 'info'
}

const getLevelText = (level) => {
  const texts = { 1: '低', 2: '中', 3: '高', 4: '严重' }
  return texts[level] || '未知'
}

// 根据字符串等级获取颜色
const getRiskLevelTagType = (level) => {
  const types = {
    'LOW': 'success',
    'MEDIUM': 'info',
    'HIGH': 'warning',
    'SEVERE': 'danger'
  }
  return types[level] || 'info'
}

// 根据字符串等级获取中文
const getRiskLevelTextByLevel = (level) => {
  const texts = {
    'LOW': '低风险',
    'MEDIUM': '中风险',
    'HIGH': '高风险',
    'SEVERE': '严重风险'
  }
  return texts[level] || '低风险'
}

// 风险类型名称
const getRiskTypeName = (type) => {
  const names = {
    'WEEKEND_CLAIM': '周末报销',
    'HIGH_AMOUNT': '高金额',
    'HIGH_FREQUENCY': '高频报销',
    'BUDGET_EXCEED': '预算超支'
  }
  return names[type] || type
}

// 格式化风险分（0分显示0.0而不是-）
const formatRiskScore = (score) => {
  if (score === null || score === undefined) return '-'
  return score.toFixed(1)
}

// 删除报销单（仅财务）
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除报销单 ${row.claimNo} 吗？此操作会留下审计痕迹。`,
      '删除确认',
      { type: 'warning' }
    )
    const res = await api.deleteClaim(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await api.getClaims()
    if (res.code === 200 && res.data) {
      let data = res.data
      if (query.status !== null && query.status !== '') {
        data = data.filter(d => d.status === query.status)
      }
      if (query.keyword) {
        data = data.filter(d =>
          (d.title && d.title.includes(query.keyword)) ||
          (d.claimNo && d.claimNo.includes(query.keyword))
        )
      }
      tableData.value = data
      pagination.total = data.length
    }
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.status = null
  query.keyword = ''
  loadData()
}

const viewDetail = (row) => {
  currentRow.value = row
  detailVisible.value = true
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

// 重新检测风险
const handleReDetect = async () => {
  if (!riskDetail.value) return
  reDetecting.value = true
  try {
    const res = await api.reDetectRisk(riskDetail.value.claimId)
    if (res.code === 200) {
      ElMessage.success('重新检测完成')
      riskDetail.value = res.data
      loadData() // 刷新列表
    }
  } catch (e) {
    ElMessage.error('重新检测失败')
  } finally {
    reDetecting.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.risk-detail { padding: 0 10px; }
</style>