<template>
  <div class="page-card">
    <div class="toolbar">
      <h2 class="page-title">公告管理</h2>
      <div class="toolbar-actions">
        <el-button @click="loadAnnouncements">刷新</el-button>
        <el-button type="primary" @click="openCreateDialog">发布公告</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="announcements">
      <el-table-column prop="title" label="标题" min-width="220" />
      <el-table-column prop="content" label="内容" min-width="320" show-overflow-tooltip />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '已发布' : '已下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="publishedAt" label="发布时间" min-width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑公告' : '发布公告'" width="620px">
      <el-form label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            placeholder="请输入公告内容"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">已发布</el-radio>
            <el-radio :label="0">已下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAnnouncement">
          {{ editingId ? '保存' : '发布' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createAnnouncement,
  getAdminAnnouncements,
  updateAnnouncement
} from '../api/announcement'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const announcements = ref([])
const form = reactive(createDefaultForm())

function createDefaultForm() {
  return {
    title: '',
    content: '',
    status: 1
  }
}

function resetForm() {
  Object.assign(form, createDefaultForm())
}

function openCreateDialog() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  editingId.value = row.announcementId
  Object.assign(form, {
    title: row.title || '',
    content: row.content || '',
    status: row.status ?? 1
  })
  dialogVisible.value = true
}

async function loadAnnouncements() {
  loading.value = true
  try {
    announcements.value = await getAdminAnnouncements()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function submitAnnouncement() {
  if (!form.title || !form.content) {
    ElMessage.warning('请完整填写公告信息')
    return
  }
  submitting.value = true
  try {
    const payload = {
      title: form.title,
      content: form.content,
      status: form.status
    }
    if (editingId.value) {
      await updateAnnouncement(editingId.value, payload)
      ElMessage.success('公告已更新')
    } else {
      await createAnnouncement(payload)
      ElMessage.success('公告发布成功')
    }
    dialogVisible.value = false
    await loadAnnouncements()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    submitting.value = false
  }
}

loadAnnouncements()
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
}
</style>
