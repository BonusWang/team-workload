<template>
  <div class="page-container">
    <div class="card">
      <div class="toolbar">
        <div>
          <h2 class="page-title">用户管理</h2>
          <p class="page-desc">管理系统用户账号与权限</p>
        </div>
        <el-button type="primary" @click="openDialog()">
          <el-icon><Plus /></el-icon>
          新增用户
        </el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" class="dark-table">
        <el-table-column prop="username" label="用户名" min-width="100">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-cell-avatar">{{ (row.name || '?')[0] }}</div>
              <span>{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" min-width="80" />
        <el-table-column prop="dept" label="部门" min-width="100" />
        <el-table-column prop="role" label="角色" min-width="90">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small" effect="dark" round>
              {{ roleLabel(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="级别" min-width="80">
          <template #default="{ row }">
            {{ levelLabel(row.level) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="80">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="(val: any) => handleStatusChange(row, val)"
              inline-prompt
              active-text="启"
              inactive-text="停"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="handleResetPassword(row)">重置密码</el-button>
            <el-button link type="info" size="small" @click="openPermissionDialog(row)">权限分配</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchUsers"
          @current-change="fetchUsers"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '新增用户'"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="用户名" prop="username" v-if="!isEdit">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="部门" prop="dept">
          <el-input v-model="form.dept" placeholder="请输入部门" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="成员" value="MEMBER" />
            <el-option label="负责人" value="LEADER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="级别" prop="level">
          <el-select v-model="form.level" placeholder="请选择级别" style="width: 100%">
            <el-option label="初级" value="JUNIOR" />
            <el-option label="中级" value="MIDDLE" />
            <el-option label="高级" value="SENIOR" />
          </el-select>
        </el-form-item>
        <el-form-item label="单价" prop="unitPrice">
          <el-input-number v-model="form.unitPrice" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="上级领导" prop="leaderId">
          <el-select v-model="form.leaderId" placeholder="请选择上级领导" clearable style="width: 100%">
            <el-option
              v-for="leader in leaders"
              :key="leader.id"
              :label="leader.name"
              :value="leader.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="状态" prop="status" v-if="isEdit">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 权限分配对话框 -->
    <el-dialog
      v-model="permissionDialogVisible"
      title="权限分配"
      width="1000px"
      :close-on-click-modal="false"
    >
      <div v-loading="permissionLoading">
        <div v-if="allPermissions.length === 0" class="empty-state">
          <el-empty description="暂无权限数据" />
        </div>
        <el-space direction="vertical" class="permission-space" v-else>
          <!-- 按模块分组显示权限 -->
          <div v-for="module in getPermissionModules()" :key="module" class="permission-module">
            <div class="module-header">
              <el-checkbox
                :indeterminate="isModuleIndeterminate(module)"
                :model-value="isModuleChecked(module)"
                @change="handleModuleCheck(module, $event)"
              >{{ moduleLabel(module) }}</el-checkbox>
            </div>
            <div class="module-permissions">
              <el-checkbox
                v-for="permission in getPermissionsByModule(module)"
                :key="permission.code"
                v-model="userPermissions"
                :label="permission.code"
              >{{ permission.name }}</el-checkbox>
            </div>
          </div>
        </el-space>
      </div>
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="permissionSubmitLoading" @click="handlePermissionSubmit">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getUsers, createUser, updateUser, updateUserStatus, resetPassword, getLeaders, getAllPermissions, getUserPermissions, getUserPermissionCodes, assignUserPermissions } from '@/api'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number>(0)
const tableData = ref<any[]>([])
const leaders = ref<any[]>([])
const formRef = ref<FormInstance>()

// 权限分配相关变量
const permissionDialogVisible = ref(false)
const permissionLoading = ref(false)
const permissionSubmitLoading = ref(false)
const allPermissions = ref<any[]>([])
const userPermissions = ref<string[]>([])
const currentUserId = ref<number>(0)

const pagination = reactive({
    current: 1,
    size: 10,
    total: 0,
  })

const form = reactive({
  username: '',
  name: '',
  dept: '',
  role: '',
  level: '',
  unitPrice: 0,
  leaderId: null as number | null,
  email: '',
  status: 1,
})

const formRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  dept: [{ required: true, message: '请输入部门', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  level: [{ required: true, message: '请选择级别', trigger: 'change' }],
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
}

function roleTagType(role: string) {
  const map: Record<string, string> = { MEMBER: 'info', LEADER: 'warning', ADMIN: 'danger' }
  return map[role] || 'info'
}

function roleLabel(role: string) {
  const map: Record<string, string> = { MEMBER: '成员', LEADER: '负责人', ADMIN: '管理员' }
  return map[role] || role
}

function levelLabel(level: string) {
  const map: Record<string, string> = { JUNIOR: '初级', MIDDLE: '中级', SENIOR: '高级' }
  return map[level] || level
}

async function fetchUsers() {
  loading.value = true
  try {
    const res: any = await getUsers({ current: pagination.current, size: pagination.size })
    tableData.value = res.data.records || res.data.list || res.data || []
    pagination.total = res.data.total || 0
  } catch {} finally {
    loading.value = false
  }
}

async function fetchLeaders() {
  try {
    const res: any = await getLeaders()
    leaders.value = res.data || []
  } catch {}
}

function resetForm() {
  form.username = ''
  form.name = ''
  form.dept = ''
  form.role = ''
  form.level = ''
  form.unitPrice = 0
  form.leaderId = null
  form.email = ''
  form.status = 1
}

function openDialog(row?: any) {
  resetForm()
  formRef.value?.resetFields()
  if (row) {
    isEdit.value = true
    editId.value = row.id
    form.username = row.username
    form.name = row.name
    form.dept = row.dept
    form.role = row.role
    form.level = row.level
    form.unitPrice = row.unitPrice
    form.leaderId = row.leaderId
    form.email = row.email
    form.status = row.status
  } else {
    isEdit.value = false
    editId.value = 0
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateUser(editId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createUser({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchUsers()
  } catch {} finally {
    submitLoading.value = false
  }
}

async function handleStatusChange(row: any, val: any) {
  const status = val ? 1 : 0
  try {
    await updateUserStatus(row.id, status)
    row.status = status
    ElMessage.success('状态更新成功')
  } catch {}
}

async function handleResetPassword(row: any) {
  try {
    await ElMessageBox.confirm(`确定重置用户 ${row.name} 的密码？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    await resetPassword(row.id)
    ElMessage.success('密码已重置')
  } catch {}
}

// 权限分配相关函数
async function openPermissionDialog(row: any) {
  permissionDialogVisible.value = true
  permissionLoading.value = true
  currentUserId.value = row.id
  
  try {
    // 获取所有权限
    const permissionsRes: any = await getAllPermissions()
    allPermissions.value = permissionsRes.data || []
    
    // 获取用户当前权限
    const userPermsRes: any = await getUserPermissionCodes(row.id)
    userPermissions.value = userPermsRes.data || []
  } catch (error) {
    console.error('获取权限失败:', error)
    ElMessage.error('获取权限失败')
  } finally {
    permissionLoading.value = false
  }
}

function getPermissionModules() {
  const modules = new Set<string>()
  allPermissions.value.forEach(perm => modules.add(perm.module))
  return Array.from(modules)
}

function moduleLabel(module: string) {
  const map: Record<string, string> = {
    user: '用户管理',
    project: '项目管理',
    report: '报表管理',
    leave: '请假管理',
    workday: '工作日管理',
    dashboard: '仪表盘',
    permission: '权限管理'
  }
  return map[module] || module
}

function getPermissionsByModule(module: string) {
  return allPermissions.value.filter(perm => perm.module === module)
}

function isModuleChecked(module: string) {
  const modulePermissions = getPermissionsByModule(module)
  return modulePermissions.every(perm => userPermissions.value.includes(perm.code))
}

function isModuleIndeterminate(module: string) {
  const modulePermissions = getPermissionsByModule(module)
  const checkedCount = modulePermissions.filter(perm => userPermissions.value.includes(perm.code)).length
  return checkedCount > 0 && checkedCount < modulePermissions.length
}

function handleModuleCheck(module: string, checked: boolean) {
  const modulePermissions = getPermissionsByModule(module)
  const modulePermissionCodes = modulePermissions.map(perm => perm.code)
  
  if (checked) {
    // 全选该模块的所有权限
    modulePermissionCodes.forEach(code => {
      if (!userPermissions.value.includes(code)) {
        userPermissions.value.push(code)
      }
    })
  } else {
    // 取消选择该模块的所有权限
    userPermissions.value = userPermissions.value.filter(code => !modulePermissionCodes.includes(code))
  }
}

async function handlePermissionSubmit() {
  permissionSubmitLoading.value = true
  
  try {
    await assignUserPermissions(currentUserId.value, userPermissions.value)
    ElMessage.success('权限分配成功')
    permissionDialogVisible.value = false
  } catch (error) {
    console.error('权限分配失败:', error)
    ElMessage.error('权限分配失败')
  } finally {
    permissionSubmitLoading.value = false
  }
}

onMounted(() => {
  fetchUsers()
  fetchLeaders()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.page-desc {
  color: $text-secondary;
  font-size: 13px;
  margin: 4px 0 0;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-cell-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: $brand-gradient-subtle;
  border: 1px solid $border-color-brand;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: $brand-primary;
  flex-shrink: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

/* 权限分配对话框样式 */
.permission-space {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  align-items: start;
  justify-items: stretch;
}

.permission-module {
  border: 1px solid $border-color;
  border-radius: 6px;
  overflow: hidden;
  width: 100%;
  min-height: 200px;
}

.module-header {
  padding: 12px 16px;
  background-color: $bg-card-elevated;
  border-bottom: 1px solid $border-color;
  font-weight: 600;
  height: 48px;
  display: flex;
  align-items: center;
}

.module-permissions {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
