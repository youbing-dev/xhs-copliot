<script setup lang="ts">
import { ref, computed } from 'vue'
import { generateNoteSSE, checkSensitive } from '../api/content'
import type { SseEvent } from '../api/content'

// ─── 响应式状态 ───
const topic = ref('如何提高工作效率')
const isGenerating = ref(false)
const hasResult = ref(false)
const loadingStage = ref('')
const error = ref('')

const generatedTitle = ref('')
const titleScore = ref(0)
const generatedBody = ref('')
const generatedTags = ref<string[]>([])
const sensitiveWords = ref<string[]>([])
const contentId = ref<number | null>(null)

// ─── 计算属性 ───
const showEmpty = computed(() => !hasResult.value && !isGenerating.value)
const showLoading = computed(() => isGenerating.value && !generatedTitle.value && !generatedBody.value)
const showStreaming = computed(() => isGenerating.value && (generatedTitle.value || generatedBody.value))
const showResult = computed(() => hasResult.value)
const showSensitive = computed(() => sensitiveWords.value.length > 0)

const bodyParagraphs = computed(() => {
  if (!generatedBody.value) return []
  return generatedBody.value
    .split('\n')
    .filter((line) => line.trim())
    .map((line) => line)
})

const bodyPreview = computed(() => {
  const text = generatedBody.value.replace(/\n/g, ' ')
  return text.length > 80 ? text.slice(0, 80) + '…' : text
})

const charCount = computed(() => {
  return generatedBody.value.replace(/\s/g, '').length
})

// ─── 交互方法 ───
async function handleGenerate() {
  if (!topic.value.trim() || isGenerating.value) return

  // 重置状态
  isGenerating.value = true
  hasResult.value = false
  error.value = ''
  generatedTitle.value = ''
  titleScore.value = 0
  generatedBody.value = ''
  generatedTags.value = []
  sensitiveWords.value = []
  contentId.value = null

  try {
    await generateNoteSSE(
      {
        topic: topic.value,
        wordCount: 600,
        includeTags: true,
        includeCover: false,
      },
      handleSseEvent,
    )
    // 安全网：如果流结束但没收到 done 事件
    if (isGenerating.value) {
      isGenerating.value = false
      if (generatedTitle.value || generatedBody.value) {
        hasResult.value = true
      }
    }
  } catch (e: any) {
    error.value = e.message || '生成失败'
    isGenerating.value = false
  }
}

function handleSseEvent(event: SseEvent) {
  switch (event.event) {
    case 'analyzing':
      loadingStage.value = event.data.message || '正在分析热门数据...'
      break
    case 'generating':
      loadingStage.value = event.data.message || '正在生成...'
      break
    case 'title':
      generatedTitle.value = event.data.title || ''
      titleScore.value = event.data.score || 0
      loadingStage.value = '正在生成正文...'
      break
    case 'body-chunk':
      generatedBody.value += event.data.chunk || ''
      break
    case 'body':
      generatedBody.value = event.data.body || ''
      loadingStage.value = '正在生成标签...'
      break
    case 'tags':
      generatedTags.value = event.data.tags || []
      break
    case 'warning':
      sensitiveWords.value = event.data.words || []
      break
    case 'done':
      contentId.value = event.data.contentId || null
      isGenerating.value = false
      hasResult.value = true
      break
    case 'error':
      error.value = event.data.message || '生成失败'
      isGenerating.value = false
      break
  }
}

async function handleCopy() {
  const text = [generatedTitle.value, '', generatedBody.value, '', generatedTags.value.join(' ')].join('\n')
  try {
    await navigator.clipboard.writeText(text)
  } catch {
    // 忽略剪贴板错误
  }
}

async function handleFilterSensitive() {
  if (!generatedBody.value) return
  try {
    const result = await checkSensitive(generatedTitle.value + ' ' + generatedBody.value)
    if (result.hasSensitive) {
      sensitiveWords.value = result.sensitiveWords
      generatedBody.value = result.filteredText
    }
  } catch {
    // 忽略错误
  }
}

// Tab 切换
const activeTab = ref<'preview' | 'cover'>('preview')
function switchTab(tab: 'preview' | 'cover') {
  activeTab.value = tab
}

// 快捷主题
function useHint(hint: string) {
  topic.value = hint
}

