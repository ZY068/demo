<template>
  <div class="invoices-page">
    <div class="page-header">
      <h2 class="page-title">发票 OCR 识别</h2>
    </div>

    <div class="invoice-layout">
      <!-- 左侧：上传区 -->
      <div class="upload-panel">
        <div class="panel-header">
          <div class="panel-icon"><el-icon><Upload /></el-icon></div>
          <span>上传发票</span>
        </div>

        <div class="upload-area" :class="{ 'drag-over': isDragging }" @dragover.prevent="isDragging = true" @dragleave="isDragging = false" @drop.prevent="handleDrop" @click="triggerUpload">
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <p class="upload-text">点击或拖拽图片到此处上传</p>
          <p class="upload-hint">支持 JPG、PNG、PDF 格式</p>
          <input type="file" ref="fileInput" @change="handleFileChange" accept="image/*,.pdf" style="display:none" />
        </div>

        <div v-if="previewUrl" class="preview-area">
          <img :src="previewUrl" alt="预览" class="preview-img" />
          <el-button size="small" text @click="clearResult" style="color: rgba(255,255,255,0.6)">清除</el-button>
        </div>

        <div v-if="loading" class="loading-state">
          <el-icon class="is-loading" :size="40"><Loading /></el-icon>
          <p>正在识别发票...</p>
        </div>

        <!-- 识别结果 -->
        <div v-if="result && !loading" class="result-card">
          <div class="result-header">
            <el-icon style="color: #34d399"><CircleCheck /></el-icon>
            <span>识别成功</span>
          </div>
          <div class="result-fields">
            <div class="field-row" v-for="field in resultFields" :key="field.label">
              <span class="field-label">{{ field.label }}</span>
              <span class="field-value">{{ field.value }}</span>
            </div>
          </div>
          <div class="confidence">
            <span class="conf-label">识别置信度</span>
            <el-progress :percentage="Math.round(result.confidence * 100)" :color="getConfidenceColor(result.confidence)" :stroke-width="6" />
          </div>
          <div class="result-actions">
            <el-button type="primary" size="small" @click="useResult">使用此发票</el-button>
            <el-button size="small" @click="clearResult">重新识别</el-button>
          </div>
        </div>
      </div>

      <!-- 右侧：历史记录 -->
      <div class="history-panel">
        <div class="panel-header">
          <div class="panel-icon"><el-icon><Document /></el-icon></div>
          <span>历史发票</span>
        </div>

        <div class="history-list" v-if="invoiceList.length > 0">
          <div v-for="inv in invoiceList" :key="inv.id" class="history-item">
            <div class="inv-info">
              <div class="inv-no">{{ inv.invoiceNumber }}</div>
              <div class="inv-meta">{{ inv.invoiceType }} · {{ inv.issueDate }}</div>
            </div>
            <span class="ocr-status" :class="`status-${inv.ocrStatus}`">
              {{ ['', '待识别', '成功', '失败'][inv.ocrStatus] || '' }}
            </span>
          </div>
        </div>

        <div v-else class="empty-hint">
          <p>暂无历史发票记录</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { UploadFilled, Loading, CircleCheck, Upload, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const fileInput = ref()
const previewUrl = ref('')
const loading = ref(false)
const result = ref(null)
const invoiceList = ref([])
const isDragging = ref(false)

const resultFields = ref([])

const triggerUpload = () => fileInput.value.click()

const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (file) uploadFile(file)
}

const handleDrop = (e) => {
  isDragging.value = false
  const file = e.dataTransfer.files[0]
  if (file) uploadFile(file)
}

const uploadFile = async (file) => {
  previewUrl.value = URL.createObjectURL(file)
  loading.value = true
  result.value = null

  const formData = new FormData()
  formData.append('file', file)

  try {
    const res = await api.uploadInvoice(formData)
    if (res.code === 200 && res.data?.parsedData) {
      result.value = res.data.parsedData
      result.value.confidence = res.data.confidence || 0.95
      resultFields.value = [
        { label: '发票号码', value: result.value.invoiceNumber || '-' },
        { label: '发票代码', value: result.value.invoiceCode || '-' },
        { label: '发票类型', value: result.value.invoiceType || '-' },
        { label: '开票日期', value: result.value.issueDate || '-' },
        { label: '金额', value: result.value.amount ? `¥ ${result.value.amount}` : '-' },
        { label: '销售方', value: result.value.sellerName || '-' },
        { label: '购买方', value: result.value.buyerName || '-' },
      ]
      ElMessage.success('识别成功')
    } else { ElMessage.error('识别失败') }
  } catch { ElMessage.error('识别失败') } finally { loading.value = false }
}

