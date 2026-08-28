import {defineConfig, globalIgnores} from "eslint/config";
import prettier from "eslint-plugin-prettier";
import simpleImportSort from "eslint-plugin-simple-import-sort";

import prettierRecommended from "eslint-plugin-prettier/recommended";
import tseslint from "typescript-eslint";
import unicorn from "eslint-plugin-unicorn";

const IGNORED_UNICORN_RULES = {
    "unicorn/filename-case": "off",
    // server.ts er Express-appens entrypoint – toppnivå-oppsett (app.use, setupX(app))
    // er det korrekte mønsteret, ikke en tree-shaking-bekymring som regelen er ment for.
    "unicorn/no-top-level-side-effects": "off",
    // "navIdent" er NAV-domenebegrep (JWT-claim heter "NAVident"), ikke en forkortelse å døpe om.
    "unicorn/name-replacements": "off",
};

export default defineConfig([
    globalIgnores(["**/dist/", "eslint.config.mjs"]),
    ...tseslint.configs.recommended,
    prettierRecommended,
    unicorn.configs.recommended,
    {
        plugins: {
            prettier,
            "simple-import-sort": simpleImportSort,
        },

        rules: {
            "prettier/prettier": ["error"],
            "import/prefer-default-export": "off",
            "simple-import-sort/imports": "error",
            "simple-import-sort/exports": "error",
            ...IGNORED_UNICORN_RULES,
        },
    },
]);
