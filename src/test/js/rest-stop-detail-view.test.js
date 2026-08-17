import assert from 'node:assert/strict';
import test from 'node:test';
import {
    formatEvChargerAvailability,
    formatEvChargerCount,
    renderEventSection,
    renderOilInfo,
    renderThemeBadges,
    restStopDetailEmptyMessage
} from '../../main/resources/static/js/rest-stop-detail-view.js';

function createFakeElement(classNames = []) {
    const classes = new Set(classNames);
    return {
        attributes: new Map(),
        children: [],
        eventListeners: new Map(),
        textContent: '',
        classList: {
            add: (className) => classes.add(className),
            remove: (className) => classes.delete(className),
            contains: (className) => classes.has(className),
            toggle: (className, force) => {
                const shouldAdd = force === undefined ? !classes.has(className) : force;
                if (shouldAdd) {
                    classes.add(className);
                    return true;
                }
                classes.delete(className);
                return false;
            }
        },
        appendChild(child) {
            this.children.push(child);
        },
        addEventListener(name, listener) {
            this.eventListeners.set(name, listener);
        },
        setAttribute(name, value) {
            this.attributes.set(name, String(value));
        },
        replaceChildren(...children) {
            this.children = children;
        }
    };
}

function withFakeOilInfoDocument(callback) {
    const previousDocument = globalThis.document;
    const elements = new Map([
        ['restStopOilSection', createFakeElement(['d-none'])],
        ['restStopOilGasolinePrice', createFakeElement()],
        ['restStopOilDieselPrice', createFakeElement()],
        ['restStopOilLpgPrice', createFakeElement()],
        ['restStopOilCompany', createFakeElement()],
        ['restStopOilTelNo', createFakeElement()],
        ['restStopOilRefreshStatus', createFakeElement()],
        ['restStopOilConvenienceTags', createFakeElement()],
        ['restStopOilConvenienceFallback', createFakeElement(['d-none'])],
        ['restStopOilConvenienceDetails', createFakeElement()]
    ]);

    globalThis.document = {
        createElement: () => createFakeElement(),
        getElementById: (id) => elements.get(id) ?? null
    };

    try {
        return callback(elements);
    } finally {
        globalThis.document = previousDocument;
    }
}

function withFakeThemeDocument(callback) {
    const previousDocument = globalThis.document;
    const elements = new Map([['restStopDetailThemes', createFakeElement(['d-none'])]]);

    globalThis.document = {
        createElement: () => createFakeElement(),
        getElementById: (id) => elements.get(id) ?? null
    };

    try {
        return callback(elements);
    } finally {
        globalThis.document = previousDocument;
    }
}

function withFakeEventDocument(callback) {
    const previousDocument = globalThis.document;
    const elements = new Map([
        ['restStopDetailEventSection', createFakeElement(['d-none'])],
        ['restStopDetailEventList', createFakeElement()]
    ]);

    globalThis.document = {
        createElement: () => createFakeElement(),
        getElementById: (id) => elements.get(id) ?? null
    };

    try {
        return callback(elements);
    } finally {
        globalThis.document = previousDocument;
    }
}

test('restStopDetailEmptyMessage guides only rest stops without renderable related detail', () => {
    assert.equal(
        restStopDetailEmptyMessage({ unitName: '목감(서울)휴게소', routeName: '서해안선' }),
        '이 휴게소의 상세 정보를 준비하고 있습니다.'
    );
    assert.equal(restStopDetailEmptyMessage({ address: '경기도 시흥시' }), '');
    assert.equal(
        restStopDetailEmptyMessage({ unitName: '목감(서울)휴게소' }, true),
        '상세 정보를 불러오지 못했습니다.'
    );
});

test('formatEvChargerAvailability only displays an indicator for true values', () => {
    assert.equal(formatEvChargerAvailability({ hasEvCharger: true }), '전기차 충전 가능');
    assert.equal(formatEvChargerAvailability({ hasEvCharger: false }), '');
    assert.equal(formatEvChargerAvailability({}), '');
});

test('formatEvChargerCount only displays positive charger counts', () => {
    assert.equal(formatEvChargerCount(6), '6대');
    assert.equal(formatEvChargerCount(0), '');
    assert.equal(formatEvChargerCount(undefined), '');
});

