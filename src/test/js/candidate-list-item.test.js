import assert from 'node:assert/strict';
import test from 'node:test';

import { createCandidateListItem } from '../../main/resources/static/js/candidate-list-item.js';

function fakeElement(tagName) {
    const el = {
        tagName,
        className: '',
        textContent: '',
        type: '',
        children: [],
        listeners: {},
        appendChild(child) {
            el.children.push(child);
            return child;
        },
        addEventListener(type, handler) {
            el.listeners[type] = handler;
        },
        click() {
            el.listeners.click?.();
        },
        querySelectorAll(selector) {
            const tag = selector.toUpperCase();
            return el.children.filter((child) => child.tagName === tag);
        },
        querySelector(selector) {
            return el.querySelectorAll(selector)[0];
        }
    };
    return el;
}

function fakeDocument() {
    return {
        createElement: (tag) => fakeElement(tag.toUpperCase())
    };
}

test('renders primary and secondary text with the given class names', () => {
    const document = fakeDocument();

    const item = createCandidateListItem(document, {
        itemClassName: 'item-class',
        buttonClassName: 'button-class',
        primaryClassName: 'primary-class',
        secondaryClassName: 'secondary-class',
        primaryText: '서울만남휴게소',
        secondaryText: '경부선',
        onSelect: () => {}
    });

    assert.equal(item.tagName, 'LI');
    assert.equal(item.className, 'item-class');
    const button = item.querySelector('button');
    assert.equal(button.className, 'button-class');
    assert.equal(button.type, 'button');
    const [primary, secondary] = button.querySelectorAll('p');
    assert.equal(primary.className, 'primary-class');
    assert.equal(primary.textContent, '서울만남휴게소');
    assert.equal(secondary.className, 'secondary-class');
    assert.equal(secondary.textContent, '경부선');
});

test('omits the secondary paragraph entirely when secondaryText is falsy', () => {
    const document = fakeDocument();

    const item = createCandidateListItem(document, {
        primaryText: '오사카',
        secondaryText: undefined,
        onSelect: () => {}
    });

    assert.equal(item.querySelector('button').querySelectorAll('p').length, 1);
});

test('leaves className unset when none is given', () => {
    const document = fakeDocument();

    const item = createCandidateListItem(document, {
        primaryText: '이름',
        onSelect: () => {}
    });

    assert.equal(item.className, '');
    assert.equal(item.querySelector('button').className, '');
});

test('clicking the button calls onSelect once', () => {
    const document = fakeDocument();
    let calls = 0;

    const item = createCandidateListItem(document, {
        primaryText: '이름',
        onSelect: () => {
            calls += 1;
        }
    });

    item.querySelector('button').click();

    assert.equal(calls, 1);
});
