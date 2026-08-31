/* ===================================================
   finder-app.js — 이름·거리로 찾기 / 목적지로 추천받기 진입점
   =================================================== */

import { closeDialogById, openDialogById } from './utils.js';
import { requestCurrentPosition } from './finder-geolocation.js';
import { formatDistance, sortByDistance } from './finder-distance.js';
import { DESTINATION_CHIPS, resolveDestinationQuery } from './finder-destination-chips.js';
import { CONDITION_FILTERS, badgesFor, filterItems } from './finder-condition.js';
import { createFinderRestStopListRequest } from './finder-rest-stop-list-request.js';
import { createRestStopNameSearchRequest } from './rest-stop-name-search-request.js';
import { createRouteRestStopRequest } from './route-rest-stop-request.js';

function showScreen(screenName) {
    document.querySelectorAll('[data-finder-screen]').forEach((section) => {
        section.hidden = section.dataset.finderScreen !== screenName;
    });
}

function setLoading(isLoading) {
    const overlay = document.getElementById('finderLoadingOverlay');
    if (overlay) {
        overlay.hidden = !isLoading;
    }
}

function setStatus(element, message) {
    if (!element) {
        return;
    }
    if (!message) {
        element.hidden = true;
        element.textContent = '';
        return;
    }
    element.hidden = false;
    element.textContent = message;
}

function renderBadges(container, item) {
    badgesFor(item).forEach((badge) => {
        const span = document.createElement('span');
        span.className = 'finder-badge';
        if (badge.key === 'FUEL_CHEAPEST') {
            span.classList.add('finder-badge-accent');
        }
        span.textContent = badge.label;
        container.appendChild(span);
    });
}

function renderResultCard({ name, routeLabel, distanceLabel, badgeItem }) {
    const li = document.createElement('li');
    li.className = 'finder-result-card';

    const main = document.createElement('div');
    main.className = 'finder-result-main';

    const nameEl = document.createElement('p');
    nameEl.className = 'finder-result-name';
    nameEl.textContent = name;
    main.appendChild(nameEl);

    if (routeLabel) {
        const routeEl = document.createElement('p');
        routeEl.className = 'finder-result-route';
        routeEl.textContent = routeLabel;
        main.appendChild(routeEl);
    }

    const badgeRow = document.createElement('div');
    badgeRow.className = 'finder-result-badges';
    renderBadges(badgeRow, badgeItem);
    if (badgeRow.childElementCount > 0) {
        main.appendChild(badgeRow);
    }

    li.appendChild(main);

    if (distanceLabel) {
        const distanceEl = document.createElement('span');
        distanceEl.className = 'finder-result-distance';
        distanceEl.textContent = distanceLabel;
        li.appendChild(distanceEl);
    }

    return li;
}

