import { createRouter, createWebHashHistory } from 'vue-router'
import LandingView from '../views/LandingView.vue'
import { getToken } from '../api/request'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      name: 'landing',
      component: LandingView,
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
    },
    {
      path: '/workbench',
      name: 'workbench',
      component: () => import('../views/WorkbenchView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('../views/DashboardView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach((to, _from, next) => {
  if (to.meta.requiresAuth && !getToken()) {
    next({ name: 'login' })
  } else if (to.name === 'login' && getToken()) {
    next({ name: 'workbench' })
  } else {
    next()
  }
})

export default router
