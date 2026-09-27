<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

onMounted(() => {
  auth.restore()
})

async function handleLogout() {
  await auth.logout()
  router.push('/')
}
</script>

<template>
  <header class="topbar">
    <a href="/" class="topbar-logo">
      <span class="topbar-logo-icon">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
      </span>
      墨析
    </a>
    <nav class="topbar-nav">
      <RouterLink to="/workbench" class="topbar-nav-item" active-class="topbar-nav-item--active">创作工作台</RouterLink>
      <RouterLink to="/dashboard" class="topbar-nav-item" active-class="topbar-nav-item--active">数据看板</RouterLink>
      <RouterLink to="/profile" class="topbar-nav-item" active-class="topbar-nav-item--active">个人中心</RouterLink>
    </nav>
    <div class="topbar-right">
      <RouterLink to="/profile" class="topbar-avatar" :title="auth.nickname">{{ auth.avatarInitial }}</RouterLink>
      <button v-if="auth.isLoggedIn" class="topbar-logout" @click="handleLogout">退出</button>
    </div>
  </header>
</template>

<style scoped>
.topbar {
  height: var(--topbar-height);
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  padding: 0 var(--space-lg);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
}
.topbar-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.02em;
  margin-right: var(--space-xl);
}
.topbar-logo-icon {
  width: 28px;
  height: 28px;
  background: var(--accent);
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.topbar-nav {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
  flex: 1;
}
.topbar-nav-item {
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.02em;
  color: var(--muted);
  padding: 6px 0;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}
.topbar-nav-item--active {
  color: var(--fg);
  border-bottom-color: var(--accent);
}
.topbar-nav-item:hover { color: var(--fg); }
.topbar-right {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}
.topbar-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--accent-light);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: var(--accent-hover);
  transition: opacity 0.15s;
}
.topbar-avatar:hover { opacity: 0.8; }
.topbar-logout {
  font-size: 13px;
  color: var(--muted);
  transition: color 0.15s;
}
.topbar-logout:hover { color: var(--danger); }
</style>
