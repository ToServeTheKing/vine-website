import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import svgr from 'vite-plugin-svgr';
import path from 'node:path';

export default defineConfig({
  // svgr keeps the `import Logo from './x.svg?react'` style the old @svgr/webpack setup used, so the
  // logo marks stay inline SVG and inherit currentColor.
  plugins: [react(), tailwindcss(), svgr()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
  server: {
    port: 2024,
    // Backend on :8080 during development; the built SPA is served by Spring, same origin.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    outDir: 'dist',
    // The photos live in MinIO, so what's left is small — a warning here would mean a real regression.
    chunkSizeWarningLimit: 600,
  },
});
