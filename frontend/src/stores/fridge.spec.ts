import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useFridgeStore } from './fridge'
import { httpClient } from '@/lib/httpClient'

vi.mock('@/lib/httpClient', () => ({
  httpClient: {
    get: vi.fn(),
  },
}))

const sampleItem = {
  id: 'i1',
  groupId: 'g1',
  foodMasterId: null,
  displayName: 'にんじん',
  quantity: 2,
  unit: null,
  category: '野菜',
  expiresAt: '2026-07-25',
  purchasedAt: null,
  purchasedBy: null,
  imagePath: null,
  memo: null,
  status: 'ACTIVE',
}

describe('useFridgeStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(httpClient.get).mockReset()
  })

  it('fetchItems: 取得したアイテムをストアへ反映する', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({ data: [sampleItem] })
    const store = useFridgeStore()

    await store.fetchItems('g1')

    expect(httpClient.get).toHaveBeenCalledWith('/groups/g1/fridge-items', {
      params: { category: undefined, q: undefined },
    })
    expect(store.items).toEqual([sampleItem])
    expect(store.loading).toBe(false)
    expect(store.errorMessage).toBeNull()
  })

  it('fetchItems: カテゴリ・検索語をクエリパラメータへ渡す（空文字は送らない）', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({ data: [] })
    const store = useFridgeStore()

    await store.fetchItems('g1', { category: '野菜', q: '' })

    expect(httpClient.get).toHaveBeenCalledWith('/groups/g1/fridge-items', {
      params: { category: '野菜', q: undefined },
    })
  })

  it('fetchItems: 失敗時は items を空にしエラーメッセージを設定する', async () => {
    vi.mocked(httpClient.get).mockRejectedValue(new Error('failed'))
    const store = useFridgeStore()

    await store.fetchItems('g1')

    expect(store.items).toEqual([])
    expect(store.errorMessage).not.toBeNull()
    expect(store.loading).toBe(false)
  })
})
