import { httpRequest, setStoredToken } from './http'

export type AuthUser = {
  id: number
  username: string
  nickname: string
  role: string
}

export type LoginRequest = {
  username: string
  password: string
}

export type LoginResponse = {
  token: string
  user: AuthUser
}

export type RegisterRequest = {
  username: string
  password: string
  nickname: string
}

export type RegisterResponse = {
  userId: number
}

export async function login(input: LoginRequest): Promise<LoginResponse> {
  const data = await httpRequest<LoginResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify(input),
  })
  if (data.token) setStoredToken(data.token)
  return data
}

export async function register(input: RegisterRequest): Promise<RegisterResponse> {
  return httpRequest<RegisterResponse>('/api/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export async function getCurrentUser(): Promise<AuthUser> {
  return httpRequest<AuthUser>('/api/v1/auth/me', { method: 'GET' })
}
