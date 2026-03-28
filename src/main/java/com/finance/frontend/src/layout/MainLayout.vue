<template>
  <el-container class="layout-container">
    <el-aside width="240px">
      <div class="logo">
        <el-icon color="#409EFF" size="24"><Platform /></el-icon>
        <span>智能审计平台</span>
      </div>
      <el-menu :default-active="$route.path" router class="el-menu-vertical">
        <el-menu-item v-if="user.role === 0" index="/employee">
          <el-icon><EditPen /></el-icon><span>报销申请</span>
        </el-menu-item>
        <el-menu-item v-if="user.role === 1" index="/manager">
          <el-icon><Checked /></el-icon><span>经理审批</span>
        </el-menu-item>
        <el-menu-item v-if="user.role === 2" index="/auditor">
          <el-icon><Aim /></el-icon><span>风险探针</span>
        </el-menu-item>
        <el-menu-item v-if="user.role === 9" index="/admin">
          <el-icon><Setting /></el-icon><span>系统维护</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header>
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>工作台</el-breadcrumb-item>
            <el-breadcrumb-item>{{ roleName }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tag type="success" effect="light">{{ roleName }}</el-tag>
          <span class="user-name">{{ user.username }}</span>
          <el-button link type="danger" @click="logout">退出</el-button>
        </div>
      </el-header>
      
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Platform, EditPen, Checked, Aim, Setting } from '@element-plus/icons-vue'

const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || '{}')

const roleName = computed(() => {
  const roles = { 0: '普通员工', 1: '部门经理', 2: '财务内审', 9: 'IT管理员' }
  return roles[user.role] || '未知角色'
})

const logout = () => {
  localStorage.removeItem('user')
  router.push('/login')
}
</script>

<style scoped>
.layout-container { height: 100vh; background: #f0f2f5; }
.el-aside { background: #001529; color: #fff; }
.logo { height: 64px; display: flex; align-items: center; padding: 0 20px; font-weight: bold; background: #002140; gap: 10px; }
.el-menu { border-right: none; }
.el-menu-vertical { background: transparent; }
:deep(.el-menu-item) { color: rgba(255, 255, 255, 0.65); }
:deep(.el-menu-item.is-active) { background: #1890ff !important; color: #fff; }
.el-header { background: #fff; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #ddd; padding: 0 20px; }
.header-right { display: flex; align-items: center; gap: 15px; }
.user-name { font-weight: 600; color: #606266; }
</style>