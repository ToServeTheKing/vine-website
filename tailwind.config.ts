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
        'bakery': {
          50: '#FDF8F6',
          100: '#F2E8E5',
          200: '#E8D6D0',
          300: '#D4B4A5',
          400: '#C19277',
          500: '#A67759',
          600: '#8B5E43',
          700: '#6F4A34',
          800: '#523627',
          900: '#352318',
        },
      },
      fontFamily: {
        'serif': ['var(--font-playfair)'],
        'sans': ['var(--font-raleway)'],
      },
    },
  },
  plugins: [],
};
export default config;