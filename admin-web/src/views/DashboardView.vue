<template>
  <div class="page-card">
    <div class="header-row">
      <h2 class="page-title">首页</h2>
      <el-button text @click="loadData">刷新数据</el-button>
    </div>

    <el-skeleton :loading="loading" animated :rows="4">
      <el-row :gutter="16">
        <el-col
          v-for="item in cards"
          :key="item.label"
          :span="authStore.role === 'ADMIN' ? 8 : 6"
        >
          <div class="stat-card">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
          </div>
        </el-col>
      </el-row>
    </el-skeleton>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { getMerchantDashboard, getPendingActivities } from '../api/activity'
import { getAdminUsers, getAdminMerchants } from '../api/admin'
import { getAdminAnnouncements } from '../api/announcement'
import { getCurrentMerchantInfo } from '../api/merchant'

const authStore = useAuthStore()
const loading = ref(false)
const merchantDashboard = ref({
  activityCount: 0,
  joinedCount: 0,
  pendingCount: 0
})
const merchantInfo = ref({
  avgScore: 0,
  ratingCount: 0
})
const adminSummary = ref({
  pendingCount: 0,
  userCount: 0,
  merchantCount: 0,
  announcementCount: 0
})

const cards = computed(() => {
  if (authStore.role === 'ADMIN') {
    return [
      { label: '待审核活动', value: adminSummary.value.pendingCount },
      { label: '平台用户数', value: adminSummary.value.userCount },
      { label: '平台商户数', value: adminSummary.value.merchantCount }
    ]
  }

  return [
    { label: '活动总数', value: merchantDashboard.value.activityCount },
    { label: '活动参与量', value: merchantDashboard.value.joinedCount },
    { label: '待审核活动', value: merchantDashboard.value.pendingCount },
    { label: '商户评分', value: Number(merchantInfo.value.avgScore || 0).toFixed(1) },
    { label: '评价总数', value: merchantInfo.value.ratingCount || 0 }
  ]
})

async function loadData() {
  loading.value = true
  try {
    if (authStore.role === 'ADMIN') {
      const [pending, users, merchants, announcements] = await Promise.all([
        getPendingActivities(),
        getAdminUsers(),
        getAdminMerchants(),
        getAdminAnnouncements()
      ])
      adminSummary.value = {
        pendingCount: pending.length,
        userCount: users.length,
        merchantCount: merchants.length,
        announcementCount: announcements.length
      }
    } else {
      const [dashboard, info] = await Promise.all([
        getMerchantDashboard(),
        getCurrentMerchantInfo()
      ])
      merchantDashboard.value = dashboard
      merchantInfo.value = info
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 20px;
  margin-bottom: 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, #eff6ff, #ffffff);
  border: 1px solid #dbeafe;
}

.stat-card strong {
  font-size: 32px;
  color: #1d4ed8;
}
</style>
