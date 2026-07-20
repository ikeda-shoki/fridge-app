/** バックエンドの `FridgeItemResponse` に対応する冷蔵庫アイテム。 */
export interface FridgeItem {
  id: string
  groupId: string
  foodMasterId: string | null
  displayName: string
  quantity: number
  unit: string | null
  /** カテゴリの日本語ラベル（ADR-008 により DB・API とも日本語ラベルでやり取りする）。 */
  category: string | null
  /** 賞味/消費期限（`YYYY-MM-DD`）。未設定なら期限ハイライトの対象外。 */
  expiresAt: string | null
  purchasedAt: string | null
  purchasedBy: string | null
  /** ストレージ上の不透明なパス。画像 URL の組み立てとキャッシュ破棄に使う。 */
  imagePath: string | null
  memo: string | null
  status: 'ACTIVE' | 'CONSUMED' | 'DELETED'
}

/** カテゴリの選択肢（`FridgeItemCategory` の日本語ラベルと同順・同値）。 */
export const FRIDGE_ITEM_CATEGORIES = [
  '野菜',
  'きのこ',
  '果物',
  '肉類',
  '魚介類',
  '乳製品',
  '豆腐・大豆',
  '調味料',
  '穀類・麺',
  '飲み物',
  'その他',
] as const
