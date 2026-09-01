/* ===================================================
   finder-app.js — 이름·거리로 찾기 / 목적지로 추천받기 진입점
   =================================================== */

import { closeDialogById, openDialogById } from './utils.js';
import { initThemeToggle } from './theme.js';
import { requestCurrentPosition } from './finder-geolocation.js';
import { formatDistance, latLngCoordinateOf, sortByDistance } from './finder-distance.js';
import { DESTINATION_CHIPS, resolveDestinationQuery } from './finder-destination-chips.js';
import { CONDITION_FILTERS, INTEREST_OPTIONS, badgesFor, filterItems, nearbyBadgesFor } from './finder-condition.js';
import { createFinderRestStopNearbyRequest } from './finder-rest-stop-nearby-request.js';
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

// "이름·거리로 찾기"(nearby) 카드용 색상 — 상세 패널(index.html)과 같은 톤으로 맞춘 것들이 섞여
// 있어서, "목적지로 추천받기"용 맵과 색이 다른 배지가 있다(예: 이용량 상위 10%).
const MODE1_BADGE_COLOR_CLASS_BY_KEY = {
    SIZE_LARGE: 'finder-badge-size',
    TOP_TRAFFIC: 'finder-badge-warn',
    HAS_THEME: 'finder-badge-accent',
    HAS_EVENT: 'finder-badge-event',
    EV_COUNT: 'finder-badge-ev',
    FUEL_BELOW_AVERAGE: 'finder-badge-savings'
};

const MODE2_BADGE_COLOR_CLASS_BY_KEY = {
    SIZE_LARGE: 'finder-badge-size',
    TOP_TRAFFIC: 'finder-badge-traffic',
    FUEL_CHEAPEST: 'finder-badge-accent',
    FUEL_BELOW_AVERAGE: 'finder-badge-savings',
    EV_CHARGER: 'finder-badge-ev',
    HAS_FOOD: 'finder-badge-food'
};

function renderBadges(container, badges, colorClassByKey) {
    badges.forEach((badge) => {
        const span = document.createElement('span');
        span.className = 'finder-badge';
        const colorClass = colorClassByKey[badge.key];
        if (colorClass) {
            span.classList.add(colorClass);
        }
        span.textContent = badge.label;
        container.appendChild(span);
    });
}

