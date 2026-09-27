<script setup lang="ts">
import { onMounted } from 'vue'

onMounted(() => {
  // Mobile nav toggle
  const toggle = document.querySelector('.nav-mobile-toggle') as HTMLElement | null
  const links = document.querySelector('.nav-links') as HTMLElement | null
  if (toggle && links) {
    toggle.setAttribute('aria-expanded', 'false')
    toggle.addEventListener('click', function () {
      const expanded = toggle.getAttribute('aria-expanded') === 'true'
      toggle.setAttribute('aria-expanded', String(!expanded))
      if (!expanded) {
        links.style.display = 'flex'
        links.style.flexDirection = 'column'
        links.style.position = 'absolute'
        links.style.top = '64px'
        links.style.left = '0'
        links.style.right = '0'
        links.style.background = '#fff'
        links.style.padding = '16px 24px'
        links.style.borderBottom = '1px solid var(--border)'
        links.style.gap = '16px'
      } else {
        links.removeAttribute('style')
      }
    })
  }

  // Billing toggle
  ;(window as any).toggleBilling = function (mode: string) {
    const isAnnual = mode === 'annual'
    const toggleBtn = document.getElementById('billingToggle') as HTMLElement | null
    const dot = document.getElementById('billingDot') as HTMLElement | null
    const monthLabel = document.getElementById('monthlyLabel') as HTMLElement | null
    const yearLabel = document.getElementById('annualLabel') as HTMLElement | null
    const notes = document.querySelectorAll('.annual-note')

    if (toggleBtn) {
      toggleBtn.style.background = isAnnual ? 'var(--accent)' : 'var(--border)'
      toggleBtn.setAttribute('aria-checked', String(isAnnual))
    }
    if (dot) {
      dot.style.left = isAnnual ? '22px' : '2px'
    }
    if (monthLabel) {
      monthLabel.style.color = isAnnual ? 'var(--muted)' : 'var(--fg)'
      monthLabel.style.fontWeight = isAnnual ? '400' : '600'
    }
    if (yearLabel) {
      yearLabel.style.color = isAnnual ? 'var(--fg)' : 'var(--muted)'
      yearLabel.style.fontWeight = isAnnual ? '600' : '400'
    }

    document.querySelectorAll('.pricing-value[data-monthly]').forEach((el) => {
      el.textContent = isAnnual ? el.getAttribute('data-annual') : el.getAttribute('data-monthly')
    })
    notes.forEach((n) => {
      ;(n as HTMLElement).style.display = isAnnual ? 'block' : 'none'
    })
  }
})
</script>

<template>

<!-- Navigation -->
<nav class="nav">
  <div class="nav-inner">
    <a href="#" class="nav-logo">
      <span class="nav-logo-icon">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
      </span>
      墨析
    </a>
    <ul class="nav-links">
      <li><a href="#features">核心功能</a></li>
      <li><a href="#demo">产品演示</a></li>
      <li><a href="#pricing">定价方案</a></li>
    </ul>
    <a href="#/workbench" class="nav-cta">免费开始</a>
    <button class="nav-mobile-toggle" aria-label="菜单">
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
    </button>
  </div>
</nav>

<!-- Hero -->
<section class="hero" data-component="hero">
  <div class="container">
    <div class="hero-inner">
      <div class="hero-content">
        <div class="hero-badge">
          <span class="hero-badge-dot"></span>
          专为知识干货博主设计
        </div>
        <h1 class="hero-title">
          先看数据，<br>再写<span class="hero-title-highlight">爆款</span>笔记
        </h1>
        <p class="hero-subtitle">
          墨析不是普通的AI写作工具。它先分析小红书上正在火的知识类笔记模式，再基于真实数据帮你生成标题、正文、标签和封面图。
        </p>
        <div class="hero-actions">
          <a href="#/workbench" class="btn-primary">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></svg>
            开始创作
          </a>
          <a href="#demo" class="btn-secondary">看看怎么用</a>
        </div>
        <p class="hero-note">免费版每天可生成 3 篇笔记，无需绑定支付方式</p>
      </div>

      <div class="hero-mockup" aria-hidden="true">
        <div class="mockup-header">
          <span class="mockup-dot"></span>
          <span class="mockup-dot"></span>
          <span class="mockup-dot"></span>
          <span class="mockup-url">moxi.app/workbench</span>
        </div>
        <div class="mockup-body">
          <div class="mockup-input-row">
            <div class="mockup-input">职场沟通技巧</div>
            <div class="mockup-btn">分析并生成</div>
          </div>
          <div class="mockup-cards">
            <div class="mockup-card">
              <div class="mockup-card-label">爆款标题模式</div>
              <div class="mockup-card-value">数字清单型</div>
              <div class="mockup-card-change">占比 47%</div>
            </div>
            <div class="mockup-card">
              <div class="mockup-card-label">最佳发布时间</div>
              <div class="mockup-card-value">20:00</div>
              <div class="mockup-card-change">互动峰值</div>
            </div>
            <div class="mockup-card">
              <div class="mockup-card-label">热门标签</div>
              <div class="mockup-card-value">#职场干货</div>
              <div class="mockup-card-change">12.3万笔记</div>
            </div>
          </div>
          <div class="mockup-text-block">
            <div class="mockup-text-line"></div>
            <div class="mockup-text-line"></div>
            <div class="mockup-text-line"></div>
            <div class="mockup-text-line"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</section>

