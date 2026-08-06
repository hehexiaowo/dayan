import { defineConfig } from 'vite';
import uni from '@dcloudio/vite-plugin-uni';

export default defineConfig({
  plugins: [uni()],
  server: {
    port: 5182,
    proxy: {
      '/agent-api': {
        target: 'http://localhost:8000',
        changeOrigin: true,
      },
    },
  },
});
