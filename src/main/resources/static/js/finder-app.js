/* ===================================================
   finder-app.js — 이름·거리로 찾기 / 목적지로 추천받기 진입점
   =================================================== */

import { closeDialogById, openDialogById } from './utils.js';
import { initThemeToggle } from './theme.js';
import { requestCurrentPosition } from './finder-geolocation.js';
import { formatDistance } from './finder-distance.js';
import { DESTINATION_CHIPS } from './finder-destination-chips.js';
import {
    INTEREST_OPTIONS,
    mode2BadgesFor,
    mode2ConditionFilters,
    mode2FilterItems,
    nearbyBadgesFor
} from './finder-condition.js';
import { createFinderRestStopNearbyRequest } from './finder-rest-stop-nearby-request.js';
import { createRouteRestStopListRequest } from './finder-route-rest-stop-list-request.js';
import { createPlaceSearchRequest } from './place-search-request.js';
import {
    getRememberedInterest,
    getRememberedLocationAnswer,
    rememberInterest,
    rememberLocationAnswer,
    resetFinderMemory
} from './finder-session-memory.js';

const FUEL_INTERESTS = new Set(['GASOLINE', 'DIESEL', 'LPG']);

function fuelTypeParam(interest) {
    return FUEL_INTERESTS.has(interest) ? interest : undefined;
}

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

// "이름·거리로 찾기"(nearby) 카드용 색상. mode2와 판정 함수·색상 맵이 완전히 분리돼 있다.
const MODE1_BADGE_COLOR_CLASS_BY_KEY = {
    SIZE_LARGE: 'finder-badge-size',
    TOP_TRAFFIC: 'finder-badge-warn',
    HAS_THEME: 'finder-badge-accent',
    HAS_EVENT: 'finder-badge-event',
    EV_COUNT: 'finder-badge-ev',
    FUEL_BELOW_AVERAGE: 'finder-badge-savings'
};

