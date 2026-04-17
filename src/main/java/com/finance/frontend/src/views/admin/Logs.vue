<template>
  <div class="logs-page">
    <div class="page-header">
      <h2 class="page-title">审计日志</h2>
      <el-button size="small" @click="loadData" class="btn-refresh">
        <el-icon><Refresh /></el-icon> 刷新
      </el-button>
    </div>

    <div class="glass-card">
      <el-table :data="tableData" v-loading="loading" style="width:100%" row-key="id">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="operatorId" label="操作人" width="100" />
        <el-table-column label="操作类型" width="110">
          <template #default="{ row }">
            <span class="action-tag" :class="`action-${row.actionType}`">{{ row.actionType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作描述" min-width="260" />
        <el-table-column prop="ipAddress" label="IP" width="130" />
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column label="详情" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="showDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 详情对话框 -->
    <el-drawer v-model="dialogVisible" title="操作详情" size="560px" direction="rtl">
      <div class="drawer-inner" v-if="currentLog">
        <div class="log-meta">
          <div class="meta-row">
            <span class="meta-label">操作人ID</span>
            <span class="meta-value">{{ currentLog.operatorId }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">操作类型</span>
            <span class="action-tag" :class="`action-${currentLog.actionType}`">{{ currentLog.actionType }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">操作描述</span>
            <span class="meta-value">{{ currentLog.action }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">IP地址</span>
            <span class="meta-value">{{ currentLog.ipAddress }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">设备信息</span>
            <span class="meta-value">{{ currentLog.deviceInfo || '无' }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">操作时间</span>
            <span class="meta-value">{{ currentLog.createdAt }}</span>
          </div>
        </div>

        <div class="diff-section">
          <div class="diff-title">变更数据</div>
          <div class="diff-grid">
            <div class="diff-col">
              <div class="diff-col-header">变更前</div>
              <pre class="json-box">{{ formatJson(currentLog.beforeValue) }}</pre>
            </div>
            <div class="diff-col">
              <div class="diff-col-header">变更后</div>
              <pre class="json-box">{{ formatJson(currentLog.afterValue) }}</pre>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import api from '../../api'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const currentLog = ref(null)

const formatJson = (str) => {
  if (!str || str === '{}') return '无'
  try { return JSON.stringify(JSON.parse(str), null, 2) } catch { return str }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await api.getAuditLogs({})
    if (res.code === 200) tableData.value = res.data || []
  } catch {} finally { loading.value = false }
}

const showDetail = (row) => { currentLog.value = row; dialogVisible.value = true }

onMounted(() => loadData())
</script>

<style scoped>
.logs-page { padding: 0; min-height: 100%; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.page-title { font-size: 22px; font-weight: 700; color: #fff; margin: 0; }
.btn-refresh { background: rgba(255,255,255,0.08) !important; border: 1px solid rgba(255,255,255,0.15) !important; color: rgba(255,255,255,0.7) !important; border-radius: 10px !important; }

.glass-card {
  background: rgba(255,255,255,0.08); backdrop-filter: blur(14px);
  border: 1px solid rgba(255,255,255,0.15); border-radius: 18px; overflow: hidden;
}

.action-tag {
  display: inline-flex; align-items: center; padding: 2px 8px;
  border-radius: 6px; font-size: 12px; font-weight: 600;
  border: 1px solid transparent;
}
.action-SUBMIT { background: rgba(96,165,250,0.2); color: #60a5fa; border-color: rgba(96,165,250,0.3); }
.action-APPROVE { background: rgba(52,211,153,0.2); color: #34d399; border-color: rgba(52,211,153,0.3); }
.action-REJECT, action-DELETE { background: rgba(248,113,113,0.2); color: #f87171; border-color: rgba(248,113,113,0.3); }
.action-AUDIT { background: rgba(251,191,36,0.2); color: #fbbf24; border-color: rgba(251,191,36,0.3); }
.action-MODIFY { background: rgba(148,163,184,0.2); color: #94a3b8; border-color: rgba(148,163,184,0.3); }

/* 抽屉 */
:deep(.el-drawer) { background: rgba(15, 10, 40, 0.97) !important; backdrop-filter: blur(20px); border-left: 1px solid rgba(255,255,255,0.12); }
:deep(.el-drawer__header) { color: #fff !important; border-bottom: 1px solid rgba(255,255,255,0.1); padding: 20px 24px; margin-bottom: 0; }

.drawer-inner { padding: 24px; color: #fff; display: flex; flex-direction: column; gap: 24px; }

.log-meta { display: flex; flex-direction: column; gap: 14px; background: rgba(255,255,255,0.05); border-radius: 12px; padding: 16px; border: 1px solid rgba(255,255,255,0.1); }
.meta-row { display: flex; gap: 16px; align-items: flex-start; }
.meta-label { font-size: 12px; color: rgba(255,255,255,0.45); min-width: 80px; padding-top: 1px; }
.meta-value { font-size: 13px; color: #fff; line-height: 1.5; }

.diff-section { display: flex; flex-direction: column; gap: 12px; }
.diff-title { font-size: 13px; color: rgba(255,255,255,0.5); text-transform: uppercase; letter-spacing: 1px; }
.diff-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.diff-col-header { font-size: 12px; color: rgba(255,255,255,0.5); margin-bottom: 8px; }
.json-box {
  background: rgba(0,0,0,0.3); padding: 12px; border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.1);
  font-size: 12px; color: rgba(255,255,255,0.8);
  max-height: 260px; overflow: auto; line-height: 1.6;
}
</style>