</script>

<template>

<!-- Main Content -->
<div class="main">

  <!-- Left Panel -->
  <div class="panel-left">
    <!-- Input Bar -->
    <div class="input-bar">
      <input
        v-model="topic"
        type="text"
        class="input-field"
        placeholder="输入你想写的主题或关键词…"
        @keyup.enter="handleGenerate"
      >
      <button class="input-submit" :disabled="isGenerating" @click="handleGenerate">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        {{ isGenerating ? '生成中…' : '分析并生成' }}
      </button>
    </div>

    <div class="panel-left-scroll">
      <!-- Error State -->
      <div v-if="error" class="sensitive-alert">
        <svg class="sensitive-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        <span class="sensitive-alert-text">{{ error }}</span>
      </div>

      <!-- Empty State -->
      <div v-if="showEmpty" class="empty-state">
        <div class="empty-state-icon">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        </div>
        <div class="empty-state-title">输入你的主题，开始创作</div>
        <div class="empty-state-desc">墨析会先分析小红书上正在火的知识类笔记，再帮你生成高质量内容。试试这些热门主题：</div>
        <div class="empty-state-hints">
          <span class="empty-state-hint" @click="useHint('如何提高工作效率')">如何提高工作效率</span>
          <span class="empty-state-hint" @click="useHint('时间管理方法')">时间管理方法</span>
          <span class="empty-state-hint" @click="useHint('读书笔记分享')">读书笔记分享</span>
          <span class="empty-state-hint" @click="useHint('职场沟通技巧')">职场沟通技巧</span>
          <span class="empty-state-hint" @click="useHint('自律习惯养成')">自律习惯养成</span>
        </div>
      </div>

      <!-- Loading State -->
      <div v-if="showLoading" class="loading-state loading-state--active">
        <div class="wb-section-label">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12V7H5a2 2 0 0 1 0-4h14v4"/><path d="M3 5v14a2 2 0 0 0 2 2h16v-5"/><path d="M18 12a2 2 0 0 0 0 4h4v-4Z"/></svg>
          {{ loadingStage || '正在分析…' }}
          <span class="wb-section-label-line"></span>
        </div>
        <div class="analysis-grid">
          <div class="skeleton skeleton-card"></div>
          <div class="skeleton skeleton-card"></div>
          <div class="skeleton skeleton-card"></div>
          <div class="skeleton skeleton-card"></div>
        </div>
        <div class="wb-section-label" style="margin-top:8px;">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7V4h16v3"/><path d="M9 20h6"/><path d="M12 4v16"/></svg>
          AI 生成标题中…
          <span class="wb-section-label-line"></span>
        </div>
        <div class="skeleton skeleton-title"></div>
        <div class="skeleton skeleton-title"></div>
        <div class="skeleton skeleton-title"></div>
      </div>

      <!-- Streaming State (progressive content during generation) -->
      <div v-if="showStreaming" class="streaming-state">
        <div class="wb-section-label">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-6.219-8.56"/></svg>
          {{ loadingStage || '正在生成…' }}
          <span class="wb-section-label-line"></span>
        </div>

        <div v-if="generatedTitle" class="title-options" style="margin-bottom:12px;">
          <div class="title-option title-option--selected" role="radio" tabindex="0" aria-checked="true">
            <span class="title-radio"></span>
            <span class="title-text">{{ generatedTitle }}</span>
            <span v-if="titleScore" class="title-score">{{ titleScore }}分</span>
          </div>
        </div>

        <div v-if="generatedBody" class="editor-content">
          <p v-for="(para, i) in bodyParagraphs" :key="i">{{ para }}</p>
          <span class="streaming-cursor">▎</span>
        </div>
      </div>

      <!-- Result Section -->
      <template v-if="showResult">
        <!-- Title -->
        <div class="wb-section-label">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7V4h16v3"/><path d="M9 20h6"/><path d="M12 4v16"/></svg>
          AI 生成标题
          <span class="wb-section-label-line"></span>
        </div>

        <div class="title-options">
          <div class="title-option title-option--selected" role="radio" tabindex="0" aria-checked="true">
            <span class="title-radio"></span>
            <span class="title-text">{{ generatedTitle || '（标题生成中…）' }}</span>
            <span v-if="titleScore" class="title-score">{{ titleScore }}分</span>
          </div>
        </div>

        <!-- Sensitive word alert -->
        <div v-if="showSensitive" class="sensitive-alert">
          <svg class="sensitive-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          <span class="sensitive-alert-text">检测到 <strong>{{ sensitiveWords.length }} 个敏感词</strong>：{{ sensitiveWords.join('、') }}，可能被平台限流</span>
          <button class="sensitive-alert-fix" @click="handleFilterSensitive">一键修复</button>
        </div>

        <!-- Content Editor -->
        <div class="wb-section-label" style="margin-top:8px;">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><line x1="10" y1="9" x2="8" y2="9"/></svg>
          正文内容
          <span class="wb-section-label-line"></span>
        </div>

        <div class="editor-toolbar">
          <button class="editor-btn editor-btn--active" title="加粗">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M6 4h8a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"/><path d="M6 12h9a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"/></svg>
          </button>
          <button class="editor-btn" title="列表">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>
          </button>
          <button class="editor-btn" title="引用">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 21c3 0 7-1 7-8V5c0-1.25-.756-2.017-2-2H4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2 1 0 1 0 1 1v1c0 1-1 2-2 2s-1 .008-1 1.031V21z"/><path d="M15 21c3 0 7-1 7-8V5c0-1.25-.757-2.017-2-2h-4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2h.75c0 2.25.25 4-2.75 4v3z"/></svg>
          </button>
          <span class="editor-divider"></span>
          <button class="editor-btn" title="表情符号">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><line x1="9" y1="9" x2="9.01" y2="9"/><line x1="15" y1="9" x2="15.01" y2="9"/></svg>
          </button>
          <span class="editor-spacer"></span>
        </div>

        <div class="editor-content">
          <p v-for="(para, i) in bodyParagraphs" :key="i">{{ para }}</p>
        </div>

        <!-- Tags -->
        <div v-if="generatedTags.length" class="tag-row">
          <span v-for="tag in generatedTags" :key="tag" class="tag-item">{{ tag.startsWith('#') ? tag : '#' + tag }}</span>
        </div>

        <!-- Actions -->
        <div class="action-bar">
          <button class="action-btn action-btn--primary" @click="handleCopy">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="14" height="14" x="8" y="8" rx="2" ry="2"/><path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/></svg>
            一键复制文案
          </button>
          <span class="action-spacer"></span>
          <span class="char-count">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7V4h16v3"/><path d="M9 20h6"/><path d="M12 4v16"/></svg>
            {{ charCount }} 字
          </span>
        </div>
      </template>
    </div>
  </div>

  <!-- Right Panel -->
  <div class="panel-right">
    <div class="preview-header">
      <div class="preview-tabs" role="tablist" aria-label="预览切换">
        <button
          class="preview-tab"
          :class="{ 'preview-tab--active': activeTab === 'preview' }"
          role="tab"
          :aria-selected="activeTab === 'preview'"
          @click="switchTab('preview')"
          type="button"
        >小红书预览</button>
        <button
          class="preview-tab"
          :class="{ 'preview-tab--active': activeTab === 'cover' }"
          role="tab"
          :aria-selected="activeTab === 'cover'"
          @click="switchTab('cover')"
          type="button"
        >封面图</button>
      </div>
    </div>

    <!-- Cover Image Selection -->
    <div v-if="activeTab === 'cover'" class="cover-section tab-panel tab-panel--active">
      <div class="cover-label">封面图方案</div>
      <div class="cover-grid" role="radiogroup" aria-label="封面图方案">
        <div class="cover-item cover-1 cover-item--selected" role="radio" tabindex="0" aria-checked="true">
          <div class="cover-title">{{ generatedTitle ? generatedTitle.slice(0, 8) : '效率真相' }}<br>4个方法</div>
          <div class="cover-sub">暖色知识风</div>
        </div>
        <div class="cover-item cover-2" role="radio" tabindex="0" aria-checked="false">
          <div class="cover-sub">冷色简约风</div>
        </div>
        <div class="cover-item cover-3" role="radio" tabindex="0" aria-checked="false">
          <div class="cover-sub">清新自然风</div>
        </div>
      </div>
      <button class="cover-regen">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/><path d="M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"/><path d="M16 16h5v5"/></svg>
        重新生成封面
      </button>
    </div>

    <!-- Xiaohongshu Preview -->
    <div v-if="activeTab === 'preview'" class="xhs-preview tab-panel tab-panel--active">
      <div class="xhs-phone">
        <div class="xhs-phone-status">
          <div class="xhs-phone-notch"></div>
        </div>
        <div class="xhs-cover-area">
          <div class="xhs-cover-title">{{ generatedTitle || '等待生成' }}</div>
          <div class="xhs-cover-sub">{{ generatedTags.length ? generatedTags[0] : '墨析 AI 创作' }}</div>
          <div class="xhs-cover-dots">
            <span class="xhs-cover-dot xhs-cover-dot--active"></span>
            <span class="xhs-cover-dot"></span>
            <span class="xhs-cover-dot"></span>
            <span class="xhs-cover-dot"></span>
          </div>
        </div>
        <div class="xhs-body">
          <div class="xhs-author">
            <div class="xhs-avatar"></div>
            <span class="xhs-author-name">墨析用户</span>
            <span class="xhs-follow-btn">+ 关注</span>
          </div>
          <div class="xhs-note-title">{{ generatedTitle || '（标题待生成）' }}</div>
          <div class="xhs-note-body">{{ bodyPreview || '（正文待生成）' }}</div>
          <div class="xhs-tags">
            <span v-for="tag in generatedTags" :key="tag" class="xhs-tag">{{ tag.startsWith('#') ? tag + ' ' : '#' + tag + ' ' }}</span>
          </div>
          <div class="xhs-actions">
            <div class="xhs-action-item">
              <svg class="xhs-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
              <span class="xhs-action-count">赞</span>
            </div>
            <div class="xhs-action-item">
              <svg class="xhs-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/><path d="M12 5 9.04 7.96a5 5 0 0 0 0 7.08L12 18"/></svg>
              <span class="xhs-action-count">收藏</span>
            </div>
            <div class="xhs-action-item">
              <svg class="xhs-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
              <span class="xhs-action-count">评论</span>
            </div>
            <div class="xhs-action-item">
              <svg class="xhs-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/></svg>
              <span class="xhs-action-count">分享</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

