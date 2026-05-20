<template>
  <div class="page-container">
    <div class="card">
      <div class="toolbar">
        <div>
          <h2 class="page-title">工作日管理</h2>
          <p class="page-desc">管理工作日历，同步节假日数据</p>
        </div>
        <div class="toolbar-actions">
          <el-select v-model="currentYear" @change="fetchData" style="width: 100px">
            <el-option v-for="y in yearOptions" :key="y" :label="y + '年'" :value="y" />
          </el-select>
          <el-button type="primary" :loading="syncLoading" @click="handleSync">
            <el-icon><Refresh /></el-icon>
            年度同步
          </el-button>
        </div>
      </div>

      <div class="month-nav">
        <el-button text circle @click="prevMonth">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <span class="month-label">{{ currentYear }}年{{ currentMonth }}月</span>
        <el-button text circle @click="nextMonth">
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>

      <div class="calendar-wrapper">
        <div class="calendar-header">
          <div v-for="d in weekDays" :key="d" class="calendar-header-cell">{{ d }}</div>
        </div>
        <div class="calendar-body">
          <div
            v-for="(day, idx) in calendarDays"
            :key="idx"
            class="calendar-cell"
            :class="{
              'other-month': !day.currentMonth,
              'is-today': day.isToday,
              'is-workday': day.currentMonth && day.isWorkday,
              'is-weekend': day.currentMonth && day.type === 'WEEKEND',
              'is-holiday': day.currentMonth && day.type === 'HOLIDAY',
              'is-makeup': day.currentMonth && day.type === 'MAKEUP',
            }"
            @click="day.currentMonth && day.id && openEditDialog(day)"
          >
            <div class="day-number">{{ day.day }}</div>
            <div v-if="day.currentMonth && day.type" class="day-type">
              {{ typeLabel(day.type) }}
            </div>
          </div>
        </div>
      </div>

      <div class="legend">
        <span class="legend-item"><span class="legend-dot workday"></span>工作日</span>
        <span class="legend-item"><span class="legend-dot weekend"></span>周末</span>
        <span class="legend-item"><span class="legend-dot holiday"></span>节假日</span>
        <span class="legend-item"><span class="legend-dot makeup"></span>调休补班</span>
      </div>
    </div>

    <el-dialog v-model="editVisible" title="编辑工作日" width="400px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="日期">
          <span class="mono">{{ editForm.date }}</span>
        </el-form-item>
        <el-form-item label="是否工作日">
          <el-switch v-model="editForm.isWorkday" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="editForm.type" style="width: 100%">
            <el-option label="工作日" value="WORKDAY" />
            <el-option label="周末" value="WEEKEND" />
            <el-option label="节假日" value="HOLIDAY" />
            <el-option label="调休补班" value="MAKEUP" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWorkDays, syncWorkDays, updateWorkDay } from '@/api'
import dayjs from 'dayjs'

const weekDays = ['一', '二', '三', '四', '五', '六', '日']
const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth() + 1)
const syncLoading = ref(false)
const editVisible = ref(false)
const workDays = ref<any[]>([])

const yearOptions = computed(() => {
  const y = new Date().getFullYear()
  return [y - 1, y, y + 1, y + 2]
})

const editForm = reactive({
  id: 0,
  date: '',
  isWorkday: 1,
  type: 'WORKDAY',
  description: '',
})

const calendarDays = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value
  const firstDay = dayjs(`${year}-${String(month).padStart(2, '0')}-01`)
  const daysInMonth = firstDay.daysInMonth()
  let startWeekday = firstDay.day()
  if (startWeekday === 0) startWeekday = 7
  startWeekday -= 1

  const prevMonth = month === 1 ? 12 : month - 1
  const prevYear = month === 1 ? year - 1 : year
  const prevDaysInMonth = dayjs(`${prevYear}-${String(prevMonth).padStart(2, '0')}-01`).daysInMonth()

  const today = dayjs().format('YYYY-MM-DD')
  const days: any[] = []

  for (let i = startWeekday - 1; i >= 0; i--) {
    days.push({ day: prevDaysInMonth - i, currentMonth: false, isToday: false })
  }

  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    const wd = workDays.value.find((w: any) => w.date === dateStr || dayjs(w.date).format('YYYY-MM-DD') === dateStr)
    days.push({
      day: d,
      currentMonth: true,
      isToday: dateStr === today,
      id: wd?.id || 0,
      date: dateStr,
      isWorkday: wd?.isWorkday ?? 1,
      type: wd?.type || '',
      description: wd?.description || '',
    })
  }

  const remaining = 42 - days.length
  for (let i = 1; i <= remaining; i++) {
    days.push({ day: i, currentMonth: false, isToday: false })
  }

  return days
})

