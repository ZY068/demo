<template>
  <div class="dashboard">
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <div class="stat-card blue">
          <div class="stat-icon"><el-icon><Document /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalClaims }}</div>
            <div class="stat-label">{{ userRole === 1 ? '本部门报销单' : '报销单总量' }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card orange">
          <div class="stat-icon"><el-icon><Clock /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.pendingClaims }}</div>
            <div class="stat-label">{{ userRole === 1 ? '待我审批' : '待审单据' }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card red" v-if="userRole === 2">
          <div class="stat-icon"><el-icon><Warning /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.riskAlerts }}</div>
            <div class="stat-label">风险预警</div>
          </div>
        </div>
        <div class="stat-card red" v-else>
          <div class="stat-icon"><el-icon><Close /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.rejectedClaims }}</div>
            <div class="stat-label">已驳回</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card green">
          <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.approvedAmount }}</div>
            <div class="stat-label">已入账金额(元)</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>报销趋势（近7天）</span>
            </div>
          </template>
          <div ref="trendChartRef" style="height: 280px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="chart-card" v-if="userRole === 2">
          <template #header><span>风险预警分布</span></template>
          <div ref="riskChartRef" style="height: 280px"></div>
        </el-card>
        <el-card class="chart-card" v-else>
          <template #header><span>报销状态分布</span></template>
          <div ref="statusChartRef" style="height: 280px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>{{ userRole === 1 ? '本部门待审批报销' : '待审计报销' }}</span>
              <el-tag type="warning">{{ pendingList.length }} 条</el-tag>
            </div>
          </template>
          <el-table :data="pendingList" style="width: 100%" v-loading="loading">
            <el-table-column prop="claimNo" label="单号" width="160" />
            <el-table-column prop="title" label="报销事由" min-width="180" />
            <el-table-column label="金额(元)" width="100">
              <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
            </el-table-column>
            <el-table-column prop="expenseDate" label="消费日期" width="110" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && pendingList.length === 0" description="暂无待处理报销单" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>{{ userRole === 1 ? '本部门近期报销' : '近期已入账报销' }}</span>
              <el-button type="primary" size="small" link @click="$router.push('/claims')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="recentClaims" style="width: 100%">
            <el-table-column prop="claimNo" label="单号" width="160" />
            <el-table-column prop="title" label="事由" min-width="180" />
            <el-table-column label="金额(元)" width="100">
              <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { Document, Clock, Warning, CircleCheck, Close } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import api from '../api'

const loading = ref(false)
const pendingList = ref([])
const recentClaims = ref([])
const trendChartRef = ref()
const riskChartRef = ref()
const statusChartRef = ref()

const stats = reactive({
  totalClaims: 0,
  pendingClaims: 0,
  rejectedClaims: 0,
  riskAlerts: 0,
  approvedAmount: '0'
})

// 获取当前用户角色
const userRole = computed(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const user = JSON.parse(userStr)
    return user.role
  }
  return null
})

const formatAmount = (amount) => {
  if (!amount) return '0.00'
  return (amount / 100).toFixed(2)
}

// 状态映射
const getStatusType = (status) => {
  const types = { 0: 'info', 1: 'warning', 2: 'primary', 3: 'danger', 4: 'success', 5: 'danger' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { 0: '草稿', 1: '待经理审批', 2: '待财务审计', 3: '经理驳回', 4: '已入账', 5: '财务驳回' }
  return texts[status] || '未知'
}

// 初始化趋势图表
const initTrendChart = (data) => {
  nextTick(() => {
    if (!trendChartRef.value) return
    const chart = echarts.init(trendChartRef.value)

    const xData = data.map(d => d.date)
    const yData = data.map(d => d.amount)

    chart.setOption({
      tooltip: { trigger: 'axis', formatter: '{b}<br/>金额: {c} 元' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: xData, boundaryGap: false },
      yAxis: { type: 'value', name: '金额(元)' },
      series: [{
        name: '报销金额',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.3 },
        data: yData,
        itemStyle: { color: '#409eff' }
      }]
    })
  })
}

// 初始化风险图表（财务用）
const initRiskChart = (data) => {
  nextTick(() => {
    if (!riskChartRef.value) return
    const chart = echarts.init(riskChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        name: '风险分布',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {c}' },
        data: data
      }]
    })
  })
}

// 初始化状态分布图表（经理用）
const initStatusChart = (data) => {
  nextTick(() => {
    if (!statusChartRef.value) return
    const chart = echarts.init(statusChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        name: '状态分布',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {c}' },
        data: data
      }]
    })
  })
}

onMounted(async () => {
  if (userRole.value === 1) {
    await loadManagerData()
  } else if (userRole.value === 2) {
    await loadFinanceData()
  }
})

