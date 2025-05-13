import eslintPluginTs from "@typescript-eslint/eslint-plugin";
import parserTs from "@typescript-eslint/parser";
import lodash from "eslint-plugin-lodash";
import prettier from "eslint-plugin-prettier";
import react from "eslint-plugin-react";
import simpleImportSort from "eslint-plugin-simple-import-sort";
import unicorn from "eslint-plugin-unicorn";

/** @type {import("eslint").Linter.FlatConfig[]} */
export default [
    {
        ignores: [
            "src/vite-env.d.ts",
            "dist",
            "src/routeTree.gen.ts",
            "src/types/nom-admin/nom-admin-models.ts",
            "src/types/nom-api/nom-api-models.ts",
            "src/types/nom-api/nom-api-admin-models.ts",
            "src/types/nom-remedy-integrasjon/nom-remedy-integrasjon-models.ts",
        ],
    },
    {
        files: ["**/*.ts", "**/*.tsx", "**/*.js", "**/*.jsx"],
        languageOptions: {
            parser: parserTs,
            ecmaVersion: "latest",
            sourceType: "module",
        },
        plugins: {
            "@typescript-eslint": eslintPluginTs,
            prettier,
            lodash,
            react,
            unicorn,
            "simple-import-sort": simpleImportSort,
        },
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
            "unicorn/filename-case": "off",
            "unicorn/no-null": "off",
            "unicorn/prevent-abbreviations": "off",
            "unicorn/no-await-expression-member": "off",
            "unicorn/prefer-at": "off",
        },
    },
];
