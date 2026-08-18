import { addFlightHoliday, deleteFlightHoliday, fetchFlightHolidays } from './admin-flight-holiday-request.js';

export function monthLabel(year, month) {
    return `${year}년 ${month + 1}월`;
}

export function daysInMonth(year, month) {
    return new Date(year, month + 1, 0).getDate();
}

export function firstWeekdayOfMonth(year, month) {
    return new Date(year, month, 1).getDay();
}

export function isoDate(year, month, day) {
    const mm = String(month + 1).padStart(2, '0');
    const dd = String(day).padStart(2, '0');
    return `${year}-${mm}-${dd}`;
}

/**
 * 달력 한 칸씩을 순서대로 나열한다. 1일 이전의 빈 칸은 null로 채운다 —
 * 렌더링 쪽에서 null이면 숨김/비활성 칸으로 그리면 된다.
 */
export function buildCalendarCells(year, month, holidayNameByDate) {
    const leadingBlanks = firstWeekdayOfMonth(year, month);
    const totalDays = daysInMonth(year, month);
    const cells = [];
    for (let i = 0; i < leadingBlanks; i++) {
        cells.push(null);
    }
    for (let day = 1; day <= totalDays; day++) {
        const date = isoDate(year, month, day);
        cells.push({ day, date, holidayName: holidayNameByDate.get(date) ?? null });
    }
    return cells;
}

function csrfFrom(source) {
    return {
        headerName: source.dataset.csrfHeader || 'X-CSRF-TOKEN',
        token: source.querySelector('input[name="_csrf"]')?.value || ''
    };
}

export function initializeAdminFlightHoliday(document, {
    fetchImpl = fetch,
    confirmImpl = globalThis.confirm,
    onNotice = () => {},
    today = new Date()
} = {}) {
    const csrfSource = document.getElementById('flightHolidayCsrfSource');
    const status = document.getElementById('flightHolidayStatus');
    const prevButton = document.getElementById('flightHolidayPrevMonth');
    const nextButton = document.getElementById('flightHolidayNextMonth');
    const monthLabelEl = document.getElementById('flightHolidayMonthLabel');
    const grid = document.getElementById('flightHolidayCalendarGrid');
    const modal = document.getElementById('flightHolidayModal');
    const modalClose = document.getElementById('flightHolidayModalClose');
    const modalDate = document.getElementById('flightHolidayModalDate');
    const addView = document.getElementById('flightHolidayModalAddView');
    const removeView = document.getElementById('flightHolidayModalRemoveView');
    const nameInput = document.getElementById('flightHolidayNameInput');
    const addButton = document.getElementById('flightHolidayAddButton');
    const existingName = document.getElementById('flightHolidayExistingName');
    const deleteButton = document.getElementById('flightHolidayDeleteButton');

    if (!csrfSource || !status || !prevButton || !nextButton || !monthLabelEl || !grid || !modal || !modalClose
        || !modalDate || !addView || !removeView || !nameInput || !addButton || !existingName || !deleteButton) {
        return;
    }

    let viewYear = today.getFullYear();
    let viewMonth = today.getMonth();
    let holidays = [];
    let selectedDate = null;

    function pageCsrf() {
        return csrfFrom(csrfSource);
    }

    function holidayNameByDate() {
        return new Map(holidays.map((holiday) => [holiday.date, holiday.name]));
    }

    function holidayByDate(date) {
        return holidays.find((holiday) => holiday.date === date) ?? null;
    }

    function createDayCell(cell) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'flight-holiday-day';
        if (!cell) {
            button.classList.add('is-outside-month');
            button.disabled = true;
            return button;
        }

        const number = document.createElement('span');
        number.className = 'flight-holiday-day-number';
        number.textContent = String(cell.day);
        button.appendChild(number);

        if (cell.holidayName) {
            button.classList.add('has-holiday');
            const name = document.createElement('span');
            name.className = 'flight-holiday-day-name';
            name.textContent = cell.holidayName;
            button.appendChild(name);
        }

        button.addEventListener('click', () => openModal(cell.date));
        return button;
    }

    function renderCalendar() {
        monthLabelEl.textContent = monthLabel(viewYear, viewMonth);
        const cells = buildCalendarCells(viewYear, viewMonth, holidayNameByDate());
        grid.replaceChildren(...cells.map((cell) => createDayCell(cell)));
    }

    async function loadHolidays() {
        status.textContent = '불러오는 중입니다.';
        const result = await fetchFlightHolidays(fetchImpl);
        if (result.status !== 'success') {
            status.textContent = '목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.';
            return;
        }
        holidays = result.holidays;
        status.textContent = '';
        renderCalendar();
    }

    function openModal(date) {
        selectedDate = date;
        modalDate.textContent = date;
        const existing = holidayByDate(date);
        if (existing) {
            addView.hidden = true;
            removeView.hidden = false;
            existingName.textContent = existing.name;
            deleteButton.dataset.holidayId = String(existing.id);
        } else {
            addView.hidden = false;
            removeView.hidden = true;
            nameInput.value = '';
        }
        modal.showModal();
    }

    function closeModal() {
        modal.close();
        selectedDate = null;
    }

    modalClose.addEventListener('click', closeModal);

    prevButton.addEventListener('click', () => {
        viewMonth -= 1;
        if (viewMonth < 0) {
            viewMonth = 11;
            viewYear -= 1;
        }
        renderCalendar();
    });

    nextButton.addEventListener('click', () => {
        viewMonth += 1;
        if (viewMonth > 11) {
            viewMonth = 0;
            viewYear += 1;
        }
        renderCalendar();
    });

    addButton.addEventListener('click', async () => {
        const name = nameInput.value.trim();
        if (!selectedDate || name === '') {
            return;
        }
        const result = await addFlightHoliday(selectedDate, name, pageCsrf(), fetchImpl);
        if (result.status !== 'success') {
            onNotice('공휴일 추가에 실패했습니다.', 'error');
            return;
        }
        onNotice('공휴일을 추가했습니다.');
        closeModal();
        await loadHolidays();
    });

    deleteButton.addEventListener('click', async () => {
        const holidayId = deleteButton.dataset.holidayId;
        if (!holidayId) {
            return;
        }
        if (!confirmImpl('이 공휴일을 삭제할까요?')) {
            return;
        }
        const result = await deleteFlightHoliday(holidayId, pageCsrf(), fetchImpl);
        if (result.status !== 'success') {
            onNotice('공휴일 삭제에 실패했습니다.', 'error');
            return;
        }
        onNotice('공휴일을 삭제했습니다.');
        closeModal();
        await loadHolidays();
    });

    loadHolidays();
}
