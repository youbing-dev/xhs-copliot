/**
 * 认证相关 API。
 */
import { request, setToken, clearToken } from './request'

export interface UserInfo {
  id: number
  nickname: string
  avatar: string | null
  plan: string
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: UserInfo
}

export async function sendCode(type: string, target: string): Promise<void> {
  await request('/auth/send-code', {
    method: 'POST',
    body: JSON.stringify({ type, target }),
  })
}

export async function register(
  type: string,
  phone: string,
  code: string,
  password: string,
  nickname: string,
): Promise<LoginResult> {
  const data = await request<LoginResult>('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ type, phone, code, password, nickname }),
  })
  setToken(data.accessToken)
  return data
}

export async function login(
  type: string,
  phone: string,
  password: string,
): Promise<LoginResult> {
  const data = await request<LoginResult>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ account: phone, password }),
  })
  setToken(data.accessToken)
  return data
}

export async function getMe(): Promise<UserInfo> {
  return request<UserInfo>('/auth/me')
}

export async function logout(): Promise<void> {
  await request('/auth/logout', { method: 'POST' })
  clearToken()
}
