import { describe, it, expect } from 'vitest'
import { expiryStatusOf, expiryLabelOf, daysUntilExpiry } from './expiry'

const TODAY = '2026-07-21'

describe('expiry', () => {
  describe('daysUntilExpiry', () => {
    it('当日は0、未来は正、過去は負を返す', () => {
      expect(daysUntilExpiry('2026-07-21', TODAY)).toBe(0)
      expect(daysUntilExpiry('2026-07-24', TODAY)).toBe(3)
      expect(daysUntilExpiry('2026-07-20', TODAY)).toBe(-1)
    })
  })

  describe('expiryStatusOf（FRG-05）', () => {
    it('期限切れは expired', () => {
      expect(expiryStatusOf('2026-07-20', TODAY)).toBe('expired')
    })

    it('当日〜3日以内は danger', () => {
      expect(expiryStatusOf('2026-07-21', TODAY)).toBe('danger')
      expect(expiryStatusOf('2026-07-24', TODAY)).toBe('danger')
    })

    it('4〜7日以内は warning', () => {
      expect(expiryStatusOf('2026-07-25', TODAY)).toBe('warning')
      expect(expiryStatusOf('2026-07-28', TODAY)).toBe('warning')
    })

    it('8日以上先は none', () => {
      expect(expiryStatusOf('2026-07-29', TODAY)).toBe('none')
    })

    it('期限未設定は none', () => {
      expect(expiryStatusOf(null, TODAY)).toBe('none')
    })
  })

  describe('expiryLabelOf', () => {
    it('期限切れ・当日・残日数の文言を返す', () => {
      expect(expiryLabelOf('2026-07-20', TODAY)).toBe('期限切れ')
      expect(expiryLabelOf('2026-07-21', TODAY)).toBe('今日まで')
      expect(expiryLabelOf('2026-07-25', TODAY)).toBe('あと4日')
    })

    it('期限未設定は null', () => {
      expect(expiryLabelOf(null, TODAY)).toBeNull()
    })
  })
})
