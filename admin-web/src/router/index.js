import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import MerchantActivityView from '../views/MerchantActivityView.vue'
import MerchantProfileView from '../views/MerchantProfileView.vue'
import MerchantRatingView from '../views/MerchantRatingView.vue'
import UserManagementView from '../views/UserManagementView.vue'
import MerchantManagementView from '../views/MerchantManagementView.vue'
import ActivityAuditView from '../views/ActivityAuditView.vue'
import AnnouncementManagementView from '../views/AnnouncementManagementView.vue'
import MainLayout from '../layout/MainLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      {
        path: '/dashboard',
        name: 'dashboard',
        component: DashboardView,
        meta: { title: '首页', roles: ['ADMIN', 'MERCHANT'] }
      },
      {
        path: '/merchant-activities',
        name: 'merchant-activities',
        component: MerchantActivityView,
        meta: { title: '活动管理', roles: ['MERCHANT'] }
      },
      {
        path: '/merchant-profile',
        name: 'merchant-profile',
        component: MerchantProfileView,
        meta: { title: '我的信息', roles: ['MERCHANT'] }
      },
      {
        path: '/merchant-ratings',
        name: 'merchant-ratings',
        component: MerchantRatingView,
        meta: { title: '评分与评论', roles: ['MERCHANT'] }
      },
      {
        path: '/users',
        name: 'users',
        component: UserManagementView,
        meta: { title: '用户管理', roles: ['ADMIN'] }
      },
      {
        path: '/merchants',
        name: 'merchants',
        component: MerchantManagementView,
        meta: { title: '商户管理', roles: ['ADMIN'] }
      },
      {
        path: '/activity-audit',
        name: 'activity-audit',
        component: ActivityAuditView,
        meta: { title: '活动审核', roles: ['ADMIN'] }
      },
      {
        path: '/announcements',
        name: 'announcements',
        component: AnnouncementManagementView,
        meta: { title: '公告管理', roles: ['ADMIN'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  if (to.path === '/login') {
    next()
    return
  }

  if (!authStore.isLoggedIn) {
    next('/login')
    return
  }

  const roles = to.meta.roles
  if (roles && !roles.includes(authStore.role)) {
    next('/dashboard')
    return
  }

  next()
})

export default router
