<template>
  <div class="page-card">
    <div class="toolbar">
      <h2 class="page-title">活动审核</h2>
      <div class="toolbar-actions">
        <el-radio-group v-model="auditFilter" size="default">
          <el-radio-button label="ALL">全部</el-radio-button>
          <el-radio-button label="PENDING">待审核</el-radio-button>
          <el-radio-button label="REVIEWED">已审核</el-radio-button>
        </el-radio-group>
        <el-button @click="loadActivities">刷新</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="filteredActivities">
      <el-table-column prop="title" label="活动标题" min-width="220" />
      <el-table-column prop="merchantName" label="商家" min-width="180" />
      <el-table-column prop="activityType" label="活动类型" width="120" />
      <el-table-column prop="createdAt" label="提交时间" min-width="180" />
      <el-table-column label="审核状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.auditStatus === 'PENDING' ? 'warning' : 'success'">
            {{ row.auditStatus === 'PENDING' ? '待审核' : '已审核' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="处理结果" width="120">
        <template #default="{ row }">
          <el-tag :type="resultTypeMap[row.status] || 'info'">
            {{ resultLabelMap[row.status] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="auditRemark" label="审核备注" min-width="200" />
      <el-table-column prop="auditedAt" label="审核时间" min-width="180" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetailDialog(row)">查看详情</el-button>
          <template v-if="row.auditStatus === 'PENDING'">
            <el-button link type="success" @click="submitAudit(row, 'APPROVED')">通过</el-button>
            <el-button link type="danger" @click="openRejectDialog(row)">驳回</el-button>
          </template>
          <span v-else class="reviewed-text">已处理</span>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="detailVisible" title="审核详情" width="720px">
      <div v-loading="detailLoading" class="detail-wrap">
        <template v-if="detailData && currentRow">
          <div class="detail-header">
            <div>
              <div class="detail-title">{{ detailData.activity.title }}</div>
              <div class="detail-subtitle">
                {{ detailData.merchant.merchantName }} · {{ detailData.activity.activityType }}
              </div>
            </div>
            <el-tag :type="currentRow.auditStatus === 'PENDING' ? 'warning' : 'success'">
              {{ currentRow.auditStatus === 'PENDING' ? '待审核' : '已审核' }}
            </el-tag>
          </div>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="商家名称">{{ detailData.merchant.merchantName }}</el-descriptions-item>
            <el-descriptions-item label="商家类型">{{ detailData.merchant.merchantType }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ currentRow.createdAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核时间">{{ currentRow.auditedAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="活动开始">{{ detailData.activity.startTime }}</el-descriptions-item>
            <el-descriptions-item label="活动结束">{{ detailData.activity.endTime }}</el-descriptions-item>
            <el-descriptions-item label="活动名额">{{ detailData.activity.quota || '不限' }}</el-descriptions-item>
            <el-descriptions-item label="已参与人数">{{ detailData.activity.joinedCount }}</el-descriptions-item>
            <el-descriptions-item label="商家地址" :span="2">{{ detailData.merchant.address }}</el-descriptions-item>
            <el-descriptions-item label="联系电话" :span="2">
              {{ detailData.merchant.contactPhone || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="活动说明" :span="2">
              {{ detailData.activity.content }}
            </el-descriptions-item>
            <el-descriptions-item label="审核备注" :span="2">
              {{ currentRow.auditRemark || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </template>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <template v-if="currentRow?.auditStatus === 'PENDING'">
          <el-button type="success" :loading="submitting" @click="submitAudit(currentRow, 'APPROVED')">审核通过</el-button>
          <el-button type="danger" :loading="submitting" @click="openRejectDialog(currentRow)">驳回</el-button>
        </template>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" title="驳回活动" width="520px">
      <el-form :model="auditForm" label-width="100px">
        <el-form-item label="活动标题">
          <el-input :model-value="currentRow?.title || ''" disabled />
        </el-form-item>
        <el-form-item label="驳回原因">
          <el-input
            v-model="auditForm.auditRemark"
            type="textarea"
            :rows="4"
            placeholder="请输入驳回原因"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { auditActivity, getActivityDetail, getAuditActivities } from '../api/activity'

const loading = ref(false)
const detailLoading = ref(false)
const submitting = ref(false)
const activities = ref([])
const auditFilter = ref('ALL')
const dialogVisible = ref(false)
const detailVisible = ref(false)
const currentRow = ref(null)
const detailData = ref(null)
const auditForm = reactive({
  auditRemark: ''
})

const resultLabelMap = {
  PENDING: '待处理',
  APPROVED: '审核通过',
  REJECTED: '已驳回',
  OFFLINE: '已下架',
  FINISHED: '已结束'
}

const resultTypeMap = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  OFFLINE: 'info',
  FINISHED: ''
}

const filteredActivities = computed(() => {
  if (auditFilter.value === 'ALL') {
    return activities.value
  }
  return activities.value.filter((item) => item.auditStatus === auditFilter.value)
})

async function loadActivities() {
  loading.value = true
  try {
    activities.value = await getAuditActivities()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function openDetailDialog(row) {
  currentRow.value = row
  detailVisible.value = true
  detailLoading.value = true
  try {
    detailData.value = await getActivityDetail(row.activityId)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    detailLoading.value = false
  }
}

function openRejectDialog(row) {
  currentRow.value = row
  auditForm.auditRemark = ''
  dialogVisible.value = true
}

async function submitAudit(row, auditResult, auditRemark = '内容合规') {
  submitting.value = true
  try {
    await auditActivity(row.activityId, {
      auditResult,
      auditRemark
    })
    ElMessage.success(auditResult === 'APPROVED' ? '审核通过' : '已驳回')
    dialogVisible.value = false
    detailVisible.value = false
    await loadActivities()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    submitting.value = false
  }
}

async function confirmReject() {
  if (!auditForm.auditRemark) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  await submitAudit(currentRow.value, 'REJECTED', auditForm.auditRemark)
}

loadActivities()
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.reviewed-text {
  color: #94a3b8;
}

.detail-wrap {
  min-height: 220px;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.detail-title {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
}

.detail-subtitle {
  margin-top: 8px;
  color: #6b7280;
}
</style>
