<template>
  <div class="page-container">
    <div class="welcome-bar">
      <div class="welcome-text">
        <h2 class="welcome-title">{{ greeting }}，{{ userStore.userInfo?.name || '用户' }}</h2>
        <p class="welcome-desc">{{ dashboard.weekStartDate }} ~ {{ dashboard.weekEndDate }}</p>
      </div>
    </div>

    <div class="stats-grid">
      <div class="stat-card stat-hover">
        <div class="stat-icon-wrap week">
          <el-icon :size="20"><Clock /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-label">本周工时</div>
          <div class="stat-value mono">{{ dashboard.weekHours || 0 }}<span class="stat-unit">h</span></div>
        </div>
      </div>
      <div class="stat-card stat-hover">
        <div class="stat-icon-wrap month">
          <el-icon :size="20"><Calendar /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-label">本月累计</div>
          <div class="stat-value mono">{{ dashboard.monthHours || 0 }}<span class="stat-unit">h</span></div>
        </div>
      </div>
      <div class="stat-card stat-hover">
        <div class="stat-icon-wrap leave">
          <el-icon :size="20"><Sunny /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-label">本月请假</div>
          <div class="stat-value mono">{{ dashboard.leaveDays || 0 }}<span class="stat-unit">天</span></div>
        </div>
      </div>
      <div class="stat-card stat-hover">
        <div class="stat-icon-wrap" :class="saturationIconClass">
          <el-icon :size="20"><DataBoard /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-label">工作饱和度</div>
          <div class="stat-value" :class="saturationClass">{{ saturationLabel }}</div>
        </div>
      </div>
    </div>

    <div class="card calendar-card">
      <div class="calendar-toolbar">
        <h3 class="section-title">月度日历</h3>
        <div class="legend">
          <span class="legend-item"><span class="legend-dot normal"></span>正常</span>
          <span class="legend-item"><span class="legend-dot insufficient"></span>不足</span>
          <span class="legend-item"><span class="legend-dot leave"></span>请假</span>
          <span class="legend-item"><span class="legend-dot unfilled"></span>未填报</span>
          <span class="legend-item"><span class="legend-dot nonwork"></span>非工作日</span>
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
          <div v-for="d in weekDayLabels" :key="d" class="calendar-header-cell">{{ d }}</div>
        </div>
        <div class="calendar-body">
          <div
            v-for="(day, idx) in calendarDays"
            :key="idx"
            class="calendar-cell"
            :class="{
              'other-month': !day.currentMonth,
              'is-today': day.isToday,
              'is-normal': day.currentMonth && day.status === 'NORMAL',
              'is-insufficient': day.currentMonth && day.status === 'INSUFFICIENT',
              'is-leave': day.currentMonth && day.status === 'LEAVE',
              'is-unfilled': day.currentMonth && day.status === 'UNFILLED',
              'is-nonworkday': day.currentMonth && day.status === 'NONWORKDAY',
            }"
          >
            <div class="day-number">{{ day.day }}</div>
            <div v-if="day.currentMonth && day.status" class="day-type">
              {{ statusTag(day.status) }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getPersonalDashboard, getCalendarData } from '@/api'
import { useUserStore } from '@/store/user'
import dayjs from 'dayjs'

const userStore = useUserStore()
const dashboard = ref<any>({})
const calendarData = ref<any[]>([])
const weekDayLabels = ['一', '二', '三', '四', '五', '六', '日']

const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth() + 1)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 12) return '早上好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const saturationLabel = computed(() => {
  const s = dashboard.value.saturation
  if (s === 'HIGH') return '高负荷'
  if (s === 'LOW') return '低负荷'
  return '正常'
})

const saturationClass = computed(() => {
  const s = dashboard.value.saturation
  if (s === 'HIGH') return 'high'
  if (s === 'LOW') return 'low'
  return 'normal'
})

const saturationIconClass = computed(() => {
  const s = dashboard.value.saturation
  if (s === 'HIGH') return 'danger'
  if (s === 'LOW') return 'warning'
  return 'success'
})

function statusTag(status: string) {
  const map: Record<string, string> = {
    NORMAL: '正常',
    INSUFFICIENT: '不足',
    LEAVE: '请假',
    UNFILLED: '未填',
    NONWORKDAY: '休',
  }
  return map[status] || ''
}

