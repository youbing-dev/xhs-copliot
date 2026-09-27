<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const mode = ref<'login' | 'register'>('login')
const phone = ref('')
const code = ref('')
const password = ref('')
const nickname = ref('')
const error = ref('')
const countdown = ref(0)

const canSubmit = computed(() => {
  if (!phone.value || !password.value) return false
  if (mode.value === 'register' && !code.value) return false
  if (mode.value === 'register' && !nickname.value) return false
  return true
})

async function handleSendCode() {
  if (!phone.value) {
    error.value = '请输入手机号'
    return
  }
  if (countdown.value > 0) return
  error.value = ''
  try {
    await auth.sendCode('phone', phone.value)
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e: any) {
    error.value = e.message || '验证码发送失败'
  }
}

async function handleSubmit() {
  if (!canSubmit.value) return
  error.value = ''
  try {
    if (mode.value === 'login') {
      await auth.login('phone', phone.value, password.value)
    } else {
      await auth.register('phone', phone.value, code.value, password.value, nickname.value)
    }
    router.push('/workbench')
  } catch (e: any) {
    error.value = e.message || '操作失败'
  }
}
</script>

<template>
<div class="login-page">
  <div class="login-card">
    <div class="login-logo">
      <span class="login-logo-icon">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
      </span>
      <span class="login-title">墨析</span>
    </div>
    <div class="login-subtitle">AI 驱动的小红书内容创作工具</div>

    <div class="login-tabs">
      <button
        :class="['login-tab', mode === 'login' && 'login-tab--active']"
        @click="mode = 'login'"
      >登录</button>
      <button
        :class="['login-tab', mode === 'register' && 'login-tab--active']"
        @click="mode = 'register'"
      >注册</button>
    </div>

    <div class="login-form">
      <div class="form-field">
        <input v-model="phone" type="tel" maxlength="11" class="form-input" placeholder="手机号" />
      </div>

      <template v-if="mode === 'register'">
        <div class="form-field form-field-code">
          <input v-model="code" type="text" maxlength="6" class="form-input" placeholder="验证码" />
          <button
            class="code-btn"
            :disabled="countdown > 0"
            @click="handleSendCode"
          >{{ countdown > 0 ? `${countdown}s` : '发送验证码' }}</button>
        </div>

        <div class="form-field">
          <input v-model="nickname" type="text" class="form-input" placeholder="昵称" />
        </div>
      </template>

      <div class="form-field">
        <input v-model="password" type="password" class="form-input" placeholder="密码" @keyup.enter="handleSubmit" />
      </div>

      <div v-if="error" class="form-error">{{ error }}</div>

      <button
        class="form-submit"
        :disabled="!canSubmit || auth.loading"
        @click="handleSubmit"
      >{{ auth.loading ? '处理中...' : (mode === 'login' ? '登录' : '注册') }}</button>
    </div>
  </div>
</div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-subtle);
}
.login-card {
  width: 400px;
  background: var(--surface);
  border-radius: var(--radius-lg);
  padding: 48px 40px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.06);
}
.login-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: center;
  margin-bottom: 8px;
}
.login-logo-icon {
  width: 36px;
  height: 36px;
  background: var(--accent);
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.login-title {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 700;
}
.login-subtitle {
  text-align: center;
  font-size: 14px;
  color: var(--muted);
  margin-bottom: 32px;
}
.login-tabs {
  display: flex;
  gap: 0;
  border-bottom: 2px solid var(--border-light);
  margin-bottom: 24px;
}
.login-tab {
  flex: 1;
  padding: 10px 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--muted);
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s;
}
.login-tab--active {
  color: var(--fg);
  border-bottom-color: var(--accent);
}
.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.form-field {
  position: relative;
}
.form-field-code {
  display: flex;
  gap: 8px;
}
.form-input {
  width: 100%;
  height: 48px;
  padding: 0 16px;
  background: var(--bg-warm);
  border: 1.5px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: 15px;
  color: var(--fg);
  outline: none;
  transition: border-color 0.2s;
}
.form-input:focus {
  border-color: var(--accent);
}
.form-input::placeholder {
  color: var(--muted);
}
.code-btn {
  white-space: nowrap;
  padding: 0 16px;
  height: 48px;
  background: var(--surface);
  border: 1.5px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--accent);
  font-weight: 500;
  transition: all 0.15s;
}
.code-btn:hover:not(:disabled) {
  border-color: var(--accent);
}
.code-btn:disabled {
  color: var(--muted);
  cursor: not-allowed;
}
.form-error {
  font-size: 13px;
  color: var(--danger);
  padding: 4px 0;
}
.form-submit {
  height: 48px;
  background: var(--accent);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  transition: background 0.2s;
  margin-top: 8px;
}
.form-submit:hover:not(:disabled) {
  background: var(--accent-hover);
}
.form-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
