<template>
  <div class="page-container">
    <div class="card">
      <div class="toolbar">
        <div>
          <h2 class="page-title">团队周报详情</h2>
          <p class="page-desc">查看当前登录人及其下属的周报数据</p>
        </div>
        <div class="actions">
          <el-button type="primary" @click="handleExport">
            <el-icon><Download /></el-icon>
            导出详情
          </el-button>
        </div>
      </div>
      
      <div class="pagination-container">
        <el-button @click="prevWeek" :disabled="!hasPrevWeek || weekLoading">
          <el-icon><ArrowLeft /></el-icon>
          上一周
        </el-button>
        <span class="week-range">{{ currentWeekRange }}</span>
        <el-button @click="nextWeek" :disabled="!hasNextWeek || weekLoading">
          下一周
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
      
      <el-table 
        ref="tableRef"
        :data="teamWeeklyData" 
        v-loading="teamWeeklyLoading" 
        class="modern-table"
        size="medium"
        stripe
        border
        :cell-class-name="cellClassName"
        :row-style="{ height: 'auto' }"
      >
        <el-table-column prop="leader" label="负责人" min-width="100" align="left" />
        <el-table-column prop="member" label="组员" min-width="100" align="left" />
        <el-table-column prop="thisWeekTasks" label="本周事项" min-width="200" align="left" class-name="input-column">
          <template #default="{ row }">
            <el-input
              v-if="canEditThisWeek(row)"
              v-model="row.thisWeekTasks"
              type="textarea"
              :autosize="autosizeConfig"
              placeholder="请输入本周事项"
              @change="handleCellChange(row, 'thisWeekTasks')"
              size="small"
              class="dark-input"
            />
            <div v-else class="readonly-text">{{ row.thisWeekTasks }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="regularHours" label="常规工时（h）" min-width="110" align="center">
          <template #default="{ row }">
            <span class="mono">{{ row.regularHours || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="projectHours" label="项目工时（h）" min-width="110" align="center">
          <template #default="{ row }">
            <span class="mono">{{ row.projectHours || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalHours" label="总工时（h）" min-width="100" align="center">
          <template #default="{ row }">
            <span class="mono">{{ row.totalHours || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="nextWeekPlans" :label="nextWeekLabel" min-width="200" align="left" class-name="input-column">
          <template #default="{ row }">
            <el-input
              v-if="canEditNextWeek(row)"
              v-model="row.nextWeekPlans"
              type="textarea"
              :autosize="autosizeConfig"
              placeholder="请输入下周预计事项"
              @change="handleCellChange(row, 'nextWeekPlans')"
              size="small"
              class="dark-input"
            />
            <div v-else class="readonly-text">{{ row.nextWeekPlans }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="notes" label="补充说明" min-width="200" align="left" class-name="input-column">
          <template #default="{ row }">
            <el-input
              v-if="canEditNotes(row)"
              v-model="row.notes"
              type="textarea"
              :autosize="autosizeConfig"
              placeholder="请输入补充说明"
              @change="handleCellChange(row, 'notes')"
              size="small"
              class="dark-input"
            />
            <div v-else class="readonly-text">{{ row.notes }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <template v-if="weekMode === 'current'">
              <el-button 
                v-if="!row.submitted"
                type="primary" 
                size="small" 
                @click="handleRowSubmit(row)"
                :disabled="!canEditNotes(row)"
                :loading="submittingIds.includes(row.id || row.member)"
                class="submit-btn"
              >
                提交
              </el-button>
              <span v-else class="submitted-tag">已提交</span>
            </template>
            <template v-else-if="weekMode === 'next'">
              <span class="view-only-tag">填写中</span>
            </template>
            <span v-else class="view-only-tag">仅查看</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { getCurrentWeekly, submitWeekly, submitWeeklyForUser, getTeamWeeklyDetails, exportTeamWeeklyDetails, getUserInfo, updateWeeklyDetails, updateCurrentWeeklyDetails, updateWeeklyDetailsByUserId } from '@/api'

const weekly = ref<any>({})
const teamWeeklyData = ref<any[]>([])
const teamWeeklyLoading = ref(false)
const userInfo = ref<any>({})
const submittingIds = ref<number[]>([])
const weekLoading = ref(false)
const tableRef = ref<any>(null)

const autosizeConfig = { minRows: 1, maxRows: 20 }

const currentDate = ref(new Date())

const currentUsername = computed(() => userInfo.value?.user?.name || '')

const formatDate = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 获取本周三（统计周期结束日）
// 规则：上周四～本周三，本周四归入下周
const getRealCurrentWeekRange = () => {
  const now = new Date()
  const dayOfWeek = now.getDay() // 0=周日, 1=周一, ..., 4=周四, ..., 6=周六
  
  let thisThursday = new Date(now)
  thisThursday.setDate(now.getDate() + (4 - dayOfWeek + 7) % 7)
  thisThursday.setHours(0, 0, 0, 0)
  
  let lastThursday = new Date(thisThursday)
  lastThursday.setDate(thisThursday.getDate() - 7)
  
  let thisWednesday = new Date(thisThursday)
  thisWednesday.setDate(thisThursday.getDate() - 1)
  
  return { lastThursday, thisWednesday, thisThursday }
}

// 获取当前查看周的日期范围（上周四～本周三）
const getWeekDates = () => {
  const date = new Date(currentDate.value)
  const dayOfWeek = date.getDay()
  
  let thisThursday = new Date(date)
  thisThursday.setDate(date.getDate() + (4 - dayOfWeek + 7) % 7)
  
  let lastThursday = new Date(thisThursday)
  lastThursday.setDate(thisThursday.getDate() - 7)
  
  let thisWednesday = new Date(thisThursday)
  thisWednesday.setDate(thisThursday.getDate() - 1)
  
  return {
    startDate: formatDate(lastThursday),
    endDate: formatDate(thisWednesday)
  }
}

// 判断当前查看的周属于哪种模式
// 'current' = 本周（上周四～本周三），可编辑可提交
// 'next' = 本周四及以后（属于下周），只允许填写下周预计事项
// 'history' = 历史周，仅查看
const weekMode = computed(() => {
  const { lastThursday, thisWednesday, thisThursday } = getRealCurrentWeekRange()
  const viewRange = getWeekDates()
  const viewStart = new Date(viewRange.startDate)
  viewStart.setHours(0, 0, 0, 0)
  const viewEnd = new Date(viewRange.endDate)
  viewEnd.setHours(0, 0, 0, 0)
  
  const currentStart = new Date(formatDate(lastThursday))
  currentStart.setHours(0, 0, 0, 0)
  const currentEnd = new Date(formatDate(thisWednesday))
  currentEnd.setHours(0, 0, 0, 0)
  
  if (viewStart.getTime() === currentStart.getTime() && viewEnd.getTime() === currentEnd.getTime()) {
    return 'current'
  }
  
  if (viewStart.getTime() > currentStart.getTime()) {
    return 'next'
  }
  
  return 'history'
})

// 下周预计事项列标题：显示对应周期
const nextWeekLabel = computed(() => {
  const { startDate } = getWeekDates()
  const viewStart = new Date(startDate)
  const nextThursday = new Date(viewStart)
  nextThursday.setDate(viewStart.getDate() + 7)
  const nextWednesday = new Date(nextThursday)
  nextWednesday.setDate(nextThursday.getDate() + 6)
  return `下周预计事项（${formatDate(nextThursday)}～${formatDate(nextWednesday)}）`
})

const currentWeekRange = computed(() => {
  const { startDate, endDate } = getWeekDates()
  return `${startDate} ~ ${endDate}`
})

const hasPrevWeek = computed(() => {
  return true
})

const hasNextWeek = computed(() => {
  const { thisThursday } = getRealCurrentWeekRange()
  const viewRange = getWeekDates()
  const viewEnd = new Date(viewRange.endDate)
  viewEnd.setHours(0, 0, 0, 0)
  return viewEnd < thisThursday
})

function getUniqueTasks(tasks: string): string[] {
  if (!tasks) return []
  
  const seen = new Set<string>()
  const result: string[] = []
  
  tasks.split(/[\n\r,，;；]+/)
    .map(task => task.trim())
    .filter(task => task)
    .forEach(task => {
      if (!seen.has(task)) {
        seen.add(task)
        result.push(task)
      }
    })
  
  return result
}

function prevWeek() {
  weekLoading.value = true
  const newDate = new Date(currentDate.value)
  newDate.setDate(newDate.getDate() - 7)
  currentDate.value = newDate
  fetchTeamWeeklyDetails()
}

function nextWeek() {
  weekLoading.value = true
  const newDate = new Date(currentDate.value)
  newDate.setDate(newDate.getDate() + 7)
  currentDate.value = newDate
  fetchTeamWeeklyDetails()
}

async function fetchData() {
  try {
    const res: any = await getCurrentWeekly()
    weekly.value = res.data || {}
  } catch {}
}

async function fetchUserInfo() {
  try {
    const res: any = await getUserInfo()
    userInfo.value = res.data || {}
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

// 本周：可编辑下周预计事项
// 下周（本周四及以后）：可编辑下周预计事项
// 历史周：不可编辑
function canEditNextWeek(row: any): boolean {
  if (row.submitted) return false
  return row.leader === currentUsername.value && weekMode.value !== 'history'
}

function canEditThisWeek(row: any): boolean {
  if (row.submitted) return false
  return row.leader === currentUsername.value && weekMode.value === 'current'
}

// 补充说明：仅本周可编辑
function canEditNotes(row: any): boolean {
  if (row.submitted) return false
  return row.leader === currentUsername.value && weekMode.value === 'current'
}

function cellClassName({ column }: any): string {
  if (column && (column.property === 'thisWeekTasks' || column.property === 'nextWeekPlans' || column.property === 'notes')) {
    return 'input-cell'
  }
  return ''
}

async function fetchTeamWeeklyDetails() {
  teamWeeklyLoading.value = true
  try {
    const { startDate, endDate } = getWeekDates()
    const res: any = await getTeamWeeklyDetails(startDate, endDate)
    const data = res.data || []
    
    teamWeeklyData.value = data.map((item: any) => ({
      ...item,
      submitted: item.status === 'SUBMITTED'
    }))
  } catch (error) {
    console.error('获取团队周报详情失败:', error)
    ElMessage.error('获取团队周报详情失败')
  } finally {
    teamWeeklyLoading.value = false
    weekLoading.value = false
  }
  
  await nextTick()
  resizeAllTextarea()
}

function resizeAllTextarea() {
  nextTick(() => {
    const textareas = document.querySelectorAll('.modern-table .el-textarea__inner')
    textareas.forEach((textarea: any) => {
      if (textarea && textarea.style) {
        textarea.style.height = 'auto'
        textarea.style.height = textarea.scrollHeight + 'px'
      }
    })
  })
}

async function handleCellChange(row: any, field: string) {
  const rowKey = row.id || row.member
  if (submittingIds.value.includes(rowKey)) {
    return
  }
  try {
    if (!row.userId) {
      throw new Error('成员ID不存在')
    }
    await updateWeeklyDetailsByUserId(row.userId, field, row[field])
    ElMessage.success('保存成功')
    fetchTeamWeeklyDetails()
  } catch (error: any) {
    console.error('保存失败:', error)
    const msg = error?.message || '保存失败，请重试'
    ElMessage.error(msg)
  }
}

async function handleRowSubmit(row: any) {
  const rowKey = row.id || row.member
  try {
    submittingIds.value.push(rowKey)
    
    const submitData = {
      nextWeekPlans: row.nextWeekPlans || '',
      notes: row.notes || ''
    }
    
    const currentUserId = userInfo.value?.user?.id
    if (row.userId && row.userId !== currentUserId) {
      await submitWeeklyForUser(row.userId, submitData)
    } else {
      await submitWeekly(submitData)
    }
    
    ElMessage.success('提交成功')
    
    await fetchTeamWeeklyDetails()
  } catch (error: any) {
    console.error('提交失败:', error)
    const msg = error?.message || '提交失败，请重试'
    ElMessage.error(msg)
  } finally {
    submittingIds.value = submittingIds.value.filter(id => id !== rowKey)
  }
}

async function handleExport() {
  try {
    const { startDate, endDate } = getWeekDates()
    const res = await exportTeamWeeklyDetails(startDate, endDate)
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `团队周报详情_${startDate}~${endDate}.xlsx`
    document.body.appendChild(link)
    link.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(link)
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请重试')
  }
}

onMounted(async () => {
  await fetchUserInfo()
  fetchData()
  fetchTeamWeeklyDetails()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.page-container {
  padding: 24px;
  min-height: calc(1vh - 80px);
  background-color: $bg-primary;
}

.card {
  background: $bg-card;
  border-radius: 8px;
  border: 1px solid $border-color;
  overflow: hidden;
}

.toolbar {
  padding: 24px;
  border-bottom: 1px solid $border-color;
  background: $bg-secondary;
  display: flex;
  justify-content: space-between;
  align-items: center;
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
  margin: 4px 0 0;
  line-height: 20px;
}

.actions {
  display: flex;
  gap: 12px;
}

.pagination-container {
  display: flex;
  align-items: center;
  gap: 16px;
  margin: 16px 24px;
  padding: 12px 16px;
  background: $bg-secondary;
  border-radius: $radius-sm;
  border: 1px solid $border-color;
}

.week-range {
  font-size: 14px;
  font-weight: 500;
  color: $text-primary;
  min-width: 200px;
  text-align: center;
}

.task-list {
  line-height: 1.6;
  white-space: normal;
  word-break: break-word;
}

.task-item {
  margin-bottom: 4px;
  color: $text-primary;
  &:last-child {
    margin-bottom: 0;
  }
}

.mono {
  font-family: $font-mono;
  font-weight: 500;
}

.modern-table {
  margin: 0 24px 24px;
  border-radius: 6px;
  overflow: hidden;

  :deep(.el-table) {
    border-radius: 8px;
    overflow: hidden;
    border: 1px solid $border-color;
    background: $bg-card;
    table-layout: auto;
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
      padding: 12px 16px;
      border-right: 1px solid $border-color;
      border-bottom: 1px solid $border-color;
      text-align: left;
      vertical-align: top;

      &:last-child {
        border-right: none;
      }
    }

    .input-cell {
      padding: 4px 0 !important;
      vertical-align: top;

      .cell {
        padding: 0 !important;
      }
    }

    tr {
      transition: background-color $transition-fast;

      &:hover {
        td {
          background: $bg-hover !important;
        }
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
        td {
          background: $bg-secondary;
        }

        &:hover {
          td {
            background: $bg-hover !important;
          }
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

.dark-input {
  :deep(.el-textarea__inner) {
    background: $bg-input;
    border: 1px solid $border-color;
    color: $text-primary;
    border-radius: 6px;
    padding: 8px 12px;
    line-height: 1.6;
    
    &:hover {
      border-color: $border-color-light;
    }
    
    &:focus {
      border-color: $brand-primary;
    }
    
    &::placeholder {
      color: $text-muted;
    }
  }
}

.readonly-text {
  color: $text-primary;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 8px 12px;
}

.view-only-tag {
  color: $text-muted;
  font-size: 12px;
}

.submitted-tag {
  color: $brand-primary;
  font-size: 12px;
  font-weight: 500;
}

.submit-btn {
  background-color: $brand-primary;
  border-color: $brand-primary;
  color: #0a0b0d;
  font-weight: 500;
  
  &:hover {
    background-color: $brand-primary-dark;
    border-color: $brand-primary-dark;
  }
  
  &:disabled {
    background-color: $bg-secondary;
    border-color: $border-color;
    color: $text-muted;
  }
}

:deep(.el-button) {
  background-color: $bg-secondary;
  border-color: $border-color;
  color: $text-secondary;
  
  &:hover {
    background-color: $bg-hover;
    border-color: $border-color-light;
    color: $text-primary;
  }
  
  &.el-button--primary {
    background-color: $brand-primary;
    border-color: $brand-primary;
    color: #0a0b0d;
    
    &:hover {
      background-color: $brand-primary-dark;
      border-color: $brand-primary-dark;
    }
  }
}
</style>