<template>
  <div class="page-card">
    <div class="toolbar">
      <h2 class="page-title">用户管理</h2>
      <div class="toolbar-actions">
        <el-button type="primary" @click="openCreateDialog">新增用户</el-button>
        <el-button @click="loadUsers">刷新</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="users">
      <el-table-column prop="displayName" label="姓名" min-width="140" />
      <el-table-column prop="userNo" label="学工号" min-width="140" />
      <el-table-column label="类型" width="120">
        <template #default="{ row }">
          {{ formatUserType(row.userType) }}
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="联系电话" min-width="140" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="removeUser(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="editingAccountId ? '编辑用户' : '新增用户'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="学工号" prop="userNo">
          <el-input v-model="form.userNo" placeholder="请输入学号或工号" />
        </el-form-item>
        <el-form-item label="类型" prop="userType">
          <el-select v-model="form.userType" placeholder="请选择类型" style="width: 100%">
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item :label="editingAccountId ? '重置密码' : '登录密码'" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="editingAccountId ? '留空则保持当前密码' : '请输入登录密码'"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">正常</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
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
  createAdminUser,
  deleteAdminUser,
  getAdminUsers,
  updateAdminUser
} from '../api/admin'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingAccountId = ref(null)
const formRef = ref()
const users = ref([])

const validatePassword = (_rule, value, callback) => {
  if (!editingAccountId.value && !value) {
    callback(new Error('请输入登录密码'))
    return
  }
  callback()
}

const form = reactive(createDefaultForm())

const rules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  userNo: [{ required: true, message: '请输入学工号', trigger: 'blur' }],
  userType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

function createDefaultForm() {
  return {
    realName: '',
    userNo: '',
    userType: 'STUDENT',
    phone: '',
    password: '',
    status: 1
  }
}

function resetForm() {
  Object.assign(form, createDefaultForm())
}

function formatUserType(userType) {
  return userType === 'TEACHER' ? '教师' : '学生'
}

function openCreateDialog() {
  editingAccountId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  editingAccountId.value = row.accountId
  Object.assign(form, {
    realName: row.displayName || '',
    userNo: row.userNo || '',
    userType: row.userType || 'STUDENT',
    phone: row.phone || '',
    password: '',
    status: row.status ?? 1
  })
  dialogVisible.value = true
}

async function loadUsers() {
  loading.value = true
  try {
    users.value = await getAdminUsers()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function submitForm() {
  try {
    await formRef.value.validate()
    submitting.value = true
    const payload = { ...form }
    if (editingAccountId.value) {
      await updateAdminUser(editingAccountId.value, payload)
      ElMessage.success('用户已更新')
    } else {
      await createAdminUser(payload)
      ElMessage.success('用户已创建')
    }
    dialogVisible.value = false
    await loadUsers()
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error.message || '保存失败')
    }
  } finally {
    submitting.value = false
  }
}

async function removeUser(row) {
  try {
    await ElMessageBox.confirm(`确认删除用户“${row.displayName}”吗？`, '删除用户', {
      type: 'warning'
    })
    await deleteAdminUser(row.accountId)
    ElMessage.success('用户已删除')
    await loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

loadUsers()
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
