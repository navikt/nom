// eslint-disable-next-line no-undef
module.exports = {
  parser: "@typescript-eslint/parser",
  env: {
    browser: true,
    es2021: true,
  },
  extends: [
    // 'plugin:react/recommended',
    // 'standard-with-typescript'
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
    "prettier",
  ],
  overrides: [],
  parserOptions: {
    ecmaVersion: "latest",
    sourceType: "module",
  },
  plugins: ["react", "prettier", "@typescript-eslint"],
  rules: {
    "prettier/prettier": ["error"],
    "@typescript-eslint/no-explicit-any": "off",
    "@typescript-eslint/prefer-namespace-keyword": "off",
    "@typescript-eslint/no-namespace": "off",
  },
  ignorePatterns: [
      "**/graphql_generated.ts"
  ],
};
