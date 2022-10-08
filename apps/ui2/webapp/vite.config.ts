import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/nom-api": {
        target: "http://localhost:8080/",
        changeOrigin: true
      },
      "/sessionInfo": {
        target: "http://localhost:8080/",
        changeOrigin: true
      },
      "/teamcat": {
        target: "http://localhost:8080/",
        changeOrigin: true
      }
    },
    hmr: {
      // necessary to make hot reloading work in uidev mode in prod and dev environments
      // otherwise secure web sockets protocol (wss) will be inferred from https prefix
      protocol: "ws"
    },
  }
})
