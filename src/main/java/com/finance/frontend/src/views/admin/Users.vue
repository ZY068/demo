<template>
  <div class="users-page">
    <div class="page-header">
      <h2 class="page-title">用户管理</h2>
      <el-button type="primary" class="btn-add" @click="showDialog()">
        <el-icon><Plus /></el-icon> 新增用户
      </el-button>
    </div>

    <!-- 用户卡片列表 -->
    <div class="users-grid" v-if="!loading && tableData.length > 0">
      <div v-for="user in tableData" :key="user.id" class="user-card">
        <div class="card-left">
          <div class="user-avatar" :class="`role-${user.role}`">
            {{ (user.realName || user.username || 'U').charAt(0).toUpperCase() }}
          </div>
          <div class="user-info">
            <div class="user-name">{{ user.realName || user.username }}</div>
            <div class="user-meta">@{{ user.username }}</div>
          </div>
        </div>
        <div class="card-right">
          <div class="user-role-tag" :class="`role-tag-${user.role}`">
            {{ getRoleName(user.role) }}
          </div>
          <div class="user-dept" v-if="user.deptId">部门 {{ user.deptId }}</div>
          <div class="user-status">
            <span class="status-dot" :class="user.status === 1 ? 'online' : 'offline'"></span>
            {{ user.status === 1 ? '启用' : '禁用' }}
          </div>
        </div>
        <div class="card-actions">
          <el-button type="primary" size="small" link @click="showDialog(user)">编辑</el-button>
          <el-button type="danger" size="small" link @click="handleDelete(user)">删除</el-button>
        </div>
      </div>
    </div>

    <div v-if="!loading && tableData.length === 0" class="empty-state">
      <div class="empty-icon"><el-icon><UserFilled /></el-icon></div>
      <p>暂无用户数据</p>
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="36"><Loading /></el-icon>
    </div>

    <!-- 用户编辑抽屉 -->
    <el-drawer v-model="dialogVisible" :title="editUser ? '编辑用户' : '新增用户'" size="480px" direction="rtl">
      <div class="drawer-inner" v-if="form">
        <el-form :model="form" label-position="top">
          <el-form-item label="用户名">
            <el-input v-model="form.username" :disabled="!!editUser" placeholder="登录用户名" />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="form.realName" placeholder="真实姓名" />
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
            <el-input v-model="form.phone" placeholder="联系电话" />
          </el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
            <span class="status-hint">{{ form.status === 1 ? '启用中' : '已禁用' }}</span>
          </el-form-item>
        </el-form>
        <div class="drawer-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">保存</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus, UserFilled, Loading } from '@element-plus/icons-vue'
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

const getRoleName = (role) => ({ 0: '普通员工', 1: '部门经理', 2: '财务内审', 9: 'IT管理员' }[role] || '未知')

const loadData = async () => {
  loading.value = true
  try {
    const [usersRes, deptRes] = await Promise.all([api.getUsers(), api.getDepartments()])
    if (usersRes.code === 200) tableData.value = usersRes.data || []
    if (deptRes.code === 200) departments.value = deptRes.data || []
  } catch {} finally { loading.value = false }
}

const showDialog = (user = null) => {
  editUser.value = user
  if (user) Object.assign(form, user)
  else Object.assign(form, { username: '', realName: '', role: 0, deptId: null, phone: '', status: 1 })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    const res = editUser.value
      ? await api.updateUser(editUser.value.id, form)
      : await api.createUser(form)
    if (res.code === 200) { ElMessage.success('保存成功'); dialogVisible.value = false; loadData() }
    else ElMessage.error(res.msg || '保存失败')
  } catch { ElMessage.error('保存失败') }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该用户？', '提示', { type: 'warning' })
    const res = await api.deleteUser(row.id)
    if (res.code === 200) { ElMessage.success('删除成功'); loadData() }
  } catch { ElMessage.error('删除失败') }
}

onMounted(() => loadData())
</script>

<style scoped>
.users-page { padding: 0; min-height: 100%; }

.page-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 24px;
}