</div>
</template>

<style scoped>
/* ─── TOP NAV (kept for scoped override safety) ─── */
.topbar {
  height: 56px;
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

/* ─── MAIN LAYOUT ─── */
.main {
  margin-top: 56px;
  display: flex;
  height: calc(100vh - 56px);
}

/* ─── LEFT PANEL ─── */
.panel-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-right: 1px solid var(--border);
  background: var(--surface);
}

/* Input Bar */
.input-bar {
  padding: var(--space-md) var(--space-lg);
  border-bottom: 1px solid var(--border-light);
  display: flex;
  gap: var(--space-sm);
  align-items: center;
  flex-shrink: 0;
}
.input-field {
  flex: 1;
  height: 44px;
  padding: 0 var(--space-md);
  background: var(--bg-warm);
  border: 1.5px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: 15px;
  color: var(--fg);
  outline: none;
  transition: border-color 0.2s;
}
.input-field:focus { border-color: var(--accent); }
.input-field::placeholder { color: var(--muted); }
.input-submit {
  height: 44px;
  padding: 0 24px;
  background: var(--accent);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.02em;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
  transition: background 0.2s;
}
.input-submit:hover { background: var(--accent-hover); }

/* Scrollable Content */
.panel-left-scroll {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-lg);
}

/* Section Label */
.wb-section-label {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.01em;
  color: var(--muted);
  margin-bottom: var(--space-md);
}
.wb-section-label-line {
  flex: 1;
  height: 1px;
  background: var(--border-light);
}

