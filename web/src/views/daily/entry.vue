<template>
  <div class="page-container">
    <div class="card">
      <div class="toolbar">
        <div class="header-section">
          <h2 class="page-title">日报录入</h2>
          <p class="page-desc">手动录入单条日报数据</p>
        </div>
      </div>

      <div class="form-container">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="120px"
          class="entry-form"
          @submit.prevent="handleSubmit"
        >
          <el-form-item label="所属项目/产品" prop="projectName">
            <el-select
              v-model="form.projectName"
              placeholder="请选择项目分类"
              filterable
              allow-create
              style="width: 100%"
            >
              <el-option
                v-for="opt in projectOptions"
                :key="opt"
                :label="opt"
                :value="opt"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="任务号" prop="taskNo">
            <el-input v-model="form.taskNo" placeholder="请输入任务编号" clearable />
          </el-form-item>

          <el-form-item label="任务名称" prop="taskName">
            <el-input v-model="form.taskName" placeholder="请输入任务标题" clearable />
          </el-form-item>

          <el-form-item label="工作日期" prop="workDate">
            <el-date-picker
              v-model="form.workDate"
              type="date"
              placeholder="选择工作日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="实际工时" prop="hours">
            <el-input-number
              v-model="form.hours"
              :min="0.5"
              :max="24"
              :step="0.5"
              :precision="1"
              placeholder="投入工时（小时）"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="工作描述" prop="workDescription" class="align-top">
            <el-input
              v-model="form.workDescription"
              type="textarea"
              :rows="3"
              placeholder="请输入工作描述"
              resize="vertical"
            />
          </el-form-item>

          <el-form-item class="form-actions">
            <el-button type="primary" @click="handleSubmit" :loading="submitting" class="btn-primary">
              {{ submitting ? '提交中...' : '提交' }}
            </el-button>
            <el-button @click="handleReset" class="btn-secondary">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <div class="card history-section">
      <div class="toolbar">
        <h3 class="section-title">最近录入</h3>
      </div>
      <el-table 
        :data="recentList" 
        class="modern-table" 
        size="medium" 
        max-height="400" 
        v-loading="loadingList"
        stripe
        border
      >
        <el-table-column prop="projectName" label="所属项目/产品" min-width="180" align="left" />
        <el-table-column prop="taskNo" label="任务号" width="140" align="left" />
        <el-table-column prop="taskName" label="任务名称" min-width="240" show-overflow-tooltip align="left" />
        <el-table-column prop="workDate" label="工作日期" width="140" align="left" />
        <el-table-column prop="hours" label="实际工时" width="120" align="left">
          <template #default="{ row }">
            <span class="mono">{{ row.hours }}h</span>
          </template>
        </el-table-column>
        <el-table-column prop="workDescription" label="工作描述" min-width="200" show-overflow-tooltip align="left">
          <template #default="{ row }">
            <span class="work-desc">{{ row.workDescription || '' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row)">
              编辑
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loadingList && recentList.length === 0" class="empty-state">
        <el-empty description="暂无录入记录" />
      </div>
    </div>

    <el-dialog
      v-model="editDialogVisible"
      title="编辑日报"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="rules"
        label-width="120px"
        class="entry-form"
      >
        <el-form-item label="所属项目/产品" prop="projectName">
          <el-select
            v-model="editForm.projectName"
            placeholder="请选择项目分类"
            filterable
            allow-create
            style="width: 100%"
          >
            <el-option
              v-for="opt in projectOptions"
              :key="opt"
              :label="opt"
              :value="opt"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="任务号" prop="taskNo">
          <el-input v-model="editForm.taskNo" placeholder="请输入任务编号" clearable />
        </el-form-item>

        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="editForm.taskName" placeholder="请输入任务标题" clearable />
        </el-form-item>

        <el-form-item label="工作日期" prop="workDate">
          <el-date-picker
            v-model="editForm.workDate"
            type="date"
            placeholder="选择工作日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="实际工时" prop="hours">
          <el-input-number
            v-model="editForm.hours"
            :min="0.5"
            :max="24"
            :step="0.5"
            :precision="1"
            placeholder="投入工时（小时）"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="工作描述" prop="workDescription" class="align-top">
          <el-input
            v-model="editForm.workDescription"
            type="textarea"
            :rows="3"
            placeholder="请输入工作描述"
            resize="vertical"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false" class="btn-secondary">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit" :loading="editSubmitting" class="btn-primary">
          {{ editSubmitting ? '保存中...' : '保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const editFormRef = ref<FormInstance>()
const submitting = ref(false)
const editSubmitting = ref(false)
const loadingList = ref(false)
const editDialogVisible = ref(false)
const editingId = ref<number | null>(null)
const recentList = ref<any[]>([])
const projectOptions = ref<string[]>(['日常需求管理', '日常运维管理'])

const form = reactive({
  projectName: '',
  taskNo: '',
  taskName: '',
  workDate: '',
  hours: 8,
  workDescription: '',
})

const editForm = reactive({
  projectName: '',
  taskNo: '',
  taskName: '',
  workDate: '',
  hours: 8,
  workDescription: '',
})

const rules = reactive<FormRules>({
  projectName: [
    { required: true, message: '请选择所属项目/产品', trigger: 'change' },
  ],
  taskName: [
    { required: true, message: '请输入任务名称', trigger: 'blur' },
  ],
  workDate: [
    { required: true, message: '请选择工作日期', trigger: 'change' },
  ],
  hours: [
    { required: true, message: '请输入实际工时', trigger: 'blur' },
  ],
  workDescription: [
    { required: true, message: '请输入工作描述', trigger: 'blur' },
  ],
})

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await request.post('/daily/submit', {
      projectName: form.projectName,
      taskNo: form.taskNo || null,
      taskName: form.taskName,
      workDate: form.workDate,
      hours: form.hours,
      workDescription: form.workDescription,
    })
    ElMessage.success('日报录入成功')
    form.taskNo = ''
    form.taskName = ''
    form.hours = 8
    form.workDescription = ''
    loadRecentList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '录入失败')
  } finally {
    submitting.value = false
  }
}

