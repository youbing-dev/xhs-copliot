<script setup lang="ts">
import { computed, watch } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppTopBar from './components/AppTopBar.vue'

const route = useRoute()
const showTopBar = computed(() => route.path !== '/')

// Dynamically set body background per route: white for landing, warm for product pages
watch(() => route.path, (path) => {
  if (path === '/') {
    document.body.style.background = 'var(--bg)'
  } else {
    document.body.style.background = 'var(--bg-subtle)'
  }
}, { immediate: true })
</script>

<template>
  <AppTopBar v-if="showTopBar" />
  <RouterView />
</template>
