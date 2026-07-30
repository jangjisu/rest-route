export default [
  {
    files: ['src/main/resources/static/js/**/*.js'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        bootstrap: 'readonly',
        clearTimeout: 'readonly',
        console: 'readonly',
        document: 'readonly',
        encodeURIComponent: 'readonly',
        fetch: 'readonly',
        FormData: 'readonly',
        navigator: 'readonly',
        Number: 'readonly',
        Promise: 'readonly',
        setTimeout: 'readonly',
        window: 'readonly'
      }
    },
    rules: {
      eqeqeq: 'error',
      'no-undef': 'error',
      'no-unused-vars': 'error',
      'no-var': 'error',
      'prefer-const': 'error'
    }
  }
];
