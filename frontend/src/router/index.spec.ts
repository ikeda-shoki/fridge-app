import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import router from './index'
import { httpClient } from '@/lib/httpClient'
import type { Group } from '@/types/group'

vi.mock('@/lib/httpClient', () => ({
  httpClient: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

function meResponse(groups: Group[]) {
  return { data: { id: 'u1', displayName: '太郎', avatarUrl: null, groups } }
}

describe('router guard', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(httpClient.get).mockReset()
  })

  it('未ログインで非公開ページに来た場合はログイン画面へ流す', async () => {
    vi.mocked(httpClient.get).mockRejectedValue(new Error('401'))

    await router.push('/')

    expect(router.currentRoute.value.name).toBe('login')
  })

  it('グループ未所属のログイン済みユーザーはガイダンス画面へ流す', async () => {
    vi.mocked(httpClient.get).mockResolvedValue(meResponse([]))

    await router.push('/')

    expect(router.currentRoute.value.name).toBe('guidance')
  })

  it('グループ所属済みのログイン済みユーザーはホームへ流す', async () => {
    vi.mocked(httpClient.get).mockResolvedValue(meResponse([{ id: 'g1', name: '我が家' }]))

    await router.push('/')

    expect(router.currentRoute.value.name).toBe('home')
  })

  it('グループ所属済みユーザーがガイダンス画面に来た場合はホームへ流す', async () => {
    vi.mocked(httpClient.get).mockResolvedValue(meResponse([{ id: 'g1', name: '我が家' }]))

    await router.push('/guidance')

    expect(router.currentRoute.value.name).toBe('home')
  })

  it('ログイン済みユーザーがログイン画面に来た場合は所属状況に応じて振り分ける', async () => {
    vi.mocked(httpClient.get).mockResolvedValue(meResponse([]))

    await router.push('/login')

    expect(router.currentRoute.value.name).toBe('guidance')
  })
})
