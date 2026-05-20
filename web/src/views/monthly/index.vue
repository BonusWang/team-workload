<template>
  <div class="page-container">
    <div class="card">
      <div class="toolbar">
        <div>
          <h2 class="page-title">月度汇总</h2>
          <p class="page-desc">查看月度工作量统计数据</p>
        </div>
      </div>
      <div class="summary-grid">
        <div class="summary-item">
          <div class="summary-icon month-icon">
            <el-icon :size="18"><Calendar /></el-icon>
          </div>
          <div class="summary-content">
            <div class="summary-label">月份</div>
            <div class="summary-value mono">{{ summary.yearMonth || '-' }}</div>
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-icon hours-icon">
            <el-icon :size="18"><Clock /></el-icon>
          </div>
          <div class="summary-content">
            <div class="summary-label">总工时</div>
            <div class="summary-value mono">{{ summary.totalHours || 0 }}<span class="summary-unit">h</span></div>
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-icon workday-icon">
            <el-icon :size="18"><Timer /></el-icon>
          </div>
          <div class="summary-content">
            <div class="summary-label">应工作天数</div>
            <div class="summary-value mono">{{ summary.workDays || 0 }}<span class="summary-unit">天</span></div>
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-icon leave-icon">
            <el-icon :size="18"><Sunny /></el-icon>
          </div>
          <div class="summary-content">
            <div class="summary-label">请假天数</div>
            <div class="summary-value mono">{{ summary.leaveDays || 0 }}<span class="summary-unit">天</span></div>
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-icon avg-icon">
            <el-icon :size="18"><DataBoard /></el-icon>
          </div>
          <div class="summary-content">
            <div class="summary-label">日均投入</div>
            <div class="summary-value mono">{{ summary.dailyAvg || 0 }}<span class="summary-unit">h</span></div>
          </div>
        </div>
      </div>
    </div>

    <div class="card mt-24" v-if="isAdmin">
      <div class="toolbar">
        <div>
          <h2 class="page-title">团队月报详情</h2>
          <p class="page-desc">查看团队月度工时统计</p>
        </div>
        <div class="actions">
          <el-date-picker
            v-model="selectedYearMonth"
            type="month"
            placeholder="选择月份"
            value-format="YYYY-MM"
            @change="handleMonthChange"
          />
          <el-button type="primary" @click="handleExport">
            <el-icon><Download /></el-icon>
            导出详情
          </el-button>
        </div>
      </div>
      <el-table :data="teamMonthlyData" v-loading="teamMonthlyLoading" class="dark-table" border>
        <el-table-column prop="submitter" label="提交人" min-width="120">
          <template #default="{ row }">
            <strong v-if="row.submitter === '合计'">{{ row.submitter }}</strong>
            <span v-else>{{ row.submitter }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="actualHours" label="实际工时" min-width="100">
          <template #default="{ row }">
            <strong v-if="row.submitter === '合计'">{{ row.actualHours }}</strong>
            <span v-else>{{ row.actualHours }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="workDays" label="工作天数" min-width="100" />
        <el-table-column prop="expectedHours" label="应工作工时" min-width="120" />
        <el-table-column prop="diff" label="差值" min-width="100">
          <template #default="{ row }">
            <span :class="{ 'diff-positive': row.diff > 0, 'diff-negative': row.diff < 0 }">
              {{ row.diff }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="supplementaryNote" label="补充说明" min-width="200" show-overflow-tooltip />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { getMonthlySummary, getTeamMonthlyDetails, exportTeamMonthlyDetails, getUserInfo } from '@/api'

const summary = ref<any>({})
const teamMonthlyData = ref<any[]>([])
const teamMonthlyLoading = ref(false)
const selectedYearMonth = ref('')
const userInfo = ref<any>({})

const isAdmin = computed(() => userInfo.value?.user?.role === 'ADMIN')

async function fetchData() {
  try {
    const res: any = await getMonthlySummary()
    summary.value = res.data || {}
  } catch (e) {
    console.error('获取月度汇总失败:', e)
  }
}

async function fetchTeamMonthlyDetails() {
  teamMonthlyLoading.value = true
  try {
    const res: any = await getTeamMonthlyDetails(selectedYearMonth.value || undefined)
    teamMonthlyData.value = res.data || []
  } catch (error) {
    console.error('获取团队月报详情失败:', error)
    ElMessage.error('获取团队月报详情失败')
  } finally {
    teamMonthlyLoading.value = false
  }
}

async function handleMonthChange() {
  fetchTeamMonthlyDetails()
}

async function handleExport() {
  try {
    const res = await exportTeamMonthlyDetails(selectedYearMonth.value || undefined)
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const month = selectedYearMonth.value || new Date().toISOString().slice(0, 7)
    link.download = `月报详情_${month}.xlsx`
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

async function fetchUserInfo() {
  try {
    const res: any = await getUserInfo()
    userInfo.value = res.data || {}
    console.log('用户信息:', userInfo.value)
    if (isAdmin.value) {
      fetchTeamMonthlyDetails()
    }
  } catch (e) {
    console.error('获取用户信息失败:', e)
  }
}

onMounted(() => {
  fetchData()
  fetchUserInfo()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.page-desc {
  color: $text-secondary;
  font-size: 13px;
  margin: 4px 0 0;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: $bg-secondary;
  border: 1px solid $border-color;
  border-radius: $radius-md;
  transition: all $transition-normal;

  &:hover {
    border-color: $border-color-light;
    transform: translateY(-1px);
  }
}

.summary-icon {
  width: 36px;
  height: 36px;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.month-icon { background: rgba(50, 152, 240, 0.12); color: #3298f0; }
  &.hours-icon { background: rgba(50, 240, 140, 0.12); color: #32f08c; }
  &.workday-icon { background: rgba(160, 253, 231, 0.12); color: #60f2bd; }
  &.leave-icon { background: rgba(240, 200, 50, 0.12); color: #f0c832; }
  &.avg-icon { background: rgba(50, 152, 240, 0.12); color: #60b0f0; }
}

.summary-content {
  min-width: 0;
}

.summary-label {
  font-size: 12px;
  color: $text-secondary;
  margin-bottom: 2px;
}

.summary-value {
  font-size: 20px;
  font-weight: 700;
  color: $text-primary;
  font-family: $font-mono;
}

.summary-unit {
  font-size: 12px;
  font-weight: 500;
  color: $text-muted;
  margin-left: 2px;
}

.diff-positive {
  color: #67c23a;
}

.diff-negative {
  color: #f56c6c;
}
</style>
