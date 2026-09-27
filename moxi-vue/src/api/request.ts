/**
 * HTTP 请求封装，自动注入 JWT token 和统一错误处理。
 */

const BASE_URL = '/api/v1'

export function getToken(): string | null {
  return localStorage.getItem('moxi_token')
}

export function setToken(token: string): void {
  localStorage.setItem('moxi_token', token)
}

export function clearToken(): void {
  localStorage.removeItem('moxi_token')
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp?: number
}

export async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const token = getToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((options.headers as Record<string, string>) || {}),
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const resp = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers,
  })

  if (!resp.ok) {
    throw new Error(`HTTP ${resp.status}`)
  }

  const result: ApiResult<T> = await resp.json()
  if (result.code !== 0) {
    throw new Error(result.message || '请求失败')
  }
  return result.data
}

export async function requestRaw(
  path: string,
  options: RequestInit = {},
): Promise<Response> {
  const token = getToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((options.headers as Record<string, string>) || {}),
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  return fetch(`${BASE_URL}${path}`, {
    ...options,
    headers,
  })
}
