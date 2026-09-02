import assert from 'node:assert/strict';
import test from 'node:test';

import {
    getRememberedInterest,
    getRememberedLocationAnswer,
    rememberInterest,
    rememberLocationAnswer
} from '../../main/resources/static/js/finder-session-memory.js';

function fakeSessionStorage() {
    const store = new Map();
    return {
        getItem: (key) => (store.has(key) ? store.get(key) : null),
        setItem: (key, value) => store.set(key, String(value)),
        removeItem: (key) => store.delete(key)
    };
}

test('getRememberedLocationAnswer returns null before anything is remembered', () => {
    const storage = fakeSessionStorage();
    assert.equal(getRememberedLocationAnswer('nearby-search', storage), null);
});

test('rememberLocationAnswer stores per-mode and is read back independently', () => {
    const storage = fakeSessionStorage();
    rememberLocationAnswer('nearby-search', 'granted', storage);
    rememberLocationAnswer('destination-recommendation', 'skipped', storage);

    assert.equal(getRememberedLocationAnswer('nearby-search', storage), 'granted');
    assert.equal(getRememberedLocationAnswer('destination-recommendation', storage), 'skipped');
});

test('getRememberedInterest returns undefined before anything is remembered (distinct from a remembered skip)', () => {
    const storage = fakeSessionStorage();
    assert.equal(getRememberedInterest(storage), undefined);
});

test('rememberInterest stores a chosen fuel/EV value', () => {
    const storage = fakeSessionStorage();
    rememberInterest('DIESEL', storage);

    assert.equal(getRememberedInterest(storage), 'DIESEL');
});

test('rememberInterest(null) is remembered as an explicit skip, not "unanswered"', () => {
    const storage = fakeSessionStorage();
    rememberInterest(null, storage);

    assert.equal(getRememberedInterest(storage), null);
});

test('all functions no-op safely when sessionStorage is unavailable (private browsing etc.)', () => {
    assert.doesNotThrow(() => {
        rememberLocationAnswer('nearby-search', 'granted', undefined);
        rememberInterest('EV', undefined);
    });
    assert.equal(getRememberedLocationAnswer('nearby-search', undefined), null);
    assert.equal(getRememberedInterest(undefined), undefined);
});
