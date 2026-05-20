<template>
  <div class="page-container">
    <!-- 待审批列表 -->
    <div class="card">
      <div class="toolbar">
        <div>
          <h2 class="page-title">待审批请假</h2>
          <p class="page-desc">审批团队成员的请假申请</p>
        </div>
      </div>

      <el-table :data="pendingLeaves" class="dark-table compact-table" border>
        <el-table-column prop="userName" label="申请人" min-width="90" />
        <el-table-column prop="leaveType" label="类型" min-width="80">
          <template #default="{ row }">{{ typeLabel(row.leaveType) }}</template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" min-width="100" />
        <el-table-column prop="startTime" label="开始时间" min-width="80">
          <template #default="{ row }">{{ timeLabel(row.startTime) }}</template>
        </el-table-column>
        <el-table-column prop="endDate" label="结束日期" min-width="100" />
        <el-table-column prop="endTime" label="结束时间" min-width="80">
          <template #default="{ row }">{{ timeLabel(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="days" label="天数" min-width="70">
          <template #default="{ row }"><span class="mono">{{ row.days }}</span></template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="120" />
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="success" size="small" @click="handleApprove(row.id)">
                通过
              </el-button>
              <el-button type="danger" size="small" @click="handleReject(row.id)">
                驳回
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 月度请假趋势 -->
    <div class="card">
      <div class="toolbar">
        <div>
          <h2 class="page-title">月度请假趋势</h2>
          <p class="page-desc">查看团队成员月度请假情况</p>
        </div>
        <el-select v-model="selectedMonth" placeholder="选择月份" style="width: 120px" @change="handleMonthChange">
          <el-option v-for="m in months" :key="m" :label="`${m}月`" :value="m" />
        </el-select>
      </div>

      <div class="chart-wrapper">
        <div id="monthlyLeaveChart" ref="monthlyLeaveChartRef" class="chart"></div>
      </div>
    </div>

    <!-- 历史审批记录 -->
    <div class="card">
      <div class="toolbar">
        <div>
          <h2 class="page-title">历史审批记录</h2>
          <p class="page-desc">查看所选月份的所有请假记录</p>
        </div>
        <div class="filters">
          <el-select v-model="filterUserNames" multiple placeholder="选择申请人" clearable style="width: 150px">
            <el-option v-for="user in uniqueUserNames" :key="user" :label="user" :value="user" />
          </el-select>
          <el-select v-model="filterLeaveTypes" multiple placeholder="选择请假类型" clearable style="width: 150px">
            <el-option v-for="type in leaveTypes" :key="type" :label="typeLabel(type)" :value="type" />
          </el-select>
          <el-select v-model="filterStatuses" multiple placeholder="选择状态" clearable style="width: 150px">
            <el-option v-for="status in statuses" :key="status" :label="statusLabel(status)" :value="status" />
          </el-select>
        </div>
      </div>

      <el-table :data="paginatedRecords" class="dark-table compact-table" border>
        <el-table-column prop="userName" label="申请人" min-width="90" />
        <el-table-column prop="leaveType" label="类型" min-width="80">
          <template #default="{ row }">{{ typeLabel(row.leaveType) }}</template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" min-width="100" />
        <el-table-column prop="startTime" label="开始时间" min-width="80">
          <template #default="{ row }">{{ timeLabel(row.startTime) }}</template>
        </el-table-column>
        <el-table-column prop="endDate" label="结束日期" min-width="100" />
        <el-table-column prop="endTime" label="结束时间" min-width="80">
          <template #default="{ row }">{{ timeLabel(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="days" label="天数" min-width="70">
          <template #default="{ row }"><span class="mono">{{ row.days }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="dark">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="120" />
        <el-table-column prop="rejectReason" label="驳回原因" min-width="120" />
        <el-table-column prop="approveTime" label="审批时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.approveTime) }}</template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="filteredRecords.length"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </div>

    <el-dialog v-model="rejectDialogVisible" title="驳回申请" width="440px" :close-on-click-modal="false">
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="80px">
        <el-form-item label="驳回原因" prop="reason">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="3" placeholder="请输入驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, FormInstance } from 'element-plus'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { getPendingLeaves, approveLeave, rejectLeave, getHistoryLeaves, getMonthlyLeaveStats } from '@/api'

