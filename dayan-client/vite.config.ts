import { defineConfig } from 'vite';
import uni from '@dcloudio/vite-plugin-uni';

export default defineConfig({
  plugins: [uni()],
  server: {
    port: 5183,
    proxy: {
      '/client-api': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },
    },
  },
});
