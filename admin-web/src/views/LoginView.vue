<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="panel-copy">
        <h1>校园生活优惠服务平台</h1>
        <p>商家端与管理员端统一入口</p>
      </div>

      <el-form :model="form" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
          />
        </el-form-item>
        <el-button type="primary" class="submit-btn" @click="handleLogin">
          登录
        </el-button>
        <p class="tips">
          输入 <code>admin</code> 登录管理员端，其余商家账号登录商家端。
        </p>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  username: '',
  password: ''
})

async function handleLogin() {
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error) {
    ElMessage.error(error.message || '登录失败，请检查账号、密码和后端服务')
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.35), transparent 30%),
    linear-gradient(135deg, #e0f2fe 0%, #eff6ff 48%, #dbeafe 100%);
}

.login-panel {
  width: 420px;
  padding: 32px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 20px 40px rgba(37, 99, 235, 0.15);
}

.panel-copy h1 {
  margin: 0 0 12px;
}

.panel-copy p {
  margin: 0 0 24px;
  color: #6b7280;
}

.submit-btn {
  width: 100%;
}

.tips {
  margin: 12px 0 0;
  color: #6b7280;
  font-size: 12px;
}
</style>
