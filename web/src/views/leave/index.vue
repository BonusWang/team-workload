<template>
  <div class="page-container">
    <div class="card">
      <div class="toolbar">
        <div>
          <h2 class="page-title">请假管理</h2>
          <p class="page-desc">申请请假并查看请假记录</p>
        </div>
        <el-button type="primary" class="submit-btn" @click="dialogVisible = true" v-if="userStore.hasPermission('leave_submit')">
          <el-icon><Plus /></el-icon>
          申请请假
        </el-button>
      </div>

      <div class="balance-row">
        <div class="balance-item">
          <div class="balance-icon annual">
            <el-icon :size="16"><Sunny /></el-icon>
          </div>
          <div class="balance-info">
            <span class="balance-label">年假余额</span>
            <span class="balance-value mono">{{ balance.annualLeaveBalance || 0 }}<span class="balance-unit">天</span></span>
          </div>
        </div>
        <div class="balance-item">
          <div class="balance-icon used">
            <el-icon :size="16"><Calendar /></el-icon>
          </div>
          <div class="balance-info">
            <span class="balance-label">本月已请</span>
            <span class="balance-value mono">{{ balance.monthLeaveDays || 0 }}<span class="balance-unit">天</span></span>
          </div>
        </div>
      </div>

      <el-table :data="leaves" class="dark-table compact-table" border>
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
            <el-tag :type="statusType(row.status)" size="small" effect="dark" round>{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="120" />
        <el-table-column prop="rejectReason" label="驳回原因" min-width="120" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" type="danger" size="small" link @click="handleRevoke(row)">撤回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="申请请假" width="440px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="类型" prop="leaveType">
          <el-select v-model="form.leaveType" style="width: 100%">
            <el-option label="年假" value="ANNUAL" />
            <el-option label="病假" value="SICK" />
            <el-option label="事假" value="PERSONAL" />
            <el-option label="婚假" value="MARRIAGE" />
            <el-option label="产假" value="MATERNITY" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker v-model="form.startDate" type="date" style="width: 100%" :disabled-date="disabledStartDate" :picker-options="datePickerOptions" />
        </el-form-item>
        <el-form-item label="开始时段" prop="startPeriod">
          <el-select v-model="form.startPeriod" style="width: 100%">
            <el-option label="上午" value="morning" />
            <el-option label="下午" value="afternoon" />
          </el-select>
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker v-model="form.endDate" type="date" style="width: 100%" :disabled-date="disabledEndDate" :picker-options="datePickerOptions" />
        </el-form-item>
        <el-form-item label="结束时段" prop="endPeriod">
          <el-select v-model="form.endPeriod" style="width: 100%">
            <el-option label="上午" value="morning" />
            <el-option label="下午" value="afternoon" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请输入请假原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyLeaves, getLeaveBalance, submitLeave, getWorkDays, revokeLeave } from '@/api'
import { useUserStore } from '@/store/user'
import dayjs from 'dayjs'
import locale from 'element-plus/es/locale/lang/zh-cn'

const leaves = ref<any[]>([])
const balance = ref<any>({})
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ leaveType: 'ANNUAL', startDate: '', endDate: '', startPeriod: 'morning', endPeriod: 'afternoon', reason: '' })
const workDays = ref<any[]>([])
const currentYear = new Date().getFullYear()
const currentMonth = new Date().getMonth() + 1
const userStore = useUserStore()

