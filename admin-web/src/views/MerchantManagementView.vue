<template>
  <div class="page-card">
    <div class="toolbar">
      <h2 class="page-title">商户管理</h2>
      <div class="toolbar-actions">
        <el-button type="primary" @click="openCreateDialog">新增商户</el-button>
        <el-button @click="loadMerchants">刷新</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="merchants">
      <el-table-column prop="merchantName" label="商户名称" min-width="180" />
      <el-table-column prop="username" label="登录账号" min-width="130" />
      <el-table-column prop="merchantType" label="商户类型" width="120" />
      <el-table-column label="评分" width="100">
        <template #default="{ row }">
          {{ formatScore(row.avgScore) }}
        </template>
      </el-table-column>
      <el-table-column label="评价数" width="100">
        <template #default="{ row }">
          {{ row.ratingCount ?? 0 }}
        </template>
      </el-table-column>
      <el-table-column label="距离(km)" width="110">
        <template #default="{ row }">
          {{ row.distanceKm ?? '--' }}
        </template>
      </el-table-column>
      <el-table-column prop="address" label="地址" min-width="220" />
      <el-table-column prop="contactPhone" label="联系电话" min-width="140" />
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
          <el-button link type="danger" @click="removeMerchant(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="editingMerchantId ? '编辑商户' : '新增商户'"
      width="620px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入商户登录账号" />
        </el-form-item>
        <el-form-item label="登录密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="editingMerchantId ? '留空则保持原密码' : '请输入登录密码'"
          />
        </el-form-item>
        <el-form-item label="商户名称" prop="merchantName">
          <el-input v-model="form.merchantName" placeholder="请输入商户名称" />
        </el-form-item>
        <el-form-item label="商户类型" prop="merchantType">
          <el-select v-model="form.merchantType" placeholder="请选择商户类型" style="width: 100%">
            <el-option label="餐饮" value="餐饮" />
            <el-option label="零售" value="零售" />
            <el-option label="生活服务" value="生活服务" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="距离(km)" prop="distanceKm">
          <el-input-number v-model="form.distanceKm" :min="0" :step="0.1" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="经度" prop="longitude">
          <el-input-number v-model="form.longitude" :precision="6" :step="0.000001" style="width: 100%" />
        </el-form-item>
        <el-form-item label="纬度" prop="latitude">
          <el-input-number v-model="form.latitude" :precision="6" :step="0.000001" style="width: 100%" />
        </el-form-item>
        <el-form-item label="商户简介" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入商户简介" />
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
  createAdminMerchant,
  deleteAdminMerchant,
  getAdminMerchants,
  updateAdminMerchant
} from '../api/admin'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingMerchantId = ref(null)
const formRef = ref()
const merchants = ref([])

const form = reactive(createDefaultForm())

const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  merchantName: [{ required: true, message: '请输入商户名称', trigger: 'blur' }],
  merchantType: [{ required: true, message: '请选择商户类型', trigger: 'change' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  password: [
    {
      validator: (_, value, callback) => {
        if (!editingMerchantId.value && !value) {
          callback(new Error('请输入登录密码'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

function createDefaultForm() {
  return {
    username: '',
    password: '',
    merchantName: '',
    merchantType: '餐饮',
    contactName: '',
    contactPhone: '',
    distanceKm: 0.3,
    address: '',
    longitude: null,
    latitude: null,
    description: '',
    status: 1
  }
}

function resetForm() {
  Object.assign(form, createDefaultForm())
}

function formatScore(score) {
  const value = Number(score)
  if (!Number.isFinite(value) || value <= 0) {
    return '--'
  }
  return value.toFixed(1)
}

function openCreateDialog() {
  editingMerchantId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  editingMerchantId.value = row.merchantId
  Object.assign(form, {
    username: row.username || '',
    password: '',
    merchantName: row.merchantName || '',
    merchantType: row.merchantType || '餐饮',
    contactName: row.contactName || '',
    contactPhone: row.contactPhone || '',
    distanceKm: row.distanceKm ?? 0,
    address: row.address || '',
    longitude: row.longitude,
    latitude: row.latitude,
    description: row.description || '',
    status: row.status ?? 1
  })
  dialogVisible.value = true
}

async function loadMerchants() {
  loading.value = true
  try {
    merchants.value = await getAdminMerchants()
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
    const payload = {
      ...form,
      distanceKm: form.distanceKm === null || form.distanceKm === undefined ? null : Number(form.distanceKm)
    }
    if (editingMerchantId.value) {
      await updateAdminMerchant(editingMerchantId.value, payload)
      ElMessage.success('商户已更新')
    } else {
      await createAdminMerchant(payload)
      ElMessage.success('商户已创建')
    }
    dialogVisible.value = false
    await loadMerchants()
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error.message || '保存失败')
    }
  } finally {
    submitting.value = false
  }
}

async function removeMerchant(row) {
  try {
    await ElMessageBox.confirm(`确认删除商户“${row.merchantName}”吗？`, '删除商户', {
      type: 'warning'
    })
    await deleteAdminMerchant(row.merchantId)
    ElMessage.success('商户已删除')
    await loadMerchants()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

loadMerchants()
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
