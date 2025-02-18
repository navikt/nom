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
  build: {
    assetsInlineLimit: 0,
  },
  server: {
    cors: {
      origin: [
        "https://nom-admin.ansatt.dev.nav.no",
        "https://nom-admin.intern.dev.nav.no",
        "https://nom-admin.ansatt.nav.no",
        /^http:\/\/localhost:\d{4}$/,
      ],
    },
    proxy: {
      "/api": {
        target: "http://localhost:8080/",
        changeOrigin: true,
      },
    },
  },
});