test('renderThemeBadges renders one badge per theme and reveals the list', () => {
    withFakeThemeDocument((elements) => {
        renderThemeBadges([
            { name: '4계절 꽃이 있는 휴게소', detail: '365일 꽃향기가 나는 휴게소입니다' },
            { name: '포토존', detail: '' }
        ]);

        const list = elements.get('restStopDetailThemes');
        assert.equal(list.children.length, 2);
        assert.equal(list.children[0].textContent, '4계절 꽃이 있는 휴게소');
        assert.equal(list.children[0].title, '365일 꽃향기가 나는 휴게소입니다');
        assert.equal(list.children[1].textContent, '포토존');
        assert.equal(list.children[1].title, undefined);
        assert.equal(list.classList.contains('d-none'), false);
    });
});

test('renderThemeBadges hides the list when there are no themes', () => {
    withFakeThemeDocument((elements) => {
        renderThemeBadges([]);

        assert.equal(elements.get('restStopDetailThemes').children.length, 0);
        assert.equal(elements.get('restStopDetailThemes').classList.contains('d-none'), true);
    });
});

test('renderThemeBadges tolerates missing or malformed input without throwing', () => {
    withFakeThemeDocument((elements) => {
        assert.doesNotThrow(() => renderThemeBadges(undefined));
        assert.doesNotThrow(() => renderThemeBadges(null));
        renderThemeBadges([{ name: '   ' }, { name: null }, 'not-an-object']);

        assert.equal(elements.get('restStopDetailThemes').children.length, 0);
        assert.equal(elements.get('restStopDetailThemes').classList.contains('d-none'), true);
    });
});

test('renderEventSection renders one item per event and reveals the section', () => {
    withFakeEventDocument((elements) => {
        renderEventSection([
            { name: 'TEN+1 이벤트', detail: '한식당 식사 10번 이용 시 1번 무료', period: '2026.01.01 ~ 2026.12.31' },
            { name: '포토존 이벤트', detail: '', period: '2026.03.01 ~ 2026.03.31' }
        ]);

        const list = elements.get('restStopDetailEventList');
        assert.equal(list.children.length, 2);
        assert.equal(list.children[0].children[0].textContent, 'TEN+1 이벤트');
        assert.equal(list.children[0].children[1].textContent, '2026.01.01 ~ 2026.12.31');
        assert.equal(list.children[0].children[2].textContent, '한식당 식사 10번 이용 시 1번 무료');
        assert.equal(list.children[1].children.length, 2);
        assert.equal(elements.get('restStopDetailEventSection').classList.contains('d-none'), false);
    });
});

test('renderEventSection hides the section when there are no active events', () => {
    withFakeEventDocument((elements) => {
        renderEventSection([]);

        assert.equal(elements.get('restStopDetailEventList').children.length, 0);
        assert.equal(elements.get('restStopDetailEventSection').classList.contains('d-none'), true);
    });
});

test('renderEventSection tolerates missing or malformed input without throwing', () => {
    withFakeEventDocument((elements) => {
        assert.doesNotThrow(() => renderEventSection(undefined));
        assert.doesNotThrow(() => renderEventSection(null));
        renderEventSection([{ name: '   ' }, { name: null }, 'not-an-object']);

        assert.equal(elements.get('restStopDetailEventList').children.length, 0);
        assert.equal(elements.get('restStopDetailEventSection').classList.contains('d-none'), true);
    });
});

test('renderOilInfo keeps the oil section visible with empty state when oil info is missing', () => {
    withFakeOilInfoDocument((elements) => {
        renderOilInfo(null);

        assert.equal(elements.get('restStopOilSection').classList.contains('d-none'), false);
        assert.equal(elements.get('restStopOilGasolinePrice').textContent, '정보 없음');
        assert.equal(elements.get('restStopOilDieselPrice').textContent, '정보 없음');
        assert.equal(elements.get('restStopOilLpgPrice').textContent, '정보 없음');
        assert.equal(elements.get('restStopOilCompany').textContent, '정보 없음');
        assert.equal(elements.get('restStopOilTelNo').textContent, '정보 없음');
        assert.equal(elements.get('restStopOilRefreshStatus').textContent, '최근 갱신: 갱신 정보 없음');
        assert.equal(elements.get('restStopOilConvenienceFallback').textContent, '주유소 편의시설 정보 없음');
        assert.equal(elements.get('restStopOilConvenienceFallback').classList.contains('d-none'), false);
    });
});
