<script setup lang="ts">
</script>

<template>
<!-- Main Content -->
<div class="main">
  <!-- Page Header -->
  <div class="page-header">
    <div>
      <div class="page-title">数据看板</div>
      <div class="page-subtitle">追踪知识干货赛道的热门趋势，找到下一个爆款选题</div>
      <div class="page-subtitle" style="margin-top:4px;font-size:12px;color:var(--muted);">* 页面数据为示例展示，上线后基于实际搜索分析</div>
    </div>
    <div class="page-filter">
      <button class="filter-btn filter-btn--active">近 7 天</button>
      <button class="filter-btn">近 30 天</button>
      <button class="filter-btn" style="display:inline-flex;align-items:center;gap:4px;">
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="4" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
  自定义
</button>
    </div>
  </div>

  <!-- Overview Cards -->
  <div class="overview-grid">
    <div class="overview-card">
      <div class="overview-card-label">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>
        本周热门话题
      </div>
      <div class="overview-card-value">24</div>
      <div class="overview-card-change change-up">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="m18 15-6-6-6 6"/></svg>
        较上周 +6
      </div>
    </div>
    <div class="overview-card">
      <div class="overview-card-label">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3v18h18"/><path d="m19 9-5 5-4-4-3 3"/></svg>
        知识赛道互动均值
      </div>
      <div class="overview-card-value">1,847</div>
      <div class="overview-card-change change-up">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="m18 15-6-6-6 6"/></svg>
        +12.3%
      </div>
    </div>
    <div class="overview-card">
      <div class="overview-card-label">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
        最佳发布时段
      </div>
      <div class="overview-card-value">20-22 点</div>
      <div class="overview-card-change" style="color:var(--muted);">工作日互动峰值</div>
    </div>
    <div class="overview-card">
      <div class="overview-card-label">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
        爆款标题模式
      </div>
      <div class="overview-card-value">数字清单</div>
      <div class="overview-card-change" style="color:var(--muted);">占爆款笔记 47%</div>
    </div>
  </div>

  <!-- Two Column Layout -->
  <div class="content-grid">
    <!-- Left: Main Content -->
    <div>
      <!-- Hot Topics -->
      <div class="panel">
        <div class="panel-header">
          <div class="panel-title">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/></svg>
            知识赛道热门话题
            <span class="panel-badge">实时更新</span>
          </div>
          <button class="panel-action">
            查看全部
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg>
          </button>
        </div>
        <ul class="topic-list">
          <li class="topic-item">
            <span class="topic-rank topic-rank--1">1</span>
            <div class="topic-info">
              <div class="topic-title">AI工具提效指南：打工人必备的10个神器</div>
              <div class="topic-meta">
                <span class="topic-heat">
                  <span class="heat-bar"><span class="heat-bar-fill" style="width:95%"></span></span>
                </span>
                <span>近7天 2,340 篇笔记</span>
                <span>·</span>
                <span>互动 4.8万</span>
              </div>
            </div>
            <button class="topic-action">去创作</button>
          </li>
          <li class="topic-item">
            <span class="topic-rank topic-rank--2">2</span>
            <div class="topic-info">
              <div class="topic-title">30岁才明白的职场真相：别把同事当朋友</div>
              <div class="topic-meta">
                <span class="topic-heat">
                  <span class="heat-bar"><span class="heat-bar-fill" style="width:88%"></span></span>
                </span>
                <span>近7天 1,870 篇笔记</span>
                <span>·</span>
                <span>互动 3.6万</span>
              </div>
            </div>
            <button class="topic-action">去创作</button>
          </li>
          <li class="topic-item">
            <span class="topic-rank topic-rank--3">3</span>
            <div class="topic-info">
              <div class="topic-title">读书笔记怎么发？这个方法让我月涨3000粉</div>
              <div class="topic-meta">
                <span class="topic-heat">
                  <span class="heat-bar"><span class="heat-bar-fill" style="width:82%"></span></span>
                </span>
                <span>近7天 1,520 篇笔记</span>
                <span>·</span>
                <span>互动 2.9万</span>
              </div>
            </div>
            <button class="topic-action">去创作</button>
          </li>
          <li class="topic-item">
            <span class="topic-rank topic-rank--default">4</span>
            <div class="topic-info">
              <div class="topic-title">时间管理不是自律，是系统设计</div>
              <div class="topic-meta">
                <span class="topic-heat">
                  <span class="heat-bar"><span class="heat-bar-fill" style="width:71%"></span></span>
                </span>
                <span>近7天 980 篇笔记</span>
                <span>·</span>
                <span>互动 2.1万</span>
              </div>
            </div>
            <button class="topic-action">去创作</button>
          </li>
          <li class="topic-item">
            <span class="topic-rank topic-rank--default">5</span>
            <div class="topic-info">
              <div class="topic-title">副业从0到月入5000：我踩过的5个坑</div>
              <div class="topic-meta">
                <span class="topic-heat">
                  <span class="heat-bar"><span class="heat-bar-fill" style="width:65%"></span></span>
                </span>
                <span>近7天 860 篇笔记</span>
                <span>·</span>
                <span>互动 1.8万</span>
              </div>
            </div>
            <button class="topic-action">去创作</button>
          </li>
          <li class="topic-item">
            <span class="topic-rank topic-rank--default">6</span>
            <div class="topic-info">
              <div class="topic-title">高效学习法：费曼技巧的3个实践步骤</div>
              <div class="topic-meta">
                <span class="topic-heat">
                  <span class="heat-bar"><span class="heat-bar-fill" style="width:58%"></span></span>
                </span>
                <span>近7天 720 篇笔记</span>
                <span>·</span>
                <span>互动 1.4万</span>
              </div>
            </div>
            <button class="topic-action">去创作</button>
          </li>
        </ul>
      </div>

      <!-- Content Pattern Analysis -->
      <div class="panel pattern-section">
        <div class="panel-header">
          <div class="panel-title">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/></svg>
            爆款内容模式分析
            <span class="panel-badge">知识赛道</span>
          </div>
        </div>
        <div class="pattern-grid">
          <div class="pattern-card">
            <div class="pattern-card-header">
              <div class="pattern-card-title">标题结构：数字清单型</div>
              <div class="pattern-card-pct">47%</div>
            </div>
            <div class="pattern-card-desc">标题中包含具体数字 + 清单式承诺，如"5个方法""3个步骤"</div>
            <div class="pattern-card-example">例：工作5年才懂的效率真相：别再瞎忙了，试试这4个方法</div>
          </div>
          <div class="pattern-card">
            <div class="pattern-card-header">
              <div class="pattern-card-title">标题结构：痛点前置型</div>
              <div class="pattern-card-pct">28%</div>
            </div>
            <div class="pattern-card-desc">先说出读者的痛点场景，再给出解决方向</div>
            <div class="pattern-card-example">例：90%的人都在假装努力 | 高效工作者的3个底层习惯</div>
          </div>
          <div class="pattern-card">
            <div class="pattern-card-header">
              <div class="pattern-card-title">正文框架：问题→方法</div>
              <div class="pattern-card-pct">52%</div>
            </div>
            <div class="pattern-card-desc">开头抛出问题/痛点，中间给出结构化方法，结尾总结行动</div>
            <div class="pattern-card-example">结构：个人经历引入 → 常见误区 → 正确方法（3-5条） → 行动建议</div>
          </div>
          <div class="pattern-card">
            <div class="pattern-card-header">
              <div class="pattern-card-title">配图风格：信息图表</div>
              <div class="pattern-card-pct">41%</div>
            </div>
            <div class="pattern-card-desc">使用流程图、对比表、清单截图等信息密度高的配图</div>
            <div class="pattern-card-example">趋势：纯美学配图互动率下降，信息图互动率高出 1.8 倍</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Right Sidebar -->
    <div class="sidebar-stack">
      <!-- Tag Trends -->
      <div class="panel">
        <div class="panel-header">
          <div class="panel-title">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
            热门标签趋势
          </div>
          <span style="font-size:12px;color:var(--muted);">近7天</span>
        </div>
        <div class="tag-chart">
          <div class="chart-bar-group">
            <div class="chart-bar-row">
              <span class="chart-bar-label">#效率提升</span>
              <div class="chart-bar-track">
                <div class="chart-bar-fill chart-bar-fill--1" style="width:92%">
                  <span class="chart-bar-value">8.2万</span>
                </div>
              </div>
              <span class="chart-bar-change">+18%</span>
            </div>
            <div class="chart-bar-row">
              <span class="chart-bar-label">#时间管理</span>
              <div class="chart-bar-track">
                <div class="chart-bar-fill chart-bar-fill--2" style="width:78%">
                  <span class="chart-bar-value">6.7万</span>
                </div>
              </div>
              <span class="chart-bar-change">+12%</span>
            </div>
            <div class="chart-bar-row">
              <span class="chart-bar-label">#职场干货</span>
              <div class="chart-bar-track">
                <div class="chart-bar-fill chart-bar-fill--3" style="width:65%">
                  <span class="chart-bar-value">5.1万</span>
                </div>
              </div>
              <span class="chart-bar-change">+24%</span>
            </div>
            <div class="chart-bar-row">
              <span class="chart-bar-label">#自律打卡</span>
              <div class="chart-bar-track">
                <div class="chart-bar-fill chart-bar-fill--4" style="width:48%">
                  <span class="chart-bar-value">4.1万</span>
                </div>
              </div>
              <span class="chart-bar-change">+8%</span>
            </div>
            <div class="chart-bar-row">
              <span class="chart-bar-label">#读书笔记</span>
              <div class="chart-bar-track">
                <div class="chart-bar-fill chart-bar-fill--5" style="width:38%">
                  <span class="chart-bar-value">3.2万</span>
                </div>
              </div>
              <span class="chart-bar-change">+31%</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Best Posting Time -->
      <div class="panel">
        <div class="panel-header">
          <div class="panel-title">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            最佳发布时间
          </div>
        </div>
        <div class="time-legend">
          <span style="margin-right:4px;">互动热度：</span>
          <span class="time-legend-dot" style="background:var(--bg-subtle);"></span> 低
          <span class="time-legend-dot" style="background:#fef3c7;"></span> 中
          <span class="time-legend-dot" style="background:#fde68a;"></span> 高
          <span class="time-legend-dot" style="background:var(--accent);"></span> 峰值
        </div>
        <div class="time-grid">
          <!-- Header row -->
          <span></span>
          <span class="time-header">6时</span>
          <span class="time-header">9时</span>
          <span class="time-header">12时</span>
          <span class="time-header">15时</span>
          <span class="time-header">18时</span>
          <span class="time-header">21时</span>
          <span class="time-header">0时</span>
          <!-- Monday -->
          <span class="time-day-label">周一</span>
          <div class="time-cell time-cell--low"></div>
          <div class="time-cell time-cell--mid"></div>
          <div class="time-cell time-cell--high"></div>
          <div class="time-cell time-cell--low"></div>
          <div class="time-cell time-cell--mid"></div>
          <div class="time-cell time-cell--peak"></div>
          <div class="time-cell time-cell--low"></div>
          <!-- Tuesday -->
          <span class="time-day-label">周二</span>
          <div class="time-cell time-cell--low"></div>
          <div class="time-cell time-cell--mid"></div>
          <div class="time-cell time-cell--high"></div>
          <div class="time-cell time-cell--low"></div>
          <div class="time-cell time-cell--high"></div>
          <div class="time-cell time-cell--peak"></div>
          <div class="time-cell time-cell--low"></div>
          <!-- Wednesday -->
          <span class="time-day-label">周三</span>
          <div class="time-cell time-cell--mid"></div>
          <div class="time-cell time-cell--low"></div>
          <div class="time-cell time-cell--high"></div>
          <div class="time-cell time-cell--mid"></div>
          <div class="time-cell time-cell--mid"></div>
          <div class="time-cell time-cell--peak"></div>
          <div class="time-cell time-cell--low"></div>
          <!-- Thursday -->
          <span class="time-day-label">周四</span>
          <div class="time-cell time-cell--low"></div>
          <div class="time-cell time-cell--mid"></div>
          <div class="time-cell time-cell--high"></div>
          <div class="time-cell time-cell--low"></div>
          <div class="time-cell time-cell--high"></div>
          <div class="time-cell time-cell--peak"></div>
          <div class="time-cell time-cell--low"></div>
          <!-- Friday -->
          <span class="time-day-label">周五</span>
          <div class="time-cell time-cell--mid"></div>
          <div class="time-cell time-cell--mid"></div>
          <div class="time-cell time-cell--high"></div>
          <div class="time-cell time-cell--low"></div>
          <div class="time-cell time-cell--mid"></div>
          <div class="time-cell time-cell--peak"></div>
          <div class="time-cell time-cell--mid"></div>
          <!-- Saturday -->
          <span class="time-day-label">周六</span>
          <div class="time-cell time-cell--mid"></div>
          <div class="time-cell time-cell--high"></div>
          <div class="time-cell time-cell--high"></div>
          <div class="time-cell time-cell--mid"></div>
          <div class="time-cell time-cell--high"></div>
          <div class="time-cell time-cell--peak"></div>
          <div class="time-cell time-cell--mid"></div>
          <!-- Sunday -->
          <span class="time-day-label">周日</span>
          <div class="time-cell time-cell--mid"></div>
          <div class="time-cell time-cell--high"></div>
          <div class="time-cell time-cell--high"></div>
          <div class="time-cell time-cell--mid"></div>
          <div class="time-cell time-cell--mid"></div>
          <div class="time-cell time-cell--peak"></div>
          <div class="time-cell time-cell--low"></div>
        </div>
      </div>

      <!-- Competitors -->
      <div class="panel">
        <div class="panel-header">
          <div class="panel-title">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
            竞品博主追踪
          </div>
          <button class="panel-action">
            管理
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg>
          </button>
        </div>
        <div class="competitor-item">
          <div class="competitor-avatar competitor-avatar--1">陈</div>
          <div class="competitor-info">
            <div class="competitor-name">陈思远的职场笔记</div>
            <div class="competitor-stats">
              <span>5.2万粉丝</span>
              <span>·</span>
              <span>本周发布 8 篇</span>
            </div>
          </div>
          <span class="competitor-trend change-up">+1,200</span>
        </div>
        <div class="competitor-item">
          <div class="competitor-avatar competitor-avatar--2">知</div>
          <div class="competitor-info">
            <div class="competitor-name">知识星球Daily</div>
            <div class="competitor-stats">
              <span>3.8万粉丝</span>
              <span>·</span>
              <span>本周发布 12 篇</span>
            </div>
          </div>
          <span class="competitor-trend change-up">+860</span>
        </div>
        <div class="competitor-item">
          <div class="competitor-avatar competitor-avatar--3">效</div>
          <div class="competitor-info">
            <div class="competitor-name">效率工具箱</div>
            <div class="competitor-stats">
              <span>2.1万粉丝</span>
              <span>·</span>
              <span>本周发布 5 篇</span>
            </div>
          </div>
          <span class="competitor-trend change-down">-120</span>
        </div>
      </div>

      <!-- Pro Upsell -->
      <div class="pro-banner">
        <div class="pro-banner-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
        </div>
        <div class="pro-banner-text">
          <div class="pro-banner-title">升级专业版</div>
          <div class="pro-banner-desc">解锁竞品深度分析、批量生成、API 调用</div>
        </div>
        <button class="pro-banner-btn">¥79/月</button>
      </div>
    </div>
  </div>
