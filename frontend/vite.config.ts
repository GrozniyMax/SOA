import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const movieTarget = env.MOVIE_PROXY_TARGET ?? 'http://localhost:8080'
  const oscarTarget = env.OSCAR_PROXY_TARGET ?? 'http://localhost:8081'

  return {
    base: './',
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      port: 5173,
      proxy: {
        '/api/movie': {
          target: movieTarget,
          changeOrigin: true,
          secure: false,
          rewrite: (path) => path.replace(/^\/api\/movie/, ''),
        },
        '/api/oscar': {
          target: oscarTarget,
          changeOrigin: true,
          secure: false,
          rewrite: (path) => path.replace(/^\/api\/oscar/, '/oscar'),
        },
      },
    },
  }
})
