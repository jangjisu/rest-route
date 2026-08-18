import assert from 'node:assert/strict';
import test from 'node:test';

import {
    buildCalendarCells,
    daysInMonth,
    firstWeekdayOfMonth,
    initializeAdminFlightHoliday,
    isoDate,
    monthLabel
} from '../../main/resources/static/js/admin-flight-holiday.js';

test('monthLabel은 0-indexed 월을 사람이 읽는 연/월로 표기한다', () => {
    assert.equal(monthLabel(2026, 8), '2026년 9월');
});

test('daysInMonth는 윤년 2월을 정확히 계산한다', () => {
    assert.equal(daysInMonth(2026, 1), 28);
    assert.equal(daysInMonth(2028, 1), 29);
});

test('isoDate는 월/일을 두 자리로 패딩한 yyyy-MM-dd를 만든다', () => {
    assert.equal(isoDate(2026, 8, 5), '2026-09-05');
    assert.equal(isoDate(2026, 11, 31), '2026-12-31');
});

test('buildCalendarCells는 1일 이전을 null로 채우고 나머지는 날짜/공휴일 정보를 담는다', () => {
    const leadingBlanks = firstWeekdayOfMonth(2026, 8);
    const holidayNameByDate = new Map([['2026-09-26', '대체공휴일']]);

    const cells = buildCalendarCells(2026, 8, holidayNameByDate);

    assert.equal(cells.length, leadingBlanks + daysInMonth(2026, 8));
    for (let i = 0; i < leadingBlanks; i++) {
        assert.equal(cells[i], null);
    }
    const firstDayCell = cells[leadingBlanks];
    assert.deepEqual(firstDayCell, { day: 1, date: '2026-09-01', holidayName: null });
    const holidayCell = cells[leadingBlanks + 25];
    assert.deepEqual(holidayCell, { day: 26, date: '2026-09-26', holidayName: '대체공휴일' });
});

function interactiveElement(initial = {}) {
    const classes = new Set();
    return {
        disabled: false,
        hidden: false,
        open: false,
        textContent: '',
        value: '',
        className: '',
        dataset: {},
        children: [],
        handlers: {},
        classList: {
            add: (name) => classes.add(name),
            remove: (name) => classes.delete(name),
            contains: (name) => classes.has(name)
        },
        addEventListener(event, handler) {
            this.handlers[event] = handler;
        },
        appendChild(child) {
            this.children.push(child);
            return child;
        },
        replaceChildren(...children) {
            this.children = children;
        },
        showModal() {
            this.open = true;
        },
        close() {
            this.open = false;
        },
        setAttribute(name, value) {
            this[name] = value;
        },
        ...initial
    };
}

function flightHolidayDocument() {
    const csrfInput = { value: 'csrf-token' };
    const csrfSource = interactiveElement({
        dataset: { csrfHeader: 'X-CSRF-TOKEN' },
        querySelector: () => csrfInput
    });
    const elements = new Map([
        ['flightHolidayCsrfSource', csrfSource],
        ['flightHolidayStatus', interactiveElement()],
        ['flightHolidayPrevMonth', interactiveElement()],
        ['flightHolidayNextMonth', interactiveElement()],
        ['flightHolidayMonthLabel', interactiveElement()],
        ['flightHolidayCalendarGrid', interactiveElement()],
        ['flightHolidayModal', interactiveElement()],
        ['flightHolidayModalClose', interactiveElement()],
        ['flightHolidayModalDate', interactiveElement()],
        ['flightHolidayModalAddView', interactiveElement()],
        ['flightHolidayModalRemoveView', interactiveElement()],
        ['flightHolidayNameInput', interactiveElement()],
        ['flightHolidayAddButton', interactiveElement()],
        ['flightHolidayExistingName', interactiveElement()],
        ['flightHolidayDeleteButton', interactiveElement()]
    ]);
    return {
        createElement: () => interactiveElement(),
        getElementById: (id) => elements.get(id),
        elements
    };
}

async function flushPromises() {
    await Promise.resolve();
    await Promise.resolve();
    await Promise.resolve();
}

function holidaysResponse(holidays) {
    return { ok: true, json: async () => ({ code: 'SUCCESS', data: holidays }) };
}

const TODAY = new Date(2026, 8, 15);

async function initAndLoad(document, fetchImpl, overrides = {}) {
    initializeAdminFlightHoliday(document, { fetchImpl, today: TODAY, ...overrides });
    await flushPromises();
}

function dayCell(document, day) {
    const leadingBlanks = firstWeekdayOfMonth(2026, 8);
    return document.elements.get('flightHolidayCalendarGrid').children[leadingBlanks + day - 1];
}

test('로드하면 이번 달 라벨과 공휴일이 표시된 달력을 그린다', async () => {
    const document = flightHolidayDocument();
    const fetchImpl = async () => holidaysResponse([{ id: 1, date: '2026-09-26', name: '대체공휴일' }]);

    await initAndLoad(document, fetchImpl);

    assert.equal(document.elements.get('flightHolidayMonthLabel').textContent, '2026년 9월');
    const holidayCell = dayCell(document, 26);
    assert.equal(holidayCell.classList.contains('has-holiday'), true);
    const normalCell = dayCell(document, 5);
    assert.equal(normalCell.classList.contains('has-holiday'), false);
});

