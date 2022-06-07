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
      }
    }
  }
})
