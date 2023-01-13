import { fileURLToPath, URL } from "node:url";

import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "~": fileURLToPath(new URL("src", import.meta.url)),
    },
  },
  server: {
    proxy: {
      "/nom-api": {
        target: "http://localhost:8080/",
        changeOrigin: true,
      },
      "/sessionInfo": {
        target: "http://localhost:8080/",
        changeOrigin: true,
      },
      "/teamcat": {
        target: "http://localhost:8080/",
        changeOrigin: true,
      },
    },
    hmr: {
      // necessary to make hot reloading work in uidev mode in prod and dev environments
      // otherwise secure web sockets protocol (wss) will be inferred from https prefix
      protocol: "ws",
    },
  },
});
