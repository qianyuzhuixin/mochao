const THEME_KEY = 'mochao_theme'

export const themes = ['light', 'dark', 'eye-care']

export const themeNames = {
  light: '亮色',
  dark: '暗色',
  'eye-care': '护眼绿'
}

export function getTheme() {
  return localStorage.getItem(THEME_KEY) || 'light'
}

export function setTheme(theme) {
  if (!themes.includes(theme)) {
    theme = 'light'
  }
  document.documentElement.setAttribute('data-theme', theme)
  localStorage.setItem(THEME_KEY, theme)
}

export function initTheme() {
  const theme = getTheme()
  setTheme(theme)
}
