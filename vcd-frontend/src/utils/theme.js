/** localStorage 键，管理员与普通用户共用 */
export const THEME_STORAGE_KEY = 'vcd_theme'

/** @returns {'light' | 'dark'} */
export function getStoredTheme() {
  const v = localStorage.getItem(THEME_STORAGE_KEY)
  if (v === 'dark' || v === 'light') return v
  return 'light'
}

/**
 * 应用主题并持久化（Element Plus 使用 document.documentElement 的 .dark）
 * @param {'light' | 'dark'} mode
 */
export function applyTheme(mode) {
  const isDark = mode === 'dark'
  document.documentElement.classList.toggle('dark', isDark)
  localStorage.setItem(THEME_STORAGE_KEY, mode)
}

export function initTheme() {
  applyTheme(getStoredTheme())
}

/** @returns {'light' | 'dark'} */
export function toggleTheme() {
  const cur = getStoredTheme()
  const next = cur === 'dark' ? 'light' : 'dark'
  applyTheme(next)
  return next
}
