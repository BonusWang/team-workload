<template>
  <div class="page-container">
    <div class="card">
      <div class="toolbar">
        <div>
          <h2 class="page-title">日报导入</h2>
          <p class="page-desc">通过 Excel 模板批量导入日报数据</p>
        </div>
      </div>

      <div class="upload-section">

        <div class="upload-area">
          <el-upload
            ref="uploadRef"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :data="{ source: 'QUANKAI' }"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            drag
          >
            <div class="upload-inner">
              <el-icon :size="32" class="upload-icon"><Upload /></el-icon>
              <div class="upload-text">拖拽文件到此处，或 <span class="upload-link">点击上传</span></div>
              <div class="upload-hint">支持 .xlsx / .xls 格式，单次最多 5000 条</div>
            </div>
          </el-upload>
          <div class="upload-action">
            <el-button type="primary" @click="submitUpload" :loading="uploading">
              {{ uploading ? '导入中...' : '开始导入' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="importResult" class="card result-section">
      <h3 class="section-title">导入结果</h3>
      <div class="result-summary">
        <div class="result-item">
          <span class="result-label">总计</span>
          <span class="result-value mono">{{ importResult.totalCount }}</span>
        </div>
        <div class="result-item success">
          <span class="result-label">成功</span>
          <span class="result-value mono">{{ importResult.successCount }}</span>
        </div>
        <div class="result-item fail">
          <span class="result-label">失败</span>
          <span class="result-value mono">{{ importResult.failCount }}</span>
        </div>
        <div class="result-item duplicate">
          <span class="result-label">重复</span>
          <span class="result-value mono">{{ importResult.duplicateList?.length || 0 }}</span>
        </div>
      </div>

      <div v-if="importResult.failList && importResult.failList.length > 0" class="detail-section">
        <h4 class="detail-title">失败明细</h4>
        <el-table :data="importResult.failList" class="dark-table" size="small" max-height="300">
          <el-table-column prop="row" label="行号" width="70" />
          <el-table-column prop="submitterName" label="提交人" width="100" />
          <el-table-column prop="workDate" label="工作日期" width="120" />
          <el-table-column prop="taskNo" label="任务号" width="120" />
          <el-table-column prop="reason" label="失败原因" />
        </el-table>
      </div>

      <div v-if="importResult.duplicateList && importResult.duplicateList.length > 0" class="detail-section">
        <h4 class="detail-title">重复数据</h4>
        <el-table :data="importResult.duplicateList" class="dark-table" size="small" max-height="300">
          <el-table-column prop="row" label="行号" width="70" />
          <el-table-column prop="submitterName" label="提交人" width="100" />
          <el-table-column prop="workDate" label="工作日期" width="120" />
          <el-table-column prop="taskNo" label="任务号" width="120" />
          <el-table-column label="操作" width="100">
            <template #default>
              <el-tag type="warning" size="small" effect="dark">待确认</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div class="confirm-actions">
          <el-button type="warning" @click="handleConfirmOverwrite" :loading="confirmLoading">
            确认覆盖重复数据
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadInstance } from 'element-plus'
import request from '@/utils/request'

const uploadRef = ref<UploadInstance>()
const uploading = ref(false)
const confirmLoading = ref(false)
const importResult = ref<any>(null)

const uploadUrl = '/api/daily/import'
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

function beforeUpload(file: any) {
  const isExcel = file.name.endsWith('.xlsx') || file.name.endsWith('.xls')
  if (!isExcel) {
    ElMessage.error('仅支持 .xlsx / .xls 格式文件')
    return false
  }
  uploading.value = true
  return true
}

function submitUpload() {
  uploadRef.value?.submit()
}

function handleUploadSuccess(response: any) {
  uploading.value = false
  if (response.code === 200) {
    importResult.value = response.data
    ElMessage.success('导入完成')
  } else {
    ElMessage.error(response.message || '导入失败')
  }
}

function handleUploadError() {
  uploading.value = false
  ElMessage.error('导入失败，请检查文件格式')
}

async function handleConfirmOverwrite() {
  if (!importResult.value?.batchNo) return
  try {
    await ElMessageBox.confirm('确认覆盖重复数据？覆盖后原数据将被替换。', '提示', {
      type: 'warning',
      confirmButtonText: '确认覆盖',
      cancelButtonText: '取消',
    })
    confirmLoading.value = true
    const res: any = await request.post('/daily/import/confirm', null, {
      params: { batchNo: importResult.value.batchNo },
    })
    importResult.value = res.data
    ElMessage.success('覆盖成功')
  } catch {
  } finally {
    confirmLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.page-desc {
  color: $text-secondary;
  font-size: 13px;
  margin: 4px 0 0;
}

.upload-section {
  margin-top: 4px;
}

.template-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.section-label {
  color: $text-secondary;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}

.template-btns {
  display: flex;
  gap: 8px;
}

.upload-area {
  margin-top: 8px;
}

.upload-inner {
  padding: 40px 20px;
  text-align: center;
}

.upload-icon {
  color: $text-muted;
  margin-bottom: 12px;
}

.upload-text {
  color: $text-secondary;
  font-size: 14px;
  margin-bottom: 6px;
}

.upload-link {
  color: $brand-primary;
  cursor: pointer;
  font-weight: 500;
}

.upload-hint {
  color: $text-muted;
  font-size: 12px;
}

.upload-action {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

:deep(.el-upload-dragger) {
  background: $bg-secondary;
  border: 1px dashed $border-color-light;
  border-radius: $radius-md;
  transition: all $transition-fast;

  &:hover {
    border-color: $brand-primary;
    background: rgba(50, 240, 140, 0.03);
  }
}

.result-section {
  margin-top: 20px;
}

.result-summary {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  border-radius: $radius-sm;
  background: $bg-secondary;
  border: 1px solid $border-color;

  &.success .result-value { color: $color-success; }
  &.fail .result-value { color: $color-danger; }
  &.duplicate .result-value { color: $color-warning; }
}

.result-label {
  color: $text-secondary;
  font-size: 13px;
}

.result-value {
  font-family: $font-mono;
  font-size: 20px;
  font-weight: 700;
  color: $text-primary;
}

.detail-section {
  margin-top: 16px;
}

.detail-title {
  color: $text-primary;
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
}

.confirm-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
