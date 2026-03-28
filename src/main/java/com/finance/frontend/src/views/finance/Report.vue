<template>
  <div class="report-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>审计报告</span>
          <el-button type="primary" size="small">生成报告</el-button>
        </div>
      </template>

      <el-empty description="审计报告功能开发中" />
    </el-card>
  </div>
</template>

<script setup>
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>