<!-- Features -->
<section class="features" id="features" data-component="features">
  <div class="container">
    <div class="section-label">核心流程</div>
    <h2 class="section-title">三步完成一篇数据驱动的笔记</h2>
    <p class="section-desc">不是凭空让AI瞎写，而是先搞清楚什么内容能火，再让AI按爆款逻辑生成。</p>

    <div class="features-grid">
      <div class="feature-card" data-component="feature-card">
        <div class="feature-step">1</div>
        <h3 class="feature-card-title">爆款模式分析</h3>
        <p class="feature-card-desc">输入你的主题关键词，墨析会自动分析小红书上该领域正在火的笔记——标题用了什么结构、正文怎么组织的、哪些标签流量最高。</p>
        <div class="feature-card-detail">数据来源：实时搜索近 30 天热门笔记，分析标题模式、内容框架、互动数据和标签热度。</div>
      </div>

      <div class="feature-card" data-component="feature-card">
        <div class="feature-step">2</div>
        <h3 class="feature-card-title">AI 智能生成</h3>
        <p class="feature-card-desc">基于爆款分析结果，AI 生成 3-5 个标题备选、结构化正文、推荐标签组合和封面图方案。每个生成决策都附带数据依据。</p>
        <div class="feature-card-detail">为什么选这个标题？因为「数字清单型」标题在该领域占爆款笔记的 47%，平均互动率高出 2.3 倍。</div>
      </div>

      <div class="feature-card" data-component="feature-card">
        <div class="feature-step">3</div>
        <h3 class="feature-card-title">去AI味 · 人味化</h3>
        <p class="feature-card-desc">一键「人味化」改写：植入个人化表达、增加口语感、锐化观点。让内容读起来像真人博主写的，而不是AI模板。</p>
        <div class="feature-card-detail">内置敏感词检测，实时标记平台可能限流的用词，帮你规避降权风险。</div>
      </div>
    </div>
  </div>
</section>

<!-- Product Demo -->
<section class="demo" id="demo" data-component="demo">
  <div class="container">
    <div class="demo-header">
      <div class="section-label">产品演示</div>
      <h2 class="section-title">创作工作台长这样</h2>
      <p class="section-desc">左侧输入和分析，右侧生成和编辑。从关键词到成稿，一个页面搞定。</p>
    </div>

    <div class="demo-window">
      <div class="demo-titlebar">
        <span class="demo-titlebar-dot demo-titlebar-dot--red"></span>
        <span class="demo-titlebar-dot demo-titlebar-dot--yellow"></span>
        <span class="demo-titlebar-dot demo-titlebar-dot--green"></span>
      </div>
      <div class="demo-content">
        <div class="demo-sidebar">
          <div class="demo-sidebar-title">输入主题关键词</div>
          <div class="demo-sidebar-input">如何提高工作效率</div>
          <div class="demo-sidebar-label">热门标签参考</div>
          <div class="demo-tag-group">
            <span class="demo-tag demo-tag--active">#效率提升</span>
            <span class="demo-tag demo-tag--active">#时间管理</span>
            <span class="demo-tag">#自律打卡</span>
            <span class="demo-tag">#工具推荐</span>
            <span class="demo-tag">#职场成长</span>
          </div>
          <div class="demo-sidebar-heading">爆款模式洞察</div>
          <div class="demo-analysis-item">
            <div class="demo-analysis-label">标题结构偏好</div>
            <div class="demo-analysis-value">痛点前置 + 解决方案承诺</div>
          </div>
          <div class="demo-analysis-item">
            <div class="demo-analysis-label">内容框架</div>
            <div class="demo-analysis-value">问题 → 误区 → 方法 → 工具</div>
          </div>
          <div class="demo-analysis-item">
            <div class="demo-analysis-label">最佳发布时间</div>
            <div class="demo-analysis-value">工作日 12:00-13:00 或 21:00-22:00</div>
          </div>
          <div class="demo-analysis-item">
            <div class="demo-analysis-label">平均互动量 TOP10</div>
            <div class="demo-analysis-value">1,847 次互动 / 篇</div>
          </div>
        </div>
        <div class="demo-main">
          <div class="demo-title-card">
            <span class="demo-title-radio demo-title-radio--selected"></span>
            <span class="demo-title-text">工作5年才懂的效率真相：别再瞎忙了，试试这4个方法</span>
          </div>
          <div class="demo-title-card">
            <span class="demo-title-radio"></span>
            <span class="demo-title-text">90%的人都在假装努力 | 高效工作者的3个底层习惯</span>
          </div>
          <div class="demo-title-card">
            <span class="demo-title-radio"></span>
            <span class="demo-title-text">告别低效加班：一个被验证有效的时间管理框架</span>
          </div>
          <div class="demo-body-block">
            <p class="demo-body-text"><strong>1. 两分钟规则</strong><br>能在两分钟内做完的事，立刻做。这条帮我干掉了 60% 的"假忙碌"。</p>
            <p class="demo-body-text" style="margin-top:8px;"><strong>2. 深度工作时间块</strong><br>每天固定 2 小时只做一件事，手机静音。</p>
            <p class="demo-body-text" style="margin-top:8px;"><strong>3. 周回顾机制</strong><br>每周五花 30 分钟复盘：哪些事有价值？</p>
            <div class="demo-body-line" style="margin-top:10px;"></div>
            <div class="demo-body-line"></div>
          </div>
          <div class="demo-actions-bar">
            <span class="demo-action-btn demo-action-btn--primary">一键复制文案</span>
            <span class="demo-action-btn demo-action-btn--ghost">人味化改写</span>
            <span class="demo-action-btn demo-action-btn--ghost">下载封面图</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</section>

