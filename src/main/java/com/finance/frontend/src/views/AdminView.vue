<template>
  <div class="admin-container">
    <el-card header="系统权限管理 (IT Admin)">
      <el-alert 
        title="职责分离提醒：IT管理员仅拥有用户维护权限，禁止接触财务审批流。" 
        type="info" 
        show-icon 
        style="margin-bottom: 20px"
      />
      
      <el-table :data="userList" border>
        <el-table-column prop="id" label="UID" width="80" />
        <el-table-column prop="username" label="账户名称" />
        <el-table-column label="系统角色">
          <template #default="scope">
            <el-tag :type="getRoleTag(scope.row.role)">
              {{ getRoleName(scope.row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号状态">
          <template #default><el-switch :model-value="true" /></template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default>
            <el-button type="primary" size="small" link>重置密码</el-button>
            <el-button type="danger" size="small" link>注销</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'

// 模拟用户列表，实际项目中你可以再写个 /api/auth/all 接口
const userList = ref([
  { id: 1, username: '张三_员工', role: 0 },
  { id: 2, username: '李四_经理', role: 1 },
  { id: 3, username: '王五_财务', role: 2 },
  { id: 4, username: '赵六_管理员', role: 9 }
])

const getRoleName = (role) => {
  const map = { 0: '普通员工', 1: '部门经理', 2: '财务内审', 9: '系统管理员' }
  return map[role]
}

const getRoleTag = (role) => {
  const map = { 0: 'info', 1: '', 2: 'danger', 9: 'success' }
  return map[role]
}
</script>