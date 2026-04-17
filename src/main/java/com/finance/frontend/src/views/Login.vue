<template>
  <div class="login-page">
    <!-- 左侧品牌展示区 -->
    <div class="brand-panel">
      <!-- 装饰性浮动光斑 -->
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
      <div class="orb orb-4"></div>
      <!-- 装饰斜线 -->
      <div class="deco-lines">
        <div class="line line-1"></div>
        <div class="line line-2"></div>
        <div class="line line-3"></div>
      </div>

      <div class="brand-content">
        <div class="brand-logo">
          <div class="logo-icon">
            <el-icon :size="56"><Money /></el-icon>
          </div>
          <div class="logo-glow"></div>
        </div>
        <h1 class="brand-title">智能报销<br/>审计系统</h1>
        <p class="brand-subtitle">Smart Expense & Audit Platform</p>
        <div class="brand-features">
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>全流程数字化报销</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>AI 驱动的风险预警</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>精准预算管控</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧登录表单区 -->
    <div class="form-panel">
      <div class="form-card">
        <div class="form-header">
          <h2>欢迎回来</h2>
          <p>请登录您的账号以继续</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" class="login-form" @submit.prevent="handleLogin">
          <el-form-item prop="username">
            <div class="input-group">
              <el-icon class="input-icon"><User /></el-icon>
              <el-input
                v-model="form.username"
                placeholder="用户名"
                size="large"
                :prefix-icon="User"
              />
            </div>
          </el-form-item>

          <el-form-item prop="password">
            <div class="input-group">
              <el-icon class="input-icon"><Lock /></el-icon>
              <el-input
                v-model="form.password"
                type="password"
                placeholder="密码"
                size="large"
                :prefix-icon="Lock"
                show-password
                @keyup.enter="handleLogin"
              />
            </div>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="demo-hint">
          <span class="demo-label">演示账号</span>
          <div class="demo-accounts">
            <span>admin / 123456</span>
            <span class="sep">·</span>
            <span>emp001 / 123456</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Money, Check } from '@element-plus/icons-vue'
import api from '../api'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: '123456'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await api.login(form)
    if (res.code === 200) {
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify(res.data))
      ElMessage.success('登录成功')

      const role = res.data.role
      if (role === 0) router.push('/claims')
      else if (role === 9) router.push('/admin/users')
      else router.push('/dashboard')
    } else {
      ElMessage.error(res.msg || '登录失败')
    }
  } catch (e) {
    ElMessage.error('登录失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* =================== 全页面双栏布局 =================== */
.login-page {
  display: flex;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

/* =================== 左侧品牌展示区 =================== */
.brand-panel {
  width: 42%;
  height: 100%;
  position: relative;
  overflow: hidden;
  background: linear-gradient(145deg, #1a0533 0%, #2d1b69 40%, #4c2a9e 70%, #667eea 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 装饰性浮动光斑 */
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.6;
  pointer-events: none;
}

.orb-1 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(102, 126, 234, 0.8), transparent);
  top: -80px;
  left: -80px;
  animation: float1 8s ease-in-out infinite;
}

.orb-2 {
  width: 220px;
  height: 220px;
  background: radial-gradient(circle, rgba(118, 75, 162, 0.9), transparent);
  top: 30%;
  right: -60px;
  animation: float2 10s ease-in-out infinite;
}

.orb-3 {
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, rgba(79, 70, 229, 0.7), transparent);
  bottom: 20%;
  left: 10%;
  animation: float3 7s ease-in-out infinite;
}

.orb-4 {
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(167, 139, 250, 0.6), transparent);
  bottom: -60px;
  right: 20%;
  animation: float2 9s ease-in-out infinite reverse;
}

@keyframes float1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(30px, 20px) scale(1.1); }
}

@keyframes float2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-25px, 15px) scale(0.95); }
}

@keyframes float3 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(20px, -20px) scale(1.05); }
}

/* 装饰斜线 */
.deco-lines {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.line {
  position: absolute;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.06), transparent);
  height: 1px;
  width: 120%;
  left: -10%;
  transform-origin: center;
}

.line-1 { top: 20%; transform: rotate(-12deg); }
.line-2 { top: 45%; transform: rotate(-8deg); }
.line-3 { top: 70%; transform: rotate(-15deg); }

