<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1>智能报销审计系统</h1>
        <p>Smart Expense & Audit System</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <el-collapse>
          <el-collapse-item title="演示账号" name="demo">
            <div class="demo-accounts">
              <p><strong>普通员工：</strong>emp001 / 123456</p>
              <p><strong>部门经理：</strong>mgr001 / 123456</p>
              <p><strong>财务内审：</strong>fin001 / 123456</p>
              <p><strong>IT管理员：</strong>admin / 123456</p>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
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
      localStorage.setItem('token', res.data?.token || 'demo-token')
      localStorage.setItem('user', JSON.stringify(res.data?.user || { username: form.username }))
      ElMessage.success('登录成功')
      router.push('/dashboard')
    } else {
      ElMessage.error(res.msg || '登录失败')
    }
  } catch (e) {
    // 演示模式：直接跳转
    localStorage.setItem('token', 'demo-token')
    localStorage.setItem('user', JSON.stringify({ username: form.username }))
    ElMessage.success('演示模式登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-box {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}
.login-header {
  text-align: center;
  margin-bottom: 32px;
}
.login-header h1 {
  font-size: 24px;
  color: #333;
  margin-bottom: 8px;
}
.login-header p {
  font-size: 14px;
  color: #999;
}
.login-form {
  margin-bottom: 24px;
}
.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
}
.login-footer {
  text-align: center;
  color: #999;
  font-size: 12px;
}
</style>
