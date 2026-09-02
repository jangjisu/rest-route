import assert from 'node:assert/strict';
import test from 'node:test';

import { renderResultCard, setLoading, setStatus, showScreen } from '../../main/resources/static/js/finder-render.js';

function fakeElement() {
    const el = {
        className: '',
        textContent: '',
        hidden: false,
        children: [],
        classList: {
            classes: new Set(),
            add(cls) {
                this.classes.add(cls);
            }
        },
        appendChild(child) {
            el.children.push(child);
            return child;
        },
        get childElementCount() {
            return el.children.length;
        }
    };
    return el;
}

function fakeDocument() {
    return {
        createElement: () => fakeElement()
    };
}

function fakeScreenSections(names) {
    return names.map((name) => ({ dataset: { finderScreen: name }, hidden: false }));
}

test('showScreen unhides only the section matching the given name', () => {
    const sections = fakeScreenSections(['landing', 'mode1', 'mode2']);
    const document = {
        querySelectorAll: () => sections
    };

    showScreen(document, 'mode1');

    assert.deepEqual(
        sections.map((section) => ({ name: section.dataset.finderScreen, hidden: section.hidden })),
        [
            { name: 'landing', hidden: true },
            { name: 'mode1', hidden: false },
            { name: 'mode2', hidden: true }
        ]
    );
});

test('setLoading toggles the loading overlay and no-ops when it is missing', () => {
    const overlay = { hidden: true };
    const document = { getElementById: () => overlay };

    setLoading(document, true);
    assert.equal(overlay.hidden, false);
    setLoading(document, false);
    assert.equal(overlay.hidden, true);

    assert.doesNotThrow(() => setLoading({ getElementById: () => null }, true));
});

test('setStatus shows a message or hides the element when message is empty', () => {
    const element = { hidden: false, textContent: '' };

    setStatus(element, '검색 결과가 없어요.');
    assert.equal(element.hidden, false);
    assert.equal(element.textContent, '검색 결과가 없어요.');

    setStatus(element, '');
    assert.equal(element.hidden, true);
    assert.equal(element.textContent, '');

    assert.doesNotThrow(() => setStatus(null, '메시지'));
});

test('renderResultCard renders name, route, distance and colored badges', () => {
    const document = fakeDocument();

    const card = renderResultCard(document, {
        name: 'A휴게소',
        routeLabel: '경부선',
        distanceLabel: '1.2km',
        badges: [
            { key: 'SIZE_LARGE', label: '규모 큰 곳' },
            { key: 'UNKNOWN_KEY', label: '색 없는 배지' }
        ],
        colorClassByKey: { SIZE_LARGE: 'finder-badge-size' }
    });

    assert.equal(card.className, 'finder-result-card');
    const [main, distanceEl] = card.children;
    const [nameEl, routeEl, badgeRow] = main.children;
    assert.equal(nameEl.textContent, 'A휴게소');
    assert.equal(routeEl.textContent, '경부선');
    assert.equal(badgeRow.children.length, 2);
    assert.deepEqual([...badgeRow.children[0].classList.classes], ['finder-badge-size']);
    assert.deepEqual([...badgeRow.children[1].classList.classes], []);
    assert.equal(distanceEl.textContent, '1.2km');
});

test('renderResultCard omits the route line, badge row and distance span when absent', () => {
    const document = fakeDocument();

    const card = renderResultCard(document, {
        name: 'B휴게소',
        routeLabel: '',
        distanceLabel: '',
        badges: [],
        colorClassByKey: {}
    });

    const [main] = card.children;
    assert.equal(main.children.length, 1);
    assert.equal(card.children.length, 1);
});
