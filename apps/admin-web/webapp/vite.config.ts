import { fileURLToPath, URL } from "node:url";

import { TanStackRouterVite } from "@tanstack/router-vite-plugin";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react(), TanStackRouterVite()],
  resolve: {
    alias: {
      "~": fileURLToPath(new URL("src", import.meta.url)),
    },
  },
  server: {
    origin: "http://localhost:5173",
    proxy: {
      "/api": {
        target: "http://localhost:8080/",
        changeOrigin: true,
      },
    },
  },
});