const calendarDays = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value
  const firstDay = dayjs(`${year}-${String(month).padStart(2, '0')}-01`)
  const daysInMonth = firstDay.daysInMonth()
  let startWeekday = firstDay.day()
  if (startWeekday === 0) startWeekday = 7
  startWeekday -= 1

  const prevMonthNum = month === 1 ? 12 : month - 1
  const prevYearNum = month === 1 ? year - 1 : year
  const prevDaysInMonth = dayjs(`${prevYearNum}-${String(prevMonthNum).padStart(2, '0')}-01`).daysInMonth()

  const today = dayjs().format('YYYY-MM-DD')
  const days: any[] = []

  for (let i = startWeekday - 1; i >= 0; i--) {
    days.push({ day: prevDaysInMonth - i, currentMonth: false, isToday: false, status: '' })
  }

  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    const cd = calendarData.value.find((item: any) => {
      return item.date === dateStr || dayjs(item.date).format('YYYY-MM-DD') === dateStr
    })
    days.push({
      day: d,
      currentMonth: true,
      isToday: dateStr === today,
      date: dateStr,
      status: cd?.status || '',
    })
  }

  const remaining = 42 - days.length
  for (let i = 1; i <= remaining; i++) {
    days.push({ day: i, currentMonth: false, isToday: false, status: '' })
  }

  return days
})

function prevMonth() {
  if (currentMonth.value === 1) {
    currentMonth.value = 12
    currentYear.value--
  } else {
    currentMonth.value--
  }
  fetchCalendarData()
}

function nextMonth() {
  if (currentMonth.value === 12) {
    currentMonth.value = 1
    currentYear.value++
  } else {
    currentMonth.value++
  }
  fetchCalendarData()
}

async function fetchDashboard() {
  try {
    const res: any = await getPersonalDashboard()
    dashboard.value = res.data || {}
  } catch {}
}

async function fetchCalendarData() {
  try {
    const yearMonth = `${currentYear.value}-${String(currentMonth.value).padStart(2, '0')}`
    const res: any = await getCalendarData(yearMonth)
    calendarData.value = res.data || []
  } catch {}
}

onMounted(() => {
  fetchDashboard()
  fetchCalendarData()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.welcome-bar {
  margin-bottom: 28px;
}

.welcome-title {
  color: $text-primary;
  margin: 0 0 4px;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.3px;
}

.welcome-desc {
  color: $text-secondary;
  font-size: 14px;
  margin: 0;
  font-family: $font-mono;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-md;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all $transition-normal;
}

.stat-hover:hover {
  border-color: $border-color-light;
  box-shadow: $shadow-card-hover;
  transform: translateY(-2px);
}

.stat-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: $brand-primary-dim;
  color: $brand-primary;

  &.week { background: rgba(50, 240, 140, 0.12); color: #32f08c; }
  &.month { background: rgba(50, 152, 240, 0.12); color: #3298f0; }
  &.leave { background: rgba(240, 200, 50, 0.12); color: #f0c832; }
  &.success { background: rgba(50, 240, 140, 0.12); color: #32f08c; }
  &.warning { background: rgba(240, 200, 50, 0.12); color: #f0c832; }
  &.danger { background: rgba(240, 85, 85, 0.12); color: #f05555; }
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-label {
  color: $text-secondary;
  font-size: 13px;
  margin-bottom: 4px;
  font-weight: 450;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: $text-primary;
  font-family: $font-mono;
  letter-spacing: -0.5px;

  &.high { color: $color-danger; }
  &.low { color: $color-warning; }
  &.normal { color: $brand-primary; }
}

.stat-unit {
  font-size: 14px;
  font-weight: 500;
  color: $text-secondary;
  margin-left: 2px;
}

.calendar-card {
  padding: 24px;
}

.calendar-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
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

  &.is-normal {
    .day-type {
      background: rgba(50, 240, 140, 0.15);
      color: #60f2bd;
    }
  }

  &.is-insufficient {
    .day-type {
      background: rgba(240, 200, 50, 0.15);
      color: #f0d860;
    }
  }

  &.is-leave {
    .day-type {
      background: rgba(50, 152, 240, 0.15);
      color: #60b0f0;
    }
  }

  &.is-unfilled {
    .day-type {
      background: rgba(240, 85, 85, 0.15);
      color: #f07070;
    }
  }

  &.is-nonworkday {
    .day-type {
      background: rgba(90, 90, 90, 0.15);
      color: $text-muted;
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
  gap: 16px;
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

  &.normal { background: $color-calendar-normal; }
  &.insufficient { background: $color-calendar-insufficient; }
  &.leave { background: $color-calendar-leave; }
  &.unfilled { background: $color-calendar-unfilled; }
  &.nonwork { background: $color-calendar-nonwork; }
}
</style>
