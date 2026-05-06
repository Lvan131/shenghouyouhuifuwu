<template>
  <div class="profile-page">
    <div class="page-card">
      <div class="toolbar">
        <div>
          <h2 class="page-title">我的信息</h2>
          <p class="page-subtitle">维护商户资料</p>
        </div>
        <div class="toolbar-actions">
          <el-tag :type="form.status === 1 ? 'success' : 'info'">
            {{ form.status === 1 ? '正常' : '停用' }}
          </el-tag>
          <el-button @click="loadProfile">刷新</el-button>
        </div>
      </div>

      <el-form
        v-loading="loading"
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
        class="merchant-form"
      >
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入登录账号" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="留空表示不修改密码"
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
        <el-form-item label="联系人">
          <el-input v-model="form.contactName" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="距离(km)">
          <el-input-number v-model="form.distanceKm" :min="0" :step="0.1" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="经度">
          <el-input-number v-model="form.longitude" :precision="6" :step="0.000001" style="width: 100%" />
        </el-form-item>
        <el-form-item label="纬度">
          <el-input-number v-model="form.latitude" :precision="6" :step="0.000001" style="width: 100%" />
        </el-form-item>
        <el-form-item label="商户简介">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入商户简介" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitForm">保存修改</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentMerchantInfo, updateCurrentMerchantInfo } from '../api/merchant'

const loading = ref(false)
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  username: '',
  password: '',
  merchantName: '',
  merchantType: '餐饮',
  contactName: '',
  contactPhone: '',
  distanceKm: 0,
  address: '',
  longitude: null,
  latitude: null,
  description: '',
  status: 1
})

const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  merchantName: [{ required: true, message: '请输入商户名称', trigger: 'blur' }],
  merchantType: [{ required: true, message: '请选择商户类型', trigger: 'change' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }]
}

function fillForm(data) {
  form.username = data.username || ''
  form.password = ''
  form.merchantName = data.merchantName || ''
  form.merchantType = data.merchantType || '餐饮'
  form.contactName = data.contactName || ''
  form.contactPhone = data.contactPhone || ''
  form.distanceKm = data.distanceKm ?? 0
  form.address = data.address || ''
  form.longitude = data.longitude
  form.latitude = data.latitude
  form.description = data.description || ''
  form.status = data.status ?? 1
}

async function loadProfile() {
  loading.value = true
  try {
    const data = await getCurrentMerchantInfo()
    fillForm(data)
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
      username: form.username,
      password: form.password,
      merchantName: form.merchantName,
      merchantType: form.merchantType,
      contactName: form.contactName,
      contactPhone: form.contactPhone,
      distanceKm: form.distanceKm === null || form.distanceKm === undefined ? null : Number(form.distanceKm),
      address: form.address,
      longitude: form.longitude,
      latitude: form.latitude,
      description: form.description
    }
    const data = await updateCurrentMerchantInfo(payload)
    fillForm(data)
    ElMessage.success('商户信息已更新')
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error.message || '保存失败')
    }
  } finally {
    submitting.value = false
  }
}

loadProfile()
</script>

<style scoped>
.profile-page {
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

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-subtitle {
  margin: 8px 0 0;
  color: #6b7280;
}

.merchant-form {
  max-width: 720px;
}
</style>
