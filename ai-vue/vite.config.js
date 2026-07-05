import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import  { resolve } from 'path'


// https://vite.dev/config/  http://159.75.169.224:1235
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://36.138.103.18:38180',
        changeOrigin: true
      },
    },
  },
})

