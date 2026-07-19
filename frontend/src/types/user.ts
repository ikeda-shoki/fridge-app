import type { Group } from './group'

/** バックエンドの `GET /api/v1/me` が返すユーザー情報。 */
export interface User {
  id: string
  displayName: string
  avatarUrl: string | null
  /** 所属する家族グループの一覧（GRP-00: 空ならガイダンス画面へ誘導する）。 */
  groups: Group[]
}
