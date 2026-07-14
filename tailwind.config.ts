import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        // Sage & Cream. 500 is the signature sage; 600+ are the darker tones that
        // white text can actually sit on (500 on white is only 3.6:1 — too low).
        'bakery': {
          50: '#faf7f0',
          100: '#f0efe3',
          200: '#dde0cc',
          300: '#c3cbae',
          400: '#a2ae8b',
          500: '#7c8b6b',
          600: '#5f6f52',
          700: '#4a5740',
          800: '#37412f',
          900: '#232b1e',
        },
      },
      fontFamily: {
        'sans': ['var(--font-raleway)'],
        'adbhashitha': ['var(--font-adbhashitha)'],
        'lejour': ['var(--font-lejour)'],
      },
    },
  },
  plugins: [],
};
export default config;