import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'
import { createVuetify } from 'vuetify'
import { md3 } from 'vuetify/blueprints'

/**
 * アプリ全体のデザインシステム（ステップ13）。
 *
 * `docs/overview.md` の「ポップ＆カジュアル」「タップターゲット最低 44×44px」を満たすため、
 * ボタン類は `size: 'large'` を既定にし、角丸を強めにしたテーマを定義する。
 * `expiry-danger` / `expiry-warning` は賞味期限ハイライト（3日以内=赤・7日以内=黄）用の
 * カスタムカラートークンで、`bg-expiry-danger` のようなユーティリティクラスとして利用できる。
 */
export default createVuetify({
  blueprint: md3,
  icons: {
    defaultSet: 'mdi',
  },
  theme: {
    defaultTheme: 'fridgeLight',
    themes: {
      fridgeLight: {
        dark: false,
        colors: {
          primary: '#2BB6A3',
          'primary-darken-1': '#1F8F80',
          secondary: '#FFB74D',
          'secondary-darken-1': '#F59E0B',
          error: '#E53935',
          warning: '#FDD835',
          success: '#43A047',
          background: '#FFF8F0',
          surface: '#FFFFFF',
          'expiry-danger': '#E53935',
          'expiry-warning': '#FDD835',
        },
      },
    },
  },
  defaults: {
    VBtn: {
      rounded: 'pill',
      size: 'large',
      class: 'text-none',
    },
    VCard: {
      rounded: 'xl',
      elevation: 2,
    },
    VTextField: {
      rounded: 'lg',
      variant: 'outlined',
      density: 'comfortable',
    },
    VChip: {
      rounded: 'pill',
    },
  },
})
