import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Dev server: every /api request is proxied to the Spring Boot API on :8080,
// so the browser never hits a CORS wall and the code can use relative URLs.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
      // GitHub OAuth2 handshake. Only the endpoint that starts the flow is proxied: proxying the
      // whole /oauth2 prefix would also swallow the SPA route /oauth2/callback. changeOrigin makes
      // Spring build the callback URL on :8080, so register
      // http://localhost:8080/login/oauth2/code/github in the GitHub OAuth App.
      '/oauth2/authorization': {
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
      '/login/oauth2': {
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
})
