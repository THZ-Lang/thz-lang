import { defineConfig } from 'vite';
import path from 'path';

export default defineConfig({
  root: path.resolve(__dirname, 'playground'),
  publicDir: false,
  server: {
    port: 5173,
    open: false,
    fs: { allow: [path.resolve(__dirname)] },
  },
  build: {
    outDir: path.resolve(__dirname, 'dist/playground'),
    emptyOutDir: true,
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
});
