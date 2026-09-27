/**
 * 认证状态管理。
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '../api/auth'
import type { UserInfo } from '../api/auth'
import { getToken, setToken, clearToken } from '../api/request'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserInfo | null>(null)
  const loading = ref(false)

  const isLoggedIn = computed(() => !!getToken())
  const nickname = computed(() => user.value?.nickname || '')
  const plan = computed(() => user.value?.plan || 'free')
  const avatarInitial = computed(() => {
    const name = user.value?.nickname
    return name ? name.charAt(0).toUpperCase() : '?'
  })

  async function fetchMe() {
    if (!getToken()) return
    try {
      user.value = await authApi.getMe()
    } catch {
      clearToken()
    }
  }

  async function sendCode(type: string, target: string) {
    await authApi.sendCode(type, target)
  }

  async function register(
    type: string,
    phone: string,
    code: string,
    password: string,
    nickname: string,
  ) {
    loading.value = true
    try {
      const result = await authApi.register(type, phone, code, password, nickname)
      user.value = result.user
      return result
    } finally {
      loading.value = false
    }
  }

  async function login(type: string, phone: string, password: string) {
    loading.value = true
    try {
      const result = await authApi.login(type, phone, password)
      user.value = result.user
      return result
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch {
      // 忽略登出 API 错误
    }
    user.value = null
    clearToken()
  }

  /** 应用启动时恢复登录状态 */
  async function restore() {
    if (getToken()) {
      await fetchMe()
    }
  }

  return {
    user,
    loading,
    isLoggedIn,
    nickname,
    plan,
    avatarInitial,
    fetchMe,
    sendCode,
    register,
    login,
    logout,
    restore,
  }
})
