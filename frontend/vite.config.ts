import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// Proxy para o backend em dev — mesma origem efetiva (ADR-002), sem CORS.
// /ws: WebSocket SockJS do chat (ADR-008) — ws:true para upgrade de protocolo.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws': { target: 'http://localhost:8080', ws: true },
    },
  },
})
