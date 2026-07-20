import type { FridgeItem } from '@/types/fridgeItem'

/**
 * アイテム画像の URL を組み立てる。画像が未登録なら null。
 *
 * 画像はグループのデータなので静的配信ではなく API 経由で取得する（Cookie が自動送信されメンバー認可が通る）。
 * URL は差し替えのたびに変わる `imagePath` を `v` に載せることで、サーバー側の長期キャッシュを破棄する。
 */
export function fridgeItemImageUrl(item: FridgeItem): string | null {
  if (!item.imagePath) return null
  return `/api/v1/fridge-items/${item.id}/image?v=${encodeURIComponent(item.imagePath)}`
}
