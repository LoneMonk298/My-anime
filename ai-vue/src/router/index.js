import { createRouter, createWebHistory } from 'vue-router'
import BackEndLayout from '@/components/backendlayout.vue'
import AuthLayout from '@/components/Authlayout.vue'
import {
  canUseLoginRedirect,
  hasKnownRole,
  isAdminRole,
  resolveHomePath,
} from '@/utils/authRole'

const clearAuthState = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('privacyAccepted')
}

const getStoredUserInfo = () => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch {
    return {}
  }
}

const routes = [
  {
    path: '/',
    name: 'AnimeHome',
    component: () => import('@/views/anime.vue'),
    meta: {
      public: true,
      title: '二次元记录站',
    },
  },
  {
    path: '/anime',
    redirect: '/',
  },
  {
    path: '/article/:id',
    name: 'ArticleDetail',
    component: () => import('@/views/articleDetail.vue'),
    meta: {
      public: true,
      title: '文章详情',
    },
  },
  {
    path: '/auth',
    component: AuthLayout,
    children: [
      {
        path: 'login',
        name: 'Login',
        component: () => import('@/views/login.vue'),
        meta: {
          title: '后台登录',
          public: true,
        },
      },
    ],
  },
  {
    path: '/user',
    redirect: '/user/dashboard',
    component: BackEndLayout,
    meta: {
      requiresAuth: true,
      requiresAdmin: true,
    },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: {
          title: '仪表盘',
          icon: 'DataBoard',
          requiresAuth: true,
          requiresAdmin: true,
        },
      },
      {
        path: 'articles',
        name: 'ArticleAdmin',
        component: () => import('@/views/articles.vue'),
        meta: {
          title: '记录文章',
          icon: 'Notebook',
          requiresAuth: true,
          requiresAdmin: true,
        },
      },
      {
        path: 'resources',
        name: 'ResourceAdmin',
        component: () => import('@/views/admin/Resources.vue'),
        meta: {
          title: '资源管理',
          icon: 'FolderOpened',
          requiresAuth: true,
          requiresAdmin: true,
        },
      },
      {
        path: 'users',
        name: 'UserAdmin',
        component: () => import('@/views/admin/Users.vue'),
        meta: {
          title: '用户管理',
          icon: 'User',
          requiresAuth: true,
          requiresAdmin: true,
        },
      },
      {
        path: 'settings',
        name: 'SystemSettings',
        component: () => import('@/views/admin/Settings.vue'),
        meta: {
          title: '系统设置',
          icon: 'Setting',
          requiresAuth: true,
          requiresAdmin: true,
        },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  document.title = to.meta.title || '二次元记录站'

  if (!token) {
    if (to.meta.requiresAuth || to.path.startsWith('/user')) {
      next({ path: '/auth/login', query: { redirect: to.fullPath }, replace: true })
      return
    }

    next()
    return
  }

  if (to.path === '/auth/login' && to.query.forceLogin === '1') {
    clearAuthState()
    next()
    return
  }

  const userInfo = getStoredUserInfo()
  if (!hasKnownRole(userInfo)) {
    clearAuthState()
    if (to.path === '/auth/login') {
      next()
      return
    }
    next({ path: '/auth/login', query: { redirect: to.fullPath }, replace: true })
    return
  }

  if (to.path === '/auth/login') {
    const redirect = canUseLoginRedirect(to.query.redirect, userInfo)
      ? to.query.redirect
      : resolveHomePath(userInfo)
    next({ path: redirect, replace: true })
    return
  }

  if (to.meta.requiresAdmin && !isAdminRole(userInfo)) {
    next({ path: '/', replace: true })
    return
  }

  next()
})

export default router
