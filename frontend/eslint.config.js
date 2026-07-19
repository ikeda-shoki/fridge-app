import pluginVue from 'eslint-plugin-vue'
import { withVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import eslintPluginPrettierRecommended from 'eslint-plugin-prettier/recommended'

export default withVueTs(
  { rootDir: import.meta.dirname },
  { ignores: ['dist/**', 'node_modules/**', 'coverage/**'] },
  pluginVue.configs['flat/recommended'],
  vueTsConfigs.recommended,
  eslintPluginPrettierRecommended,
)
