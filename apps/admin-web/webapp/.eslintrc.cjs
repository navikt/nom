const IGNORED_UNICORN_RULES = {
  "unicorn/filename-case": "off",
  "unicorn/no-null": "off",
  "unicorn/prevent-abbreviations": "off",
};

// eslint-disable-next-line unicorn/prefer-module,no-undef
module.exports = {
  parser: "@typescript-eslint/parser",
  env: {
    browser: true,
    es2021: true,
  },
  extends: [
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
    "plugin:@tanstack/eslint-plugin-query/recommended",
    "prettier",
    "plugin:unicorn/recommended",
  ],
  overrides: [],
  parserOptions: {
    ecmaVersion: "latest",
    sourceType: "module",
  },
  plugins: [
    "lodash",
    "react",
    "prettier",
    "@typescript-eslint",
    "simple-import-sort",
  ],
  rules: {
    eqeqeq: ["error", "always"],
    "no-console": "error",
    "lodash/import-scope": ["error", "method"],
    "react/jsx-key": "error",
    "react/jsx-sort-props": "error",
    "prettier/prettier": ["error"],
    "import/prefer-default-export": "off",
    "simple-import-sort/imports": "error",
    "simple-import-sort/exports": "error",
    "@typescript-eslint/consistent-type-imports": ["warn"],
    ...IGNORED_UNICORN_RULES,
  },
};