const clearResult = () => {
  result.value = null
  previewUrl.value = ''
  resultFields.value = []
}

const useResult = () => ElMessage.success('已填充到报销单')

const getConfidenceColor = (c) => c >= 0.9 ? '#34d399' : c >= 0.7 ? '#fbbf24' : '#f87171'
</script>

<style scoped>
.invoices-page { padding: 0; min-height: 100%; }
.page-header { margin-bottom: 24px; }
.page-title { font-size: 22px; font-weight: 700; color: #fff; margin: 0; }

.invoice-layout { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }

/* 通用面板 */
.upload-panel, .history-panel {
  background: rgba(255,255,255,0.08);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: 20px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.panel-header {
  display: flex; align-items: center; gap: 10px;
  font-size: 15px; font-weight: 600; color: #fff;
}

.panel-icon {
  width: 34px; height: 34px; border-radius: 10px;
  background: rgba(96,165,250,0.2); border: 1px solid rgba(96,165,250,0.25);
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; color: #60a5fa;
}

/* 上传区 */
.upload-area {
  border: 2px dashed rgba(255,255,255,0.2);
  border-radius: 16px;
  padding: 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(255,255,255,0.04);
}

.upload-area:hover, .upload-area.drag-over {
  border-color: rgba(96,165,250,0.6);
  background: rgba(96,165,250,0.08);
  box-shadow: 0 0 20px rgba(96,165,250,0.15);
}

.upload-icon { font-size: 48px; color: rgba(255,255,255,0.3); margin-bottom: 12px; }
.upload-text { font-size: 15px; color: rgba(255,255,255,0.8); margin-bottom: 6px; }
.upload-hint { font-size: 12px; color: rgba(255,255,255,0.4); }

.preview-area { text-align: center; }
.preview-img { max-width: 100%; border-radius: 12px; box-shadow: 0 8px 24px rgba(0,0,0,0.3); margin-bottom: 8px; }

.loading-state { text-align: center; padding: 30px; color: rgba(255,255,255,0.5); }
.loading-state p { margin-top: 12px; font-size: 14px; }

/* 识别结果 */
.result-card {
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 14px;
  padding: 18px;
  display: flex; flex-direction: column; gap: 14px;
}

.result-header { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 600; color: #34d399; }
.result-fields { display: flex; flex-direction: column; gap: 8px; }
.field-row { display: flex; justify-content: space-between; gap: 12px; }
.field-label { font-size: 12px; color: rgba(255,255,255,0.45); flex-shrink: 0; }
.field-value { font-size: 13px; color: #fff; text-align: right; }

.confidence { display: flex; align-items: center; gap: 12px; }
.conf-label { font-size: 12px; color: rgba(255,255,255,0.5); white-space: nowrap; }

.result-actions { display: flex; gap: 10px; }

/* 历史记录 */
.history-list { display: flex; flex-direction: column; gap: 10px; }

.history-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 16px;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 12px;
  transition: all 0.2s;
}

.history-item:hover { background: rgba(255,255,255,0.09); }

.inv-no { font-size: 13px; font-family: monospace; color: rgba(255,255,255,0.8); }
.inv-meta { font-size: 11px; color: rgba(255,255,255,0.4); margin-top: 2px; }

.ocr-status {
  padding: 2px 8px; border-radius: 6px; font-size: 11px; font-weight: 600;
}
.status-1 { background: rgba(251,191,36,0.2); color: #fbbf24; }
.status-2 { background: rgba(52,211,153,0.2); color: #34d399; }
.status-3 { background: rgba(248,113,113,0.2); color: #f87171; }

.empty-hint { text-align: center; padding: 40px; color: rgba(255,255,255,0.35); font-size: 14px; }

@media (max-width: 768px) {
  .invoice-layout { grid-template-columns: 1fr; }
}
</style>
