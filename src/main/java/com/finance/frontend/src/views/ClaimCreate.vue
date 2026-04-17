<template>
  <div class="claim-create">
    <!-- 背景装饰 -->
    <div class="bg-orb orb-1"></div>
    <div class="bg-orb orb-2"></div>

    <div class="create-container">
      <!-- 返回按钮 -->
      <el-button class="back-btn" text @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>

      <!-- 表单主卡 -->
      <div class="form-card">
        <div class="form-card-header">
          <div class="header-icon">
            <el-icon><DocumentAdd /></el-icon>
          </div>
          <div>
            <h2 class="form-title">创建报销单</h2>
            <p class="form-subtitle">填写以下信息提交您的报销申请</p>
          </div>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" class="create-form" label-position="top">
          <div class="form-section">
            <div class="section-label">
              <el-icon><Tickets /></el-icon> 报销信息
            </div>

            <el-form-item label="报销事由" prop="title">
              <el-input
                v-model="form.title"
                placeholder="请输入报销事由，如：北京出差费用"
                maxlength="100"
                show-word-limit
                size="large"
              />
            </el-form-item>

            <el-form-item label="详细说明" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="3"
                placeholder="补充说明消费内容、供应商等信息（选填）"
                maxlength="500"
              />
            </el-form-item>
          </div>

          <div class="form-divider"></div>

          <div class="form-section">
            <div class="section-label">
              <el-icon><Coin /></el-icon> 金额与日期
            </div>

            <el-form-item label="消费日期" prop="expenseDate" class="half-item">
              <el-date-picker
                v-model="form.expenseDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择消费日期"
                size="large"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="总金额（元）" prop="amountYuan" class="half-item">
              <el-input-number
                v-model="form.amountYuan"
                :precision="2"
                :min="0"
                :step="100"
                :controls="false"
                placeholder="请输入金额"
                size="large"
                style="width: 100%"
              >
                <template #prefix>¥</template>
              </el-input-number>
            </el-form-item>
          </div>

          <div class="form-actions">
            <el-button size="large" class="btn-cancel" @click="$router.back()">取消</el-button>
            <el-button type="primary" size="large" class="btn-submit" :loading="submitting" @click="submitForm">
              <el-icon><Promotion /></el-icon>
              提交报销单
            </el-button>
          </div>
        </el-form>
      </div>

      <!-- 右侧提示卡 -->
      <div class="tips-card">
        <div class="tips-title">
          <el-icon><InfoFilled /></el-icon> 报销说明
        </div>
        <ul class="tips-list">
          <li>报销单提交后将进入审批流程</li>
          <li>经理审批通过后，将提交至财务内审</li>
          <li>财务审核通过后，款项将打入您的账户</li>
          <li>高金额或周末产生的报销将触发风险预警</li>
          <li>请确保发票真实性，一经查实虚假报销将追究责任</li>
        </ul>
        <div class="demo-note">
          <el-icon><Clock /></el-icon>
          审批周期：1-3 个工作日
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { DocumentAdd, Tickets, Coin, Promotion, ArrowLeft, InfoFilled, Clock } from '@element-plus/icons-vue'
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
      amount: Math.round(form.amountYuan * 100)
    }
    const res = await api.createClaim(claimData)
    if (res.code === 200) {
      ElMessage.success('提交成功，报销单已进入审批流程')
      router.push('/claims')
    } else { ElMessage.error(res.msg || '提交失败') }
  } catch { ElMessage.error('提交失败，请检查网络') }
  finally { submitting.value = false }
}
</script>

<style scoped>
.claim-create {
  min-height: 100%;
  position: relative;
  overflow: hidden;
}

.bg-orb {
  position: fixed;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
  opacity: 0.35;
}

.orb-1 {
  width: 400px; height: 400px;
  background: radial-gradient(circle, rgba(102,126,234,0.8), transparent);
  top: -100px; right: 10%;
  animation: float 10s ease-in-out infinite;
}

.orb-2 {
  width: 300px; height: 300px;
  background: radial-gradient(circle, rgba(118,75,162,0.8), transparent);
  bottom: -80px; left: 5%;
  animation: float 8s ease-in-out infinite reverse;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(20px, -20px); }
}

.create-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 20px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  position: relative;
  z-index: 2;
}

.back-btn {
  color: rgba(255,255,255,0.7) !important;
  font-size: 14px;
  align-self: flex-start;
  padding-left: 0;
}