const pendingLeaves = ref<any[]>([])
const rejectDialogVisible = ref(false)
const rejectForm = ref({ reason: '', leaveId: 0 })
const rejectFormRef = ref()
const allHistoryRecords = ref<any[]>([])
const monthFilteredRecords = ref<any[]>([])

const months = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]
const selectedMonth = ref(dayjs().month() + 1)
const currentYear = dayjs().year()

const leaveTypes = ['ANNUAL', 'SICK', 'PERSONAL', 'MARRIAGE', 'MATERNITY', 'OTHER']
const statuses = ['PENDING', 'APPROVED', 'REJECTED', 'REVOKED']

const rejectRules = {
  reason: [{ required: true, message: '请输入驳回原因', trigger: 'blur' }]
}

const filterUserNames = ref<string[]>([])
const filterLeaveTypes = ref<string[]>([])
const filterStatuses = ref<string[]>([])

const currentPage = ref(1)
const pageSize = ref(10)

const monthlyLeaveChartRef = ref<HTMLElement | null>(null)
let monthlyLeaveChart: echarts.ECharts | null = null

const uniqueUserNames = computed(() => {
  const names = new Set<string>()
  monthFilteredRecords.value.forEach(record => {
    if (record.userName) {
      names.add(record.userName)
    }
  })
  return Array.from(names).sort()
})

const filteredRecords = computed(() => {
  let records = [...monthFilteredRecords.value]

  if (filterUserNames.value.length > 0) {
    records = records.filter(record => filterUserNames.value.includes(record.userName))
  }

  if (filterLeaveTypes.value.length > 0) {
    records = records.filter(record => filterLeaveTypes.value.includes(record.leaveType))
  }

  if (filterStatuses.value.length > 0) {
    records = records.filter(record => filterStatuses.value.includes(record.status))
  }

  return records
})

const paginatedRecords = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredRecords.value.slice(start, end)
})

watch([filterUserNames, filterLeaveTypes, filterStatuses], () => {
  currentPage.value = 1
})

function typeLabel(t: string) {
  const map: any = { ANNUAL: '年假', SICK: '病假', PERSONAL: '事假', MARRIAGE: '婚假', MATERNITY: '产假', OTHER: '其他' }
  return map[t] || t
}

function timeLabel(t: string) {
  if (!t) return ''
  const hour = parseInt(t.substring(0, 2))
  const minute = parseInt(t.substring(3, 5))
  if (hour < 12 || (hour === 12 && minute === 0)) {
    return '上午'
  } else {
    return '下午'
  }
}

