import { isAxiosError } from 'axios'

/** バックエンドの `ErrorResponse{code, message}` の `message` を、失敗時のフォールバック付きで取り出す。 */
export function extractErrorMessage(error: unknown, fallback: string): string {
  if (isAxiosError<{ code: string; message: string }>(error) && error.response?.data?.message) {
    return error.response.data.message
  }
  return fallback
}