<!-- Stats -->
<section class="stats" data-component="stats">
  <div class="container">
    <div class="stats-grid">
      <div>
        <div class="stat-number">12,400+</div>
        <div class="stat-label">博主正在使用</div>
      </div>
      <div>
        <div class="stat-number">86,000</div>
        <div class="stat-label">篇笔记已生成</div>
      </div>
      <div>
        <div class="stat-number">2.1x</div>
        <div class="stat-label">平均阅读量提升</div>
      </div>
      <div>
        <div class="stat-number">4.2 分钟</div>
        <div class="stat-label">从输入到成稿</div>
      </div>
    </div>
    <p class="stats-disclaimer">* 以上为产品设计阶段的目标数据，上线后以实际运营数据为准</p>

    <!-- Testimonials -->
    <div class="testimonials-grid">
      <div class="testimonial-card">
        <div class="testimonial-author">
          <div class="testimonial-avatar">周</div>
          <div>
            <div class="testimonial-name">周小北</div>
            <div class="testimonial-role">职场干货博主 · 2.3万粉丝</div>
          </div>
        </div>
        <p class="testimonial-quote">"以前写一篇笔记要花2小时选题+写稿，现在墨析帮我分析完爆款模式后直接生成，我只需要微调一下。日更终于不焦虑了。"</p>
      </div>
      <div class="testimonial-card">
        <div class="testimonial-author">
          <div class="testimonial-avatar">李</div>
          <div>
            <div class="testimonial-name">李明哲</div>
            <div class="testimonial-role">自我提升博主 · 8,600粉丝</div>
          </div>
        </div>
        <p class="testimonial-quote">"最喜欢的是'人味化'功能。之前用其他AI工具写的内容太假，同事一眼就能看出来。墨析生成的内容更像我自己写的风格。"</p>
      </div>
      <div class="testimonial-card">
        <div class="testimonial-author">
          <div class="testimonial-avatar">王</div>
          <div>
            <div class="testimonial-name">王小雨</div>
            <div class="testimonial-role">读书笔记博主 · 1.5万粉丝</div>
          </div>
        </div>
        <p class="testimonial-quote">"爆款分析真的有用。它告诉我标题用'数字清单型'更容易火，我试了一个月，笔记平均点赞从80涨到了260。"</p>
      </div>
    </div>
  </div>
</section>

<!-- Pricing -->
<section class="pricing" id="pricing" data-component="pricing">
  <div class="container">
    <div class="pricing-header">
      <div class="section-label">定价方案</div>
      <h2 class="section-title">选择适合你的方案</h2>
      <p class="section-desc">免费版足够日常使用，付费版解锁批量生成和高级数据能力。</p>
      <div style="display:flex;align-items:center;justify-content:center;gap:var(--space-sm);margin-top:var(--space-md);">
        <button type="button" id="monthlyLabel" style="font-size:14px;font-weight:600;color:var(--fg);cursor:pointer;background:none;border:none;font-family:inherit;" onclick="toggleBilling('monthly')">月付</button>
        <button onclick="toggleBilling('annual')" style="position:relative;width:44px;height:24px;border-radius:12px;background:var(--border);border:none;cursor:pointer;transition:background 0.2s;" id="billingToggle" role="switch" aria-checked="false" aria-label="切换年付">
          <span id="billingDot" style="position:absolute;top:2px;left:2px;width:20px;height:20px;border-radius:50%;background:#fff;box-shadow:0 1px 3px rgba(0,0,0,0.15);transition:left 0.2s;"></span>
        </button>
        <button type="button" id="annualLabel" style="font-size:14px;color:var(--muted);cursor:pointer;background:none;border:none;font-family:inherit;" onclick="toggleBilling('annual')">年付 <span style="font-size:12px;color:var(--success);font-weight:600;">省20%</span></button>
      </div>
    </div>

    <div class="pricing-grid">
      <!-- Free -->
      <div class="pricing-card" data-component="pricing-card">
        <div class="pricing-plan-name">免费版</div>
        <div class="pricing-plan-desc">适合想先体验的博主，每天 3 篇足够日常更新。</div>
        <div class="pricing-amount">
          <span class="pricing-currency">&#165;</span>
          <span class="pricing-value">0</span>
          <span class="pricing-period">/月</span>
        </div>
        <ul class="pricing-features">
          <li>每天生成 3 篇笔记</li>
          <li>基础爆款分析</li>
          <li>AI 封面图生成（1 张/篇）</li>
          <li>敏感词基础检测</li>
          <li>小红书样式预览</li>
        </ul>
        <a href="#/workbench" class="pricing-btn pricing-btn--outline">免费开始</a>
      </div>

      <!-- Basic -->
      <div class="pricing-card pricing-card--featured" data-component="pricing-card">
        <div class="pricing-card-badge">最受欢迎</div>
        <div class="pricing-plan-name">基础版</div>
        <div class="pricing-plan-desc">认真做账号的博主，日均生成不限量。</div>
        <div class="pricing-amount">
          <span class="pricing-currency">&#165;</span>
          <span class="pricing-value" data-monthly="29" data-annual="23">29</span>
          <span class="pricing-period">/月</span>
        </div>
        <div class="annual-note" style="font-size:12px;color:var(--muted);margin-top:-12px;margin-bottom:var(--space-md);display:none;">年付 ¥276/年（省 ¥72）</div>
        <ul class="pricing-features">
          <li>每天不限量生成笔记</li>
          <li>深度爆款数据分析</li>
          <li>AI 封面图生成（3 张/篇）</li>
          <li>「人味化」改写功能</li>
          <li>高级敏感词检测</li>
          <li>生成历史保留 30 天</li>
        </ul>
        <a href="#/workbench" class="pricing-btn pricing-btn--filled">立即订阅</a>
      </div>

      <!-- Pro -->
      <div class="pricing-card" data-component="pricing-card">
        <div class="pricing-plan-name">专业版</div>
        <div class="pricing-plan-desc">多账号矩阵运营，全功能解锁。</div>
        <div class="pricing-amount">
          <span class="pricing-currency">&#165;</span>
          <span class="pricing-value" data-monthly="79" data-annual="63">79</span>
          <span class="pricing-period">/月</span>
        </div>
        <div class="annual-note" style="font-size:12px;color:var(--muted);margin-top:-12px;margin-bottom:var(--space-md);display:none;">年付 ¥756/年（省 ¥192）</div>
        <ul class="pricing-features">
          <li>基础版全部功能</li>
          <li>批量生成（一次最多 10 篇）</li>
          <li>竞品账号数据追踪</li>
          <li>个人风格克隆训练</li>
          <li>API 接口调用</li>
          <li>生成历史永久保留</li>
          <li>优先客服支持</li>
        </ul>
        <a href="#/workbench" class="pricing-btn pricing-btn--outline">选择专业版</a>
      </div>
    </div>
  </div>