function typeLabel(type: string) {
  const map: Record<string, string> = { WORKDAY: '班', WEEKEND: '休', HOLIDAY: '假', MAKEUP: '补' }
  return map[type] || ''
}

function prevMonth() {
  if (currentMonth.value === 1) {
    currentMonth.value = 12
    currentYear.value--
  } else {
    currentMonth.value--
  }
  fetchData()
}

function nextMonth() {
  if (currentMonth.value === 12) {
    currentMonth.value = 1
    currentYear.value++
  } else {
    currentMonth.value++
  }
  fetchData()
}

async function fetchData() {
  try {
    const res: any = await getWorkDays(currentYear.value, currentMonth.value)
    workDays.value = res.data || []
  } catch {}
}

async function handleSync() {
  try {
    await ElMessageBox.confirm(`确定同步 ${currentYear.value} 年工作日数据？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    syncLoading.value = true
    await syncWorkDays(currentYear.value)
    ElMessage.success('同步成功')
    fetchData()
  } catch {} finally {
    syncLoading.value = false
  }
}

function openEditDialog(day: any) {
  editForm.id = day.id
  editForm.date = day.date
  editForm.isWorkday = day.isWorkday
  editForm.type = day.type || 'WORKDAY'
  editForm.description = day.description || ''
  editVisible.value = true
}

async function handleUpdate() {
  try {
    await updateWorkDay(editForm.id, {
      isWorkday: editForm.isWorkday,
      type: editForm.type,
      description: editForm.description,
    })
    ElMessage.success('更新成功')
    editVisible.value = false
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

.toolbar-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.month-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  margin-bottom: 20px;
}

.month-label {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
  min-width: 100px;
  text-align: center;
  font-family: $font-mono;
}

.calendar-wrapper {
  border: 1px solid $border-color;
  border-radius: $radius-md;
  overflow: hidden;
}

.calendar-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background-color: $bg-secondary;
}

.calendar-header-cell {
  padding: 12px;
  text-align: center;
  color: $text-secondary;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid $border-color;
}

.calendar-body {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}

.calendar-cell {
  min-height: 72px;
  padding: 10px;
  border-right: 1px solid $border-color;
  border-bottom: 1px solid $border-color;
  cursor: pointer;
  transition: all $transition-fast;
  background: $bg-card;

  &:nth-child(7n) {
    border-right: none;
  }

  &:hover {
    background-color: $bg-hover;
  }

  &.other-month {
    background: $bg-primary;
    .day-number { color: $text-muted; }
  }

  &.is-today {
    .day-number {
      border: 2px solid $brand-primary;
      border-radius: 50%;
      width: 28px;
      height: 28px;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 0 8px rgba(50, 240, 140, 0.15);
    }
  }

  &.is-workday {
    .day-type {
      background: rgba(50, 240, 140, 0.15);
      color: #60f2bd;
    }
  }

  &.is-weekend {
    .day-type {
      background: rgba(90, 90, 90, 0.15);
      color: $text-muted;
    }
  }

  &.is-holiday {
    .day-type {
      background: rgba(240, 85, 85, 0.15);
      color: #f07070;
    }
  }

  &.is-makeup {
    .day-type {
      background: rgba(240, 200, 50, 0.15);
      color: #f0d860;
    }
  }
}

.day-number {
  font-size: 14px;
  font-weight: 500;
  color: $text-primary;
  margin-bottom: 6px;
}

.day-type {
  display: inline-block;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: $radius-xs;
  font-weight: 500;
}

.legend {
  display: flex;
  gap: 20px;
  margin-top: 16px;
  justify-content: center;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: $text-secondary;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;

  &.workday { background: $color-calendar-normal; }
  &.weekend { background: $color-calendar-nonwork; }
  &.holiday { background: $color-calendar-unfilled; }
  &.makeup { background: $color-calendar-insufficient; }
}
</style>