function handleReset() {
  formRef.value?.resetFields()
}

function openEditDialog(row: any) {
  editingId.value = row.id
  editForm.projectName = row.projectName || ''
  editForm.taskNo = row.taskNo || ''
  editForm.taskName = row.taskName || ''
  editForm.workDate = row.workDate || ''
  editForm.hours = row.hours || 8
  editForm.workDescription = row.workDescription || ''
  editDialogVisible.value = true
}

async function handleEditSubmit() {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid || editingId.value === null) return

  editSubmitting.value = true
  try {
    await request.put(`/daily/${editingId.value}`, {
      projectName: editForm.projectName,
      taskNo: editForm.taskNo || null,
      taskName: editForm.taskName,
      workDate: editForm.workDate,
      hours: editForm.hours,
      workDescription: editForm.workDescription,
    })
    ElMessage.success('日报更新成功')
    editDialogVisible.value = false
    loadRecentList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '更新失败')
  } finally {
    editSubmitting.value = false
  }
}

async function loadRecentList() {
  loadingList.value = true
  try {
    let userId = userStore.userInfo?.id
    // 如果userId为空，等待一段时间后重试
    if (!userId) {
      await new Promise(resolve => setTimeout(resolve, 500))
      userId = userStore.userInfo?.id
      if (!userId) return
    }
    const res: any = await request.get('/daily/list', {
      params: { userId, source: 'OTHER' },
    })
    recentList.value = (res.data || []).slice(0, 20)
  } catch (error) {
    console.error('加载最近录入失败:', error)
  } finally {
    loadingList.value = false
  }
}

async function loadProjects() {
  try {
    const res: any = await request.get('/project/list')
    console.log('项目列表响应:', res)
    const projects = res.data || []
    console.log('项目数据:', projects)
    const categories = [...new Set(projects.map((p: any) => p.category).filter(Boolean))] as string[]
    console.log('提取的分类:', categories)
    if (categories.length > 0) {
      projectOptions.value = categories
      console.log('更新后的选项:', projectOptions.value)
    }
  } catch (e: any) {
    console.error('加载项目分类失败:', e)
    console.error('错误详情:', e.response?.data)
  }
}

