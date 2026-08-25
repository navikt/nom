import eslintPluginTs from "@typescript-eslint/eslint-plugin";
import parserTs from "@typescript-eslint/parser";
import eslintPluginImport from "eslint-plugin-import";
import lodash from "eslint-plugin-lodash";
import prettier from "eslint-plugin-prettier";
import react from "eslint-plugin-react";
import simpleImportSort from "eslint-plugin-simple-import-sort";
import unicorn from "eslint-plugin-unicorn";

/** @type {import("eslint").Linter.FlatConfig[]} */
export default [
    {
        ignores: ["src/vite-env.d.ts", "dist", "src/types/api-models.ts", "src/routeTree.gen.ts", "**/graphql_generated.ts"],
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
            eslintPluginImport,
        },
        settings: {
            "import/parsers": {
                "@typescript-eslint/parser": [".ts", ".tsx"],
            },
            "import/resolver": {
                typescript: {
                    project: "./tsconfig.json",
                },
            },
        },
        rules: {
            eqeqeq: ["error", "always"],
            "no-console": "error",
            "lodash/import-scope": ["error", "method"],
            "react/jsx-key": "error",
            "react/jsx-sort-props": "error",
            "prettier/prettier": "error",
            "import/prefer-default-export": "off",
            "eslintPluginImport/no-cycle": "error",
            "simple-import-sort/imports": "error",
            "simple-import-sort/exports": "error",
            "@typescript-eslint/consistent-type-imports": ["warn"],
            "unicorn/filename-case": "off",
            "unicorn/no-null": "off",
            "unicorn/prefer-spread": "off",
        },
    },
];