.page-title { font-size: 22px; font-weight: 700; color: #fff; margin: 0; }

.btn-add {
  background: rgba(96,165,250,0.35) !important;
  border: 1px solid rgba(96,165,250,0.35) !important;
  color: #fff !important;
  border-radius: 10px !important;
}

.btn-add:hover {
  background: rgba(96,165,250,0.6) !important;
}

/* 用户卡片网格 */
.users-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.user-card {
  background: rgba(255,255,255,0.08);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: 18px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition: all 0.25s ease;
}

.user-card:hover {
  transform: translateY(-2px);
  border-color: rgba(96,165,250,0.35);
  box-shadow: 0 8px 30px rgba(31,38,135,0.25);
}

.card-left { display: flex; align-items: center; gap: 14px; }

.user-avatar {
  width: 50px; height: 50px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; font-weight: 800;
  flex-shrink: 0;
}

.user-avatar.role-0 { background: rgba(148,163,184,0.3); color: #94a3b8; border: 1px solid rgba(148,163,184,0.3); }
.user-avatar.role-1 { background: rgba(52,211,153,0.2); color: #34d399; border: 1px solid rgba(52,211,153,0.3); }
.user-avatar.role-2 { background: rgba(251,191,36,0.2); color: #fbbf24; border: 1px solid rgba(251,191,36,0.3); }
.user-avatar.role-9 { background: rgba(248,113,113,0.2); color: #f87171; border: 1px solid rgba(248,113,113,0.3); }

.user-name { font-size: 15px; font-weight: 600; color: #fff; }
.user-meta { font-size: 12px; color: rgba(255,255,255,0.4); margin-top: 2px; }

.card-right { display: flex; flex-direction: column; gap: 6px; }

.user-role-tag {
  display: inline-flex; align-items: center;
  padding: 3px 10px; border-radius: 6px;
  font-size: 12px; font-weight: 600;
  width: fit-content;
}

.role-tag-0 { background: rgba(148,163,184,0.2); color: #94a3b8; border: 1px solid rgba(148,163,184,0.3); }
.role-tag-1 { background: rgba(52,211,153,0.2); color: #34d399; border: 1px solid rgba(52,211,153,0.3); }
.role-tag-2 { background: rgba(251,191,36,0.2); color: #fbbf24; border: 1px solid rgba(251,191,36,0.3); }
.role-tag-9 { background: rgba(248,113,113,0.2); color: #f87171; border: 1px solid rgba(248,113,113,0.3); }

.user-dept { font-size: 12px; color: rgba(255,255,255,0.5); }

.user-status {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; color: rgba(255,255,255,0.5);
}

.status-dot {
  width: 8px; height: 8px; border-radius: 50%;
}

.status-dot.online { background: #34d399; box-shadow: 0 0 6px rgba(52,211,153,0.6); }
.status-dot.offline { background: rgba(255,255,255,0.2); }

.card-actions {
  display: flex; gap: 8px;
  padding-top: 12px; border-top: 1px solid rgba(255,255,255,0.1);
}

/* 空/加载状态 */
.empty-state { text-align: center; padding: 80px; color: rgba(255,255,255,0.4); }
.empty-icon { width: 64px; height: 64px; border-radius: 50%; background: rgba(255,255,255,0.06); display: flex; align-items: center; justify-content: center; margin: 0 auto 16px; font-size: 28px; }
.loading-state { text-align: center; padding: 60px; color: rgba(255,255,255,0.5); }

/* 抽屉 */
:deep(.el-drawer) { background: rgba(15, 10, 40, 0.97) !important; backdrop-filter: blur(20px); border-left: 1px solid rgba(255,255,255,0.12); }
:deep(.el-drawer__header) { color: #fff !important; border-bottom: 1px solid rgba(255,255,255,0.1); padding: 20px 24px; margin-bottom: 0; }

.drawer-inner { padding: 24px; color: #fff; display: flex; flex-direction: column; gap: 24px; }

:deep(.el-form-item__label) { color: rgba(255,255,255,0.8) !important; font-weight: 500; margin-bottom: 6px; }

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  background: rgba(255,255,255,0.1) !important;
  box-shadow: 0 0 0 1px rgba(255,255,255,0.2) inset !important;
  border-radius: 10px !important;
}

:deep(.el-input__inner),
:deep(.el-select__placeholder) { color: #fff !important; }

.drawer-footer { display: flex; gap: 12px; justify-content: flex-end; padding-top: 16px; border-top: 1px solid rgba(255,255,255,0.1); }

.status-hint { margin-left: 12px; font-size: 13px; color: rgba(255,255,255,0.5); }
</style>
