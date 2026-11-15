/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        dark: {
          bg: '#09090B',
          surface: '#18181B',
          elevated: '#202024',
          border: '#27272A',
          hover: '#27272E',
        },
        electric: {
          yellow: '#FACC15',
          'yellow-hover': '#EAB308',
          'yellow-dark': '#CA8A04',
          'yellow-light': '#FEF08A',
        },
        ai: {
          purple: '#A855F7',
          'purple-dark': '#7E22CE',
          'purple-light': '#C084FC',
          'purple-glow': 'rgba(168, 85, 247, 0.25)',
        },
        text: {
          primary: '#FAFAFA',
          secondary: '#A1A1AA',
          muted: '#71717A',
        },
        brand: {
          50: '#f0fdf4',
          100: '#dcfce7',
          500: '#22c55e',
          600: '#16a34a',
          700: '#15803d',
          800: '#166534',
          900: '#14532d',
        },
        slate: {
          850: '#151f32',
          950: '#0a0f1d',
        }
      },
      boxShadow: {
        'glow-yellow': '0 0 25px -5px rgba(250, 204, 21, 0.25)',
        'glow-purple': '0 0 25px -5px rgba(168, 85, 247, 0.3)',
        'glow-sm-yellow': '0 0 15px -3px rgba(250, 204, 21, 0.2)',
        'glow-sm-purple': '0 0 15px -3px rgba(168, 85, 247, 0.2)',
      }
    },
  },
  plugins: [],
}
