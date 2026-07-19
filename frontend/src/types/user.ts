/** バックエンドの `GET /api/v1/me` が返すユーザー情報。 */
export interface User {
  id: string
  displayName: string
  avatarUrl: string | null
}
