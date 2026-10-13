type Theme = 'system' | 'light' | 'dark'

const KEY = 'theme'
let current: Theme = ((localStorage.getItem(KEY) as Theme) || 'system')

function apply(theme: Theme) {
  if (theme === 'system') {
    document.documentElement.removeAttribute('data-theme')
  } else {
    document.documentElement.setAttribute('data-theme', theme)
  }
}

export function getTheme(): Theme { return current }
export function setTheme(t: Theme) {
  current = t
  localStorage.setItem(KEY, t)
  apply(t)
}
export function initTheme() { apply(current) }
