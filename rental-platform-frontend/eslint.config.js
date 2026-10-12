import js from '@eslint/js'
import eslintPluginVue from 'eslint-plugin-vue'
import globals from 'globals'

export default [
  {
    ignores: ['dist/**', 'coverage/**', 'node_modules/**']
  },
  js.configs.recommended,
  ...eslintPluginVue.configs['flat/recommended'],
  {
    files: ['**/*.{js,vue}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.browser,
        ...globals.node
      }
    },
    rules: {
      'no-unused-vars': 'off',
      'vue/multi-word-component-names': 'off',
      'vue/max-attributes-per-line': 'off',
      'vue/html-self-closing': 'off',
      'vue/singleline-html-element-content-newline': 'off',
      'vue/multiline-html-element-content-newline': 'off',
      'vue/attribute-hyphenation': 'off',
      'vue/require-default-prop': 'off',
      'vue/require-prop-types': 'off',
      'vue/no-v-html': 'off',
      'vue/html-indent': 'off',
      'vue/attributes-order': 'off',
      'vue/valid-v-for': 'error',
      'vue/valid-template-root': 'error'
    }
  }
]