onMounted(() => {
  loadRecentList()
  loadProjects()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.page-container {
  padding: 24px;
  min-height: calc(100vh - 80px);
  background-color: $bg-primary;
}

.card {
  background: $bg-card;
  border-radius: 8px;
  border: 1px solid $border-color;
  margin-bottom: 24px;
  overflow: hidden;
}

.toolbar {
  padding: 24px;
  border-bottom: 1px solid $border-color;
  background: $bg-secondary;
}

.header-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: $text-primary;
  margin: 0;
  line-height: 28px;
}

.page-desc {
  color: $text-secondary;
  font-size: 13px;
  margin: 0;
  line-height: 20px;
}

.form-container {
  padding: 24px;
  display: flex;
  justify-content: center;
}

.entry-form {
  width: 100%;
  max-width: 600px;

  :deep(.el-form-item) {
    margin-bottom: 20px;
  }

  :deep(.el-form-item) {
    align-items: center;
  }

  :deep(.el-form-item.align-top) {
    align-items: flex-start;

    .el-form-item__label {
      line-height: 20px;
      height: auto;
      padding-top: 4px;
    }
  }

  :deep(.el-form-item__label) {
    color: $text-secondary;
    font-size: 14px;
    font-weight: 500;
    line-height: 36px;
    height: 36px;

    // Required asterisk styling
    &.is-required:not(.is-no-asterisk)::before {
      color: $color-danger;
      content: "*";
      margin-right: 4px;
      position: static;
    }
  }

  :deep(.el-form-item__content) {
    color: $text-primary;
    line-height: 36px;
  }

  :deep(.el-input__wrapper),
  :deep(.el-select__wrapper) {
    background: $bg-input;
    box-shadow: 0 0 0 1px $border-color inset;
    border-radius: 6px;
    transition: all $transition-fast;
    height: 36px;

    &:hover {
      box-shadow: 0 0 0 1px $border-color-light inset;
    }

    &.is-focus {
      box-shadow: 0 0 0 1px $brand-primary inset;
    }
  }

  :deep(.el-input__inner) {
    color: $text-primary;
    font-size: 14px;
    line-height: 20px;
    padding: 8px 12px;
    height: 36px;
  }

  :deep(.el-input-number) {
    .el-input__wrapper {
      background: $bg-input;
    }
    .el-input__inner {
      text-align: left;
    }
  }

  :deep(.el-date-editor) {
    .el-input__wrapper {
      background: $bg-input;
    }
  }

  :deep(.el-select__placeholder) {
    color: $text-muted;
  }

  :deep(.el-select) {
    .el-input__inner {
      color: $text-primary;
    }
    .el-select__selected-item {
      color: $text-primary;
      line-height: 20px;
    }
  }
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid $border-color;
}

.btn-primary {
  background-color: $brand-primary;
  border-color: $brand-primary;
  color: #fff;
  height: 36px;
  padding: 8px 24px;
  font-size: 14px;
  font-weight: 500;

  &:hover {
    background-color: $brand-primary-dark;
    border-color: $brand-primary-dark;
  }
}

.btn-secondary {
  background-color: $bg-secondary;
  border-color: $border-color;
  color: $text-secondary;
  height: 36px;
  padding: 8px 24px;
  font-size: 14px;
  font-weight: 500;

  &:hover {
    background-color: $bg-hover;
    border-color: $border-color-light;
    color: $text-primary;
  }
}

.history-section {
  .toolbar {
    border-bottom: none;
    padding-bottom: 16px;
  }

  :deep(.el-table) {
    margin: 0 24px 24px;
    max-width: 100%;
    border-radius: 6px;
    overflow: hidden;
  }
}

.section-title {
  color: $text-primary;
  font-size: 15px;
  font-weight: 600;
  margin: 0;
  line-height: 24px;
}

.modern-table {
  :deep(.el-table) {
    border-radius: 8px;
    overflow: hidden;
    border: 1px solid $border-color;
  }

  :deep(.el-table__header-wrapper) {
    background: $bg-secondary;
    border-bottom: 1px solid $border-color;

    th {
      background: $bg-secondary;
      color: $text-secondary;
      font-size: 14px;
      font-weight: 600;
      height: 48px;
      padding: 12px 16px;
      border-right: 1px solid $border-color;
      text-align: left;
      white-space: nowrap;

      &:last-child {
        border-right: none;
      }
    }
  }

  :deep(.el-table__body-wrapper) {
    td {
      background: $bg-card;
      color: $text-primary;
      font-size: 13px;
      height: 52px;
      padding: 16px;
      border-right: 1px solid $border-color;
      border-bottom: 1px solid $border-color;
      text-align: left;
      white-space: nowrap;

      &:last-child {
        border-right: none;
      }
    }

    tr {
      transition: background-color $transition-fast;

      &:hover {
        background: $bg-hover;
      }

      &:last-child {
        td {
          border-bottom: none;
        }
      }
    }
  }

  :deep(.el-table--striped) {
    .el-table__body-wrapper {
      tr:nth-child(2n) {
        background: $bg-secondary;

        &:hover {
          background: $bg-hover;
        }
      }
    }
  }

  :deep(.el-table__empty-block) {
    padding: 64px 24px;
  }

  :deep(.el-table__empty-text) {
    color: $text-muted;
    font-size: 14px;
  }
}

.empty-state {
  padding: 64px 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: $text-muted;
}

.mono {
  font-family: $font-mono;
  font-weight: 500;
}

.work-desc {
  white-space: pre-line;
  word-break: break-word;
  line-height: 1.5;
}
</style>
