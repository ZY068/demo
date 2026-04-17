<template>
  <div class="employee-container">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="box-card" header="新报销申请">
          <el-form :model="form" label-position="top">
            <el-form-item label="报销事由 (Title)">
              <el-input v-model="form.title" placeholder="例如：3月加班餐费" />
            </el-form-item>
            <el-form-item label="消费日期 (Date)">
              <el-date-picker 
                v-model="form.expenseDate" 
                type="date" 
                placeholder="选择日期" 
                style="width: 100%"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
            <el-form-item label="报销金额 (Amount)">
              <el-input-number 
                v-model="form.displayAmount" 
                :precision="2" 
                :step="0.1" 
                style="width: 100%" 
              />
            </el-form-item>
            <el-button type="primary" style="width: 100%; margin-top: 10px" @click="submit">
              提交报销单
            </el-button>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card class="box-card" header="我的报销历史记录">
          <el-table :data="myClaims" border stripe style="width: 100%">
            <el-table-column prop="expenseDate" label="消费日期" width="120" />
            <el-table-column prop="title" label="事由" />
            <el-table-column label="金额">
              <template #default="scope">
                ￥{{ (scope.row.amount / 100).toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column label="状态">
              <template #default="scope">
                <el-tag :type="scope.row.status === 0 ? 'warning' : 'success'">
                  {{ scope.row.status === 0 ? '待审核' : '已入账' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

// 从本地存储获取当前登录的张三信息
const user = JSON.parse(localStorage.getItem('user') || '{}')

const form = ref({
  title: '',
  displayAmount: 0,
  expenseDate: ''
})

const myClaims = ref([])

// 获取列表
const fetchMyClaims = async () => {
  const res = await axios.get('http://118.31.247.232/api/claims/list')
  if (res.data.code === 200) {
    // 实际项目中这里应该按 userId 过滤，现在先显示全部
    myClaims.value = res.data.data
  }
}

// 提交申请
const submit = async () => {
  if (!form.value.title || !form.value.expenseDate || form.value.displayAmount <= 0) {
    ElMessage.warning('请填写完整的报销单信息')
    return
  }

  const submitData = {
    userId: user.id,
    title: form.value.title,
    amount: Math.round(form.value.displayAmount * 100),
    expenseDate: form.value.expenseDate,
    status: 0
  }

  const res = await axios.post('http://118.31.247.232/api/claims/submit', submitData)
  if (res.data.code === 200) {
    ElMessage.success('报销单已提交，等待审计')
    form.value = { title: '', displayAmount: 0, expenseDate: '' } // 重置表单
    fetchMyClaims() // 刷新列表
  }
}

onMounted(() => {
  fetchMyClaims()
})
</script>

<style scoped>
.employee-container { padding: 5px; }
.box-card { margin-bottom: 20px; border-radius: 8px; }
</style>