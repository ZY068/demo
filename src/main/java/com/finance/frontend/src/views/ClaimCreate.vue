<template>
  <div class="claim-create">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>创建报销单</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 800px">
        <el-form-item label="报销事由" prop="title">
          <el-input v-model="form.title" placeholder="请输入报销事由" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="详细说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入详细说明" />
        </el-form-item>

        <el-form-item label="消费日期" prop="expenseDate">
          <el-date-picker v-model="form.expenseDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>

        <el-form-item label="总金额(元)" prop="amount">
          <el-input-number v-model="form.amountYuan" :precision="2" :min="0" :step="100" placeholder="请输入金额" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="submitting">提交报销单</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)

const form = reactive({
  title: '',
  description: '',
  expenseDate: '',
  amountYuan: 0
})

const rules = {
  title: [{ required: true, message: '请输入报销事由', trigger: 'blur' }],
  expenseDate: [{ required: true, message: '请选择消费日期', trigger: 'change' }],
  amountYuan: [{ required: true, message: '请输入金额', trigger: 'blur' }]
}

const submitForm = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const claimData = {
      title: form.title,
      description: form.description,
      expenseDate: form.expenseDate,
      amount: Math.round(form.amountYuan * 100)  // 元转分
    }
    const res = await api.createClaim(claimData)
    if (res.code === 200) {
      ElMessage.success('提交成功')
      router.push('/claims')
    } else {
      ElMessage.error(res.msg || '提交失败')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
