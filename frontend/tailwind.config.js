/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  darkMode: ['selector', '[data-theme="dark"]'],
  theme: {
    extend: {
      colors: {
        bg: 'var(--bg)', surface: 'var(--surface)', 'surface-2': 'var(--surface-2)',
        ink: 'var(--ink)', 'ink-2': 'var(--ink-2)', line: 'var(--line)',
        brand: 'var(--brand)', 'on-brand': 'var(--on-brand)', sun: 'var(--sun)',
        accent: 'var(--accent)', 'on-accent': 'var(--on-accent)', 'accent-soft': 'var(--accent-soft)',
        good: 'var(--good)', 'good-soft': 'var(--good-soft)',
        warn: 'var(--warn)', 'warn-soft': 'var(--warn-soft)',
        bad: 'var(--bad)', 'bad-soft': 'var(--bad-soft)',
        info: 'var(--info)', 'info-soft': 'var(--info-soft)',
        focus: 'var(--focus)'
      },
      fontFamily: {
        body: ['var(--f-body)'], display: ['var(--f-display)'], mono: ['var(--f-mono)']
      },
      borderRadius: { card: '12px', shell: '14px' }
    }
  },
  plugins: []
}