/* ─── ANALYSIS PANEL ─── */
.analysis-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-sm);
  margin-bottom: var(--space-lg);
}
.analysis-card {
  padding: 14px;
  background: var(--bg-warm);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
}
.analysis-card-label {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 4px;
  letter-spacing: 0.02em;
}
.analysis-card-value {
  font-size: 15px;
  font-weight: 600;
  color: var(--fg);
  letter-spacing: -0.01em;
}
.analysis-card-meta {
  font-size: 11px;
  color: var(--success);
  margin-top: 2px;
}

/* Hot tags */
.hot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: var(--space-lg);
}
.hot-tag {
  padding: 5px 12px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  font-size: 12px;
  color: var(--fg-secondary);
  cursor: pointer;
  transition: all 0.15s;
}
.hot-tag:hover { border-color: var(--accent); color: var(--accent); }
.hot-tag--active {
  background: var(--accent-surface);
  border-color: var(--accent);
  color: var(--accent-hover);
}
.hot-tag-count {
  font-size: 11px;
  color: var(--muted);
  margin-left: 4px;
}

/* ─── TITLE OPTIONS ─── */
.title-options {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  margin-bottom: var(--space-lg);
}
.title-option {
  padding: 12px 14px;
  background: var(--bg-warm);
  border: 1.5px solid var(--border-light);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: all 0.15s;
}
.title-option:hover { border-color: var(--border); }
.title-option--selected {
  border-color: var(--accent);
  background: var(--accent-surface);
}
.title-radio {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid var(--border);
  flex-shrink: 0;
  position: relative;
}
.title-option--selected .title-radio {
  border-color: var(--accent);
}
.title-option--selected .title-radio::after {
  content: "";
  position: absolute;
  top: 3px;
  left: 3px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent);
}
.title-text {
  flex: 1;
  font-size: 15px;
  font-weight: 500;
  color: var(--fg);
  line-height: 1.4;
}
.title-score {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 8px;
  background: var(--success-light);
  color: var(--success);
  border-radius: var(--radius-pill);
  white-space: nowrap;
}

