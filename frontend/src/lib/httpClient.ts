import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'

/**
 * バックエンド API 共通の axios インスタンス。
 *
 * 認証は HttpOnly Cookie のみで行う（トークンを JS 側で保持しない）ため `withCredentials: true` が必須。
 * `withXSRFToken: true` は、フロント（localhost:5173）とバックエンド（localhost:8080）がポート違いの
 * クロスオリジンとなるため、axios に `XSRF-TOKEN` Cookie → `X-XSRF-TOKEN` ヘッダーの自動付与を
 * 明示的に許可する設定（同一オリジンでないと axios はデフォルトで送らない）。
 */
export const httpClient = axios.create({
  baseURL: '/api/v1',
  withCredentials: true,
  withXSRFToken: true,
})

/**
 * CSRF トークンを初期化する（`XSRF-TOKEN` Cookie を発行させる）。
 *
 * ログイン（`/auth/google`）は CSRF 検証対象外で Cookie を発行しないため、これを呼ばないと
 * 最初の状態変更リクエスト（ログアウト・グループ作成など）が 403 になる。アプリ起動時に一度呼ぶ。
 * 失敗してもアプリ起動は妨げない（後続リクエストが 403 になったら都度対処する）。
 */
export async function primeCsrfToken(): Promise<void> {
  try {
    await httpClient.get('/auth/csrf')
  } catch {
    // 起動をブロックしない
  }
}

let refreshPromise: Promise<void> | null = null

/** アクセストークン切れ（401）時に一度だけリフレッシュする。同時多発リクエストではリフレッシュ処理を共有する。 */
function refreshAccessToken(): Promise<void> {
  if (!refreshPromise) {
    refreshPromise = axios
      .post('/api/v1/auth/refresh', null, { withCredentials: true, withXSRFToken: true })
      .then(() => undefined)
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retried?: boolean
}

/**
 * 401 応答時に `/auth/refresh` を試み、成功すれば元のリクエストを 1 回だけ再試行するインターセプター。
 * リフレッシュ自体が失敗した場合はそのままエラーを呼び出し元に返す（呼び出し側でログイン画面へ誘導する）。
 */
httpClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as RetryableRequestConfig | undefined
    const isAuthEndpoint = config?.url?.startsWith('/auth/')

    if (error.response?.status !== 401 || !config || config._retried || isAuthEndpoint) {
      return Promise.reject(error)
    }

    config._retried = true
    try {
      await refreshAccessToken()
      return httpClient(config)
    } catch (refreshError) {
      return Promise.reject(refreshError)
    }
  },
)
