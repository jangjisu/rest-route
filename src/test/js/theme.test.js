import assert from 'node:assert/strict';
import test from 'node:test';

import {
    applyTheme,
    getStoredTheme,
    initThemeToggle,
    nextTheme,
    resolveInitialTheme,
    setStoredTheme,
    themeToggleLabel
} from '../../main/resources/static/js/theme.js';

function createStorage(initial = {}) {
    const store = { ...initial };
    return {
        getItem: (key) => (key in store ? store[key] : null),
        setItem: (key, value) => { store[key] = value; },
        _store: store
    };
}

test('getStoredTheme returns the stored theme when it is valid', () => {
    const storage = createStorage({ 'rr-theme': 'dark' });
    assert.equal(getStoredTheme(storage), 'dark');
});

test('getStoredTheme returns null when nothing is stored', () => {
    const storage = createStorage();
    assert.equal(getStoredTheme(storage), null);
});

test('getStoredTheme returns null for an invalid stored value', () => {
    const storage = createStorage({ 'rr-theme': 'blue' });
    assert.equal(getStoredTheme(storage), null);
});

test('setStoredTheme writes the theme under the shared storage key', () => {
    const storage = createStorage();
    setStoredTheme(storage, 'dark');
    assert.equal(storage._store['rr-theme'], 'dark');
});

test('resolveInitialTheme prefers a stored theme over the system preference', () => {
    const storage = createStorage({ 'rr-theme': 'light' });
    assert.equal(resolveInitialTheme(storage, true), 'light');
});

test('resolveInitialTheme falls back to dark when nothing stored and system prefers dark', () => {
    const storage = createStorage();
    assert.equal(resolveInitialTheme(storage, true), 'dark');
});

test('resolveInitialTheme falls back to light when nothing stored and system does not prefer dark', () => {
    const storage = createStorage();
    assert.equal(resolveInitialTheme(storage, false), 'light');
});

test('nextTheme toggles dark to light and light to dark', () => {
    assert.equal(nextTheme('dark'), 'light');
    assert.equal(nextTheme('light'), 'dark');
});

test('applyTheme sets the data-theme attribute on the given root', () => {
    const root = { attributes: {}, setAttribute(name, value) { this.attributes[name] = value; } };
    applyTheme(root, 'dark');
    assert.equal(root.attributes['data-theme'], 'dark');
});

test('themeToggleLabel describes the action that switches away from dark', () => {
    assert.deepEqual(themeToggleLabel('dark'), {
        icon: '☀️',
        text: '다크',
        ariaLabel: '라이트 모드로 전환'
    });
});

test('themeToggleLabel describes the action that switches away from light', () => {
    assert.deepEqual(themeToggleLabel('light'), {
        icon: '🌙',
        text: '라이트',
        ariaLabel: '다크 모드로 전환'
    });
});

test('initThemeToggle syncs the button UI to the theme already applied on the root', () => {
    const button = { attributes: {}, setAttribute(name, value) { this.attributes[name] = value; }, addEventListener() {} };
    const icon = { textContent: '' };
    const label = { textContent: '' };
    const root = { getAttribute: () => 'dark', setAttribute() {} };
    const elements = { themeToggleButton: button, themeToggleIcon: icon, themeToggleLabel: label };
    const document = { documentElement: root, getElementById: (id) => elements[id] };
    const window = { localStorage: createStorage() };

    initThemeToggle(document, window);

    assert.equal(icon.textContent, '☀️');
    assert.equal(label.textContent, '다크');
    assert.equal(button.attributes['aria-label'], '라이트 모드로 전환');
});

test('initThemeToggle toggles, persists and updates the UI on click', () => {
    let clickHandler;
    const button = {
        attributes: {},
        setAttribute(name, value) { this.attributes[name] = value; },
        addEventListener: (event, handler) => { clickHandler = handler; }
    };
    const icon = { textContent: '' };
    const label = { textContent: '' };
    const root = { theme: 'light', getAttribute() { return this.theme; }, setAttribute(name, value) { this.theme = value; } };
    const elements = { themeToggleButton: button, themeToggleIcon: icon, themeToggleLabel: label };
    const document = { documentElement: root, getElementById: (id) => elements[id] };
    const storage = createStorage();
    const window = { localStorage: storage };

    initThemeToggle(document, window);
    clickHandler();

    assert.equal(root.theme, 'dark');
    assert.equal(storage._store['rr-theme'], 'dark');
    assert.equal(icon.textContent, '☀️');
    assert.equal(label.textContent, '다크');
});

test('initThemeToggle does nothing when the toggle button is not on the page', () => {
    const document = { documentElement: { getAttribute: () => 'light', setAttribute() {} }, getElementById: () => null };
    const window = { localStorage: createStorage() };

    assert.doesNotThrow(() => initThemeToggle(document, window));
});
