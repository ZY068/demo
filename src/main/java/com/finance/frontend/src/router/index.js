import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: (to) => {
      // 根据角色跳转不同首页
      const userStr = localStorage.getItem('user')
      if (userStr) {
        const user = JSON.parse(userStr)
        if (user.role === 0) return '/claims'  // 员工跳转到我的报销
        if (user.role === 9) return '/admin/users'  // 管理员跳转到用户管理
      }
      return '/dashboard'  // 其他角色跳转到工作台
    },
    children: [
      // 工作台：仅经理、财务可见
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '工作台', roles: [1, 2] } },
      { path: 'claims', name: 'Claims', component: () => import('../views/Claims.vue'), meta: { title: '报销单管理' } },

      // 员工专属
      { path: 'claims/create', name: 'ClaimCreate', component: () => import('../views/ClaimCreate.vue'), meta: { title: '创建报销单', roles: [0, 1] } },
      { path: 'invoices', name: 'Invoices', component: () => import('../views/Invoices.vue'), meta: { title: '发票管理', roles: [0] } },

      // 部门经理专属
      {
        path: 'manager',
        meta: { roles: [1] },
        children: [
          { path: 'approval', name: 'ManagerApproval', component: () => import('../views/manager/Approval.vue'), meta: { title: '待审批', roles: [1] } }
        ]
      },

      // 财务内审专属
      {
        path: 'finance',
        meta: { roles: [2] },
        children: [
          { path: 'audit', name: 'FinanceAudit', component: () => import('../views/finance/Audit.vue'), meta: { title: '财务审计', roles: [2] } },
          { path: 'risk', name: 'FinanceRisk', component: () => import('../views/finance/Risk.vue'), meta: { title: '风险预警', roles: [2] } },
          { path: 'report', name: 'FinanceReport', component: () => import('../views/finance/Report.vue'), meta: { title: '审计报告', roles: [2] } },
          { path: 'budget', name: 'FinanceBudget', component: () => import('../views/finance/Budget.vue'), meta: { title: '预算管理', roles: [2] } }
        ]
      },

      // IT管理员专属
      {
        path: 'admin',
        meta: { roles: [9] },
        children: [
          { path: 'users', name: 'AdminUsers', component: () => import('../views/admin/Users.vue'), meta: { title: '用户管理', roles: [9] } },
          { path: 'rules', name: 'AdminRules', component: () => import('../views/admin/Rules.vue'), meta: { title: '规则配置', roles: [9] } },
          { path: 'logs', name: 'AdminLogs', component: () => import('../views/admin/Logs.vue'), meta: { title: '审计日志', roles: [9] } },
          { path: 'departments', name: 'AdminDepartments', component: () => import('../views/admin/Departments.vue'), meta: { title: '部门管理', roles: [9] } }
        ]
      },

      // 兼容旧路由
      { path: 'risk', redirect: '/finance/risk' },
      { path: 'audit', redirect: '/finance/audit' },
      { path: 'rules', redirect: '/admin/rules' },
      { path: 'departments', redirect: '/admin/departments' }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 角色名称映射
 */
const roleNames = {
  0: '普通员工',
  1: '部门经理',
  2: '财务内审',
  9: 'IT管理员'
}

/**
 * 路由守卫
 * 1. 设置页面标题
 * 2. 检查登录状态
 * 3. 检查角色权限
 */
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 智能报销审计系统` : '智能报销审计系统'

  // 公开页面直接放行
  if (to.meta.public) {
    return next()
  }

  // 检查登录状态
  const token = localStorage.getItem('token')
  if (!token) {
    return next('/login')
  }

  // 已登录访问登录页，根据角色跳转首页
  if (to.path === '/login') {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const user = JSON.parse(userStr)
      if (user.role === 0) return next('/claims')
      if (user.role === 9) return next('/admin/users')
    }
    return next('/dashboard')
  }

  // 检查角色权限
  const userStr = localStorage.getItem('user')
  if (userStr && to.meta.roles) {
    try {
      const user = JSON.parse(userStr)
      const userRole = user.role

      if (!to.meta.roles.includes(userRole)) {
        // 根据角色跳转到对应首页（不显示错误，这是正常行为）
        if (userRole === 0) return next('/claims')
        if (userRole === 9) return next('/admin/users')
        return next('/dashboard')
      }
    } catch {
      return next('/login')
    }
  }

  next()
})

export default router