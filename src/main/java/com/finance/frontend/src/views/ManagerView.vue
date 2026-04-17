<template>
  <div style="padding: 20px;">
    <el-card header="经理审批工作台">
      <el-table :data="pendingList" border stripe v-loading="loading">
        <el-table-column prop="expenseDate" label="消费日期" width="150" />
        <el-table-column prop="title" label="报销摘要" />
        <el-table-column label="金额">
          <template #default="s">￥{{ (s.row.amount/100).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button type="success" size="small" @click="doAudit(scope.row.id, 1)">通过</el-button>
            <el-button type="danger" size="small" @click="doAudit(scope.row.id, 2)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const pendingList = ref([])
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const res = await axios.get('http://118.31.247.232/api/claims/list')
    if (res.data.code === 200) {
      pendingList.value = res.data.data.filter(i => i.status === 0)
    }
  } finally { loading.value = false }
}

const doAudit = async (claimId, targetStatus) => {
  console.log("准备发送请求，ID:", claimId, "状态:", targetStatus);
  try {
    const res = await axios.post('http://118.31.247.232/api/claims/audit', {
      id: claimId,
      status: targetStatus
    })
    if (res.data.code === 200) {
      ElMessage.success('审批已生效')
      loadData()
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (err) {
    console.error("请求彻底失败:", err);
    ElMessage.error('网络通讯失败，请检查后端控制台日志');
  }
}

onMounted(loadData)
</script>