</section>

<!-- CTA -->
<section class="cta-section" id="cta" data-component="cta">
  <div class="container">
    <div class="section-label">准备好了吗</div>
    <h2 class="section-title">让数据帮你写出更好的笔记</h2>
    <p class="section-desc">注册即用，免费版每天 3 篇，不绑定支付方式。</p>
    <div style="display:flex;align-items:center;justify-content:center;gap:var(--space-xl);margin-bottom:var(--space-lg);">
      <span style="display:flex;align-items:center;gap:6px;font-size:14px;color:var(--fg-secondary);">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--success)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
        免费开始
      </span>
      <span style="display:flex;align-items:center;gap:6px;font-size:14px;color:var(--fg-secondary);">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--success)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
        无需信用卡
      </span>
      <span style="display:flex;align-items:center;gap:6px;font-size:14px;color:var(--fg-secondary);">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--success)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
        30秒注册
      </span>
    </div>
    <a href="#/workbench" class="btn-primary">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></svg>
      免费开始创作
    </a>
  </div>
</section>

<!-- Footer -->
<footer class="footer">
  <div class="container">
    <div class="footer-inner" style="flex-wrap:wrap;">
      <span class="footer-brand">墨析 MoXi</span>
      <ul class="footer-links">
        <li><a href="#">产品介绍</a></li>
        <li><a href="#">定价</a></li>
        <li><a href="#">使用教程</a></li>
        <li><a href="#">帮助中心</a></li>
        <li><a href="#">隐私政策</a></li>
        <li><a href="#">服务条款</a></li>
      </ul>
      <div style="display:flex;align-items:center;gap:var(--space-lg);">
        <span class="footer-copy">&copy; 2026 墨析 MoXi</span>
        <span style="font-size:12px;color:var(--muted);">京ICP备2026XXXXXX号</span>
      </div>
    </div>
    <div style="display:flex;align-items:center;gap:var(--space-md);margin-top:var(--space-md);justify-content:center;">
      <a href="#" title="微信公众号" style="color:var(--muted);transition:color 0.2s;" onmouseover="this.style.color='var(--fg)'" onmouseout="this.style.color='var(--muted)'">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M8.5 10a.5.5 0 1 0 0-1 .5.5 0 0 0 0 1Z"/><path d="M5.5 10a.5.5 0 1 0 0-1 .5.5 0 0 0 0 1Z"/><path d="M16.5 15a.5.5 0 1 0 0-1 .5.5 0 0 0 0 1Z"/><path d="M13.5 15a.5.5 0 1 0 0-1 .5.5 0 0 0 0 1Z"/><path d="M9 19c-4.286 0-7-2.5-7-5.5S4.714 8 9 8s7 2.5 7 5.5c0 1-.5 2-1 2.5l.5 2.5-2-1.5c-1 .5-2.5 1-4.5 1Z"/><path d="M15 22c3.5 0 6-2 6-4.5S18.5 13 15 13s-6 2-6 4.5c0 .8.3 1.5.8 2l-.4 2 1.6-1.2c.8.3 1.8.7 3 .7Z"/></svg>
      </a>
      <a href="#" title="小红书" style="color:var(--muted);transition:color 0.2s;" onmouseover="this.style.color='var(--fg)'" onmouseout="this.style.color='var(--muted)'">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M8 12h8"/><path d="M12 8v8"/></svg>
      </a>
      <a href="#" title="微博" style="color:var(--muted);transition:color 0.2s;" onmouseover="this.style.color='var(--fg)'" onmouseout="this.style.color='var(--muted)'">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><line x1="9" y1="9" x2="9.01" y2="9"/><line x1="15" y1="9" x2="15.01" y2="9"/></svg>
      </a>
    </div>
  </div>
</footer>

</template>

