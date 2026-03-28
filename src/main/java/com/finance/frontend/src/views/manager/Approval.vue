<template>
  <div class="approval-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>待审批报销单（部门经理）</span>
          <el-tag type="warning">{{ tableData.length }} 条待处理</el-tag>
        </div>
      </template>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无待审批报销单" />

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="claimNo" label="单号" width="160" />
        <el-table-column prop="title" label="报销事由" min-width="180" />
        <el-table-column label="金额(元)" width="100">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: bold">{{ formatAmount(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="expenseDate" label="消费日期" width="110" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag type="warning">待审批</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handleApprove(row, true)">通过</el-button>
            <el-button type="danger" size="small" @click="handleApprove(row, false)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 审批对话框 -->
    <el-dialog v-model="dialogVisible" :title="approveAction ? '审批通过' : '审批驳回'" width="400px">
      <el-form>
        <el-form-item label="审批意见">
          <el-input v-model="comment" type="textarea" :rows="3" placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :type="approveAction ? 'success' : 'danger'" @click="submitApproval">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const currentRow = ref(null)
const approveAction = ref(true)
const comment = ref('')

const formatAmount = (amount) => {
  if (!amount) return '0.00'
  return (amount / 100).toFixed(2)
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await api.getPendingClaims()
    if (res.code === 200) {
      tableData.value = res.data || []
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleApprove = (row, approved) => {
  currentRow.value = row
  approveAction.value = approved
  comment.value = approved ? '同意' : ''
  dialogVisible.value = true
}

const submitApproval = async () => {
  if (!currentRow.value) return

  try {
    // 使用新接口：claimId, approved, comment
    const res = await api.approveClaim(currentRow.value.id, approveAction.value, comment.value)
    if (res.code === 200) {
      ElMessage.success(approveAction.value ? '审批通过，已提交财务审计' : '审批驳回')
      dialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '操作失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.approval-page { padding: 0; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>