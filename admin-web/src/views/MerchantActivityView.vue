<template>
  <div class="page-card">
    <div class="toolbar">
      <h2 class="page-title">活动管理</h2>
      <div class="toolbar-actions">
        <el-button @click="loadActivities">刷新</el-button>
        <el-button type="primary" @click="openCreateDialog">新增活动</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="activities">
      <el-table-column prop="title" label="活动标题" min-width="220" />
      <el-table-column prop="activityType" label="活动类型" width="120" />
      <el-table-column prop="quota" label="总名额" width="110" />
      <el-table-column prop="dailyQuota" label="每日名额" width="120" />
      <el-table-column prop="joinedCount" label="已报名人数" width="120" />
      <el-table-column prop="status" label="状态" width="140">
        <template #default="{ row }">
          <el-tag :type="statusTypeMap[row.status] || 'info'">
            {{ statusLabelMap[row.status] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" min-width="180" prop="startTime" />
      <el-table-column label="结束时间" min-width="180" prop="endTime" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleOffline(row)">下架</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEditing ? '编辑活动' : '新增活动'" width="680px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="活动标题">
          <el-input v-model="form.title" placeholder="请输入活动标题" />
        </el-form-item>
        <el-form-item label="活动类型">
          <el-select v-model="form.activityType" placeholder="请选择活动类型" style="width: 100%">
            <el-option label="折扣" value="折扣" />
            <el-option label="满减" value="满减" />
            <el-option label="团购" value="团购" />
            <el-option label="报名活动" value="报名活动" />
          </el-select>
        </el-form-item>
        <el-form-item label="活动介绍">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请输入活动介绍" />
        </el-form-item>
        <el-form-item label="总名额">
          <el-input-number v-model="form.quota" :min="1" :max="99999" />
        </el-form-item>
        <el-form-item label="每日名额">
          <el-input-number v-model="form.dailyQuota" :min="1" :max="99999" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="请选择开始时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="请选择结束时间"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createMerchantActivity,
  getMerchantActivities,
  offlineMerchantActivity,
  updateMerchantActivity
} from '../api/activity'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const activities = ref([])

const statusLabelMap = {
  APPROVED: '审核通过',
  PENDING: '待审核',
  REJECTED: '已驳回',
  OFFLINE: '已下架',
  FINISHED: '已结束'
}

const statusTypeMap = {
  APPROVED: 'success',
  PENDING: 'warning',
  REJECTED: 'danger',
  OFFLINE: 'info',
  FINISHED: ''
}

const form = reactive({
  title: '',
  activityType: '',
  content: '',
  quota: 1,
  dailyQuota: 1,
  startTime: '',
  endTime: ''
})

function resetForm() {
  form.title = ''
  form.activityType = ''
  form.content = ''
  form.quota = 1
  form.dailyQuota = 1
  form.startTime = ''
  form.endTime = ''
}

async function loadActivities() {
  loading.value = true
  try {
    activities.value = await getMerchantActivities()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  resetForm()
  isEditing.value = false
  editingId.value = null
  dialogVisible.value = true
}

function openEditDialog(row) {
  form.title = row.title
  form.activityType = row.activityType
  form.content = row.content || ''
  form.quota = row.quota || 1
  form.dailyQuota = row.dailyQuota || 1
  form.startTime = row.startTime
  form.endTime = row.endTime
  isEditing.value = true
  editingId.value = row.activityId
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.title || !form.activityType || !form.content || !form.startTime || !form.endTime) {
    ElMessage.warning('请完整填写活动信息')
    return
  }
  if (!form.dailyQuota || form.dailyQuota < 1) {
    ElMessage.warning('请设置每日名额')
    return
  }
  if (form.quota && form.dailyQuota > form.quota) {
    ElMessage.warning('每日名额不能大于总名额')
    return
  }
  submitting.value = true
  try {
    const payload = { ...form }
    if (isEditing.value) {
      await updateMerchantActivity(editingId.value, payload)
      ElMessage.success('活动已更新，已重新进入待审核状态')
    } else {
      await createMerchantActivity(payload)
      ElMessage.success('活动已提交，等待管理员审核')
    }
    dialogVisible.value = false
    await loadActivities()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    submitting.value = false
  }
}

async function handleOffline(row) {
  try {
    await ElMessageBox.confirm(`确认下架活动“${row.title}”吗？`, '操作确认', {
      type: 'warning'
    })
    await offlineMerchantActivity(row.activityId)
    ElMessage.success('活动已下架')
    await loadActivities()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作已取消')
    }
  }
}

loadActivities()
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
}
</style>
