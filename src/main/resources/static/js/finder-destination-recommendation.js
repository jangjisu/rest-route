/**
 * "목적지로 추천받기" 화면 전용 — 목적지 검색(인기 칩/직접 입력+후보 팝업), 조건 필터, 목록
 * 요청·렌더링, 뒤로가기. 위치·연료 관심을 어떻게 얻는지는 전혀 모르고, 진입 흐름
 * (finder-entry-flow.js)이 다 정한 뒤 {@link initializeDestinationRecommendation}이 반환하는
 * `enterDestinationRecommendation(origin, interest)`을 호출해줄 때 받기만 한다.
 */

import { closeDialogById, openDialogById } from './utils.js';
import { formatDistance } from './finder-distance.js';
import { DESTINATION_CHIPS } from './finder-destination-chips.js';
import { destinationBadgesFor, destinationConditionFilters, destinationFilterItems } from './finder-condition.js';
import { createRouteRestStopListRequest } from './finder-route-rest-stop-list-request.js';
import { createPlaceSearchRequest } from './place-search-request.js';
import { renderResultCard, setLoading, setStatus, showScreen } from './finder-render.js';

const FUEL_INTERESTS = new Set(['GASOLINE', 'DIESEL', 'LPG']);

function fuelTypeParam(interest) {
    return FUEL_INTERESTS.has(interest) ? interest : undefined;
}

// v2부터 이용량 상위 10%는 이름·거리로 찾기와 같은 색(warn)으로 통일했다. 제일 저렴/평균보다 저렴도
// 결국 같은 "저렴" 태그라 색을 통일했다(savings) — CHEAPEST/BELOW_AVERAGE는 문구만 다르고 색은 같다.
const DESTINATION_BADGE_COLOR_CLASS_BY_KEY = {
    SIZE_LARGE: 'finder-badge-size',
    TOP_TRAFFIC: 'finder-badge-warn',
    FUEL_CHEAPEST: 'finder-badge-savings',
    FUEL_BELOW_AVERAGE: 'finder-badge-savings',
    EV_CHARGER: 'finder-badge-ev'
};

export function initializeDestinationRecommendation(document, { openDetail }) {
    const destinationInputEl = document.getElementById('finderMode2DestinationInput');
    const destinationChipsEl = document.getElementById('finderDestinationChips');
    const filterSectionEl = document.getElementById('finderMode2FilterSection');
    const resultsSectionEl = document.getElementById('finderMode2ResultsSection');
    const resultsHeadingEl = document.getElementById('finderMode2ResultsHeading');
    const filtersEl = document.getElementById('finderConditionFilters');
    const statusEl = document.getElementById('finderMode2Status');
    const listEl = document.getElementById('finderMode2List');
    const candidateStatusEl = document.getElementById('finderDestinationCandidateStatus');
    const candidateListEl = document.getElementById('finderDestinationCandidateList');
    const selectedFilterKeys = new Set();

    let origin = null;
    let interest = null;
    let restStopItems = [];

    // 조건 필터(sticky 영역)와 결과 목록(스크롤 영역)은 서로 다른 컨테이너지만 항상 같이 나타나고
    // 같이 숨겨진다 — 검색 전에는 둘 다 숨김, 결과가 오면(성공/실패 모두) 둘 다 보여준다.
    function setResultsVisible(visible) {
        filterSectionEl.hidden = !visible;
        resultsSectionEl.hidden = !visible;
    }

    DESTINATION_CHIPS.forEach((chip) => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'finder-chip';
        button.textContent = chip.label;
        button.addEventListener('click', () =>
            loadResults({ destinationQuery: chip.destinationQuery, displayLabel: chip.label })
        );
        destinationChipsEl?.appendChild(button);
    });

    function renderFilters() {
        if (!filtersEl) {
            return;
        }
        filtersEl.innerHTML = '';
        destinationConditionFilters(interest).forEach((filter) => {
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
                renderList();
            });
            filtersEl.appendChild(button);
        });
    }

    function renderList() {
        const filtered = destinationFilterItems(restStopItems, [...selectedFilterKeys]);
        listEl.innerHTML = '';
        if (filtered.length === 0) {
            setStatus(statusEl, '조건에 맞는 휴게소가 없어요.');
            return;
        }
        setStatus(statusEl, '');
        filtered.forEach((item) => {
            listEl.appendChild(
                renderResultCard(document, {
                    name: item.unitName,
                    routeLabel: item.routeName,
                    distanceLabel: Number.isFinite(item.distanceMeters) ? formatDistance(item.distanceMeters) : '',
                    badges: destinationBadgesFor(item, interest),
                    colorClassByKey: DESTINATION_BADGE_COLOR_CLASS_BY_KEY,
                    onSelect: () => openDetail(item)
                })
            );
        });
    }

    const routeRestStopListRequest = createRouteRestStopListRequest({
        onState: (state) => {
            if (state.status === 'loading') {
                setLoading(document, true);
                setStatus(statusEl, '');
                return;
            }
            setLoading(document, false);
            setResultsVisible(true);
            selectedFilterKeys.clear();
            renderFilters();

            if (state.status === 'not-found') {
                restStopItems = [];
                setStatus(statusEl, state.message || '경로를 찾을 수 없어요.');
                listEl.innerHTML = '';
                return;
            }
            if (state.status !== 'success') {
                restStopItems = [];
                setStatus(statusEl, '추천 결과를 불러오지 못했어요.');
                listEl.innerHTML = '';
                return;
            }

            restStopItems = state.restStops;
            renderList();
        }
    });

    function loadResults({ destinationQuery, destinationLat, destinationLng, destinationName, displayLabel }) {
        if (!origin) {
            showScreen(document, 'landing');
            return;
        }
        resultsHeadingEl.textContent = `${displayLabel} 방향 · 앞으로 가는 길`;
        routeRestStopListRequest.load({
            originLat: origin.latitude,
            originLng: origin.longitude,
            destinationQuery,
            destinationLat,
            destinationLng,
            destinationName,
            fuelType: fuelTypeParam(interest)
        });
    }

    /* ---------- 목적지 후보 팝업 — 직접 입력한 목적지만 거친다 ---------- */

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
                loadResults({
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
        showScreen(document, 'landing');
    });

    function enterDestinationRecommendation(nextOrigin, nextInterest) {
        origin = nextOrigin;
        interest = nextInterest;

        showScreen(document, 'mode2');
        destinationInputEl.value = '';
        setResultsVisible(false);
        listEl.innerHTML = '';
        selectedFilterKeys.clear();
        renderFilters();
    }

    return { enterDestinationRecommendation };
}