const rules = {
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  startPeriod: [{ required: true, message: '请选择开始时段', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
  endPeriod: [{ required: true, message: '请选择结束时段', trigger: 'change' }],
  reason: [{ required: true, message: '请输入请假原因', trigger: 'blur' }]
}
// 日期选择器中文配置
const datePickerOptions = {
  yearFormat: 'YYYY年',
  monthFormat: 'MM月',
  firstDayOfWeek: 1,
  // 监听月份变化，加载对应月份的工作日数据
  onMonthChange: (date: any) => {
    if (date) {
      const year = date.getFullYear()
      const month = date.getMonth() + 1
      fetchWorkDays(year, month)
    }
  },
  // 中文表头配置
  cellClassName: () => 'chinese-date'
}

function typeLabel(t: string) {
  const map: any = { ANNUAL: '年假', SICK: '病假', PERSONAL: '事假', MARRIAGE: '婚假', MATERNITY: '产假', OTHER: '其他' }
  return map[t] || t
}

function statusLabel(s: string) {
  const map: any = { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回', REVOKED: '已撤回' }
  return map[s] || s
}

function statusType(s: string) {
  const map: any = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', REVOKED: 'info' }
  return map[s] || 'info'
}

async function handleRevoke(row: any) {
  try {
    await ElMessageBox.confirm('确定要撤回这个请假申请吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await revokeLeave(row.id)
    ElMessage.success('撤回成功')
    fetchData()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('撤回失败:', error)
    }
  }
}

function timeLabel(t: string) {
  if (!t) return ''
  const hour = parseInt(t.substring(0, 2))
  const minute = parseInt(t.substring(3, 5))
  // 09:00-12:00 是上午，13:30-18:30 是下午
  if (hour < 12 || (hour === 12 && minute === 0)) {
    return '上午'
  } else {
    return '下午'
  }
}

function disabledDate(time: any) {
  if (!time) return false
  const dateStr = dayjs(time).format('YYYY-MM-DD')
  
  // 根据 workday 表的数据判断
  const workDay = workDays.value.find((wd: any) => {
    const wdDate = dayjs(wd.date).format('YYYY-MM-DD')
    return wdDate === dateStr
  })
  
  if (workDay) {
    // is_workday = 1 是工作日，允许选择
    // is_workday != 1 是节假日/周末，禁用
    return workDay.isWorkday !== 1
  }
  
  // 默认：周末禁用
  const day = time.getDay()
  return day === 0 || day === 6
}

function disabledStartDate(time: any) {
  if (!time) return false
  
  // 禁用历史月（今天之前的月份）
  const today = dayjs().startOf('day')
  if (dayjs(time).isBefore(today.startOf('month'))) {
    return true
  }
  
  return false
}

function disabledEndDate(time: any) {
  if (!time) return false
  
  // 如果开始日期已选择，禁用开始日期之前的日期
  if (form.startDate) {
    const startDate = dayjs(form.startDate).startOf('day')
    if (dayjs(time).isBefore(startDate)) {
      return true
    }
    return false
  }
  
  // 开始日期未选择时，和开始日期规则一致：禁用历史月
  const today = dayjs().startOf('day')
  if (dayjs(time).isBefore(today.startOf('month'))) {
    return true
  }
  
  return false
}

async function fetchWorkDays(year: number, month: number) {
  try {
    console.log('加载工作日数据:', year, month)
    const res: any = await getWorkDays(year, month)

    if (res && res.data) {
      console.log('获取到工作日数据:', res.data.length, '条')
      // 合并新的工作日数据，避免覆盖之前的数据
      const newWorkDays = res.data || []
      const existingDates = new Set(workDays.value.map((wd: any) => dayjs(wd.date).format('YYYY-MM-DD')))

      newWorkDays.forEach((newDay: any) => {
        const newDate = dayjs(newDay.date).format('YYYY-MM-DD')
        if (!existingDates.has(newDate)) {
          workDays.value.push(newDay)
        }
      })

      console.log('当前工作日数据总数:', workDays.value.length)
    } else {
      console.error('获取工作日数据失败：响应格式错误')
    }
  } catch (error) {
    console.error('获取工作日数据失败：', error)
    // 加载失败时不要清空已有的数据，避免影响用户体验
  }
}

async function fetchData() {
  try {
    const res: any = await getMyLeaves()
    leaves.value = res.data || []
  } catch {}
  try {
    const res: any = await getLeaveBalance()
    balance.value = res.data || {}
  } catch {}

  // 加载当前月的工作日数据
  await fetchWorkDays(currentYear, currentMonth)

  // 加载5月份的数据，确保五一假期和补班能被正确识别
  await fetchWorkDays(currentYear, 5)

  // 加载全年的工作日数据，确保所有节假日都能被正确识别
  await fetchWorkDays(currentYear)

  console.log('工作日数据:', workDays.value)

  // 检查5月3号的数据
  const may3 = workDays.value.find((wd: any) => {
    const date = dayjs(wd.date).format('YYYY-MM-DD')
    return date === '2026-05-03'
  })
  console.log('5月3号的数据:', may3)
}

// 检查日期时间是否冲突
function checkDateConflict() {
  // 只检查待审批和已通过的记录
  const activeLeaves = leaves.value.filter(
    (leave: any) => leave.status === 'PENDING' || leave.status === 'APPROVED'
  )
  
  // 将新申请的时段转换为具体的时间点
  const newStartDate = dayjs(form.startDate)
  const newEndDate = dayjs(form.endDate)
  
  // 为新申请创建所有日期的时段映射
  const newPeriods: Record<string, { start: number; end: number }> = {}
  
  // 遍历从开始日期到结束日期的每一天
  let currentDate = newStartDate
  while (currentDate.isBefore(newEndDate) || currentDate.isSame(newEndDate)) {
    const dateStr = currentDate.format('YYYY-MM-DD')
    
    let startHour = 9 // 上午默认 9点
    let endHour = 18 // 下午默认 18点
    
    // 处理开始日期的开始时间
    if (currentDate.isSame(newStartDate)) {
      startHour = form.startPeriod === 'morning' ? 9 : 13
    }
    
    // 处理结束日期的结束时间
    if (currentDate.isSame(newEndDate)) {
      endHour = form.endPeriod === 'morning' ? 12 : 18
    } else {
      // 如果不是结束日期，默认请全天
      endHour = 18
      startHour = 9
    }
    
    newPeriods[dateStr] = { start: startHour, end: endHour }
    
    currentDate = currentDate.add(1, 'day')
  }
  
  // 检查与现有记录的冲突
  for (const leave of activeLeaves) {
    const leaveStartDate = dayjs(leave.startDate)
    const leaveEndDate = dayjs(leave.endDate)
    
    // 为现有记录创建所有日期的时段映射
    const leavePeriods: Record<string, { start: number; end: number }> = {}
    let leaveCurrentDate = leaveStartDate
    while (leaveCurrentDate.isBefore(leaveEndDate) || leaveCurrentDate.isSame(leaveEndDate)) {
      const dateStr = leaveCurrentDate.format('YYYY-MM-DD')
      
      // 获取现有记录的时间
      const leaveStartTime = leave.startTime || '09:00'
      const leaveEndTime = leave.endTime || '18:30'
      
      // 转换开始小时
      let startHour = 9
      if (leaveStartTime.includes('09:') || leaveStartTime.includes('12:')) {
        startHour = 9
      } else if (leaveStartTime.includes('13:') || leaveStartTime.includes('18:')) {
        startHour = 13
      }
      
      // 转换结束小时
      let endHour = 18
      if (leaveEndTime.includes('09:') || leaveEndTime.includes('12:')) {
        endHour = 12
      } else if (leaveEndTime.includes('13:') || leaveEndTime.includes('18:')) {
        endHour = 18
      }
      
      // 如果是中间日期，默认全天
      if (!leaveCurrentDate.isSame(leaveStartDate) && !leaveCurrentDate.isSame(leaveEndDate)) {
        startHour = 9
        endHour = 18
      } else {
        // 如果是开始日期
        if (leaveCurrentDate.isSame(leaveStartDate)) {
          if (leaveStartTime.includes('09:') || leaveStartTime.includes('12:')) {
            startHour = 9
          } else {
            startHour = 13
          }
        }
        // 如果是结束日期
        if (leaveCurrentDate.isSame(leaveEndDate)) {
          if (leaveEndTime.includes('09:') || leaveEndTime.includes('12:')) {
            endHour = 12
          } else {
            endHour = 18
          }
        }
      }
      
      leavePeriods[dateStr] = { start: startHour, end: endHour }
      
      leaveCurrentDate = leaveCurrentDate.add(1, 'day')
    }
    
    // 检查日期重叠
    for (const dateStr in newPeriods) {
      if (leavePeriods[dateStr]) {
        const newP = newPeriods[dateStr]
        const leaveP = leavePeriods[dateStr]
        
        // 检查时间是否重叠
        if (!(newP.end <= leaveP.start || newP.start >= leaveP.end)) {
          return {
            conflict: true,
            date: dateStr,
            leave: leave
          }
        }
      }
    }
  }
  
  return { conflict: false }
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    
    // 检查日期冲突
    const conflictCheck = checkDateConflict()
    if (conflictCheck.conflict) {
      ElMessage.error(`该时间段已有请假记录（${conflictCheck.date}），请重新选择`)
      return
    }
    
    // 将时段转换为时间
    const startTime = form.startPeriod === 'morning' ? '09:00' : '13:30'
    const endTime = form.endPeriod === 'morning' ? '12:00' : '18:30'
    
    await submitLeave({
      leaveType: form.leaveType,
      startDate: dayjs(form.startDate).format('YYYY-MM-DD'),
      endDate: dayjs(form.endDate).format('YYYY-MM-DD'),
      startTime: startTime,
      endTime: endTime,
      reason: form.reason
    })
    ElMessage.success('提交成功')
    dialogVisible.value = false
    fetchData()
  } catch {}
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.page-desc {
  color: $text-secondary;
  font-size: 13px;
  margin: 4px 0 0;
}

.balance-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.balance-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: $bg-secondary;
  border: 1px solid $border-color;
  border-radius: $radius-md;
  transition: all $transition-normal;

  &:hover {
    border-color: $border-color-light;
  }
}

.balance-icon {
  width: 36px;
  height: 36px;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;

  &.annual {
    background: rgba(50, 240, 140, 0.12);
    color: #32f08c;
  }
  &.used {
    background: rgba(240, 200, 50, 0.12);
    color: #f0c832;
  }
}

.balance-info {
  display: flex;
  flex-direction: column;
}

.balance-label {
  font-size: 12px;
  color: $text-secondary;
}

.balance-value {
  font-size: 20px;
  font-weight: 700;
  color: $text-primary;
  font-family: $font-mono;
}

.balance-unit {
  font-size: 12px;
  font-weight: 500;
  color: $text-muted;
  margin-left: 2px;
}

.compact-table {
  :deep(.el-table__cell) {
    padding: 8px 0;
  }

  :deep(.el-table__header th) {
    padding: 10px 0;
  }
}

/* 日期选择器中文表头样式 */
:deep(.el-date-table th:nth-child(1)::after) { content: '日'; }
:deep(.el-date-table th:nth-child(2)::after) { content: '一'; }
:deep(.el-date-table th:nth-child(3)::after) { content: '二'; }
:deep(.el-date-table th:nth-child(4)::after) { content: '三'; }
:deep(.el-date-table th:nth-child(5)::after) { content: '四'; }
:deep(.el-date-table th:nth-child(6)::after) { content: '五'; }
:deep(.el-date-table th:nth-child(7)::after) { content: '六'; }

/* 隐藏英文表头 */
:deep(.el-date-table th span) { display: none; }


</style>
