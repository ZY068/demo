<template>
  <div class="invoices-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>发票OCR识别</span>
        </div>
      </template>

      <el-row :gutter="40">
        <el-col :span="12">
          <div class="upload-area" @dragover.prevent @drop.prevent="handleDrop" @click="triggerUpload">
            <el-icon class="upload-icon"><UploadFilled /></el-icon>
            <p class="upload-text">点击或拖拽图片到此处上传</p>
            <p class="upload-hint">支持 JPG、PNG、PDF 格式</p>
            <input type="file" ref="fileInput" @change="handleFileChange" accept="image/*,.pdf" style="display:none" />
          </div>
          <div v-if="previewUrl" class="preview-area">
            <img :src="previewUrl" alt="预览" class="preview-img" />
          </div>
        </el-col>
        <el-col :span="12">
          <div v-if="loading" class="loading-area">
            <el-icon class="is-loading" :size="40"><Loading /></el-icon>
            <p>正在识别发票...</p>
          </div>
          <div v-else-if="result" class="result-area">
            <el-descriptions title="识别结果" :column="2" border>
              <el-descriptions-item label="发票号码">{{ result.invoiceNumber }}</el-descriptions-item>
              <el-descriptions-item label="发票代码">{{ result.invoiceCode }}</el-descriptions-item>
              <el-descriptions-item label="发票类型">{{ result.invoiceType }}</el-descriptions-item>
              <el-descriptions-item label="开票日期">{{ result.issueDate }}</el-descriptions-item>
              <el-descriptions-item label="金额">{{ result.amount }} 元</el-descriptions-item>
              <el-descriptions-item label="税额">{{ result.taxAmount }} 元</el-descriptions-item>
              <el-descriptions-item label="价税合计" :span="2">{{ result.totalAmount }} 元</el-descriptions-item>
              <el-descriptions-item label="销售方" :span="2">{{ result.sellerName }}</el-descriptions-item>
              <el-descriptions-item label="购买方" :span="2">{{ result.buyerName }}</el-descriptions-item>
            </el-descriptions>
            <div class="confidence">
              <span>识别置信度：</span>
              <el-progress :percentage="Math.round(result.confidence * 100)" :color="getConfidenceColor(result.confidence)" />
            </div>
            <div class="result-actions">
              <el-button type="primary" @click="useResult">使用此发票</el-button>
              <el-button @click="clearResult">重新识别</el-button>
            </div>
          </div>
          <div v-else class="empty-area">
            <el-icon :size="60" color="#ccc"><Document /></el-icon>
            <p>上传发票图片自动识别</p>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>历史发票</span>
        </div>
      </template>
      <el-table :data="invoiceList" stripe style="width: 100%">
        <el-table-column prop="invoiceNumber" label="发票号码" width="180" />
        <el-table-column prop="invoiceCode" label="发票代码" width="160" />
        <el-table-column prop="invoiceType" label="类型" width="120" />
        <el-table-column prop="amount" label="金额" width="100">
          <template #default="{ row }">{{ row.amount || '0.00' }}</template>
        </el-table-column>
        <el-table-column prop="issueDate" label="开票日期" width="120" />
        <el-table-column prop="sellerName" label="销售方" min-width="150" show-overflow-tooltip />
        <el-table-column prop="ocrStatus" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getOcrStatusType(row.ocrStatus)">{{ getOcrStatusText(row.ocrStatus) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { UploadFilled, Loading, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const fileInput = ref()
const previewUrl = ref('')
const loading = ref(false)
const result = ref(null)
const invoiceList = ref([])

const triggerUpload = () => fileInput.value.click()

const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (file) uploadFile(file)
}

const handleDrop = (e) => {
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
      ElMessage.success('识别成功')
    } else {
      ElMessage.error('识别失败')
    }
  } catch (e) {
    ElMessage.error('识别失败')
  } finally {
    loading.value = false
  }
}

const clearResult = () => {
  result.value = null
  previewUrl.value = ''
}

const useResult = () => {
  ElMessage.success('已填充到报销单')
}

const getConfidenceColor = (confidence) => {
  if (confidence >= 0.9) return '#67c23a'
  if (confidence >= 0.7) return '#e6a23c'
  return '#f56c6c'
}

const getOcrStatusType = (status) => ['', 'info', 'success', 'danger'][status] || ''
const getOcrStatusText = (status) => ['', '待识别', '成功', '失败'][status] || ''
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.upload-area {
  border: 2px dashed #dcdfe6;
  border-radius: 12px;
  padding: 60px 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  background: #fafafa;
}
.upload-area:hover { border-color: #409eff; background: #ecf5ff; }
.upload-icon { font-size: 60px; color: #c0c4cc; }
.upload-text { font-size: 16px; color: #606266; margin-top: 16px; }
.upload-hint { font-size: 12px; color: #909399; margin-top: 8px; }
.preview-area { margin-top: 20px; }
.preview-img { max-width: 100%; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
.loading-area, .empty-area {
  padding: 60px;
  text-align: center;
  color: #909399;
}
.loading-area p, .empty-area p { margin-top: 16px; }
.result-area { padding: 20px 0; }
.confidence { margin-top: 20px; display: flex; align-items: center; gap: 12px; }
.result-actions { margin-top: 20px; }
</style>