document.addEventListener('DOMContentLoaded', () => {
    /* ---------- 첫 화면 ---------- */

    document.getElementById('finderEnterMode1')?.addEventListener('click', () => {
        openDialogById('finderPermissionMode1');
    });
    document.getElementById('finderEnterMode2')?.addEventListener('click', () => {
        openDialogById('finderPermissionMode2');
    });

    /* ---------- 위치 동의 팝업: 이름·거리로 찾기 ---------- */

    const mode1ErrorEl = document.getElementById('finderPermissionMode1Error');
    let mode1Origin = null;

    document.getElementById('finderPermissionMode1Close')?.addEventListener('click', () => {
        closeDialogById('finderPermissionMode1');
    });
    document.getElementById('finderPermissionMode1Skip')?.addEventListener('click', () => {
        mode1Origin = null;
        closeDialogById('finderPermissionMode1');
        enterMode1();
    });
    document.getElementById('finderPermissionMode1Allow')?.addEventListener('click', async () => {
        setStatus(mode1ErrorEl, '');
        setLoading(true);
        const result = await requestCurrentPosition();
        setLoading(false);

        if (!result.granted) {
            mode1Origin = null;
            closeDialogById('finderPermissionMode1');
            enterMode1();
            return;
        }

        mode1Origin = { latitude: result.latitude, longitude: result.longitude };
        closeDialogById('finderPermissionMode1');
        enterMode1();
    });

    /* ---------- 위치 동의 팝업: 목적지로 추천받기 ---------- */

    const mode2ErrorEl = document.getElementById('finderPermissionMode2Error');
    let mode2Origin = null;

    document.getElementById('finderPermissionMode2Close')?.addEventListener('click', () => {
        closeDialogById('finderPermissionMode2');
    });
    document.getElementById('finderPermissionMode2Allow')?.addEventListener('click', async () => {
        setStatus(mode2ErrorEl, '');
        setLoading(true);
        const result = await requestCurrentPosition();
        setLoading(false);

        if (!result.granted) {
            setStatus(mode2ErrorEl, '위치 확인에 실패했어요. 다시 시도해주세요.');
            return;
        }

        mode2Origin = { latitude: result.latitude, longitude: result.longitude };
        closeDialogById('finderPermissionMode2');
        enterMode2Destination();
    });

    /* ---------- 1. 이름·거리로 찾기 ---------- */

    const mode1SubHeadingEl = document.getElementById('finderMode1SubHeading');
    const mode1SearchInputEl = document.getElementById('finderMode1SearchInput');
    const mode1StatusEl = document.getElementById('finderMode1Status');
    const mode1ListEl = document.getElementById('finderMode1List');
    let mode1AllRestStops = [];

    const restStopListRequest = createFinderRestStopListRequest({
        onState: (state) => {
            if (state.status === 'loading') {
                setStatus(mode1StatusEl, '불러오는 중...');
                return;
            }
            if (state.status === 'error') {
                setStatus(mode1StatusEl, '휴게소 목록을 불러오지 못했어요.');
                return;
            }
            mode1AllRestStops = state.restStops;
            renderMode1List(mode1AllRestStops.length === 0 ? [] : sortByDistance(mode1AllRestStops, mode1Origin));
        }
    });

    const nameSearchRequest = createRestStopNameSearchRequest({
        onState: (state) => {
            if (state.status === 'loading') {
                setStatus(mode1StatusEl, '검색 중...');
                return;
            }
            if (state.status === 'error') {
                setStatus(mode1StatusEl, '검색에 실패했어요.');
                return;
            }
            const restStops = mode1Origin ? sortByDistance(state.restStops, mode1Origin) : state.restStops;
            renderMode1List(restStops);
        }
    });

    function renderMode1List(restStops) {
        mode1ListEl.innerHTML = '';
        if (restStops.length === 0) {
            setStatus(mode1StatusEl, '검색 결과가 없어요.');
            return;
        }
        setStatus(mode1StatusEl, '');
        restStops.forEach((restStop) => {
            mode1ListEl.appendChild(
                renderResultCard({
                    name: restStop.unitName,
                    routeLabel: restStop.routeName,
                    distanceLabel: Number.isFinite(restStop.distanceMeters)
                        ? formatDistance(restStop.distanceMeters)
                        : '',
                    badgeItem: restStop
                })
            );
        });
    }

    function enterMode1() {
        showScreen('mode1');
        mode1SearchInputEl.value = '';
        if (mode1Origin) {
            mode1SubHeadingEl.textContent = '내 위치 기준 · 가까운 순';
            restStopListRequest.load();
        } else {
            mode1SubHeadingEl.textContent = '이름으로 검색해보세요';
            mode1ListEl.innerHTML = '';
            setStatus(mode1StatusEl, '휴게소 이름을 입력해주세요.');
        }
    }

    let mode1SearchDebounceTimer;
    mode1SearchInputEl?.addEventListener('input', () => {
        clearTimeout(mode1SearchDebounceTimer);
        const query = mode1SearchInputEl.value.trim();

        if (query === '') {
            if (mode1Origin) {
                renderMode1List(sortByDistance(mode1AllRestStops, mode1Origin));
            } else {
                mode1ListEl.innerHTML = '';
                setStatus(mode1StatusEl, '휴게소 이름을 입력해주세요.');
            }
            return;
        }

        mode1SearchDebounceTimer = setTimeout(() => nameSearchRequest.load(query), 250);
    });

    document.getElementById('finderMode1Back')?.addEventListener('click', () => {
        showScreen('landing');
    });

    /* ---------- 2. 목적지로 추천받기 — 목적지 입력 ---------- */

    const destinationInputEl = document.getElementById('finderMode2DestinationInput');
    const destinationChipsEl = document.getElementById('finderDestinationChips');

    DESTINATION_CHIPS.forEach((chip) => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'finder-chip';
        button.textContent = chip.label;
        button.addEventListener('click', () => loadMode2Results(chip.destinationQuery, chip.label));
        destinationChipsEl?.appendChild(button);
    });

    function enterMode2Destination() {
        showScreen('mode2-destination');
        destinationInputEl.value = '';
    }

    document.getElementById('finderMode2DestinationSubmit')?.addEventListener('click', () => {
        const query = destinationInputEl.value.trim();
        if (query === '') {
            return;
        }
        loadMode2Results(resolveDestinationQuery(query), query);
    });

    document.getElementById('finderMode2DestinationBack')?.addEventListener('click', () => {
        showScreen('landing');
    });

    /* ---------- 2. 목적지로 추천받기 — 조건 필터 + 추천 리스트 ---------- */

    const mode2ResultsTitleEl = document.getElementById('finderMode2ResultsTitle');
    const mode2FiltersEl = document.getElementById('finderConditionFilters');
    const mode2StatusEl = document.getElementById('finderMode2Status');
    const mode2ListEl = document.getElementById('finderMode2List');
    const selectedFilterKeys = new Set();
    let mode2RestStopItems = [];

    CONDITION_FILTERS.forEach((filter) => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'finder-chip';
        button.setAttribute('aria-pressed', 'false');
        button.textContent = filter.label;
        button.addEventListener('click', () => {
            const isSelected = selectedFilterKeys.has(filter.key);
            if (isSelected) {
                selectedFilterKeys.delete(filter.key);
            } else {
                selectedFilterKeys.add(filter.key);
            }
            button.setAttribute('aria-pressed', String(!isSelected));
            renderMode2List();
        });
        mode2FiltersEl?.appendChild(button);
    });

    function renderMode2List() {
        const filtered = filterItems(mode2RestStopItems, [...selectedFilterKeys]);
        mode2ListEl.innerHTML = '';
        if (filtered.length === 0) {
            setStatus(mode2StatusEl, '조건에 맞는 휴게소가 없어요.');
            return;
        }
        setStatus(mode2StatusEl, '');
        filtered.forEach((item) => {
            mode2ListEl.appendChild(
                renderResultCard({
                    name: item.unitName,
                    routeLabel: item.routeName,
                    distanceLabel: `${formatDistance(item.distanceFromRouteMeters)} 앞`,
                    badgeItem: item
                })
            );
        });
    }

    const routeRestStopRequest = createRouteRestStopRequest({
        onState: (state) => {
            if (state.status === 'loading') {
                setLoading(true);
                setStatus(mode2StatusEl, '');
                return;
            }
            setLoading(false);

            if (state.status === 'not-found') {
                mode2RestStopItems = [];
                selectedFilterKeys.clear();
                mode2FiltersEl?.querySelectorAll('.finder-chip').forEach((button) => button.setAttribute('aria-pressed', 'false'));
                setStatus(mode2StatusEl, state.message || '경로를 찾을 수 없어요.');
                mode2ListEl.innerHTML = '';
                return;
            }
            if (state.status !== 'success') {
                mode2RestStopItems = [];
                setStatus(mode2StatusEl, '추천 결과를 불러오지 못했어요.');
                mode2ListEl.innerHTML = '';
                return;
            }

            mode2RestStopItems = state.data.routes?.[0]?.restStops ?? [];
            selectedFilterKeys.clear();
            mode2FiltersEl?.querySelectorAll('.finder-chip').forEach((button) => button.setAttribute('aria-pressed', 'false'));
            renderMode2List();
        }
    });

    function loadMode2Results(destinationQuery, displayLabel) {
        if (!mode2Origin) {
            showScreen('landing');
            return;
        }
        mode2ResultsTitleEl.textContent = `${displayLabel} 방향 추천 휴게소`;
        showScreen('mode2-results');
        routeRestStopRequest.load(mode2Origin.latitude, mode2Origin.longitude, destinationQuery);
    }

    document.getElementById('finderMode2ResultsBack')?.addEventListener('click', () => {
        showScreen('mode2-destination');
    });

    showScreen('landing');
});