function renderResultCard({ name, routeLabel, distanceLabel, badges, colorClassByKey }) {
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
    renderBadges(badgeRow, badges, colorClassByKey);
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
    initThemeToggle(document, window);

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
    let mode1Interest = null;

    document.getElementById('finderPermissionMode1Close')?.addEventListener('click', () => {
        closeDialogById('finderPermissionMode1');
    });
    document.getElementById('finderPermissionMode1Skip')?.addEventListener('click', () => {
        mode1Origin = null;
        closeDialogById('finderPermissionMode1');
        openDialogById('finderInterestPopup');
    });
    document.getElementById('finderPermissionMode1Allow')?.addEventListener('click', async () => {
        setStatus(mode1ErrorEl, '');
        setLoading(true);
        const result = await requestCurrentPosition();
        setLoading(false);

        mode1Origin = result.granted ? { latitude: result.latitude, longitude: result.longitude } : null;
        closeDialogById('finderPermissionMode1');
        openDialogById('finderInterestPopup');
    });

    /* ---------- 연료 선택 팝업(팝업 2, 신규) ---------- */

    const interestChipsEl = document.getElementById('finderInterestChips');
    INTEREST_OPTIONS.forEach((option) => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'finder-chip';
        button.textContent = option.label;
        button.addEventListener('click', () => {
            mode1Interest = option.key;
            closeDialogById('finderInterestPopup');
            enterMode1();
        });
        interestChipsEl?.appendChild(button);
    });
    document.getElementById('finderInterestSkip')?.addEventListener('click', () => {
        mode1Interest = null;
        closeDialogById('finderInterestPopup');
        enterMode1();
    });
    document.getElementById('finderInterestClose')?.addEventListener('click', () => {
        closeDialogById('finderInterestPopup');
        showScreen('landing');
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
        enterMode2();
    });

    /* ---------- 1. 이름·거리로 찾기 ---------- */

    const mode1EmptyStateEl = document.getElementById('finderMode1EmptyState');
    const mode1SubHeadingEl = document.getElementById('finderMode1SubHeading');
    const mode1SearchInputEl = document.getElementById('finderMode1SearchInput');
    const mode1StatusEl = document.getElementById('finderMode1Status');
    const mode1ListEl = document.getElementById('finderMode1List');

    const nearbyRequest = createFinderRestStopNearbyRequest({
        onState: (state) => {
            if (state.status === 'loading') {
                setStatus(mode1StatusEl, mode1SearchInputEl.value.trim() ? '검색 중...' : '불러오는 중...');
                return;
            }
            if (state.status === 'error') {
                setStatus(mode1StatusEl, '휴게소 목록을 불러오지 못했어요.');
                return;
            }
            renderMode1List(state.restStops);
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
                    badges: nearbyBadgesFor(restStop, mode1Interest),
                    colorClassByKey: MODE1_BADGE_COLOR_CLASS_BY_KEY
                })
            );
        });
    }

    function runMode1Query(name) {
        const trimmedName = (name ?? '').trim();
        if (!mode1Origin && trimmedName === '') {
            mode1ListEl.innerHTML = '';
            setStatus(mode1StatusEl, '');
            return;
        }
        nearbyRequest.load({
            originLat: mode1Origin?.latitude,
            originLng: mode1Origin?.longitude,
            name: trimmedName,
            interest: mode1Interest
        });
    }

    function enterMode1() {
        showScreen('mode1');
        mode1SearchInputEl.value = '';
        mode1ListEl.innerHTML = '';
        setStatus(mode1StatusEl, '');

        if (mode1Origin) {
            mode1EmptyStateEl.hidden = true;
            mode1SubHeadingEl.hidden = false;
            mode1SubHeadingEl.textContent = '내 위치 기준 · 가까운 순';
        } else {
            mode1EmptyStateEl.hidden = false;
            mode1SubHeadingEl.hidden = true;
        }
        runMode1Query('');
    }

    let mode1SearchDebounceTimer;
    mode1SearchInputEl?.addEventListener('input', () => {
        clearTimeout(mode1SearchDebounceTimer);
        const query = mode1SearchInputEl.value.trim();
        mode1SearchDebounceTimer = setTimeout(() => runMode1Query(query), 250);
    });

    document.getElementById('finderMode1Back')?.addEventListener('click', () => {
        showScreen('landing');
    });

    /* ---------- 2. 목적지로 추천받기 ---------- */

    const destinationInputEl = document.getElementById('finderMode2DestinationInput');
    const destinationChipsEl = document.getElementById('finderDestinationChips');
    const conditionSectionEl = document.getElementById('finderMode2ConditionSection');
    const mode2ResultsHeadingEl = document.getElementById('finderMode2ResultsHeading');
    const mode2FiltersEl = document.getElementById('finderConditionFilters');
    const mode2StatusEl = document.getElementById('finderMode2Status');
    const mode2ListEl = document.getElementById('finderMode2List');
    const selectedFilterKeys = new Set();
    let mode2RestStopItems = [];

    DESTINATION_CHIPS.forEach((chip) => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'finder-chip';
        button.textContent = chip.label;
        button.addEventListener('click', () => loadMode2Results(chip.destinationQuery, chip.label));
        destinationChipsEl?.appendChild(button);
    });

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

    function enterMode2() {
        showScreen('mode2');
        destinationInputEl.value = '';
        conditionSectionEl.hidden = true;
        mode2ListEl.innerHTML = '';
        selectedFilterKeys.clear();
        mode2FiltersEl?.querySelectorAll('.finder-chip').forEach((button) => button.setAttribute('aria-pressed', 'false'));
    }

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
                    // 내 위치 기준 직선 거리(sortByDistance가 채운 distanceMeters). 경로 매칭용
                    // 오차값인 distanceFromRouteMeters는 진행 거리가 아니라서 쓰지 않는다.
                    distanceLabel: Number.isFinite(item.distanceMeters) ? formatDistance(item.distanceMeters) : '',
                    badges: badgesFor(item),
                    colorClassByKey: MODE2_BADGE_COLOR_CLASS_BY_KEY
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
            conditionSectionEl.hidden = false;
            selectedFilterKeys.clear();
            mode2FiltersEl?.querySelectorAll('.finder-chip').forEach((button) => button.setAttribute('aria-pressed', 'false'));

            if (state.status === 'not-found') {
                mode2RestStopItems = [];
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

            const restStops = state.data.routes?.[0]?.restStops ?? [];
            // 기본 정렬은 지금 내 위치로부터 가까운 순 — 목적지로 추천받기는 위치 정보를 이미
            // 받아서 진행하므로 그 좌표를 그대로 재사용한다.
            mode2RestStopItems = sortByDistance(restStops, mode2Origin, latLngCoordinateOf);
            renderMode2List();
        }
    });

    function loadMode2Results(destinationQuery, displayLabel) {
        if (!mode2Origin) {
            showScreen('landing');
            return;
        }
        mode2ResultsHeadingEl.textContent = `${displayLabel} 방향 · 앞으로 가는 길`;
        routeRestStopRequest.load(mode2Origin.latitude, mode2Origin.longitude, destinationQuery);
    }

    document.getElementById('finderMode2DestinationSubmit')?.addEventListener('click', () => {
        const query = destinationInputEl.value.trim();
        if (query === '') {
            return;
        }
        loadMode2Results(resolveDestinationQuery(query), query);
    });

    document.getElementById('finderMode2Back')?.addEventListener('click', () => {
        showScreen('landing');
    });

    showScreen('landing');
});
