import eslintPluginTs from "@typescript-eslint/eslint-plugin";
import parserTs from "@typescript-eslint/parser";
import { createTypeScriptImportResolver } from "eslint-import-resolver-typescript";
import importX from "eslint-plugin-import-x";
import lodash from "eslint-plugin-lodash";
import prettier from "eslint-plugin-prettier";
import reactJsx from "eslint-plugin-react-jsx";
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
            unicorn,
            reactJsx,
            "simple-import-sort": simpleImportSort,
            "import-x": importX,
        },
        settings: {
            "import-x/parsers": {
                "@typescript-eslint/parser": [".ts", ".tsx", ".cts", ".mts"],
            },
            "import-x/resolver-next": [
                createTypeScriptImportResolver({
                    alwaysTryTypes: true,
                    project: ["tsconfig.eslint.json"],
                }),
            ],
        },
        rules: {
            eqeqeq: ["error", "always"],
            "no-console": "error",
            "lodash/import-scope": ["error", "method"],
            "prettier/prettier": "error",
            "import-x/no-cycle": "error",
            "simple-import-sort/imports": "error",
            "simple-import-sort/exports": "error",
            "@typescript-eslint/consistent-type-imports": ["warn"],
            "unicorn/filename-case": "off",
            "unicorn/no-null": "off",
            "unicorn/prefer-spread": "off",
        },
    },
];
