<template>
  <div class="audit-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>审计分析</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="报销趋势" name="trend">
          <el-form inline>
            <el-form-item label="时间范围">
              <el-date-picker v-model="trendQuery.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item label="粒度">
              <el-select v-model="trendQuery.granularity" style="width: 100px">
                <el-option label="日" value="DAY" />
                <el-option label="周" value="WEEK" />
                <el-option label="月" value="MONTH" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadTrend">查询</el-button>
            </el-form-item>
          </el-form>
          <div ref="trendChartRef" style="height: 350px; margin-top: 20px"></div>
        </el-tab-pane>

        <el-tab-pane label="异常统计" name="anomaly">
          <el-row :gutter="20">
            <el-col :span="12">
              <div ref="anomalyPieRef" style="height: 300px"></div>
            </el-col>
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header><span>异常概览</span></template>
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="异常单据总数">{{ anomalyData.totalAnomalies }}</el-descriptions-item>
                  <el-descriptions-item label="异常率">
                    <el-progress :percentage="anomalyData.anomalyRate || 0" :color="anomalyColor" />
                  </el-descriptions-item>
                </el-descriptions>
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="合规审计" name="compliance">
          <el-form inline>
            <el-form-item label="报销单ID">
              <el-input-number v-model="complianceQuery.claimId" :min="1" placeholder="输入ID" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadCompliance">审计</el-button>
            </el-form-item>
          </el-form>
          <div v-if="complianceResult" class="compliance-result">
            <el-alert :type="complianceResult.compliant ? 'success' : 'error'" :title="complianceResult.compliant ? '合规检查通过' : '发现违规项'" show-icon :closable="false" />
            <el-card style="margin-top: 20px" v-if="!complianceResult.compliant">
              <template #header><span>违规详情</span></template>
              <el-table :data="complianceResult.violations" stripe>
                <el-table-column prop="ruleName" label="规则名称" />
                <el-table-column prop="message" label="违规信息" />
                <el-table-column prop="severity" label="严重程度">
                  <template #default="{ row }">
                    <el-tag :type="row.severity === 'HIGH' ? 'danger' : 'warning'">{{ row.severity }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane label="流程审计" name="process">
          <el-form inline>
            <el-form-item label="报销单ID">
              <el-input-number v-model="processQuery.claimId" :min="1" placeholder="输入ID" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadProcess">审计</el-button>
            </el-form-item>
          </el-form>
          <div v-if="processResult" class="process-result">
            <el-result :icon="processResult.processComplete ? 'success' : 'warning'" :title="processResult.processComplete ? '流程完整' : '流程异常'">
              <template #extra>
                <el-descriptions :column="2" border style="margin-top: 20px">
                  <el-descriptions-item label="实际审批级数">{{ processResult.actualApprovers }}</el-descriptions-item>
                  <el-descriptions-item label="要求审批级数">{{ processResult.requiredApprovers }}</el-descriptions-item>
                  <el-descriptions-item label="审批链完整度" :span="2">
                    <el-progress :percentage="Math.round((processResult.actualApprovers / processResult.requiredApprovers) * 100)" />
                  </el-descriptions-item>
                </el-descriptions>
                <div v-if="processResult.processGaps?.length" style="margin-top: 20px; color: #f56c6c">
                  流程缺口: {{ processResult.processGaps.join(', ') }}
                </div>
              </template>
            </el-result>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import api from '../api'

const activeTab = ref('trend')
const trendChartRef = ref()
const anomalyPieRef = ref()

const trendQuery = reactive({
  dateRange: [],
  granularity: 'DAY'
})

const complianceQuery = reactive({ claimId: 1 })
const complianceResult = ref(null)

const processQuery = reactive({ claimId: 1 })
const processResult = ref(null)

const anomalyData = reactive({
  totalAnomalies: 0,
  anomalyRate: 0,
  distribution: {}
})

const anomalyColor = computed(() => {
  const rate = anomalyData.anomalyRate || 0
  if (rate < 5) return '#67c23a'
  if (rate < 10) return '#e6a23c'
  return '#f56c6c'
})

const loadTrend = async () => {
  try {
    const params = {}
    if (trendQuery.dateRange?.length === 2) {
      params.startDate = trendQuery.dateRange[0]
      params.endDate = trendQuery.dateRange[1]
    }
    params.granularity = trendQuery.granularity
    const res = await api.getExpenseTrend(params)
    if (res.code === 200 && res.data) {
      initTrendChart(res.data)
    }
  } catch {
    ElMessage.error('加载失败')
  }
}

const initTrendChart = (data) => {
  if (!trendChartRef.value) return
  const chart = echarts.init(trendChartRef.value)
  const xData = data.dataPoints?.map(d => d.period) || []
  const yData = data.dataPoints?.map(d => d.amount) || []
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['报销金额'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: xData, boundaryGap: false },
    yAxis: { type: 'value', name: '金额(元)' },
    series: [{
      name: '报销金额',
      type: 'bar',
      smooth: true,
      data: yData,
      itemStyle: { color: '#409eff' }
    }]
  })
}

const loadCompliance = async () => {
  try {
    const res = await api.auditCompliance(complianceQuery.claimId)
    if (res.code === 200) {
      complianceResult.value = res.data
    }
  } catch {
    ElMessage.error('加载失败')
  }
}

const loadProcess = async () => {
  try {
    const res = await api.auditProcess(processQuery.claimId)
    if (res.code === 200) {
      processResult.value = res.data
    }
  } catch {
    ElMessage.error('加载失败')
  }
}

const loadAnomalyData = async () => {
  try {
    const res = await api.getAnomalyStats({})
    if (res.code === 200 && res.data) {
      Object.assign(anomalyData, res.data)
      initAnomalyPie(res.data)
    }
  } catch {}
}

const initAnomalyPie = (data) => {
  if (!anomalyPieRef.value) return
  const chart = echarts.init(anomalyPieRef.value)
  const dist = data.anomalyTypeDistribution || {}
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      name: '异常类型',
      type: 'pie',
      radius: '60%',
      data: Object.entries(dist).map(([name, value]) => ({ name, value })),
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 }
    }]
  })
}

onMounted(() => {
  loadTrend()
  loadAnomalyData()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.compliance-result, .process-result { margin-top: 20px; }
</style>
