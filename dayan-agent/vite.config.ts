import { defineConfig } from 'vite';
import uni from '@dcloudio/vite-plugin-uni';

export default defineConfig({
  plugins: [uni()],
  server: {
    port: 5182,
    proxy: {
      '/agent-api': {
        // 联调阶段直连 dayan-agent（8082），不经过 dayan-gateway（8000）
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
    },
  },
});