</div>
</template>

<style scoped>
  /* Global tokens, resets, and topbar are provided by base.css and AppTopBar.vue */
  .main {
    margin-top: 56px;
    padding: var(--space-lg);
    max-width: 1200px;
    margin-left: auto;
    margin-right: auto;
  }

  /* Page Header */
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--space-lg);
  }
  .page-title {
    font-family: var(--font-display);
    font-size: 24px;
    font-weight: 600;
    letter-spacing: -0.02em;
    color: var(--fg);
  }
  .page-subtitle {
    font-size: 14px;
    color: var(--muted);
    margin-top: 2px;
  }
  .page-filter {
    display: flex;
    align-items: center;
    gap: var(--space-sm);
  }
  .filter-btn {
    padding: 7px 16px;
    font-size: 13px;
    font-weight: 500;
    color: var(--muted);
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    background: var(--surface);
    transition: all 0.15s;
  }
  .filter-btn:hover { border-color: var(--fg); color: var(--fg); }
  .filter-btn--active {
    background: var(--fg);
    color: var(--surface);
    border-color: var(--fg);
  }

  /* ─── OVERVIEW CARDS ─── */
  .overview-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: var(--space-md);
    margin-bottom: var(--space-lg);
  }
  .overview-card {
    padding: 20px;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius-md);
  }
  .overview-card-label {
    font-size: 12px;
    color: var(--muted);
    letter-spacing: 0.02em;
    margin-bottom: 8px;
    display: flex;
    align-items: center;
    gap: 6px;
  }
  .overview-card-value {
    font-family: var(--font-display);
    font-size: 28px;
    font-weight: 600;
    letter-spacing: -0.02em;
    color: var(--fg);
    margin-bottom: 4px;
  }
  .overview-card-change {
    font-size: 12px;
    display: flex;
    align-items: center;
    gap: 4px;
  }
  .change-up { color: var(--success); }
  .change-down { color: var(--danger); }

  /* ─── TWO COLUMN LAYOUT ─── */
  .content-grid {
    display: grid;
    grid-template-columns: 1fr 380px;
    gap: var(--space-lg);
  }

  /* Panel */
  .panel {
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius-md);
    overflow: hidden;
  }
  .panel-header {
    padding: var(--space-md) var(--space-lg);
    border-bottom: 1px solid var(--border-light);
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .panel-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--fg);
    letter-spacing: 0.01em;
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .panel-badge {
    padding: 2px 8px;
    background: var(--accent-surface);
    color: var(--accent);
    font-size: 11px;
    font-weight: 600;
    border-radius: var(--radius-pill);
  }
  .panel-action {
    font-size: 13px;
    color: var(--muted);
    display: flex;
    align-items: center;
    gap: 4px;
    transition: color 0.15s;
  }
  .panel-action:hover { color: var(--accent); }

  /* ─── HOT TOPICS LIST ─── */
  .topic-list {
    list-style: none;
  }
  .topic-item {
    display: flex;
    align-items: center;
    gap: var(--space-md);
    padding: 14px var(--space-lg);
    border-bottom: 1px solid var(--border-light);
    transition: background 0.1s;
  }
  .topic-item:last-child { border-bottom: none; }
  .topic-item:hover { background: var(--bg-warm); }
  .topic-rank {
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 700;
    border-radius: 6px;
    flex-shrink: 0;
  }
  .topic-rank--1 { background: #fef3c7; color: #92400e; }
  .topic-rank--2 { background: #e0e7ff; color: #3730a3; }
  .topic-rank--3 { background: #fce7f3; color: #9d174d; }
  .topic-rank--default { background: var(--bg-subtle); color: var(--muted); }
  .topic-info { flex: 1; min-width: 0; }
  .topic-title {
    font-size: 14px;
    font-weight: 500;
    color: var(--fg);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .topic-meta {
    font-size: 12px;
    color: var(--muted);
    display: flex;
    align-items: center;
    gap: var(--space-sm);
    margin-top: 2px;
  }
  .topic-heat {
    display: flex;
    align-items: center;
    gap: 3px;
  }
  .heat-bar {
    width: 48px;
    height: 6px;
    background: var(--bg-subtle);
    border-radius: 3px;
    overflow: hidden;
  }
  .heat-bar-fill {
    height: 100%;
    border-radius: 3px;
    background: var(--accent);
    transition: width 0.3s;
  }
  .topic-action {
    padding: 5px 12px;
    font-size: 12px;
    font-weight: 500;
    color: var(--accent);
    border: 1px solid var(--accent);
    border-radius: var(--radius-sm);
    transition: all 0.15s;
    flex-shrink: 0;
  }
  .topic-action:hover { background: var(--accent); color: #fff; }

  /* ─── TAG TRENDS ─── */
  .tag-chart {
    padding: var(--space-md) var(--space-lg);
  }
  .chart-bar-group {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .chart-bar-row {
    display: flex;
    align-items: center;
    gap: var(--space-sm);
  }
  .chart-bar-label {
    width: 80px;
    font-size: 13px;
    color: var(--fg-secondary);
    text-align: right;
    flex-shrink: 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .chart-bar-track {
    flex: 1;
    height: 24px;
    background: var(--bg-subtle);
    border-radius: 6px;
    overflow: hidden;
    position: relative;
  }
  .chart-bar-fill {
    height: 100%;
    border-radius: 6px;
    transition: width 0.5s ease;
    display: flex;
    align-items: center;
    padding-left: 8px;
  }
  .chart-bar-fill--1 { background: var(--accent); }
  .chart-bar-fill--2 { background: #f59e0b; }
  .chart-bar-fill--3 { background: #fbbf24; }
  .chart-bar-fill--4 { background: #fcd34d; }
  .chart-bar-fill--5 { background: #fde68a; }
  .chart-bar-value {
    font-size: 11px;
    font-weight: 600;
    color: #fff;
    white-space: nowrap;
  }
  .chart-bar-fill--4 .chart-bar-value,
  .chart-bar-fill--5 .chart-bar-value { color: var(--fg-secondary); }
  .chart-bar-change {
    font-size: 11px;
    color: var(--success);
    flex-shrink: 0;
    width: 48px;
    text-align: right;
  }

  /* ─── CONTENT PATTERN ─── */
  .pattern-section {
    margin-top: var(--space-lg);
  }
  .pattern-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-md);
    padding: var(--space-md) var(--space-lg) var(--space-lg);
  }
  .pattern-card {
    padding: 16px;
    background: var(--bg-warm);
    border: 1px solid var(--border-light);
    border-radius: var(--radius-sm);
  }
  .pattern-card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
  }
  .pattern-card-title {
    font-size: 13px;
    font-weight: 600;
    color: var(--fg);
  }
  .pattern-card-pct {
    font-size: 12px;
    font-weight: 600;
    color: var(--accent);
  }
  .pattern-card-desc {
    font-size: 12px;
    color: var(--muted);
    line-height: 1.5;
  }
  .pattern-card-example {
    margin-top: 8px;
    padding: 8px 10px;
    background: var(--surface);
    border: 1px solid var(--border-light);
    border-radius: 6px;
    font-size: 12px;
    color: var(--fg-secondary);
    line-height: 1.5;
  }

  /* ─── RIGHT SIDEBAR ─── */
  .sidebar-stack {
    display: flex;
    flex-direction: column;
    gap: var(--space-lg);
  }

  /* Competitor */
  .competitor-item {
    display: flex;
    align-items: center;
    gap: var(--space-md);
    padding: 14px var(--space-lg);
    border-bottom: 1px solid var(--border-light);
  }
  .competitor-item:last-child { border-bottom: none; }
  .competitor-avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 600;
  }
  .competitor-avatar--1 { background: #fce7f3; color: #9d174d; }
  .competitor-avatar--2 { background: #e0f2fe; color: #075985; }
  .competitor-avatar--3 { background: #f0fdf4; color: #166534; }
  .competitor-info { flex: 1; }
  .competitor-name {
    font-size: 14px;
    font-weight: 500;
    color: var(--fg);
  }
  .competitor-stats {
    font-size: 12px;
    color: var(--muted);
    display: flex;
    gap: var(--space-sm);
  }
  .competitor-trend {
    font-size: 12px;
    font-weight: 600;
    flex-shrink: 0;
  }

  /* Best time */
  .time-grid {
    padding: var(--space-md) var(--space-lg) var(--space-lg);
    display: grid;
    grid-template-columns: 36px repeat(7, 1fr);
    gap: 4px;
    align-items: center;
  }
  .time-day-label {
    font-size: 11px;
    color: var(--muted);
    text-align: right;
    padding-right: 6px;
  }
  .time-header {
    font-size: 10px;
    color: var(--muted);
    text-align: center;
    padding-bottom: 4px;
  }
  .time-cell {
    aspect-ratio: 1;
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    color: var(--muted);
    position: relative;
  }
  .time-cell--low { background: var(--bg-subtle); }
  .time-cell--mid { background: #fef3c7; color: #92400e; }
  .time-cell--high { background: #fde68a; color: #92400e; }
  .time-cell--peak { background: var(--accent); color: #fff; font-weight: 600; }
  .time-labels {
    padding: 0 var(--space-lg) var(--space-md);
    display: flex;
    justify-content: space-between;
    font-size: 11px;
    color: var(--muted);
  }
  .time-legend {
    padding: 0 var(--space-lg) var(--space-md);
    display: flex;
    align-items: center;
    gap: var(--space-sm);
    font-size: 11px;
    color: var(--muted);
  }
  .time-legend-dot {
    width: 10px;
    height: 10px;
    border-radius: 3px;
  }

  /* Pro upsell */
  .pro-banner {
    padding: var(--space-md) var(--space-lg);
    background: linear-gradient(135deg, var(--accent-surface) 0%, #fef3c7 100%);
    border: 1px solid var(--accent-light);
    border-radius: var(--radius-md);
    display: flex;
    align-items: center;
    gap: var(--space-md);
  }
  .pro-banner-icon {
    width: 40px;
    height: 40px;
    background: var(--accent);
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }
  .pro-banner-text {
    flex: 1;
  }
  .pro-banner-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--fg);
  }
  .pro-banner-desc {
    font-size: 12px;
    color: var(--fg-secondary);
  }
  .pro-banner-btn {
    padding: 7px 16px;
    background: var(--accent);
    color: #fff;
    font-size: 13px;
    font-weight: 500;
    border-radius: var(--radius-sm);
    white-space: nowrap;
    transition: background 0.15s;
  }
  .pro-banner-btn:hover { background: var(--accent-hover); }

  .topbar-mobile-toggle {
    display: none;
    width: 36px;
    height: 36px;
    align-items: center;
    justify-content: center;
    color: var(--fg);
  }

  /* ─── RESPONSIVE ─── */
  @media (max-width: 1024px) {
    .overview-grid { grid-template-columns: repeat(2, 1fr); }
    .content-grid { grid-template-columns: 1fr; }
  }
  @media (max-width: 768px) {
    .topbar-nav { display: none; }
    .topbar-mobile-toggle { display: flex; }
    .topbar-quota { display: none; }
    .overview-grid { grid-template-columns: 1fr; }
    .page-header { flex-direction: column; align-items: flex-start; gap: var(--space-md); }
    .pattern-grid { grid-template-columns: 1fr; }
  }
</style>