// 部门经理数据加载
const loadManagerData = async () => {
  loading.value = true
  try {
    const res = await api.getClaims()
    if (res.code === 200 && res.data) {
      const allClaims = res.data

      // 待审批列表（状态=1）
      pendingList.value = allClaims.filter(c => c.status === 1)

      // 近期报销（本部门所有，按时间倒序）
      recentClaims.value = allClaims
        .filter(c => c.status !== 0)
        .slice(0, 10)

      // 统计数据
      stats.totalClaims = allClaims.length
      stats.pendingClaims = pendingList.value.length
      stats.rejectedClaims = allClaims.filter(c => c.status === 3).length

      // 计算已入账金额
      const approvedTotal = allClaims
        .filter(c => c.status === 4)
        .reduce((sum, c) => sum + (c.amount || 0), 0)
      stats.approvedAmount = formatAmount(approvedTotal)

      // 趋势图数据（近7天）
      const trendData = getTrendData(allClaims)
      initTrendChart(trendData)

      // 状态分布图
      const statusData = [
        { value: allClaims.filter(c => c.status === 1).length, name: '待审批', itemStyle: { color: '#e6a23c' } },
        { value: allClaims.filter(c => c.status === 2).length, name: '待财务审计', itemStyle: { color: '#409eff' } },
        { value: allClaims.filter(c => c.status === 3).length, name: '已驳回', itemStyle: { color: '#f56c6c' } },
        { value: allClaims.filter(c => c.status === 4).length, name: '已入账', itemStyle: { color: '#67c23a' } }
      ]
      initStatusChart(statusData)
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 财务数据加载
const loadFinanceData = async () => {
  loading.value = true
  try {
    const res = await api.getClaims()
    if (res.code === 200 && res.data) {
      const allClaims = res.data

      // 待审计列表（状态=2）
      pendingList.value = allClaims.filter(c => c.status === 2)

      // 近期已入账
      recentClaims.value = allClaims
        .filter(c => c.status === 4)
        .slice(0, 10)

      // 统计数据
      stats.totalClaims = allClaims.length
      stats.pendingClaims = allClaims.filter(c => c.status === 2).length

      // 已入账金额
      const approvedTotal = allClaims
        .filter(c => c.status === 4)
        .reduce((sum, c) => sum + (c.amount || 0), 0)
      stats.approvedAmount = formatAmount(approvedTotal)

      // 趋势图数据
      const trendData = getTrendData(allClaims.filter(c => c.status >= 2))
      initTrendChart(trendData)

      // 风险预警
      try {
        const alerts = await api.getRiskAlerts()
        if (alerts.code === 200 && alerts.data) {
          const pendingAlerts = alerts.data.filter(a => a.handlingStatus !== 2)
          stats.riskAlerts = pendingAlerts.length

          // 风险分布图
          const riskData = [
            { value: pendingAlerts.filter(a => a.riskLevel === 4).length, name: '严重', itemStyle: { color: '#f56c6c' } },
            { value: pendingAlerts.filter(a => a.riskLevel === 3).length, name: '高', itemStyle: { color: '#e6a23c' } },
            { value: pendingAlerts.filter(a => a.riskLevel === 2).length, name: '中', itemStyle: { color: '#409eff' } },
            { value: pendingAlerts.filter(a => a.riskLevel === 1).length, name: '低', itemStyle: { color: '#67c23a' } }
          ]
          initRiskChart(riskData)
        }
      } catch (e) {
        console.error(e)
        initRiskChart([])
      }
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 获取近7天趋势数据
const getTrendData = (claims) => {
  const result = []
  const today = new Date()

  for (let i = 6; i >= 0; i--) {
    const date = new Date(today)
    date.setDate(date.getDate() - i)
    const dateStr = date.toISOString().split('T')[0]
    const dateLabel = `${date.getMonth() + 1}/${date.getDate()}`

    const dayTotal = claims
      .filter(c => c.expenseDate === dateStr || (c.createdAt && c.createdAt.startsWith(dateStr)))
      .reduce((sum, c) => sum + (c.amount || 0) / 100, 0)

    result.push({ date: dateLabel, amount: dayTotal.toFixed(2) })
  }

  return result
}
</script>

<style scoped>
.dashboard { padding: 0; }
.stat-row { margin-bottom: 20px; }
.stat-card {
  padding: 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  color: white;
}
.stat-card.blue { background: linear-gradient(135deg, #667eea, #764ba2); }
.stat-card.orange { background: linear-gradient(135deg, #f093fb, #f5576c); }
.stat-card.red { background: linear-gradient(135deg, #fa709a, #fee140); }
.stat-card.green { background: linear-gradient(135deg, #43e97b, #38f9d7); }
.stat-icon { font-size: 48px; opacity: 0.8; margin-right: 16px; }
.stat-value { font-size: 28px; font-weight: bold; }
.stat-label { font-size: 14px; opacity: 0.9; margin-top: 4px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.chart-card { height: 100%; }
</style>