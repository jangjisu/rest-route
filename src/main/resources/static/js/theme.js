/* ===================================================
   theme.js — 라이트/다크 테마 토글
   =================================================== */

const STORAGE_KEY = 'rr-theme';
const THEMES = new Set(['light', 'dark']);

export function getStoredTheme(storage) {
    const value = storage.getItem(STORAGE_KEY);
    return THEMES.has(value) ? value : null;
}

export function setStoredTheme(storage, theme) {
    storage.setItem(STORAGE_KEY, theme);
}

export function resolveInitialTheme(storage, prefersDark) {
    return getStoredTheme(storage) ?? (prefersDark ? 'dark' : 'light');
}

export function nextTheme(theme) {
    return theme === 'dark' ? 'light' : 'dark';
}

export function applyTheme(root, theme) {
    root.setAttribute('data-theme', theme);
}

export function themeToggleLabel(theme) {
    return theme === 'dark'
        ? { icon: '☀️', text: '다크', ariaLabel: '라이트 모드로 전환' }
        : { icon: '🌙', text: '라이트', ariaLabel: '다크 모드로 전환' };
}

function syncToggleUi(button, icon, label, theme) {
    const { icon: iconText, text, ariaLabel } = themeToggleLabel(theme);
    if (icon) icon.textContent = iconText;
    if (label) label.textContent = text;
    button.setAttribute('aria-label', ariaLabel);
}

export function initThemeToggle(document, window) {
    const button = document.getElementById('themeToggleButton');
    if (!button) return;

    const root = document.documentElement;
    const icon = document.getElementById('themeToggleIcon');
    const label = document.getElementById('themeToggleLabel');

    let current = root.getAttribute('data-theme') === 'dark' ? 'dark' : 'light';
    syncToggleUi(button, icon, label, current);

    button.addEventListener('click', () => {
        current = nextTheme(current);
        applyTheme(root, current);
        setStoredTheme(window.localStorage, current);
        syncToggleUi(button, icon, label, current);
    });
}
