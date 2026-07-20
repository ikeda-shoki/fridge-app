import { defineStore } from 'pinia'
import { httpClient } from '@/lib/httpClient'
import { useAuthStore } from './auth'

/**
 * 家族グループの作成・参加を扱うストア（GRP-01・GRP-03）。
 *
 * 所属グループの一覧そのものは `authStore.user.groups` を単一の情報源とする。
 * 作成・参加に成功したら `/me` を取り直してユーザー情報を最新化する。
 */
export const useGroupStore = defineStore('groups', () => {
  const authStore = useAuthStore()

  /** グループを新規作成し、オーナーとして参加する（GRP-01）。 */
  async function createGroup(name: string): Promise<void> {
    await httpClient.post('/groups', { name })
    await authStore.fetchMe()
  }

  /** 招待コードでグループに参加する（GRP-03）。 */
  async function joinGroup(code: string): Promise<void> {
    await httpClient.post('/groups/join', { code })
    await authStore.fetchMe()
  }

  return { createGroup, joinGroup }
})