/* ─── CONTENT EDITOR ─── */
.editor-toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  padding: var(--space-sm) 0;
  margin-bottom: var(--space-sm);
  border-bottom: 1px solid var(--border-light);
}
.editor-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: var(--muted);
  transition: all 0.15s;
}
.editor-btn:hover { background: var(--bg-subtle); color: var(--fg); }
.editor-btn--active { background: var(--bg-subtle); color: var(--fg); }
.editor-divider {
  width: 1px;
  height: 20px;
  background: var(--border-light);
  margin: 0 var(--space-xs);
}
.editor-spacer { flex: 1; }
.editor-humanize {
  padding: 8px 18px;
  background: var(--accent);
  border: none;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  gap: 6px;
  transition: background 0.15s, transform 0.1s;
  box-shadow: 0 2px 8px rgba(217,119,6,0.25);
}
.editor-humanize:hover { background: var(--accent-hover); transform: translateY(-1px); }
.editor-humanize:active { transform: translateY(0); }

.editor-content {
  padding: var(--space-md);
  background: var(--bg-warm);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  min-height: 260px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--fg);
  margin-bottom: var(--space-md);
  outline: none;
  transition: border-color 0.2s;
}
.editor-content:focus-within,
.editor-content[contenteditable]:focus {
  border-color: var(--accent);
}
.editor-content p { margin-bottom: 12px; }
.editor-content p:last-child { margin-bottom: 0; }

/* Empty state */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px var(--space-lg);
  text-align: center;
}
.empty-state-icon {
  width: 64px;
  height: 64px;
  background: var(--accent-surface);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-lg);
}
.empty-state-title {
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 600;
  color: var(--fg);
  margin-bottom: var(--space-sm);
}
.empty-state-desc {
  font-size: 14px;
  color: var(--muted);
  max-width: 320px;
  line-height: 1.6;
  margin-bottom: var(--space-lg);
}
.empty-state-hints {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  justify-content: center;
}
.empty-state-hint {
  padding: 6px 14px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  font-size: 13px;
  color: var(--fg-secondary);
  cursor: pointer;
  transition: border-color 0.15s;
}
.empty-state-hint:hover { border-color: var(--accent); color: var(--accent); }
.editor-highlight {
  background: #fef08a;
  padding: 0 2px;
  border-radius: 2px;
}
.editor-sensitive {
  background: var(--danger-light);
  color: var(--danger);
  padding: 0 2px;
  border-radius: 2px;
  text-decoration: underline wavy var(--danger);
  text-underline-offset: 3px;
}