<style scoped>
  :root {
    --seed-bg: #ffffff;
    --seed-fg: #111111;
    --seed-primary: #c2620d;
    --seed-accent: #d97706;
    --seed-surface: #faf9f7;
    --seed-radius: 22px;

    --bg: var(--seed-bg);
    --bg-warm: #faf9f7;
    --bg-subtle: #f5f3ef;
    --surface: #ffffff;
    --fg: var(--seed-fg);
    --fg-secondary: #4a4a4a;
    --muted: #8b8b8b;
    --border: #e8e5e0;
    --border-light: #f0ede8;
    --accent: var(--seed-accent);
    --accent-hover: #b45309;
    --accent-light: #fef3c7;
    --accent-surface: #fffbeb;
    --success: #059669;
    --warn: #d97706;
    --danger: #dc2626;

    --font-display: "Georgia", "Source Han Serif SC", "Noto Serif SC", "Songti SC", serif;
    --font-body: "PingFang SC", "Source Han Sans SC", "Noto Sans SC", -apple-system, "Helvetica Neue", sans-serif;

    --space-xs: 4px;
    --space-sm: 8px;
    --space-md: 16px;
    --space-lg: 24px;
    --space-xl: 40px;
    --space-2xl: 64px;
    --space-3xl: 96px;
    --space-4xl: 128px;

    --radius-sm: 8px;
    --radius-md: var(--seed-radius);
    --radius-lg: 24px;
    --radius-pill: 9999px;

    --max-width: 1120px;
  }

  *, *::before, *::after {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
  }

  html {
    font-size: 16px;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    scroll-behavior: smooth;
  }

  body {
    font-family: var(--font-body);
    font-weight: 400;
    line-height: 1.6;
    color: var(--fg);
    background: var(--bg);
  }

  /* Focus-visible for accessibility */
  :focus-visible {
    outline: 2px solid var(--accent);
    outline-offset: 2px;
    border-radius: 4px;
  }
  button:focus-visible, a:focus-visible {
    outline: 2px solid var(--accent);
    outline-offset: 2px;
  }

  img { max-width: 100%; display: block; }
  a { color: inherit; text-decoration: none; }
  button { font-family: inherit; cursor: pointer; border: none; background: none; }

  .container {
    max-width: var(--max-width);
    margin: 0 auto;
    padding: 0 var(--space-lg);
  }

  /* ─── NAV ─── */
  .nav {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 100;
    background: rgba(255,255,255,0.92);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    border-bottom: 1px solid var(--border-light);
  }
  .nav-inner {
    max-width: var(--max-width);
    margin: 0 auto;
    padding: 0 var(--space-lg);
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .nav-logo {
    display: flex;
    align-items: center;
    gap: 10px;
    font-family: var(--font-display);
    font-size: 22px;
    font-weight: 600;
    letter-spacing: -0.02em;
    color: var(--fg);
  }
  .nav-logo-icon {
    width: 32px;
    height: 32px;
    background: var(--accent);
    border-radius: var(--space-sm);
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .nav-links {
    display: flex;
    align-items: center;
    gap: var(--space-xl);
    list-style: none;
  }
  .nav-links a {
    font-size: 14px;
    font-weight: 500;
    letter-spacing: 0.02em;
    color: var(--fg-secondary);
    transition: color 0.2s;
  }
  .nav-links a:hover { color: var(--fg); }
  .nav-cta {
    display: inline-flex;
    align-items: center;
    padding: 8px 20px;
    background: var(--fg);
    color: var(--bg);
    font-size: 14px;
    font-weight: 500;
    letter-spacing: 0.02em;
    border-radius: var(--radius-pill);
    transition: opacity 0.2s;
  }
  .nav-cta:hover { opacity: 0.85; }

  .nav-mobile-toggle {
    display: none;
    width: 40px;
    height: 40px;
    align-items: center;
    justify-content: center;
  }

  /* ─── HERO ─── */
  .hero {
    padding: 160px 0 var(--space-3xl);
    position: relative;
    overflow: hidden;
  }
  .hero::before {
    content: "";
    position: absolute;
    top: -120px;
    right: -200px;
    width: 600px;
    height: 600px;
    background: radial-gradient(circle, rgba(217,119,6,0.06) 0%, transparent 70%);
    pointer-events: none;
  }
  .hero-inner {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: var(--space-2xl);
    align-items: center;
  }
  .hero-content { position: relative; z-index: 1; }
  .hero-badge {
    display: inline-flex;
    align-items: center;
    gap: var(--space-sm);
    padding: 6px 14px;
    background: var(--accent-surface);
    border: 1px solid var(--accent-light);
    border-radius: var(--radius-pill);
    font-size: 13px;
    font-weight: 500;
    letter-spacing: 0.02em;
    color: var(--accent-hover);
    margin-bottom: var(--space-lg);
  }
  .hero-badge-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--accent);
  }
  .hero-title {
    font-family: var(--font-display);
    font-size: 52px;
    font-weight: 600;
    line-height: 1.1;
    letter-spacing: -0.03em;
    color: var(--fg);
    margin-bottom: var(--space-lg);
  }
  .hero-title-highlight {
    color: var(--accent);
  }
  .hero-subtitle {
    font-size: 18px;
    line-height: 1.6;
    color: var(--fg-secondary);
    max-width: 480px;
    margin-bottom: var(--space-xl);
  }
  .hero-actions {
    display: flex;
    align-items: center;
    gap: var(--space-md);
  }
  .btn-primary {
    display: inline-flex;
    align-items: center;
    gap: var(--space-sm);
    padding: 14px 32px;
    background: var(--accent);
    color: #fff;
    font-size: 16px;
    font-weight: 500;
    letter-spacing: 0.02em;
    border-radius: var(--radius-pill);
    transition: background 0.2s, transform 0.15s;
  }
  .btn-primary:hover { background: var(--accent-hover); transform: translateY(-1px); }
  .btn-secondary {
    display: inline-flex;
    align-items: center;
    gap: var(--space-sm);
    padding: 14px 28px;
    background: transparent;
    color: var(--fg);
    font-size: 16px;
    font-weight: 500;
    letter-spacing: 0.02em;
    border: 1.5px solid var(--border);
    border-radius: var(--radius-pill);
    transition: border-color 0.2s;
  }
  .btn-secondary:hover { border-color: var(--fg); }
  .hero-note {
    margin-top: var(--space-md);
    font-size: 13px;
    color: var(--muted);
  }

  /* Hero Mockup */
  .hero-mockup {
    position: relative;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius-lg);
    padding: var(--space-lg);
    box-shadow: 0 8px 40px rgba(0,0,0,0.04);
  }
  .mockup-header {
    display: flex;
    align-items: center;
    gap: var(--space-sm);
    padding-bottom: var(--space-md);
    border-bottom: 1px solid var(--border-light);
    margin-bottom: var(--space-md);
  }
  .mockup-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: var(--border);
  }
  .mockup-url {
    flex: 1;
    height: 28px;
    background: var(--bg-subtle);
    border-radius: var(--radius-sm);
    margin-left: var(--space-sm);
    display: flex;
    align-items: center;
    padding: 0 12px;
    font-size: 12px;
    color: var(--muted);
  }
  .mockup-body { padding: var(--space-sm) 0; }
  .mockup-input-row {
    display: flex;
    gap: var(--space-sm);
    margin-bottom: var(--space-md);
  }
  .mockup-input {
    flex: 1;
    height: 40px;
    background: var(--bg-subtle);
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    padding: 0 14px;
    font-size: 13px;
    color: var(--fg-secondary);
    display: flex;
    align-items: center;
  }
  .mockup-btn {
    height: 40px;
    padding: 0 18px;
    background: var(--accent);
    color: #fff;
    font-size: 13px;
    font-weight: 500;
    border-radius: var(--radius-sm);
    display: flex;
    align-items: center;
    white-space: nowrap;
  }
  .mockup-cards {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: var(--space-sm);
    margin-bottom: var(--space-md);
  }
  .mockup-card {
    padding: 12px;
    background: var(--bg-warm);
    border-radius: var(--radius-sm);
    border: 1px solid var(--border-light);
  }
  .mockup-card-label {
    font-size: 11px;
    color: var(--muted);
    margin-bottom: 4px;
  }
  .mockup-card-value {
    font-size: 18px;
    font-weight: 600;
    color: var(--fg);
    letter-spacing: -0.02em;
  }
  .mockup-card-change {
    font-size: 11px;
    color: var(--success);
    margin-top: 2px;
  }
  .mockup-text-block {
    padding: 14px;
    background: var(--bg-warm);
    border-radius: var(--radius-sm);
    border: 1px solid var(--border-light);
  }
  .mockup-text-line {
    height: 8px;
    background: var(--border);
    border-radius: 4px;
    margin-bottom: 8px;
  }
  .mockup-text-line:nth-child(2) { width: 85%; }
  .mockup-text-line:nth-child(3) { width: 70%; }
  .mockup-text-line:last-child { margin-bottom: 0; width: 60%; }

  /* ─── FEATURES ─── */
  .features {
    padding: var(--space-4xl) 0;
    background: var(--bg-warm);
  }
  .section-label {
    font-size: 13px;
    font-weight: 600;
    letter-spacing: 0.02em;
    color: var(--accent);
    margin-bottom: var(--space-md);
  }
  .section-title {
    font-family: var(--font-display);
    font-size: 36px;
    font-weight: 600;
    line-height: 1.15;
    letter-spacing: -0.02em;
    color: var(--fg);
    margin-bottom: var(--space-md);
  }
  .section-desc {
    font-size: 17px;
    line-height: 1.6;
    color: var(--fg-secondary);
    max-width: 560px;
    margin-bottom: var(--space-2xl);
  }
  .features-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: var(--space-lg);
  }
  .feature-card {
    padding: var(--space-xl);
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius-md);
    transition: border-color 0.2s;
  }
  .feature-card:hover { border-color: var(--accent); }
  .feature-step {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: var(--accent-surface);
    color: var(--accent);
    font-size: 15px;
    font-weight: 600;
    margin-bottom: var(--space-lg);
  }
  .feature-card-title {
    font-family: var(--font-display);
    font-size: 22px;
    font-weight: 600;
    line-height: 1.2;
    letter-spacing: -0.01em;
    color: var(--fg);
    margin-bottom: var(--space-sm);
  }
  .feature-card-desc {
    font-size: 15px;
    line-height: 1.6;
    color: var(--fg-secondary);
  }
  .feature-card-detail {
    margin-top: var(--space-md);
    padding-top: var(--space-md);
    border-top: 1px solid var(--border-light);
    font-size: 13px;
    color: var(--muted);
    line-height: 1.5;
  }

  /* ─── PRODUCT DEMO ─── */
  .demo {
    padding: var(--space-4xl) 0;
  }
  .demo-header {
    text-align: center;
    margin-bottom: var(--space-2xl);
  }
  .demo-header .section-desc { margin: 0 auto; }
  .demo-window {
    max-width: 960px;
    margin: 0 auto;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius-lg);
    overflow: hidden;
    box-shadow: 0 12px 48px rgba(0,0,0,0.06);
  }
  .demo-titlebar {
    height: 48px;
    background: var(--bg-warm);
    border-bottom: 1px solid var(--border);
    display: flex;
    align-items: center;
    padding: 0 var(--space-md);
    gap: var(--space-sm);
  }
  .demo-titlebar-dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
  }
  .demo-titlebar-dot--red { background: #ff5f57; }
  .demo-titlebar-dot--yellow { background: #febc2e; }
  .demo-titlebar-dot--green { background: #28c840; }
  .demo-content {
    display: grid;
    grid-template-columns: 380px 1fr;
    min-height: 420px;
  }
  .demo-sidebar {
    padding: var(--space-lg);
    border-right: 1px solid var(--border-light);
    background: var(--bg-warm);
  }
  .demo-sidebar-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--fg);
    margin-bottom: var(--space-md);
    letter-spacing: 0.02em;
  }
  .demo-sidebar-input {
    width: 100%;
    padding: 10px 14px;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    font-size: 14px;
    color: var(--fg);
    margin-bottom: var(--space-md);
    display: flex;
    align-items: center;
  }
  .demo-tag-group {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-bottom: var(--space-lg);
  }
  .demo-tag {
    padding: 4px 10px;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius-pill);
    font-size: 12px;
    color: var(--fg-secondary);
  }
  .demo-tag--active {
    background: var(--accent-surface);
    border-color: var(--accent);
    color: var(--accent-hover);
  }
  .demo-analysis-item {
    padding: 10px 0;
    border-bottom: 1px solid var(--border-light);
  }
  .demo-analysis-item:last-child { border-bottom: none; }
  .demo-analysis-label {
    font-size: 12px;
    color: var(--muted);
    margin-bottom: 4px;
  }
  .demo-analysis-value {
    font-size: 14px;
    color: var(--fg);
    font-weight: 500;
  }
  .demo-main {
    padding: var(--space-lg);
    display: flex;
    flex-direction: column;
    gap: var(--space-md);
  }
  .demo-title-card {
    padding: 12px 16px;
    background: var(--bg-warm);
    border: 1px solid var(--border-light);
    border-radius: var(--radius-sm);
    display: flex;
    align-items: center;
    gap: var(--space-sm);
  }
  .demo-title-radio {
    width: 16px;
    height: 16px;
    border-radius: 50%;
    border: 2px solid var(--border);
    flex-shrink: 0;
  }
  .demo-title-radio--selected {
    border-color: var(--accent);
    background: var(--accent);
    box-shadow: inset 0 0 0 3px #fff;
  }
  .demo-title-text {
    font-size: 15px;
    font-weight: 500;
    color: var(--fg);
  }
  .demo-body-block {
    padding: 16px;
    background: var(--bg-warm);
    border: 1px solid var(--border-light);
    border-radius: var(--radius-sm);
    flex: 1;
  }
  .demo-body-line {
    height: 8px;
    background: var(--border);
    border-radius: 4px;
    margin-bottom: 10px;
  }
  .demo-body-line:nth-child(1) { width: 100%; }
  .demo-body-line:nth-child(2) { width: 92%; }
  .demo-body-line:nth-child(3) { width: 78%; }
  .demo-body-line:nth-child(4) { width: 88%; }
  .demo-body-line:nth-child(5) { width: 65%; }
  .demo-body-line:nth-child(6) { width: 95%; }
  .demo-body-line:last-child { margin-bottom: 0; }
  .demo-sidebar-label {
    font-size: 12px;
    color: var(--muted);
    margin-bottom: 8px;
    letter-spacing: 0.02em;
  }
  .demo-sidebar-heading {
    font-size: 13px;
    font-weight: 600;
    color: var(--fg);
    margin-bottom: 12px;
  }
  .demo-body-text {
    font-size: 13px;
    line-height: 1.7;
    color: var(--fg-secondary);
  }
  .demo-body-text strong { color: var(--fg); font-weight: 600; }
  .demo-actions-bar {
    display: flex;
    align-items: center;
    gap: var(--space-sm);
  }
  .demo-action-btn {
    padding: 8px 16px;
    border-radius: var(--radius-sm);
    font-size: 13px;
    font-weight: 500;
  }
  .demo-action-btn--primary {
    background: var(--accent);
    color: #fff;
  }
  .demo-action-btn--ghost {
    background: transparent;
    color: var(--fg-secondary);
    border: 1px solid var(--border);
  }

  /* ─── STATS ─── */
  .stats {
    padding: var(--space-3xl) 0;
    background: var(--bg-warm);
    border-top: 1px solid var(--border-light);
    border-bottom: 1px solid var(--border-light);
  }
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: var(--space-lg);
    text-align: center;
  }
  .stat-number {
    font-family: var(--font-display);
    font-size: 42px;
    font-weight: 600;
    letter-spacing: -0.03em;
    color: var(--fg);
    margin-bottom: var(--space-xs);
  }
  .stat-label {
    font-size: 14px;
    color: var(--muted);
    letter-spacing: 0.02em;
  }

  /* ─── PRICING ─── */
  .pricing {
    padding: var(--space-4xl) 0;
  }
  .pricing-header {
    text-align: center;
    margin-bottom: var(--space-2xl);
  }
  .pricing-header .section-desc { margin: 0 auto; }
  .pricing-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: var(--space-lg);
    max-width: 900px;
    margin: 0 auto;
  }
  .pricing-card {
    padding: var(--space-xl);
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius-md);
    display: flex;
    flex-direction: column;
  }
  .pricing-card--featured {
    border-color: var(--accent);
    position: relative;
    box-shadow: 0 8px 32px rgba(194,98,13,0.08);
  }
  .pricing-card-badge {
    position: absolute;
    top: -12px;
    left: 50%;
    transform: translateX(-50%);
    padding: 4px 16px;
    background: var(--accent);
    color: #fff;
    font-size: 12px;
    font-weight: 600;
    letter-spacing: 0.04em;
    border-radius: var(--radius-pill);
    white-space: nowrap;
  }
  .pricing-plan-name {
    font-family: var(--font-display);
    font-size: 20px;
    font-weight: 600;
    color: var(--fg);
    margin-bottom: var(--space-sm);
  }
  .pricing-plan-desc {
    font-size: 14px;
    color: var(--muted);
    margin-bottom: var(--space-lg);
    line-height: 1.5;
  }
  .pricing-amount {
    display: flex;
    align-items: baseline;
    gap: 4px;
    margin-bottom: var(--space-lg);
  }
  .pricing-currency {
    font-size: 18px;
    font-weight: 600;
    color: var(--fg);
  }
  .pricing-value {
    font-family: var(--font-display);
    font-size: 42px;
    font-weight: 600;
    letter-spacing: -0.03em;
    color: var(--fg);
    line-height: 1;
  }
  .pricing-period {
    font-size: 14px;
    color: var(--muted);
  }
  .pricing-features {
    list-style: none;
    margin-bottom: var(--space-lg);
    flex: 1;
  }
  .pricing-features li {
    padding: 8px 0;
    font-size: 14px;
    color: var(--fg-secondary);
    display: flex;
    align-items: flex-start;
    gap: var(--space-sm);
    line-height: 1.5;
  }
  .pricing-features li::before {
    content: "";
    display: inline-block;
    width: 18px;
    height: 18px;
    margin-top: 2px;
    flex-shrink: 0;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23059669' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='20 6 9 17 4 12'/%3E%3C/svg%3E");
    background-size: contain;
    background-repeat: no-repeat;
  }
  .pricing-btn {
    display: block;
    width: 100%;
    padding: 12px;
    text-align: center;
    font-size: 15px;
    font-weight: 500;
    letter-spacing: 0.02em;
    border-radius: var(--radius-sm);
    transition: all 0.2s;
  }
  .pricing-btn--outline {
    border: 1.5px solid var(--border);
    color: var(--fg);
  }
  .pricing-btn--outline:hover { border-color: var(--fg); }
  .pricing-btn--filled {
    background: var(--accent);
    color: #fff;
  }
  .pricing-btn--filled:hover { background: var(--accent-hover); }

  /* ─── CTA ─── */
  .cta-section {
    padding: var(--space-3xl) 0;
    text-align: center;
    background: var(--bg-warm);
    border-top: 1px solid var(--border-light);
  }
  .cta-section .section-title { margin-bottom: var(--space-sm); }
  .cta-section .section-desc {
    margin: 0 auto var(--space-lg);
    max-width: 480px;
  }

  /* ─── FOOTER ─── */
  .footer {
    padding: var(--space-xl) 0;
    border-top: 1px solid var(--border-light);
  }
  .footer-inner {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .footer-brand {
    font-family: var(--font-display);
    font-size: 16px;
    font-weight: 600;
    color: var(--fg);
  }
  .footer-links {
    display: flex;
    gap: var(--space-lg);
    list-style: none;
  }
  .footer-links a {
    font-size: 13px;
    color: var(--muted);
    transition: color 0.2s;
  }
  .footer-links a:hover { color: var(--fg); }
  .footer-copy {
    font-size: 13px;
    color: var(--muted);
  }

  /* Testimonials */
  .testimonials-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: var(--space-lg);
    margin-top: var(--space-2xl);
  }
  .testimonial-card {
    padding: var(--space-lg);
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius-md);
  }
  .testimonial-author {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;
  }
  .testimonial-avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: var(--accent-light);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 600;
    color: var(--accent-hover);
  }
  .testimonial-name {
    font-size: 14px;
    font-weight: 600;
    color: var(--fg);
  }
  .testimonial-role {
    font-size: 12px;
    color: var(--muted);
  }
  .testimonial-quote {
    font-size: 14px;
    line-height: 1.6;
    color: var(--fg-secondary);
  }
  .stats-disclaimer {
    text-align: center;
    font-size: 12px;
    color: var(--muted);
    margin-top: var(--space-md);
  }

  /* ─── RESPONSIVE ─── */
  @media (max-width: 1024px) {
    .hero-inner { grid-template-columns: 1fr; }
    .hero-mockup { margin-top: var(--space-xl); }
    .hero-title { font-size: 40px; }
    .demo-content { grid-template-columns: 1fr; }
    .demo-sidebar { border-right: none; border-bottom: 1px solid var(--border-light); }
  }

  @media (max-width: 768px) {
    .nav-links { display: none; }
    .nav-mobile-toggle { display: flex; }
    .hero { padding: 120px 0 var(--space-2xl); }
    .hero-title { font-size: 32px; }
    .hero-subtitle { font-size: 16px; }
    .hero-actions { flex-direction: column; align-items: flex-start; }
    .section-title { font-size: 28px; }
    .features-grid { grid-template-columns: 1fr; }
    .stats-grid { grid-template-columns: repeat(2, 1fr); gap: var(--space-xl); }
    .stat-number { font-size: 32px; }
    .pricing-grid { grid-template-columns: 1fr; max-width: 400px; }
    .testimonials-grid { grid-template-columns: 1fr; }
    .footer-inner { flex-direction: column; gap: var(--space-md); text-align: center; }
    .footer-links { flex-wrap: wrap; justify-content: center; }
  }
</style>
