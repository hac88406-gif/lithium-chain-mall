/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#009688',
        secondary: '#00796B',
        accent: '#FF5722',
        dark: '#2C3E50',
        light: '#ECF0F1'
      }
    },
  },
  plugins: [],
}