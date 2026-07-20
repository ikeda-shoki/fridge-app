import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useGroupStore } from './groups'
import { useAuthStore } from './auth'
import { httpClient } from '@/lib/httpClient'

vi.mock('@/lib/httpClient', () => ({
  httpClient: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

describe('useGroupStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(httpClient.get).mockReset()
    vi.mocked(httpClient.post).mockReset()
  })

  it('createGroup: POST /groups を呼び、成功後にユーザー情報(所属グループ)を再取得する', async () => {
    vi.mocked(httpClient.post).mockResolvedValue({ data: { id: 'g1', name: '我が家' } })
    vi.mocked(httpClient.get).mockResolvedValue({
      data: {
        id: 'u1',
        displayName: '太郎',
        avatarUrl: null,
        groups: [{ id: 'g1', name: '我が家' }],
      },
    })

    const groupStore = useGroupStore()
    const authStore = useAuthStore()
    await groupStore.createGroup('我が家')

    expect(httpClient.post).toHaveBeenCalledWith('/groups', { name: '我が家' })
    expect(httpClient.get).toHaveBeenCalledWith('/me')
    expect(authStore.user?.groups).toEqual([{ id: 'g1', name: '我が家' }])
  })

  it('joinGroup: POST /groups/join を呼び、成功後にユーザー情報(所属グループ)を再取得する', async () => {
    vi.mocked(httpClient.post).mockResolvedValue({ data: { id: 'g2', name: '実家' } })
    vi.mocked(httpClient.get).mockResolvedValue({
      data: {
        id: 'u1',
        displayName: '太郎',
        avatarUrl: null,
        groups: [{ id: 'g2', name: '実家' }],
      },
    })

    const groupStore = useGroupStore()
    const authStore = useAuthStore()
    await groupStore.joinGroup('ABC123')

    expect(httpClient.post).toHaveBeenCalledWith('/groups/join', { code: 'ABC123' })
    expect(authStore.user?.groups).toEqual([{ id: 'g2', name: '実家' }])
  })

  it('createGroup: API が失敗した場合は例外を投げ、/me は呼ばれない', async () => {
    vi.mocked(httpClient.post).mockRejectedValue(new Error('failed'))

    const groupStore = useGroupStore()
    await expect(groupStore.createGroup('我が家')).rejects.toThrow('failed')

    expect(httpClient.get).not.toHaveBeenCalled()
  })
})
