/** 期限ハイライト（FRG-05）の区分。`expired` と `danger` はいずれも赤で表示する。 */
export type ExpiryStatus = 'expired' | 'danger' | 'warning' | 'none'

const DAY_IN_MS = 24 * 60 * 60 * 1000

/**
 * 今日の日付を JST（Asia/Tokyo）で `YYYY-MM-DD` として返す。
 *
 * 判定基準を JST の 0 時に固定するため（`docs/overview.md`）、ブラウザのローカルタイムゾーンには依存させない。
 * `en-CA` ロケールは `YYYY-MM-DD` 形式で出力されるためこれを利用する。
 */
function todayInJst(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Tokyo' }).format(new Date())
}

/**
 * 期限日までの残日数を返す。負値は期限切れ、0 は当日。
 * 両者を UTC 0 時として解釈するため、夏時間などの影響を受けず日数差が正確に出る。
 */
export function daysUntilExpiry(expiresAt: string, today: string = todayInJst()): number {
  return Math.round(
    (Date.parse(`${expiresAt}T00:00:00Z`) - Date.parse(`${today}T00:00:00Z`)) / DAY_IN_MS,
  )
}

/**
 * 期限ハイライトの区分を判定する（FRG-05）。期限切れ・3日以内は赤、7日以内は黄、それ以外と未設定は対象外。
 *
 * @param today テスト用に基準日を差し替えるための引数。通常は省略して JST の今日を使う
 */
export function expiryStatusOf(expiresAt: string | null, today?: string): ExpiryStatus {
  if (!expiresAt) return 'none'

  const days = daysUntilExpiry(expiresAt, today)
  if (days < 0) return 'expired'
  if (days <= 3) return 'danger'
  if (days <= 7) return 'warning'
  return 'none'
}

/** 期限の残日数を利用者向けの短い文言にする（「期限切れ」「今日まで」「あと3日」など）。 */
export function expiryLabelOf(expiresAt: string | null, today?: string): string | null {
  if (!expiresAt) return null

  const days = daysUntilExpiry(expiresAt, today)
  if (days < 0) return '期限切れ'
  if (days === 0) return '今日まで'
  return `あと${days}日`
}

/** 期限区分に対応する Vuetify のカラートークン（`plugins/vuetify.ts` で定義）。 */
export function expiryColorOf(status: ExpiryStatus): string | undefined {
  switch (status) {
    case 'expired':
    case 'danger':
      return 'expiry-danger'
    case 'warning':
      return 'expiry-warning'
    case 'none':
      return undefined
  }
}
