/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}"
  ],
  theme: {
    extend: {
      colors: {
        'bg-primary': '#F9F8F5',
        'bg-secondary': '#FFFFFF',
        'bg-tertiary': '#F3F1EC',
        'bg-elevated': '#FFFFFF',
        'text-primary': '#1C1917',
        'text-secondary': '#57534E',
        'text-tertiary': '#A8A29E',
        'accent-primary': '#D97706',
        'accent-hover': '#F59E0B',
        'accent-muted': '#FFF7ED',
        'accent-soft': '#FDE68A',
        'surface': '#FFFFFF',
        'surface-hover': '#FAFAF9',
        'border-subtle': '#E7E5E4',
        'border-default': '#D6D3D1',
      },
      boxShadow: {
        'card': '0 1px 3px rgba(28, 25, 23, 0.04), 0 4px 12px rgba(28, 25, 23, 0.04)',
        'card-hover': '0 4px 16px rgba(28, 25, 23, 0.06), 0 1px 2px rgba(28, 25, 23, 0.04)',
        'elevated': '0 8px 40px rgba(28, 25, 23, 0.1)',
        'glass': '0 4px 24px rgba(28, 25, 23, 0.06)',
        'button': '0 1px 3px rgba(217, 119, 6, 0.2)',
        'button-hover': '0 4px 14px rgba(217, 119, 6, 0.3)',
      },
      fontFamily: {
        display: ['"DM Serif Display"', 'Georgia', 'serif'],
      },
      borderRadius: {
        '2xl': '16px',
        '3xl': '20px',
        '4xl': '24px',
      },
      backgroundImage: {
        'gradient-warm': 'linear-gradient(135deg, #F9F8F5 0%, #F3F1EC 100%)',
        'gradient-amber': 'linear-gradient(135deg, #FEF3C7 0%, #FDE68A 100%)',
      },
    }
  },
  plugins: [require('@tailwindcss/typography')]
}