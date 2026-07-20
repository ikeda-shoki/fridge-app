import { ref } from 'vue'
import { defineStore } from 'pinia'
import { httpClient } from '@/lib/httpClient'
import { extractErrorMessage } from '@/lib/apiError'
import type { FridgeItem } from '@/types/fridgeItem'

/** 一覧の絞り込み条件（FRG-06・FRG-09）。両者は併用できる。 */
export interface FridgeItemQuery {
  category?: string | null
  q?: string | null
}

/** 冷蔵庫アイテムの一覧を保持するストア。 */
export const useFridgeStore = defineStore('fridge', () => {
  const items = ref<FridgeItem[]>([])
  const loading = ref(false)
  const errorMessage = ref<string | null>(null)

  /**
   * グループの冷蔵庫アイテム一覧を取得しストアへ反映する（FRG-04）。
   * 賞味期限が近い順で返る（期限なしは末尾）。取得に失敗した場合は `errorMessage` を設定し、`items` は空にする。
   */
  async function fetchItems(groupId: string, query: FridgeItemQuery = {}): Promise<void> {
    loading.value = true
    errorMessage.value = null
    try {
      const response = await httpClient.get<FridgeItem[]>(`/groups/${groupId}/fridge-items`, {
        params: {
          category: query.category || undefined,
          q: query.q || undefined,
        },
      })
      items.value = response.data
    } catch (error) {
      items.value = []
      errorMessage.value = extractErrorMessage(
        error,
        'アイテムの取得に失敗しました。時間をおいて再度お試しください。',
      )
    } finally {
      loading.value = false
    }
  }

  return { items, loading, errorMessage, fetchItems }
})