/* Loading skeleton */
.skeleton {
  background: linear-gradient(90deg, var(--bg-subtle) 25%, var(--border-light) 50%, var(--bg-subtle) 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
  border-radius: var(--radius-sm);
}
@keyframes skeleton-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
.skeleton-card {
  height: 72px;
  margin-bottom: var(--space-sm);
}
.skeleton-title {
  height: 48px;
  margin-bottom: var(--space-sm);
}
.skeleton-text {
  height: 14px;
  margin-bottom: 8px;
  width: 90%;
}
.skeleton-text:nth-child(2) { width: 75%; }
.skeleton-text:nth-child(3) { width: 60%; }

.streaming-cursor {
  display: inline-block;
  color: #ff6600;
  font-weight: bold;
  animation: blink 0.8s steps(2) infinite;
}
@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}
.loading-state { display: none; }
.loading-state--active { display: block; }

/* Tags in editor */
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: var(--space-md);
}
.tag-item {
  padding: 4px 10px;
  background: var(--accent-surface);
  border: 1px solid var(--accent-light);
  border-radius: var(--radius-pill);
  font-size: 12px;
  color: var(--accent-hover);
}

/* Sensitive word alert */
.sensitive-alert {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: 10px 14px;
  background: var(--danger-light);
  border: 1px solid #fecaca;
  border-radius: var(--radius-sm);
  margin-bottom: var(--space-md);
}
.sensitive-alert-icon {
  width: 18px;
  height: 18px;
  color: var(--danger);
  flex-shrink: 0;
}
.sensitive-alert-text {
  flex: 1;
  font-size: 13px;
  color: var(--danger);
  line-height: 1.4;
}
.sensitive-alert-text strong { font-weight: 600; }
.sensitive-alert-fix {
  padding: 5px 12px;
  background: var(--danger);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  border-radius: 6px;
  white-space: nowrap;
}

/* Action bar */
.action-bar {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding-top: var(--space-md);
  border-top: 1px solid var(--border-light);
}
.action-btn {
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.02em;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.15s;
}
.action-btn--primary {
  background: var(--accent);
  color: #fff;
}
.action-btn--primary:hover { background: var(--accent-hover); }
.action-btn--copied {
  background: var(--success);
  color: #fff;
}
.action-btn--outline {
  border: 1.5px solid var(--border);
  color: var(--fg-secondary);
}
.action-btn--outline:hover { border-color: var(--fg); color: var(--fg); }
.action-btn--ghost {
  color: var(--muted);
}
.action-btn--ghost:hover { color: var(--fg); }
.action-spacer { flex: 1; }
.char-count {
  font-size: 12px;
  color: var(--muted);
  display: flex;
  align-items: center;
  gap: 4px;
}
.char-count-warn { color: var(--warn); }

/* ─── RIGHT PANEL ─── */
.panel-right {
  width: 380px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  background: var(--bg-warm);
}

.preview-header {
  padding: var(--space-md) var(--space-lg);
  border-bottom: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}
