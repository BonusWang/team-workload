import request from '@/utils/request'

export const login = (data: { username: string; password: string }) =>
  request.post('/auth/login', data)

export const getUserInfo = () => request.get('/auth/info')

export const changePassword = (data: { oldPassword: string; newPassword: string }) => request.post('/auth/change-password', data)

export const getUsers = (params?: any) => request.get('/user/list', { params })

export const createUser = (data: any) => request.post('/user', data)

export const updateUser = (id: number, data: any) => request.put(`/user/${id}`, data)

export const updateUserStatus = (id: number, status: number) =>
  request.put(`/user/${id}/status`, { status })

export const resetPassword = (id: number, password?: string) =>
  request.put(`/user/${id}/password`, password ? { password } : {})

export const getLeaders = () => request.get('/user/leaders')

export const getWorkDays = (year: number, month?: number) =>
  request.get('/work-day/list', { params: { year, month } })

export const syncWorkDays = (year: number) =>
  request.post('/work-day/sync', null, { params: { year } })

export const updateWorkDay = (id: number, data: { isWorkday: number; type: string; description?: string }) =>
  request.put(`/work-day/${id}`, null, { params: { ...data } })

export const getCurrentWeekly = () => request.get('/report/weekly/current')

export const getHistoryWeekly = () => request.get('/report/weekly/history')

export const submitWeekly = (data: { nextWeekPlans?: string; notes?: string }) => request.post('/report/weekly/submit', data)

export const submitWeeklyForUser = (targetUserId: number, data: { nextWeekPlans?: string; notes?: string }) => request.post(`/report/weekly/submit/${targetUserId}`, data)

export const revokeWeekly = (id: number) => request.put(`/report/weekly/${id}/revoke`)

export const getTeamWeeklyOverview = (params?: any) =>
  request.get('/report/weekly/team/overview', { params })

export const getTeamWeeklyRanking = (params?: any) =>
  request.get('/report/weekly/team/ranking', { params })

export const getTeamWeeklyDetails = (startDate?: string, endDate?: string) => 
  request.get('/report/weekly/team/details', { params: { startDate, endDate } })

export const exportTeamWeeklyDetails = (startDate?: string, endDate?: string) => 
  request.get('/report/weekly/team/details/export', { params: { startDate, endDate }, responseType: 'blob' })

export const updateWeeklyDetails = (id: number, field: string, value: string) => request.put(`/report/weekly/${id}/details`, null, { params: { field, value } })

export const updateWeeklyDetailsByUserId = (userId: number, field: string, value: string) => request.put(`/report/weekly/user/${userId}/details`, null, { params: { field, value } })

export const updateCurrentWeeklyDetails = (field: string, value: string) => request.put('/report/weekly/current/update', null, { params: { field, value } })

export const getMonthlySummary = (yearMonth?: string) =>
  request.get('/report/monthly/summary', { params: { yearMonth } })

export const getMonthlyHistory = () => request.get('/report/monthly/history')

export const exportMonthly = (yearMonth: string) =>
  window.open(`/api/report/monthly/export?yearMonth=${yearMonth}`, '_blank')

export const getTeamMonthlyDetails = (yearMonth?: string) =>
  request.get('/report/monthly/team/details', { params: { yearMonth } })

export const exportTeamMonthlyDetails = (yearMonth?: string) =>
  request.get('/report/monthly/team/details/export', { 
    params: { yearMonth }, 
    responseType: 'blob' 
  })

export const submitLeave = (data: any) => request.post('/leave', data)

export const getMyLeaves = () => request.get('/leave/my')

export const approveLeave = (id: number) => request.put(`/leave/${id}/approve`)

export const rejectLeave = (id: number, reason: string) =>
  request.put(`/leave/${id}/reject`, null, { params: { reason } })

export const getPendingLeaves = () => request.get('/leave/pending')

export const getHistoryLeaves = (params?: any) => request.get('/leave/history', { params })

export const getLeaveBalance = () => request.get('/leave/balance')

export const revokeLeave = (id: number) => request.put(`/leave/${id}/revoke`)

export const getMonthlyLeaveStats = (year: number, month: number) =>
  request.get('/leave/stats/monthly', { params: { year, month } })

export const getPersonalDashboard = () => request.get('/dashboard/personal')

export const getCalendarData = (yearMonth?: string) =>
  request.get('/dashboard/calendar', { params: { yearMonth } })

export const getTeamGapAnalysis = () => request.get('/team/analysis/gap')

export const getAllPermissions = () => request.get('/permission/list')
export const getUserPermissions = (userId: number) => request.get(`/permission/user/${userId}`)
export const getUserPermissionCodes = (userId: number) => request.get(`/permission/user/${userId}/codes`)
export const assignUserPermissions = (userId: number, permissionCodes: string[]) => request.post(`/permission/user/${userId}`, permissionCodes)

export const submitDailyReport = (data: any) => request.post('/daily/submit', data)

export const getDailyList = (params?: any) => request.get('/daily/list', { params })

export const getProjectList = () => request.get('/project/list')