test('공휴일이 없는 날짜를 클릭하면 추가 화면으로 모달이 열린다', async () => {
    const document = flightHolidayDocument();
    const fetchImpl = async () => holidaysResponse([]);

    await initAndLoad(document, fetchImpl);
    await dayCell(document, 5).handlers.click();

    assert.equal(document.elements.get('flightHolidayModal').open, true);
    assert.equal(document.elements.get('flightHolidayModalAddView').hidden, false);
    assert.equal(document.elements.get('flightHolidayModalRemoveView').hidden, true);
    assert.equal(document.elements.get('flightHolidayModalDate').textContent, '2026-09-05');
});

test('공휴일이 있는 날짜를 클릭하면 삭제 화면으로 모달이 열린다', async () => {
    const document = flightHolidayDocument();
    const fetchImpl = async () => holidaysResponse([{ id: 1, date: '2026-09-26', name: '대체공휴일' }]);

    await initAndLoad(document, fetchImpl);
    await dayCell(document, 26).handlers.click();

    assert.equal(document.elements.get('flightHolidayModalAddView').hidden, true);
    assert.equal(document.elements.get('flightHolidayModalRemoveView').hidden, false);
    assert.equal(document.elements.get('flightHolidayExistingName').textContent, '대체공휴일');
});

test('추가 버튼을 누르면 서버에 저장하고 목록을 새로고침한다', async () => {
    const document = flightHolidayDocument();
    let holidays = [];
    let posted;
    const fetchImpl = async (url, init) => {
        if (init?.method === 'POST') {
            posted = JSON.parse(init.body);
            holidays = [{ id: 2, ...posted }];
            return { ok: true, json: async () => ({ code: 'SUCCESS', data: holidays[0] }) };
        }
        return holidaysResponse(holidays);
    };
    const notices = [];

    await initAndLoad(document, fetchImpl, { onNotice: (message) => notices.push(message) });
    await dayCell(document, 5).handlers.click();
    document.elements.get('flightHolidayNameInput').value = '임시공휴일';
    await document.elements.get('flightHolidayAddButton').handlers.click();

    assert.deepEqual(posted, { date: '2026-09-05', name: '임시공휴일' });
    assert.equal(notices.at(-1), '공휴일을 추가했습니다.');
    assert.equal(dayCell(document, 5).classList.contains('has-holiday'), true);
});

test('삭제 버튼을 누르면 확인 후 서버에서 삭제하고 목록을 새로고침한다', async () => {
    const document = flightHolidayDocument();
    let holidays = [{ id: 1, date: '2026-09-26', name: '대체공휴일' }];
    let deletedId;
    const fetchImpl = async (url, init) => {
        if (init?.method === 'DELETE') {
            deletedId = url.split('/').pop();
            holidays = [];
            return { status: 204, ok: true, json: async () => null };
        }
        return holidaysResponse(holidays);
    };
    const notices = [];

    await initAndLoad(document, fetchImpl, { onNotice: (message) => notices.push(message), confirmImpl: () => true });
    await dayCell(document, 26).handlers.click();
    await document.elements.get('flightHolidayDeleteButton').handlers.click();

    assert.equal(deletedId, '1');
    assert.equal(notices.at(-1), '공휴일을 삭제했습니다.');
    assert.equal(dayCell(document, 26).classList.contains('has-holiday'), false);
});

test('삭제 확인을 취소하면 서버를 호출하지 않는다', async () => {
    const document = flightHolidayDocument();
    let deleteCalled = false;
    const fetchImpl = async (url, init) => {
        if (init?.method === 'DELETE') {
            deleteCalled = true;
        }
        return holidaysResponse([{ id: 1, date: '2026-09-26', name: '대체공휴일' }]);
    };

    await initAndLoad(document, fetchImpl, { confirmImpl: () => false });
    await dayCell(document, 26).handlers.click();
    await document.elements.get('flightHolidayDeleteButton').handlers.click();

    assert.equal(deleteCalled, false);
});

test('다음 달/이전 달 버튼을 누르면 라벨이 바뀐다', async () => {
    const document = flightHolidayDocument();
    const fetchImpl = async () => holidaysResponse([]);

    await initAndLoad(document, fetchImpl);
    await document.elements.get('flightHolidayNextMonth').handlers.click();
    assert.equal(document.elements.get('flightHolidayMonthLabel').textContent, '2026년 10월');

    await document.elements.get('flightHolidayPrevMonth').handlers.click();
    await document.elements.get('flightHolidayPrevMonth').handlers.click();
    assert.equal(document.elements.get('flightHolidayMonthLabel').textContent, '2026년 8월');
});

test('12월에서 다음 달로 넘어가면 연도가 바뀐다', async () => {
    const document = flightHolidayDocument();
    const fetchImpl = async () => holidaysResponse([]);

    await initAndLoad(document, fetchImpl, { today: new Date(2026, 11, 15) });
    await document.elements.get('flightHolidayNextMonth').handlers.click();

    assert.equal(document.elements.get('flightHolidayMonthLabel').textContent, '2027년 1월');
});