function statusLabel(s: string) {
  const map: any = { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回', REVOKED: '已撤回' }
  return map[s] || s
}

function statusType(s: string) {
  const map: any = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', REVOKED: 'info' }
  return map[s] || 'info'
}

function formatTime(t: string) {
  if (!t) return ''
  return dayjs(t).format('YYYY-MM-DD HH:mm:ss')
}

function filterByMonth(records: any[], year: number, month: number) {
  return records.filter((record: any) => {
    const startDate = dayjs(record.startDate)
    const endDate = dayjs(record.endDate)
    const targetMonthStart = dayjs(`${year}-${month}-01`)
    const targetMonthEnd = targetMonthStart.endOf('month')
    
    // 检查请假记录是否与目标月份有重叠
    return !(endDate.isBefore(targetMonthStart) || startDate.isAfter(targetMonthEnd))
  })
}

async function fetchPendingLeaves() {
  try {
    const res: any = await getPendingLeaves()
    pendingLeaves.value = res.data || []
  } catch (error) {
    console.error('获取待审批数据失败：', error)
    pendingLeaves.value = []
    ElMessage.error('获取待审批数据失败')
  }
}

async function fetchAllHistoryLeaves() {
  try {
    const res: any = await getHistoryLeaves({ page: 1, size: 1000 })
    allHistoryRecords.value = res.data.records || res.data.list || res.data || []
    filterAndRender()
  } catch (error) {
    console.error('获取历史记录失败：', error)
    allHistoryRecords.value = []
    filterAndRender()
  }
}

function filterAndRender() {
  monthFilteredRecords.value = filterByMonth(allHistoryRecords.value, currentYear, selectedMonth.value)
  currentPage.value = 1
  renderChart()
}

function handleMonthChange() {
  filterAndRender()
}

async function renderChart() {
  if (!monthlyLeaveChart) return

  try {
    // 调用后端接口获取月度统计数据（已处理跨月拆分）
    const res: any = await getMonthlyLeaveStats(currentYear, selectedMonth.value)
    const stats = res.data || []

    // 准备图表数据
    const users = stats.map((stat: any) => stat.employee)
    const daysData = stats.map((stat: any) => ({
      value: stat.days,
      itemStyle: {
        color: stat.days > 3 
          ? new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#f56c6c' },
              { offset: 1, color: '#e64a4a' }
            ])
          : new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#32f08c' },
              { offset: 1, color: '#20c06a' }
            ]),
        borderRadius: [4, 4, 0, 0]
      }
    }))

    const option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        backgroundColor: 'rgba(30, 30, 30, 0.95)',
        textStyle: { color: '#fff' },
        formatter: (params: any) => {
          const data = params[0]
          return `${data.name}: ${data.value}天`
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        top: '10%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: users,
        axisTick: { alignWithLabel: true },
        axisLabel: {
          color: '#e0e0e0',
          fontSize: 13,
          fontWeight: 500,
          interval: 0
        },
        axisLine: { lineStyle: { color: '#444' } }
      },
      yAxis: {
        type: 'value',
        name: '请假天数',
        nameTextStyle: { color: '#e0e0e0', fontSize: 12 },
        axisLabel: { color: '#e0e0e0', fontSize: 12 },
        axisLine: { lineStyle: { color: '#444' } },
        splitLine: { lineStyle: { color: '#333', type: 'dashed' } }
      },
      series: [
        {
          name: '已通过',
          data: daysData,
          type: 'bar',
          barWidth: '40%'
        }
      ]
    }

    monthlyLeaveChart.setOption(option)
  } catch (error) {
    console.error('获取月度请假统计失败:', error)
    ElMessage.error('获取月度请假统计失败')
  }
}

function handleResize() {
  monthlyLeaveChart?.resize()
}

async function refreshAll() {
  await Promise.all([
    fetchPendingLeaves(),
    fetchAllHistoryLeaves()
  ])
}

async function handleApprove(leaveId: number) {
  try {
    await approveLeave(leaveId)
    ElMessage.success('审批通过成功')
    await refreshAll()
  } catch {
    ElMessage.error('审批失败')
  }
}

function handleReject(leaveId: number) {
  rejectForm.value = { reason: '', leaveId }
  rejectDialogVisible.value = true
}

async function confirmReject() {
  if (!rejectFormRef.value) return

  try {
    await rejectFormRef.value.validate()
    await rejectLeave(rejectForm.value.leaveId, rejectForm.value.reason)
    ElMessage.success('驳回成功')
    rejectDialogVisible.value = false
    await refreshAll()
  } catch {
    ElMessage.error('驳回失败')
  }
}

onMounted(() => {
  fetchPendingLeaves()
  fetchAllHistoryLeaves()
  
  if (monthlyLeaveChartRef.value) {
    monthlyLeaveChart = echarts.init(monthlyLeaveChartRef.value)
  }
  
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  monthlyLeaveChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.page-desc {
  color: $text-secondary;
  font-size: 13px;
  margin: 4px 0 0;
}

.mono {
  font-family: 'JetBrains Mono', 'Monaco', 'Consolas', monospace;
}

.filters {
  display: flex;
  gap: 12px;
}

.chart-wrapper {
  margin-top: 20px;
}

.chart {
  height: 350px;
  width: 100%;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.compact-table {
  :deep(.el-table__cell) {
    padding: 8px 0;
  }

  :deep(.el-table__header th) {
    padding: 10px 0;
  }
}

.action-buttons {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
}


</style>
