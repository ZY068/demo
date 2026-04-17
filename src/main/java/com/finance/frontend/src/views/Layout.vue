<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon size="28"><Money /></el-icon>
        <span>智能报销</span>
      </div>

      <!-- 动态菜单：按角色显示不同菜单 -->
      <el-menu :default-active="activeMenu" router class="side-menu">

        <!-- 工作台：仅经理、财务、管理员可见 -->
        <el-menu-item v-if="userRole !== 0" index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>工作台</span>
        </el-menu-item>

        <!-- ========== 员工菜单（角色=0）========== -->
        <template v-if="userRole === 0">
          <el-menu-item index="/claims/create">
            <el-icon><Plus /></el-icon>
            <span>创建报销</span>
          </el-menu-item>
          <el-menu-item index="/claims">
            <el-icon><Document /></el-icon>
            <span>我的报销</span>
          </el-menu-item>
          <el-menu-item index="/invoices">
            <el-icon><Tickets /></el-icon>
            <span>发票管理</span>
          </el-menu-item>
        </template>

        <!-- ========== 部门经理菜单（角色=1）========== -->
        <template v-if="userRole === 1">
          <el-menu-item index="/manager/approval">
            <el-icon><Checked /></el-icon>
            <span>待审批</span>
            <el-badge v-if="pendingCount > 0" :value="pendingCount" class="menu-badge" />
          </el-menu-item>
          <el-menu-item index="/claims">
            <el-icon><Document /></el-icon>
            <span>部门报销</span>
          </el-menu-item>
          <el-menu-item index="/claims/create">
            <el-icon><Plus /></el-icon>
            <span>我的报销</span>
          </el-menu-item>
        </template>

        <!-- ========== 财务内审菜单（角色=2）========== -->
        <template v-if="userRole === 2">
          <el-menu-item index="/finance/audit">
            <el-icon><Checked /></el-icon>
            <span>财务审计</span>
            <el-badge v-if="pendingCount > 0" :value="pendingCount" class="menu-badge" />
          </el-menu-item>
          <el-menu-item index="/finance/risk">
            <el-icon><Warning /></el-icon>
            <span>风险预警</span>
          </el-menu-item>
          <el-menu-item index="/finance/report">
            <el-icon><DataLine /></el-icon>
            <span>审计报告</span>
          </el-menu-item>
          <el-menu-item index="/finance/budget">
            <el-icon><Money /></el-icon>
            <span>预算管理</span>
          </el-menu-item>
          <el-menu-item index="/claims">
            <el-icon><Document /></el-icon>
            <span>报销单查询</span>
          </el-menu-item>
        </template>

        <!-- ========== IT管理员菜单（角色=9）========== -->
        <template v-if="userRole === 9">
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/rules">
            <el-icon><Setting /></el-icon>
            <span>规则配置</span>
          </el-menu-item>
          <el-menu-item index="/admin/logs">
            <el-icon><List /></el-icon>
            <span>审计日志</span>
          </el-menu-item>
          <el-menu-item index="/admin/departments">
            <el-icon><OfficeBuilding /></el-icon>
            <span>部门管理</span>
          </el-menu-item>
          <el-menu-item index="/claims">
            <el-icon><Document /></el-icon>
            <span>报销单查询</span>
          </el-menu-item>
        </template>

      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tag :type="roleTagType" size="small" style="margin-right: 12px">{{ roleName }}</el-tag>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" style="background: #409eff">
                {{ user?.realName?.charAt(0) || user?.username?.charAt(0)?.toUpperCase() || 'A' }}
              </el-avatar>
              <span class="username">{{ user?.realName || user?.username || '管理员' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <span style="color: #999">{{ user?.deptName || '未分配部门' }}</span>
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Money, DataAnalysis, Document, Plus, Tickets, Warning, DataLine, Setting, ArrowDown, Checked, User, List, OfficeBuilding } from '@element-plus/icons-vue'
import api from '../api'

const route = useRoute()
const router = useRouter()
const user = ref(null)
const pendingCount = ref(0)

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')

// 用户角色
const userRole = computed(() => {
  return user.value?.role ?? 0
})

// 角色名称
const roleName = computed(() => {
  const roles = {
    0: '普通员工',
    1: '部门经理',
    2: '财务内审',
    9: 'IT管理员'
  }
  return roles[userRole.value] || '未知角色'
})

// 角色标签颜色
const roleTagType = computed(() => {
  const types = {
    0: '',
    1: 'success',
    2: 'warning',
    9: 'danger'
  }
  return types[userRole.value] || ''
})

onMounted(async () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      user.value = JSON.parse(userStr)
    } catch {}
  }

  // 获取待审批数量（经理和财务）
  if (userRole.value === 1 || userRole.value === 2) {
    try {
      const res = await api.getPendingClaims()
      if (res.code === 200) {
        pendingCount.value = res.data?.length || 0
      }
    } catch {}
  }
})

const handleCommand = (command) => {
  if (command === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.aside {
  background: rgba(255, 255, 255, 0.08) !important;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-right: 1px solid rgba(255, 255, 255, 0.15);
  overflow-x: hidden;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
}
.logo .el-icon { margin-right: 8px; color: #60a5fa; }
.side-menu { border-right: none !important; background: transparent !important; }
.side-menu .el-menu-item {
  height: 50px;
  line-height: 50px;
  color: rgba(255, 255, 255, 0.75) !important;
  background: transparent !important;
}
.side-menu .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.12) !important;
  color: #fff !important;
}
.side-menu .el-menu-item.is-active {
  background: rgba(64, 158, 255, 0.3) !important;
  color: #fff !important;
  border-right: 2px solid #60a5fa;
}
.menu-badge { margin-left: 8px; }
.header {
  background: rgba(255, 255, 255, 0.08) !important;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}
.header-left { display: flex; align-items: center; }
.header-right { display: flex; align-items: center; }
.user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.username { font-size: 14px; color: #fff; }
.main { background: transparent; padding: 20px; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>