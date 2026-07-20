import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { httpClient } from '@/lib/httpClient'
import type { User } from '@/types/user'

/**
 * ログイン状態とログインユーザー情報を保持するストア。
 *
 * トークン自体は HttpOnly Cookie にあり JS からは扱わない。ここで持つのは表示用の
 * ユーザー情報（`User`）と「認証済みかどうか」の判定だけ。
 */
export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  /** 起動直後にまだ `/me` を確認できていない状態。ルーターガードでの誤判定を防ぐために使う。 */
  const isInitialized = ref(false)

  const isAuthenticated = computed(() => user.value !== null)

  /**
   * 現在表示対象の家族グループ。所属していなければ null。
   *
   * 複数グループの切り替え（GRP-00 補足）はステップ18 で対応するため、現時点では先頭のグループを既定として扱う。
   */
  const currentGroup = computed(() => user.value?.groups[0] ?? null)

  /**
   * Google ID トークンでログインする（AUTH-01）。
   * 成功するとバックエンドがトークン Cookie を発行し、ユーザー情報がストアに反映される。
   */
  async function loginWithGoogle(idToken: string): Promise<void> {
    const response = await httpClient.post<User>('/auth/google', { idToken })
    user.value = response.data
    isInitialized.value = true
  }

  /**
   * 現在の Cookie でログイン中かどうかを確認し、ユーザー情報をストアに反映する。
   * 未ログイン（401）の場合は例外を投げず `user` を `null` にするだけに留める。
   */
  async function fetchMe(): Promise<void> {
    try {
      const response = await httpClient.get<User>('/me')
      user.value = response.data
    } catch {
      user.value = null
    } finally {
      isInitialized.value = true
    }
  }

  /**
   * ログアウトする（AUTH-02）。サーバー側のリフレッシュトークン失効と Cookie 削除を試みる。
   * サーバー呼び出しが失敗しても、クライアント側のログイン状態は必ず破棄する（ユーザーがログアウトできない状態に陥らせない）。
   */
  async function logout(): Promise<void> {
    try {
      await httpClient.post('/auth/logout')
    } finally {
      user.value = null
    }
  }

  return {
    user,
    isInitialized,
    isAuthenticated,
    currentGroup,
    loginWithGoogle,
    fetchMe,
    logout,
  }
})
