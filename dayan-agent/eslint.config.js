import js from '@eslint/js'
import tseslint from 'typescript-eslint'
import pluginVue from 'eslint-plugin-vue'
import eslintConfigPrettier from 'eslint-config-prettier'
import globals from 'globals'

/**
 * ESLint 9 flat config：Vue3 + TypeScript（uni-app）。
 * 规则从宽起步（no-console/any 仅 warn），存量代码逐步收紧；
 * Prettier 负责格式，ESLint 只管质量规则。
 */
export default tseslint.config(
  { ignores: ['dist', 'node_modules', 'public'] },

  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...pluginVue.configs['flat/recommended'],

  {
    files: ['**/*.vue'],
    languageOptions: {
      parserOptions: { parser: tseslint.parser }
    }
  },
  {
    languageOptions: {
      globals: { ...globals.browser }
    },
    rules: {
      'no-console': 'warn',
      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
      'vue/multi-word-component-names': 'off',
      'no-undef': 'off'
    }
  },

  eslintConfigPrettier
)
