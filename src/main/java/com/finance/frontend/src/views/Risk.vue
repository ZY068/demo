<template>
  <div class="risk-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>风险预警</span>
          <el-button type="primary" @click="loadData">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>

      <el-row :gutter="20" class="risk-stats">
        <el-col :span="6">
          <div class="risk-stat severe">
            <div class="stat-num">{{ stats.severe }}</div>
            <div class="stat-label">严重风险</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="risk-stat high">
            <div class="stat-num">{{ stats.high }}</div>
            <div class="stat-label">高风险</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="risk-stat medium">
            <div class="stat-num">{{ stats.medium }}</div>
            <div class="stat-label">中风险</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="risk-stat low">
            <div class="stat-num">{{ stats.low }}</div>
            <div class="stat-label">低风险</div>
          </div>
        </el-col>
      </el-row>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%; margin-top: 20px">
        <el-table-column prop="claimId" label="报销单ID" width="100" />
        <el-table-column prop="riskType" label="风险类型" width="120" />
        <el-table-column prop="riskLevel" label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.riskLevel)" effect="dark">{{ getLevelText(row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="riskScore" label="风险评分" width="100">
          <template #default="{ row }">
            <span :class="getScoreClass(row.riskScore)">{{ row.riskScore }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="alertMessage" label="预警信息" min-width="300" show-overflow-tooltip />
        <el-table-column prop="handlingStatus" label="处理状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.handlingStatus)">{{ getStatusText(row.handlingStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleAlert(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="handleVisible" title="处理风险预警" width="500px">
      <el-form ref="handleFormRef" :model="handleForm" label-width="100px">
        <el-form-item label="预警类型">{{ currentAlert?.riskType }}</el-form-item>
        <el-form-item label="预警信息">{{ currentAlert?.alertMessage }}</el-form-item>
        <el-form-item label="处理意见" prop="comment">
          <el-input v-model="handleForm.comment" type="textarea" :rows="3" placeholder="请输入处理意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const loading = ref(false)
const tableData = ref([])
const handleVisible = ref(false)
const currentAlert = ref(null)
const handleFormRef = ref()
const handleForm = reactive({ comment: '' })

const stats = reactive({ severe: 0, high: 0, medium: 0, low: 0 })

const getLevelType = (level) => ['', 'info', 'warning', 'danger', 'danger'][level] || ''
const getLevelText = (level) => ['', '低', '中', '高', '严重'][level] || ''
const getStatusType = (status) => ['', 'warning', 'success', 'info', 'success'][status] || ''
const getStatusText = (status) => ['', '待处理', '处理中', '已处理', '误报'][status] || ''
const getScoreClass = (score) => {
  if (!score || score < 2) return 'score-low'
  if (score < 4) return 'score-medium'
  return 'score-high'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await api.getRiskAlerts()
    if (res.code === 200 && res.data) {
      tableData.value = res.data
      stats.severe = res.data.filter(a => a.riskLevel === 4).length
      stats.high = res.data.filter(a => a.riskLevel === 3).length
      stats.medium = res.data.filter(a => a.riskLevel === 2).length
      stats.low = res.data.filter(a => a.riskLevel === 1).length
    }
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleAlert = (row) => {
  currentAlert.value = row
  handleForm.comment = ''
  handleVisible.value = true
}

const submitHandle = async () => {
  if (!handleForm.comment) {
    ElMessage.warning('请输入处理意见')
    return
  }
  try {
    const res = await api.handleAlert(currentAlert.value.id, {
      handlerId: 1,
      comment: handleForm.comment
    })
    if (res.code === 200) {
      ElMessage.success('处理成功')
      handleVisible.value = false
      loadData()
    }
  } catch {
    ElMessage.error('处理失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.risk-stats { margin-bottom: 20px; }
.risk-stat {
  padding: 24px;
  border-radius: 12px;
  text-align: center;
  color: white;
}
.risk-stat.severe { background: linear-gradient(135deg, #f56c6c, #e64242); }
.risk-stat.high { background: linear-gradient(135deg, #e6a23c, #d4920d); }
.risk-stat.medium { background: linear-gradient(135deg, #409eff, #2e86de); }
.risk-stat.low { background: linear-gradient(135deg, #67c23a, #5ab025); }
.stat-num { font-size: 36px; font-weight: bold; }
.stat-label { font-size: 14px; margin-top: 8px; opacity: 0.9; }
.score-low { color: #67c23a; }
.score-medium { color: #e6a23c; }
.score-high { color: #f56c6c; font-weight: bold; }
</style>
