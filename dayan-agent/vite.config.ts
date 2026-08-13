import { defineConfig } from 'vite';
import uni from '@dcloudio/vite-plugin-uni';

export default defineConfig({
  plugins: [uni()],
  css: {
    preprocessorOptions: {
      scss: {
        // Vite 5.2 只能走 Sass legacy JS API（modern-compiler 需 Vite 5.4+），静音该弃用警告
        silenceDeprecations: ['legacy-js-api'],
      },
    },
  },
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
