import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from './auth'
import { httpClient } from '@/lib/httpClient'

vi.mock('@/lib/httpClient', () => ({
  httpClient: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(httpClient.get).mockReset()
    vi.mocked(httpClient.post).mockReset()
  })

  it('fetchMe: ログイン中なら user を反映し isAuthenticated が true になる', async () => {
    const store = useAuthStore()
    vi.mocked(httpClient.get).mockResolvedValue({
      data: { id: 'u1', displayName: '太郎', avatarUrl: null, groups: [] },
    })

    await store.fetchMe()

    expect(store.isAuthenticated).toBe(true)
    expect(store.user?.displayName).toBe('太郎')
    expect(store.isInitialized).toBe(true)
  })

  it('fetchMe: 未ログイン（401）の場合は user を null のままにする', async () => {
    const store = useAuthStore()
    vi.mocked(httpClient.get).mockRejectedValue(new Error('401'))

    await store.fetchMe()

    expect(store.isAuthenticated).toBe(false)
    expect(store.user).toBeNull()
    expect(store.isInitialized).toBe(true)
  })

  it('loginWithGoogle: レスポンスのユーザー情報をストアへ反映する', async () => {
    const store = useAuthStore()
    vi.mocked(httpClient.post).mockResolvedValue({
      data: {
        id: 'u2',
        displayName: '花子',
        avatarUrl: 'https://example.com/a.png',
        groups: [{ id: 'g1', name: '我が家' }],
      },
    })

    await store.loginWithGoogle('dummy-id-token')

    expect(httpClient.post).toHaveBeenCalledWith('/auth/google', { idToken: 'dummy-id-token' })
    expect(store.user?.displayName).toBe('花子')
    expect(store.isAuthenticated).toBe(true)
  })

  it('logout: サーバー呼び出し成功後に user をクリアする', async () => {
    const store = useAuthStore()
    vi.mocked(httpClient.get).mockResolvedValue({
      data: { id: 'u1', displayName: '太郎', avatarUrl: null, groups: [] },
    })
    await store.fetchMe()
    vi.mocked(httpClient.post).mockResolvedValue({})

    await store.logout()

    expect(store.user).toBeNull()
    expect(store.isAuthenticated).toBe(false)
  })
})
