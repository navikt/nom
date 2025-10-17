import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import monacoEditorEsmPlugin from 'vite-plugin-monaco-editor-esm'


// https://vite.dev/config/
export default defineConfig({
  base: "/playground/",
  plugins: [react(), monacoEditorEsmPlugin({
    publicPath: "../", // workaround for å unngå at worker bundles blir lagt på `/playground/playground/....`
    languageWorkers: ["editorWorkerService", "json"],
    customWorkers: [
      {
        label: "graphql",
        entry: "monaco-graphql/esm/graphql.worker.js"
      }
    ]
  })]
});