/* 品牌内容 */
.brand-content {
  position: relative;
  z-index: 2;
  text-align: center;
  padding: 40px;
  animation: fadeInUp 0.8s ease-out both;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.brand-logo {
  position: relative;
  display: inline-block;
  margin-bottom: 32px;
}

.logo-icon {
  width: 100px;
  height: 100px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #a5b4fc;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3), 0 0 40px rgba(102, 126, 234, 0.2);
  animation: logoPulse 4s ease-in-out infinite;
}

.logo-glow {
  position: absolute;
  inset: -20px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(102, 126, 234, 0.3), transparent 70%);
  z-index: -1;
  animation: glowPulse 3s ease-in-out infinite;
}

@keyframes logoPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.03); }
}

@keyframes glowPulse {
  0%, 100% { opacity: 0.6; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.15); }
}

.brand-title {
  font-size: 42px;
  font-weight: 800;
  color: #fff;
  line-height: 1.3;
  letter-spacing: 1px;
  text-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  margin-bottom: 12px;
}

.brand-subtitle {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 3px;
  text-transform: uppercase;
  margin-bottom: 40px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 14px;
  text-align: left;
  max-width: 260px;
  margin: 0 auto;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
}

.feature-item .el-icon {
  color: #a5b4fc;
  font-size: 16px;
  flex-shrink: 0;
}

/* =================== 右侧登录表单区 =================== */
.form-panel {
  flex: 1;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  /* 渐变背景延伸到右侧 */
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow: hidden;
}

/* 表单面板的背景扩散光 */
.form-panel::before {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(118, 75, 162, 0.4), transparent 70%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.form-card {
  width: 100%;
  max-width: 440px;
  padding: 52px 48px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 28px;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.25), 0 0 60px rgba(102, 126, 234, 0.15);
  position: relative;
  z-index: 2;
  animation: cardIn 0.7s cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: 0.2s;
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(40px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.form-header {
  margin-bottom: 36px;
  text-align: center;
}

.form-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}

.form-header p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

/* 覆盖 Element Plus 默认 input 样式 */
:deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.12) !important;
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.2) inset !important;
  border-radius: 12px;
  padding: 4px 16px;
  height: 48px;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.4) inset !important;
  background-color: rgba(255, 255, 255, 0.18) !important;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.6) inset !important;
  background-color: rgba(255, 255, 255, 0.2) !important;
}

:deep(.el-input__inner) {
  color: #fff !important;
  font-size: 15px;
  height: 40px;
}

:deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.5) !important;
}

:deep(.el-input__prefix .el-icon) {
  color: rgba(255, 255, 255, 0.6) !important;
}

:deep(.el-form-item__error) {
  color: #fca5a5 !important;
}

.login-form {
  margin-bottom: 28px;
}

.login-form .el-form-item {
  margin-bottom: 20px;
}

.login-btn {
  width: 100%;
  height: 52px !important;
  font-size: 16px !important;
  font-weight: 600;
  letter-spacing: 3px;
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.7), rgba(99, 102, 241, 0.7)) !important;
  border: 1px solid rgba(147, 197, 253, 0.4) !important;
  border-radius: 14px !important;
  color: #fff !important;
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.3);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1) !important;
  cursor: pointer;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 36px rgba(99, 102, 241, 0.5) !important;
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.9), rgba(99, 102, 241, 0.9)) !important;
}

.login-btn:active {
  transform: translateY(0);
}

.demo-hint {
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
}

.demo-label {
  display: block;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 2px;
  color: rgba(255, 255, 255, 0.4);
  margin-bottom: 8px;
}

.demo-accounts {
  display: flex;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.65);
}

.demo-accounts .sep {
  color: rgba(255, 255, 255, 0.3);
}

/* =================== 响应式 =================== */
@media (max-width: 768px) {
  .login-page {
    flex-direction: column;
  }

  .brand-panel {
    width: 100%;
    height: 220px;
    flex-shrink: 0;
  }

  .brand-content {
    padding: 24px 20px;
  }

  .brand-title {
    font-size: 28px;
  }

  .brand-subtitle {
    display: none;
  }

  .brand-features {
    display: none;
  }

  .logo-icon {
    width: 72px;
    height: 72px;
    border-radius: 20px;
  }

  .logo-icon .el-icon {
    font-size: 36px;
  }

  .orb { display: none; }

  .form-card {
    margin: 0;
    border-radius: 28px 28px 0 0;
    padding: 40px 32px;
    max-width: 100%;
    flex: 1;
    box-shadow: 0 -10px 40px rgba(0, 0, 0, 0.2);
  }
}
</style>
