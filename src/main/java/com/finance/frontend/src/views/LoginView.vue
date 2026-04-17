<template>
  <div class="login-wrapper">
    <div class="login-box">
      <div class="login-banner">
        <div class="banner-content">
          <h1>Smart Audit</h1>
          <p>基于数据透视的智能报销与内控系统</p>
          <div class="role-tags">
            <el-tag size="small" type="info">职责分离 (SoD)</el-tag>
            <el-tag size="small" type="info" effect="plain">自动化审计</el-tag>
          </div>
        </div>
      </div>

      <div class="login-form-section">
        <div class="form-header">
          <h2>欢迎回来</h2>
          <span>请使用您的系统账号登录</span>
        </div>

        <el-form :model="loginForm" label-position="top" class="custom-form">
          <el-form-item label="用户名">
            <el-input 
              v-model="loginForm.username" 
              placeholder="请输入用户名" 
              prefix-icon="User"
              size="large"
            />
          </el-form-item>
          <el-form-item label="安全密码">
            <el-input 
              v-model="loginForm.password" 
              type="password" 
              placeholder="请输入密码" 
              prefix-icon="Lock"
              show-password
              size="large"
            />
          </el-form-item>
          <div class="form-options">
            <el-checkbox v-model="rememberMe">记住账号</el-checkbox>
          </div>
          <el-button 
            type="primary" 
            class="submit-btn" 
            @click="handleLogin" 
            :loading="loading"
          >
            立即登录
          </el-button>
        </el-form>

        <div class="form-footer">
          <p>测试账号: 张三_员工 / 王五_财务</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue' // 需要安装图标库

const router = useRouter()
const loading = ref(false)
const rememberMe = ref(true)
const loginForm = ref({ username: '', password: '' })

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请填写完整的身份信息')
    return
  }
  
  loading.value = true
  try {
    const res = await axios.post('http://118.31.247.232/api/auth/login', loginForm.value)
    if (res.data.code === 200) {
      // 保存 Token 和用户信息
      const token = res.data.data?.token
      const user = res.data.data?.user
      if (token) {
        localStorage.setItem('token', token)
      }
      if (user) {
        localStorage.setItem('user', JSON.stringify(user))
      }
      const role = user?.role

      ElMessage.success({
        message: `欢迎回来，${user?.username || loginForm.value.username}`,
        type: 'success'
      })

      // 角色路由分发
      if (role === 0) {
      router.push('/employee')
    } else if (role === 1) {
      router.push('/manager') // 经理李四走这里
    } else if (role === 2) {
      router.push('/auditor') // 财务王五走这里
    } else if (role === 9) {
      router.push('/admin')   // 管理员赵六走这里
    } else {
      ElMessage.warning('角色未定义，请联系系统管理员')
    }
  } else {
    ElMessage.error(res.data.msg)
    }
  } catch (error) {
    ElMessage.error('无法连接审计后端，请检查网络或 CORS 配置')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  overflow: hidden;
}

.login-box {
  width: 900px;
  height: 550px;
  display: flex;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.login-banner {
  flex: 1.2;
  background: linear-gradient(rgba(0, 33, 64, 0.8), rgba(0, 33, 64, 0.8)),
              url('https://images.unsplash.com/photo-1450101499163-c8848c66ca85?auto=format&fit=crop&q=80&w=1000') center/cover;
  display: flex;
  align-items: center;
  padding: 60px;
  color: #fff;
}

.banner-content h1 {
  font-size: 3rem;
  margin-bottom: 10px;
  letter-spacing: 2px;
}

.banner-content p {
  font-size: 1.1rem;
  opacity: 0.8;
  margin-bottom: 20px;
}

.role-tags .el-tag {
  margin-right: 10px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  color: #fff;
}

.login-form-section {
  flex: 1;
  padding: 60px 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-header {
  margin-bottom: 40px;
}

.form-header h2 {
  font-size: 1.8rem;
  color: #303133;
  margin-bottom: 8px;
}

.form-header span {
  color: #909399;
  font-size: 0.9rem;
}

.custom-form :deep(.el-form-item__label) {
  font-weight: 600;
  padding-bottom: 4px;
}

.form-options {
  margin-bottom: 20px;
}

.submit-btn {
  width: 100%;
  height: 45px;
  font-size: 1rem;
  font-weight: 600;
  border-radius: 8px;
  background: #002140;
  border: none;
}

.submit-btn:hover {
  background: #003a70;
}

.form-footer {
  margin-top: 30px;
  text-align: center;
}

.form-footer p {
  color: #c0c4cc;
  font-size: 0.8rem;
}
</style>