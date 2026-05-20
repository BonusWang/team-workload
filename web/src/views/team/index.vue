<template>
  <div class="page-container">
    <div class="card">
      <div class="toolbar">
        <div>
          <h2 class="page-title">团队视图</h2>
          <p class="page-desc">查看团队工作量分布与排名</p>
        </div>
      </div>

      <div class="overview-row">
        <el-button class="week-nav-btn" @click="prevWeek" :disabled="!hasPrevWeek || loading">
          <el-icon><ArrowLeft /></el-icon>
          上一周
        </el-button>

        <div class="overview-item period-card">
          <div class="overview-icon period">
            <el-icon :size="16"><Calendar /></el-icon>
          </div>
          <div class="overview-info">
            <span class="overview-label">统计周期</span>
            <span class="overview-value mono">{{ currentWeekRange }}</span>
          </div>
        </div>

        <el-button class="week-nav-btn" @click="nextWeek" :disabled="!hasNextWeek || loading">
          下一周
          <el-icon><ArrowRight /></el-icon>
        </el-button>

        <div class="overview-item">
          <div class="overview-icon hours">
            <el-icon :size="16"><Clock /></el-icon>
          </div>
          <div class="overview-info">
            <span class="overview-label">团队总工时</span>
            <span class="overview-value mono">{{ overview.teamTotalHours || 0 }}<span class="overview-unit">h</span></span>
          </div>
        </div>
        <div class="overview-item">
          <div class="overview-icon members">
            <el-icon :size="16"><User /></el-icon>
          </div>
          <div class="overview-info">
            <span class="overview-label">成员数</span>
            <span class="overview-value mono">{{ overview.memberCount || 0 }}</span>
          </div>
        </div>
      </div>

      <h3 class="section-title" style="margin-top: 24px;">工作量排名</h3>
      <el-table :data="ranking" class="dark-table" v-loading="loading">
        <el-table-column type="index" label="#" width="50">
          <template #default="{ $index }">
            <span class="rank-num" :class="{ 'top-3': $index < 3 }">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" min-width="120" />
        <el-table-column prop="totalHours" label="工时" min-width="100">
          <template #default="{ row }">
            <span class="mono" :class="{ 'text-danger': row.alert === 'HIGH', 'text-warning': row.alert === 'LOW', 'text-brand': !row.alert }">{{ row.totalHours }}h</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag v-if="row.alert === 'HIGH'" type="danger" size="small" effect="dark" round>高负荷</el-tag>
            <el-tag v-else-if="row.alert === 'LOW'" type="warning" size="small" effect="dark" round>低负荷</el-tag>
            <el-tag v-else type="success" size="small" effect="dark" round>正常</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getTeamWeeklyOverview, getTeamWeeklyRanking } from '@/api'

const overview = ref<any>({})
const ranking = ref<any[]>([])
const loading = ref(false)
const currentDate = ref(new Date())

const formatDate = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getRealCurrentWeekRange = () => {
  const now = new Date()
  const dayOfWeek = now.getDay()

  let thisThursday = new Date(now)
  thisThursday.setDate(now.getDate() + (4 - dayOfWeek + 7) % 7)
  thisThursday.setHours(0, 0, 0, 0)

  let lastThursday = new Date(thisThursday)
  lastThursday.setDate(thisThursday.getDate() - 7)

  let thisWednesday = new Date(thisThursday)
  thisWednesday.setDate(thisThursday.getDate() - 1)

  return { lastThursday, thisWednesday, thisThursday }
}

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

const currentWeekRange = computed(() => {
  const { startDate, endDate } = getWeekDates()
  return `${startDate} ~ ${endDate}`
})

const hasPrevWeek = computed(() => true)

const hasNextWeek = computed(() => {
  const { thisThursday } = getRealCurrentWeekRange()
  const { endDate } = getWeekDates()
  const viewEnd = new Date(endDate)
  viewEnd.setHours(0, 0, 0, 0)
  return viewEnd < thisThursday
})

function prevWeek() {
  const newDate = new Date(currentDate.value)
  newDate.setDate(newDate.getDate() - 7)
  currentDate.value = newDate
  fetchData()
}

function nextWeek() {
  const newDate = new Date(currentDate.value)
  newDate.setDate(newDate.getDate() + 7)
  currentDate.value = newDate
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const { startDate, endDate } = getWeekDates()
    const params = { startDate, endDate }

    const [overviewRes, rankingRes] = await Promise.allSettled([
      getTeamWeeklyOverview(params),
      getTeamWeeklyRanking(params)
    ])

    if (overviewRes.status === 'fulfilled') {
      overview.value = overviewRes.value.data || {}
    } else {
      console.error('团队概览加载失败:', overviewRes.reason)
      ElMessage.error('加载团队概览失败')
    }

    if (rankingRes.status === 'fulfilled') {
      ranking.value = rankingRes.value.data || []
    } else {
      console.error('工时排名加载失败:', rankingRes.reason)
      ElMessage.error('加载工时排名失败')
    }
  } catch (error) {
    console.error('加载团队数据失败:', error)
    ElMessage.error('加载数据失败，请重试')
  } finally {
    loading.value = false
  }
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

.overview-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.week-nav-btn {
  flex-shrink: 0;
  height: 36px;
  padding: 0 14px;
  font-size: 13px;
  background-color: $bg-secondary;
  border-color: $border-color;
  color: $text-secondary;

  &:hover:not(:disabled) {
    background-color: $bg-hover;
    border-color: $border-color-light;
    color: $text-primary;
  }

  &:disabled {
    opacity: 0.35;
    cursor: not-allowed;
  }

  .el-icon {
    font-size: 12px;
  }
}

.overview-item {
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

  &.period-card {
    flex: 2;
    min-width: 0;
  }

  &:not(.period-card) {
    flex: 1;
  }
}

.overview-icon {
  width: 36px;
  height: 36px;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.period { background: rgba(50, 152, 240, 0.12); color: #3298f0; }
  &.hours { background: rgba(50, 240, 140, 0.12); color: #32f08c; }
  &.members { background: rgba(160, 253, 231, 0.12); color: #60f2bd; }
}

.overview-info {
  display: flex;
  flex-direction: column;
}

.overview-label {
  font-size: 12px;
  color: $text-secondary;
}

.overview-value {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
  font-family: $font-mono;
}

.overview-unit {
  font-size: 11px;
  font-weight: 500;
  color: $text-muted;
  margin-left: 2px;
}

.rank-num {
  font-family: $font-mono;
  font-weight: 600;
  font-size: 13px;
  color: $text-muted;

  &.top-3 {
    color: $brand-primary;
  }
}

.text-danger { color: $color-danger; font-weight: 600; }
.text-warning { color: $color-warning; font-weight: 600; }
.text-brand { color: $brand-primary; font-weight: 600; }
</style>
