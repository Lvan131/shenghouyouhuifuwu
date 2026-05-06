<template>
  <div class="rating-page">
    <div class="page-card summary-card">
      <div class="toolbar">
        <div>
          <h2 class="page-title">评分与评论</h2>
          <p class="page-subtitle">查看用户对当前商户的评分与评价</p>
        </div>
        <el-button @click="loadAll">刷新</el-button>
      </div>

      <el-row :gutter="16" class="summary-grid">
        <el-col :span="8">
          <div class="summary-item">
            <div class="summary-label">当前评分</div>
            <div class="summary-value">{{ Number(profile.avgScore || 0).toFixed(1) }}</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="summary-item">
            <div class="summary-label">评论总数</div>
            <div class="summary-value">{{ profile.ratingCount || 0 }}</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="summary-item">
            <div class="summary-label">账号状态</div>
            <div class="summary-tag">
              <el-tag :type="profile.status === 1 ? 'success' : 'info'">
                {{ profile.status === 1 ? '正常' : '停用' }}
              </el-tag>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <div class="page-card">
      <div class="toolbar">
        <div>
          <h3 class="section-title">用户评论</h3>
          <p class="page-subtitle">仅展示当前商户收到的评价</p>
        </div>
      </div>

      <el-table v-loading="ratingLoading" :data="ratings">
        <el-table-column label="用户" min-width="160">
          <template #default="{ row }">
            <div class="user-name">{{ row.userName }}</div>
            <div class="user-meta">{{ row.userNo || '未填写学工号' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="activityTitle" label="关联活动" min-width="180" />
        <el-table-column label="评分" width="100">
          <template #default="{ row }">
            <el-tag type="warning">{{ row.score }} 分</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评论内容" min-width="260" show-overflow-tooltip />
        <el-table-column prop="participationDate" label="参与日期" width="120" />
        <el-table-column prop="updatedAt" label="最近更新" min-width="180" />
      </el-table>

      <el-empty v-if="!ratingLoading && !ratings.length" description="当前暂无用户评论" />
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentMerchantInfo, getCurrentMerchantRatings } from '../api/merchant'

const ratingLoading = ref(false)
const summaryLoading = ref(false)
const ratings = ref([])

const profile = reactive({
  avgScore: 0,
  ratingCount: 0,
  status: 1
})

async function loadProfileSummary() {
  summaryLoading.value = true
  try {
    const data = await getCurrentMerchantInfo()
    profile.avgScore = data.avgScore ?? 0
    profile.ratingCount = data.ratingCount ?? 0
    profile.status = data.status ?? 1
  } catch (error) {
    ElMessage.error(error.message || '加载评分统计失败')
  } finally {
    summaryLoading.value = false
  }
}

async function loadRatings() {
  ratingLoading.value = true
  try {
    ratings.value = await getCurrentMerchantRatings()
  } catch (error) {
    ElMessage.error(error.message || '加载评论失败')
  } finally {
    ratingLoading.value = false
  }
}

async function loadAll() {
  await Promise.all([loadProfileSummary(), loadRatings()])
}

loadAll()
</script>

<style scoped>
.rating-page {
  display: grid;
  gap: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-subtitle {
  margin: 8px 0 0;
  color: #6b7280;
}

.summary-grid {
  margin-top: 8px;
}

.summary-item {
  padding: 18px;
  border-radius: 16px;
  background: linear-gradient(135deg, #eff6ff, #ffffff);
  border: 1px solid #dbeafe;
}

.summary-label {
  color: #6b7280;
}

.summary-value {
  margin-top: 10px;
  font-size: 30px;
  font-weight: 700;
  color: #1d4ed8;
}

.summary-tag {
  margin-top: 12px;
}

.section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.user-name {
  color: #111827;
  font-weight: 600;
}

.user-meta {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
}
</style>