.preview-tabs {
  display: flex;
  gap: var(--space-sm);
}
.preview-tab {
  padding: 5px 14px;
  font-size: 13px;
  font-weight: 500;
  color: var(--muted);
  border-radius: var(--radius-pill);
  transition: all 0.15s;
  cursor: pointer;
  background: transparent;
  border: none;
  font-family: inherit;
}
.preview-tab--active {
  background: var(--surface);
  color: var(--fg);
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

/* Tab panels */
.tab-panel { display: none; }
.tab-panel--active { display: block; }

/* Cover Images */
.cover-section {
  padding: var(--space-md) var(--space-lg);
  border-bottom: 1px solid var(--border-light);
}
.cover-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--muted);
  letter-spacing: 0.04em;
  margin-bottom: var(--space-sm);
}
.cover-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-sm);
}
.cover-item {
  aspect-ratio: 3/4;
  border-radius: var(--radius-sm);
  border: 2px solid transparent;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.15s;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 8px;
}
.cover-item:hover { border-color: var(--border); }
.cover-item--selected { border-color: var(--accent); }
.cover-item--selected::after {
  content: "";
  position: absolute;
  top: 6px;
  right: 6px;
  width: 18px;
  height: 18px;
  background: var(--accent);
  border-radius: 50%;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='white' stroke-width='3' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='20 6 9 17 4 12'/%3E%3C/svg%3E");
  background-size: 12px;
  background-repeat: no-repeat;
  background-position: center;
}
.cover-1 {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #92400e;
}
.cover-1::before {
  content: "";
  position: absolute;
  top: 12px;
  left: 12px;
  width: 24px;
  height: 24px;
  border: 2px solid rgba(146,64,14,0.2);
  border-radius: 50%;
}
.cover-1::after {
  content: "";
  position: absolute;
  bottom: 12px;
  right: 12px;
  width: 20px;
  height: 20px;
  border: 2px solid rgba(146,64,14,0.15);
  border-radius: 4px;
  transform: rotate(45deg);
}
.cover-2 {
  background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%);
  color: #075985;
}
.cover-2::before {
  content: "";
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -60%);
  width: 40px;
  height: 2px;
  background: rgba(7,89,133,0.2);
}
.cover-2::after {
  content: "";
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  width: 30px;
  height: 30px;
  border: 2px solid rgba(7,89,133,0.12);
  border-radius: 6px;
}
.cover-3 {
  background: linear-gradient(135deg, #f0fdf4 0%, #bbf7d0 100%);
  color: #166534;
}
.cover-3::before {
  content: "";
  position: absolute;
  top: 8px;
  right: 8px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(22,101,52,0.1);
}
.cover-3::after {
  content: "";
  position: absolute;
  bottom: 10px;
  left: 10px;
  width: 28px;
  height: 4px;
  border-radius: 2px;
  background: rgba(22,101,52,0.12);
}
.cover-title {
  font-size: 11px;
  font-weight: 700;
  line-height: 1.3;
  margin-bottom: 4px;
}
.cover-sub {
  font-size: 9px;
  opacity: 0.7;
}
.cover-regen {
  margin-top: var(--space-sm);
  font-size: 12px;
  color: var(--muted);
  display: flex;
  align-items: center;
  gap: 4px;
  transition: color 0.15s;
}
.cover-regen:hover { color: var(--accent); }

/* ─── XHS PREVIEW ─── */
.xhs-preview {
  padding: var(--space-md) var(--space-lg);
  flex: 1;
}
.xhs-phone {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  max-width: 300px;
  margin: 0 auto;
}
.xhs-phone-status {
  height: 36px;
  background: var(--surface);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 var(--space-md);
}
.xhs-phone-notch {
  width: 80px;
  height: 20px;
  background: var(--fg);
  border-radius: 0 0 12px 12px;
}
.xhs-cover-area {
  aspect-ratio: 3/4;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-lg);
  text-align: center;
  position: relative;
}
.xhs-cover-title {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 700;
  line-height: 1.3;
  color: #92400e;
  margin-bottom: var(--space-sm);
}
.xhs-cover-sub {
  font-size: 13px;
  color: #a16207;
}
.xhs-cover-dots {
  position: absolute;
  bottom: 12px;
  display: flex;
  gap: 4px;
}
.xhs-cover-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(0,0,0,0.15);
}
.xhs-cover-dot--active { background: #92400e; }

.xhs-body {
  padding: var(--space-md);
}
.xhs-author {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
}
.xhs-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--accent-light);
}
.xhs-author-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--fg);
}
.xhs-follow-btn {
  margin-left: auto;
  padding: 3px 12px;
  background: #ff2442;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border-radius: var(--radius-pill);
}
.xhs-note-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--fg);
  margin-bottom: var(--space-sm);
  line-height: 1.4;
}
.xhs-note-body {
  font-size: 13px;
  line-height: 1.7;
  color: var(--fg-secondary);
  margin-bottom: var(--space-md);
}
.xhs-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: var(--space-md);
}
.xhs-tag {
  font-size: 12px;
  color: #3b82f6;
}
.xhs-actions {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: var(--space-sm) 0;
  border-top: 1px solid var(--border-light);
}
.xhs-action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.xhs-action-icon {
  width: 24px;
  height: 24px;
  color: var(--muted);
}
.xhs-action-count {
  font-size: 10px;
  color: var(--muted);
}

/* ─── RESPONSIVE ─── */
@media (max-width: 1024px) {
  .panel-right { width: 320px; }
  .analysis-grid { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .main { flex-direction: column; height: auto; min-height: calc(100vh - 56px); }
  .panel-left { border-right: none; border-bottom: 1px solid var(--border); }
  .panel-right { width: 100%; min-height: 500px; }
}
</style>
