/**
 * 内容相关 API，含 SSE 流式生成。
 */
import { requestRaw, getToken } from './request'

const BASE_URL = '/api/v1'

export interface SseEvent {
  event: string
  data: any
}

export interface GenerateNoteParams {
  topic: string
  style?: string
  wordCount?: number
  includeTags?: boolean
  includeCover?: boolean
}

/**
 * 调用笔记生成 SSE 接口，通过回调实时推送事件。
 *
 * 浏览器 EventSource 不支持 POST，所以用 fetch + ReadableStream 手动解析 SSE。
 */
export async function generateNoteSSE(
  params: GenerateNoteParams,
  onEvent: (event: SseEvent) => void,
  onError?: (error: Error) => void,
): Promise<void> {
  const token = getToken()
  const resp = await fetch(`${BASE_URL}/content/generate`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({
      topic: params.topic,
      style: params.style || 'dry-goods',
      wordCount: params.wordCount || 600,
      includeTags: params.includeTags !== false,
      includeCover: params.includeCover || false,
    }),
  })

  if (!resp.ok) {
    throw new Error(`生成请求失败: HTTP ${resp.status}`)
  }

  const reader = resp.body!.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })

    // SSE 事件以双换行分隔
    const events = buffer.split('\n\n')
    buffer = events.pop() || ''

    for (const eventStr of events) {
      const parsed = parseSseEvent(eventStr)
      if (parsed) {
        onEvent(parsed)
      }
    }
  }

  // 处理最后残留的缓冲
  if (buffer.trim()) {
    const parsed = parseSseEvent(buffer)
    if (parsed) {
      onEvent(parsed)
    }
  }
}

function parseSseEvent(raw: string): SseEvent | null {
  const lines = raw.split('\n')
  let eventName = ''
  let dataStr = ''

  for (const line of lines) {
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataStr = line.slice(5).trim()
    }
  }

  if (!eventName) return null

  let data: any = dataStr
  try {
    data = JSON.parse(dataStr)
  } catch {
    // 非 JSON 数据保持原样
  }

  return { event: eventName, data }
}

export interface ContentHistoryItem {
  id: number
  title: string
  status: string
  createdAt: string
}

export interface ContentHistoryResponse {
  total: number
  page: number
  size: number
  items: ContentHistoryItem[]
}

export async function getContentHistory(
  page = 1,
  size = 20,
): Promise<ContentHistoryResponse> {
  const { request } = await import('./request')
  return request<ContentHistoryResponse>(
    `/content/history?page=${page}&size=${size}`,
  )
}

export interface SensitiveCheckResult {
  hasSensitive: boolean
  sensitiveWords: string[]
  filteredText: string
  count: number
}

export async function checkSensitive(
  text: string,
): Promise<SensitiveCheckResult> {
  const { request } = await import('./request')
  return request<SensitiveCheckResult>('/content/check-sensitive', {
    method: 'POST',
    body: JSON.stringify({ text }),
  })
}
