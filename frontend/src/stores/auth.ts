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

  /** ログアウトする（AUTH-02）。サーバー側のリフレッシュトークン失効と Cookie 削除の後、ストアの状態も破棄する。 */
  async function logout(): Promise<void> {
    await httpClient.post('/auth/logout')
    user.value = null
  }

  return { user, isInitialized, isAuthenticated, loginWithGoogle, fetchMe, logout }
})
