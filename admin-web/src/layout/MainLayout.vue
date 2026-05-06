<template>
  <el-container class="layout">
    <el-aside width="240px" class="sidebar">
      <div class="brand">
        <h1>优惠服务平台</h1>
        <p>{{ authStore.role === 'ADMIN' ? '管理员后台' : '商户后台' }}</p>
      </div>

      <el-menu
        :default-active="route.path"
        class="menu"
        background-color="transparent"
        text-color="#dbeafe"
        active-text-color="#ffffff"
        @select="handleSelect"
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="item.path"
        >
          {{ item.label }}
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div>
          <strong>{{ authStore.userInfo?.displayName || '未登录' }}</strong>
          <span class="header-role">{{ authStore.role }}</span>
        </div>
        <el-button link type="danger" @click="logout">退出登录</el-button>
      </el-header>
      <el-main class="main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const menuConfig = {
  ADMIN: [
    { path: '/dashboard', label: '首页' },
    { path: '/users', label: '用户管理' },
    { path: '/merchants', label: '商户管理' },
    { path: '/activity-audit', label: '活动审核' },
    { path: '/announcements', label: '公告管理' }
  ],
  MERCHANT: [
    { path: '/dashboard', label: '首页' },
    { path: '/merchant-profile', label: '我的信息' },
    { path: '/merchant-ratings', label: '评分与评论' },
    { path: '/merchant-activities', label: '活动管理' }
  ]
}

const menuItems = computed(() => menuConfig[authStore.role] || [])

function handleSelect(path) {
  router.push(path)
}

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

.sidebar {
  background: linear-gradient(180deg, #0f172a 0%, #1d4ed8 100%);
  color: #fff;
  padding: 24px 16px;
}

.brand h1 {
  margin: 0;
  font-size: 22px;
}

.brand p {
  margin: 8px 0 20px;
  color: rgba(255, 255, 255, 0.72);
}

.menu {
  border-right: none;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.header-role {
  margin-left: 12px;
  color: #6b7280;
}

.main {
  background: #f3f6fb;
}
</style>
