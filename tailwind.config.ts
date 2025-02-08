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
          50: '#f2f3f2',
          100: '#d9dbd7',
          200: '#c0c3bc',
          300: '#a7aba1',
          400: '#8e9386',
          500: '#74796c',
          600: '#5a5e54',
          700: '#40433c',
          800: '#272824',
          900: '#0d0d0c',
        },
      },
      fontFamily: {
        'serif': ['var(--font-playfair)'],
        'sans': ['var(--font-raleway)'],
        'adbhashitha': ['var(--font-adbhashitha)'],
        'lejour': ['var(--font-lejour)'],
      },
    },
  },
  plugins: [],
};
export default config;