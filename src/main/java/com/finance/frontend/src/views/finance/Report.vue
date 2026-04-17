<template>
  <div class="report-page">
    <div class="page-header">
      <h2 class="page-title">审计报告</h2>
    </div>

    <div class="coming-soon-card">
      <div class="orb-bg orb-1"></div>
      <div class="orb-bg orb-2"></div>
      <div class="content">
        <div class="icon-wrap">
          <el-icon><DataAnalysis /></el-icon>
        </div>
        <h3>审计报告功能</h3>
        <p>正在构建中，即将上线</p>
        <div class="feature-preview">
          <div class="feature-item">
            <el-icon><TrendCharts /></el-icon>
            <span>月度汇总报表</span>
          </div>
          <div class="feature-item">
            <el-icon><DataLine /></el-icon>
            <span>部门费用分析</span>
          </div>
          <div class="feature-item">
            <el-icon><Warning /></el-icon>
            <span>风险趋势报告</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { DataAnalysis, DataLine, TrendCharts, Warning } from '@element-plus/icons-vue'
</script>

<style scoped>
.report-page { padding: 0; min-height: 100%; }
.page-header { margin-bottom: 24px; }
.page-title { font-size: 22px; font-weight: 700; color: #fff; margin: 0; }

.coming-soon-card {
  position: relative;
  overflow: hidden;
  background: rgba(255,255,255,0.08);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: 24px;
  padding: 80px 40px;
  text-align: center;
}

.orb-bg {
  position: absolute; border-radius: 50%; filter: blur(60px); pointer-events: none;
}
.orb-1 { width: 300px; height: 300px; background: radial-gradient(circle, rgba(102,126,234,0.4), transparent); top: -80px; left: -60px; }
.orb-2 { width: 200px; height: 200px; background: radial-gradient(circle, rgba(118,75,162,0.5), transparent); bottom: -60px; right: -40px; }

.content { position: relative; z-index: 2; }

.icon-wrap {
  width: 80px; height: 80px; border-radius: 24px;
  background: rgba(102,126,234,0.2); border: 1px solid rgba(102,126,234,0.3);
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 24px; font-size: 36px; color: #a5b4fc;
  box-shadow: 0 12px 40px rgba(102,126,234,0.25);
}

.content h3 { font-size: 24px; font-weight: 700; color: #fff; margin-bottom: 8px; }
.content p { font-size: 14px; color: rgba(255,255,255,0.5); margin-bottom: 32px; }

.feature-preview {
  display: flex; gap: 24px; justify-content: center; flex-wrap: wrap;
}

.feature-item {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 18px;
  background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.12);
  border-radius: 12px;
  font-size: 13px; color: rgba(255,255,255,0.7);
  transition: all 0.2s;
}

.feature-item:hover { background: rgba(255,255,255,0.1); border-color: rgba(102,126,234,0.3); }
.feature-item .el-icon { color: #60a5fa; }
</style>