// v2부터 이용량 상위 10%는 mode1과 같은 색(warn)으로 통일했다. 제일 저렴/평균보다 저렴도 결국 같은
// "저렴" 태그라 색을 통일했다(savings) — CHEAPEST/BELOW_AVERAGE는 문구만 다르고 색은 같다.
const MODE2_BADGE_COLOR_CLASS_BY_KEY = {
    SIZE_LARGE: 'finder-badge-size',
    TOP_TRAFFIC: 'finder-badge-warn',
    FUEL_CHEAPEST: 'finder-badge-savings',
    FUEL_BELOW_AVERAGE: 'finder-badge-savings',
    EV_CHARGER: 'finder-badge-ev'
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

    const mode1ErrorEl = document.getElementById('finderPermissionMode1Error');
    const mode2ErrorEl = document.getElementById('finderPermissionMode2Error');
    let mode1Origin = null;
    let mode1Interest = null;
    let mode2Origin = null;
    let mode2Interest = null;
    // 연료 선택 팝업은 mode1/mode2가 공유하므로, 팝업을 열기 전에 어느 화면으로 이어질지 표시해둔다.
    let interestPopupTargetMode = 'mode1';

    document.getElementById('finderEnterMode1')?.addEventListener('click', () => startMode1Entry());
    document.getElementById('finderEnterMode2')?.addEventListener('click', () => startMode2Entry());

    /**
     * 이번 탭 세션에서 이미 위치를 답한 적 있으면(허용/건너뛰기 모두) 팝업을 다시 띄우지 않고 바로
     * 다음 단계로 넘어간다 — 단, 좌표는 여기서 캐시해두지 않고 "허용"이었을 때만 매번 새로 받아온다
     * (위치가 바뀌었을 수 있어서다). 처음 답하는 탭이면 기존과 동일하게 팝업을 띄운다.
     */
    async function startMode1Entry() {
        const remembered = getRememberedLocationAnswer('mode1');
        if (remembered === null) {
            openDialogById('finderPermissionMode1');
            return;
        }
        if (remembered === 'skipped') {
            mode1Origin = null;
            proceedFromMode1Location();
            return;
        }
        setLoading(true);
        const result = await requestCurrentPosition();
        setLoading(false);
        mode1Origin = result.granted ? { latitude: result.latitude, longitude: result.longitude } : null;
        proceedFromMode1Location();
    }

    async function startMode2Entry() {
        if (getRememberedLocationAnswer('mode2') !== 'granted') {
            openDialogById('finderPermissionMode2');
            return;
        }
        setLoading(true);
        const result = await requestCurrentPosition();
        setLoading(false);
        if (!result.granted) {
            // 이전엔 허용했지만 이번엔 실패(권한 해제 등) — 재시도할 수 있게 팝업으로 안내한다.
            openDialogById('finderPermissionMode2');
            return;
        }
        mode2Origin = { latitude: result.latitude, longitude: result.longitude };
        proceedFromMode2Location();
    }

    /* ---------- 위치 동의 팝업: 이름·거리로 찾기 ---------- */

    document.getElementById('finderPermissionMode1Close')?.addEventListener('click', () => {
        closeDialogById('finderPermissionMode1');
    });
    document.getElementById('finderPermissionMode1Skip')?.addEventListener('click', () => {
        mode1Origin = null;
        rememberLocationAnswer('mode1', 'skipped');
        closeDialogById('finderPermissionMode1');
        proceedFromMode1Location();
    });
    document.getElementById('finderPermissionMode1Allow')?.addEventListener('click', async () => {
        setStatus(mode1ErrorEl, '');
        setLoading(true);
        const result = await requestCurrentPosition();
        setLoading(false);

        mode1Origin = result.granted ? { latitude: result.latitude, longitude: result.longitude } : null;
        rememberLocationAnswer('mode1', result.granted ? 'granted' : 'skipped');
        closeDialogById('finderPermissionMode1');
        proceedFromMode1Location();
    });

    function proceedFromMode1Location() {
        interestPopupTargetMode = 'mode1';
        openInterestPopupOrSkip();
    }

    /* ---------- 연료 선택 팝업 — mode1/mode2가 공유한다. 위치 동의 팝업 다음에 항상 뜬다 ---------- */

    /** 이번 탭 세션에서 이미 연료/EV 관심을 답한 적 있으면 팝업 없이 그 값을 바로 쓴다. */
    function openInterestPopupOrSkip() {
        const remembered = getRememberedInterest();
        if (remembered === undefined) {
            openDialogById('finderInterestPopup');
            return;
        }
        applyInterest(remembered);
    }

    function applyInterest(interest) {
        if (interestPopupTargetMode === 'mode2') {
            mode2Interest = interest;
            enterMode2();
        } else {
            mode1Interest = interest;
            enterMode1();
        }
    }

    const interestChipsEl = document.getElementById('finderInterestChips');
    INTEREST_OPTIONS.forEach((option) => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'finder-chip';
        button.textContent = option.label;
        button.addEventListener('click', () => {
            closeDialogById('finderInterestPopup');
            rememberInterest(option.key);
            applyInterest(option.key);
        });
        interestChipsEl?.appendChild(button);
    });
    document.getElementById('finderInterestSkip')?.addEventListener('click', () => {
        closeDialogById('finderInterestPopup');
        rememberInterest(null);
        applyInterest(null);
    });
    document.getElementById('finderInterestClose')?.addEventListener('click', () => {
        closeDialogById('finderInterestPopup');
        resetFinderMemory();
        showScreen('landing');
    });

    /* ---------- 위치 동의 팝업: 목적지로 추천받기 ---------- */

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
        rememberLocationAnswer('mode2', 'granted');
        closeDialogById('finderPermissionMode2');
        proceedFromMode2Location();
    });

    function proceedFromMode2Location() {
        interestPopupTargetMode = 'mode2';
        openInterestPopupOrSkip();
    }

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
        resetFinderMemory();
        showScreen('landing');
    });

    /* ---------- 2. 목적지로 추천받기 ---------- */

    const destinationInputEl = document.getElementById('finderMode2DestinationInput');
    const destinationChipsEl = document.getElementById('finderDestinationChips');
    const mode2FilterSectionEl = document.getElementById('finderMode2FilterSection');
    const mode2ResultsSectionEl = document.getElementById('finderMode2ResultsSection');
    const mode2ResultsHeadingEl = document.getElementById('finderMode2ResultsHeading');
    const mode2FiltersEl = document.getElementById('finderConditionFilters');
    const mode2StatusEl = document.getElementById('finderMode2Status');
    const mode2ListEl = document.getElementById('finderMode2List');
    const selectedFilterKeys = new Set();
    let mode2RestStopItems = [];

    // 조건 필터(sticky 영역)와 결과 목록(스크롤 영역)은 서로 다른 컨테이너지만 항상 같이 나타나고
    // 같이 숨겨진다 — 검색 전에는 둘 다 숨김, 결과가 오면(성공/실패 모두) 둘 다 보여준다.
    function setMode2ResultsVisible(visible) {
        mode2FilterSectionEl.hidden = !visible;
        mode2ResultsSectionEl.hidden = !visible;
    }

    DESTINATION_CHIPS.forEach((chip) => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'finder-chip';
        button.textContent = chip.label;
        button.addEventListener('click', () =>
            loadMode2Results({ destinationQuery: chip.destinationQuery, displayLabel: chip.label })
        );
        destinationChipsEl?.appendChild(button);
    });

    function renderMode2Filters() {
        if (!mode2FiltersEl) {
            return;
        }
        mode2FiltersEl.innerHTML = '';
        mode2ConditionFilters(mode2Interest).forEach((filter) => {
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
            mode2FiltersEl.appendChild(button);
        });
    }

    function enterMode2() {
        showScreen('mode2');
        destinationInputEl.value = '';
        setMode2ResultsVisible(false);
        mode2ListEl.innerHTML = '';
        selectedFilterKeys.clear();
        renderMode2Filters();
    }

    function renderMode2List() {
        const filtered = mode2FilterItems(mode2RestStopItems, [...selectedFilterKeys]);
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
                    distanceLabel: Number.isFinite(item.distanceMeters) ? formatDistance(item.distanceMeters) : '',
                    badges: mode2BadgesFor(item, mode2Interest),
                    colorClassByKey: MODE2_BADGE_COLOR_CLASS_BY_KEY
                })
            );
        });
    }

    const routeRestStopListRequest = createRouteRestStopListRequest({
        onState: (state) => {
            if (state.status === 'loading') {
                setLoading(true);
                setStatus(mode2StatusEl, '');
                return;
            }
            setLoading(false);
            setMode2ResultsVisible(true);
            selectedFilterKeys.clear();
            renderMode2Filters();

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

            mode2RestStopItems = state.restStops;
            renderMode2List();
        }
    });

    function loadMode2Results({ destinationQuery, destinationLat, destinationLng, destinationName, displayLabel }) {
        if (!mode2Origin) {
            showScreen('landing');
            return;
        }
        mode2ResultsHeadingEl.textContent = `${displayLabel} 방향 · 앞으로 가는 길`;
        routeRestStopListRequest.load({
            originLat: mode2Origin.latitude,
            originLng: mode2Origin.longitude,
            destinationQuery,
            destinationLat,
            destinationLng,
            destinationName,
            fuelType: fuelTypeParam(mode2Interest)
        });
    }

    /* ---------- 목적지 후보 팝업(v2 신규) — 직접 입력한 목적지만 거친다 ---------- */

    const candidateStatusEl = document.getElementById('finderDestinationCandidateStatus');
    const candidateListEl = document.getElementById('finderDestinationCandidateList');

    const placeSearchRequest = createPlaceSearchRequest({
        onState: (state) => {
            if (state.status === 'loading') {
                candidateListEl.innerHTML = '';
                setStatus(candidateStatusEl, '검색 중...');
                return;
            }
            if (state.status === 'external-unavailable') {
                candidateListEl.innerHTML = '';
                setStatus(candidateStatusEl, '장소 검색을 잠시 이용할 수 없어요.');
                return;
            }
            if (state.status !== 'success') {
                candidateListEl.innerHTML = '';
                setStatus(candidateStatusEl, '검색 결과를 불러오지 못했어요.');
                return;
            }
            renderCandidates(state.candidates);
        }
    });

    function renderCandidates(candidates) {
        candidateListEl.innerHTML = '';
        if (candidates.length === 0) {
            setStatus(candidateStatusEl, '검색 결과가 없어요.');
            return;
        }
        setStatus(candidateStatusEl, '');
        candidates.forEach((candidate) => {
            const li = document.createElement('li');
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'finder-candidate-item';

            const name = document.createElement('p');
            name.className = 'finder-candidate-item-name';
            name.textContent = candidate.name || '이름 정보 없음';
            button.appendChild(name);

            if (candidate.address) {
                const address = document.createElement('p');
                address.className = 'finder-candidate-item-address';
                address.textContent = candidate.address;
                button.appendChild(address);
            }

            button.addEventListener('click', () => {
                closeDialogById('finderDestinationCandidatePopup');
                loadMode2Results({
                    destinationLat: candidate.latitude,
                    destinationLng: candidate.longitude,
                    destinationName: candidate.name,
                    displayLabel: candidate.name || '목적지'
                });
            });

            li.appendChild(button);
            candidateListEl.appendChild(li);
        });
    }

    document.getElementById('finderMode2DestinationSubmit')?.addEventListener('click', () => {
        const query = destinationInputEl.value.trim();
        if (query === '') {
            return;
        }
        openDialogById('finderDestinationCandidatePopup');
        placeSearchRequest.load(query);
    });

    document.getElementById('finderDestinationCandidateClose')?.addEventListener('click', () => {
        closeDialogById('finderDestinationCandidatePopup');
    });

    document.getElementById('finderMode2Back')?.addEventListener('click', () => {
        resetFinderMemory();
        showScreen('landing');
    });

    showScreen('landing');
});