.back-btn:hover { color: #fff !important; }

.form-card {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(31, 38, 135, 0.2);
}

.form-card-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 36px;
}

.header-icon {
  width: 60px; height: 60px;
  border-radius: 18px;
  background: rgba(96,165,250,0.2);
  border: 1px solid rgba(96,165,250,0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #60a5fa;
  flex-shrink: 0;
  box-shadow: 0 8px 24px rgba(96,165,250,0.2);
}

.form-title { font-size: 22px; font-weight: 700; color: #fff; margin: 0 0 4px; }
.form-subtitle { font-size: 14px; color: rgba(255,255,255,0.5); margin: 0; }

.form-section { display: flex; flex-direction: column; gap: 0; }

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: rgba(255,255,255,0.5);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 16px;
}

.section-label .el-icon { color: #60a5fa; }

.form-divider { height: 1px; background: rgba(255,255,255,0.1); margin: 28px 0; }

.half-item { margin-bottom: 16px; }

/* Element Plus 覆盖 */
:deep(.el-form-item__label) {
  color: rgba(255,255,255,0.8) !important;
  font-weight: 500;
  margin-bottom: 6px;
}

:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
  background: rgba(255,255,255,0.1) !important;
  box-shadow: 0 0 0 1px rgba(255,255,255,0.2) inset !important;
  border-radius: 12px !important;
}

:deep(.el-input__inner),
:deep(.el-textarea__inner) {
  color: #fff !important;
  font-size: 15px;
}

:deep(.el-input__inner::placeholder),
:deep(.el-textarea__inner::placeholder) {
  color: rgba(255,255,255,0.4) !important;
}

:deep(.el-input-number) {
  width: 100%;
}

:deep(.el-input-number .el-input__wrapper) {
  padding-left: 36px !important;
}

:deep(.el-input__prefix) {
  color: rgba(255,255,255,0.5) !important;
  font-size: 15px;
}

:-deep(.el-input.is-focus .el-input__wrapper),
:deep(.el-input.is-focus .el-textarea__inner),
:deep(.el-input-number.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 2px rgba(96,165,250,0.5) inset !important;
}

/* 日期选择器 */
:deep(.el-date-editor.el-input) {
  width: 100% !important;
}

:deep(.el-picker__popper) {
  background: rgba(20, 10, 50, 0.95) !important;
  border: 1px solid rgba(255,255,255,0.15) !important;
  backdrop-filter: blur(20px);
}

.form-actions {
  display: flex;
  gap: 16px;
  justify-content: flex-end;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid rgba(255,255,255,0.1);
}

.btn-cancel {
  background: rgba(255,255,255,0.08) !important;
  border: 1px solid rgba(255,255,255,0.2) !important;
  color: rgba(255,255,255,0.8) !important;
  border-radius: 12px !important;
  height: 48px;
  padding: 0 28px;
}

.btn-cancel:hover { background: rgba(255,255,255,0.15) !important; }

.btn-submit {
  background: linear-gradient(135deg, rgba(96,165,250,0.7), rgba(99,102,241,0.7)) !important;
  border: 1px solid rgba(147,197,253,0.4) !important;
  color: #fff !important;
  border-radius: 12px !important;
  height: 48px;
  padding: 0 32px;
  font-size: 15px;
  font-weight: 600;
  box-shadow: 0 8px 24px rgba(99,102,241,0.3);
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-submit:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 36px rgba(99,102,241,0.5) !important;
  background: linear-gradient(135deg, rgba(96,165,250,0.9), rgba(99,102,241,0.9)) !important;
}

/* 右侧提示卡 */
.tips-card {
  background: rgba(255,255,255,0.07);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: 18px;
  padding: 24px;
}

.tips-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: rgba(255,255,255,0.8);
  margin-bottom: 16px;
}

.tips-title .el-icon { color: #60a5fa; }

.tips-list {
  padding-left: 18px;
  margin: 0 0 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tips-list li {
  font-size: 13px;
  color: rgba(255,255,255,0.6);
  line-height: 1.5;
}

.demo-note {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  background: rgba(96,165,250,0.1);
  border: 1px solid rgba(96,165,250,0.2);
  border-radius: 10px;
  font-size: 13px;
  color: #60a5fa;
}

.demo-note .el-icon { flex-shrink: 0; }
</style>
