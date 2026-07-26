import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import path from 'node:path';

/**
 * Builds the admin screen, and only that. The public site is server-rendered by Spring and never comes
 * through here — its stylesheet is compiled from the Thymeleaf templates by the Tailwind CLI
 * (`npm run build:css`).
 *
 * vite-plugin-svgr went with the public pages: the logo marks were inlined through it so they could
 * take their colour from the surrounding text, and the server-rendered header does that with a CSS
 * mask instead.
 */
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
  server: {
    port: 2024,
    // Backend on :8080 during development; the built admin is served by Spring, same origin.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 600,
  },
});
