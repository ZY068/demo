<template>
  <div class="users-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" size="small" @click="showDialog()">新增用户</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.role)">{{ getRoleName(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deptId" label="部门" width="120" />
        <el-table-column prop="phone" label="电话" width="120" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="showDialog(row)">编辑</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 用户编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editUser ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" :disabled="!!editUser" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="普通员工" :value="0" />
            <el-option label="部门经理" :value="1" />
            <el-option label="财务内审" :value="2" />
            <el-option label="IT管理员" :value="9" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.deptId" style="width: 100%" placeholder="请选择部门">
            <el-option v-for="d in departments" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api'

const loading = ref(false)
const tableData = ref([])
const departments = ref([])
const dialogVisible = ref(false)
const editUser = ref(null)
const form = reactive({
  username: '',
  realName: '',
  role: 0,
  deptId: null,
  phone: '',
  status: 1
})

const getRoleType = (role) => {
  const types = { 0: '', 1: 'success', 2: 'warning', 9: 'danger' }
  return types[role] || ''
}

const getRoleName = (role) => {
  const names = { 0: '普通员工', 1: '部门经理', 2: '财务内审', 9: 'IT管理员' }
  return names[role] || '未知'
}

const loadData = async () => {
  loading.value = true
  try {
    const [usersRes, deptRes] = await Promise.all([
      api.getUsers(),
      api.getDepartments()
    ])
    if (usersRes.code === 200) tableData.value = usersRes.data || []
    if (deptRes.code === 200) departments.value = deptRes.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const showDialog = (user = null) => {
  editUser.value = user
  if (user) {
    Object.assign(form, user)
  } else {
    Object.assign(form, { username: '', realName: '', role: 0, deptId: null, phone: '', status: 1 })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    let res
    if (editUser.value) {
      res = await api.updateUser(editUser.value.id, form)
    } else {
      res = await api.createUser(form)
    }
    if (res.code === 200) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该用户？', '提示', { type: 'warning' })
  try {
    const res = await api.deleteUser(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>