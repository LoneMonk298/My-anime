import { createRouter, createWebHistory } from 'vue-router'
import BackEndLayout from '@/components/backendlayout.vue'
import AuthLayout from '@/components/Authlayout.vue'

const clearAuthState = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('privacyAccepted')
}

const getRoleType = () => {
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return Number(userInfo.roleType ?? userInfo.userType)
  } catch {
    return NaN
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
    redirect: '/user/articles',
    component: BackEndLayout,
    meta: {
      requiresAuth: true,
      requiresAdmin: true,
    },
    children: [
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

  if (!token) {
    if (to.meta.requiresAuth || to.path.startsWith('/user')) {
      next({ path: '/auth/login', query: { redirect: to.fullPath }, replace: true })
      return
    }

    next()
    return
  }

  const roleType = getRoleType()
  if (Number.isNaN(roleType)) {
    clearAuthState()
    next({ path: '/auth/login', replace: true })
    return
  }

  if (to.path === '/auth/login') {
    next({ path: roleType === 2 ? '/user/articles' : '/', replace: true })
    return
  }

  if (to.meta.requiresAdmin && roleType !== 2) {
    next({ path: '/', replace: true })
    return
  }

  next()
})

export default router
