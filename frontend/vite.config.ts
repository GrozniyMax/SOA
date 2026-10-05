import { readFileSync } from 'node:fs'
import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const movieTarget = env.MOVIE_PROXY_TARGET ?? 'http://localhost:8080'
  const oscarTarget = env.OSCAR_PROXY_TARGET ?? 'http://localhost:8081'
  const https =
    env.HTTPS_CERT && env.HTTPS_KEY
      ? { cert: readFileSync(env.HTTPS_CERT), key: readFileSync(env.HTTPS_KEY) }
      : undefined

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
    preview: {
      port: 4173,
      https,
    },
  }
})
