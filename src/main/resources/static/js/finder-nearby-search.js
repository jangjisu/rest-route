/**
 * "이름·거리로 찾기" 화면 전용 — 검색 입력, 목록 요청·렌더링, 뒤로가기. 위치·연료 관심을 어떻게
 * 얻는지는 전혀 모르고, 진입 흐름(finder-entry-flow.js)이 다 정한 뒤 {@link initializeNearbySearch}가
 * 반환하는 `enterNearbySearch(origin, interest)`를 호출해줄 때 받기만 한다.
 */

import { formatDistance } from './finder-distance.js';
import { nearbyBadgesFor } from './finder-condition.js';
import { createFinderRestStopNearbyRequest } from './finder-rest-stop-nearby-request.js';
import { renderResultCard, setStatus, showScreen } from './finder-render.js';

// 상세 패널(index.html)과 같은 톤으로 맞춘 색상들이 섞여 있어서, "목적지로 추천받기"용 맵과 색이
// 다른 배지가 있다(예: 이용량 상위 10%는 같지만, 그쪽엔 없는 볼거리/이벤트가 여기만 있음).
const NEARBY_BADGE_COLOR_CLASS_BY_KEY = {
    SIZE_LARGE: 'finder-badge-size',
    TOP_TRAFFIC: 'finder-badge-warn',
    HAS_THEME: 'finder-badge-accent',
    HAS_EVENT: 'finder-badge-event',
    EV_COUNT: 'finder-badge-ev',
    FUEL_BELOW_AVERAGE: 'finder-badge-savings'
};

export function initializeNearbySearch(document) {
    const emptyStateEl = document.getElementById('finderMode1EmptyState');
    const subHeadingEl = document.getElementById('finderMode1SubHeading');
    const searchInputEl = document.getElementById('finderMode1SearchInput');
    const statusEl = document.getElementById('finderMode1Status');
    const listEl = document.getElementById('finderMode1List');

    let origin = null;
    let interest = null;

    const nearbyRequest = createFinderRestStopNearbyRequest({
        onState: (state) => {
            if (state.status === 'loading') {
                setStatus(statusEl, searchInputEl.value.trim() ? '검색 중...' : '불러오는 중...');
                return;
            }
            if (state.status === 'error') {
                setStatus(statusEl, '휴게소 목록을 불러오지 못했어요.');
                return;
            }
            renderList(state.restStops);
        }
    });

    function renderList(restStops) {
        listEl.innerHTML = '';
        if (restStops.length === 0) {
            setStatus(statusEl, '검색 결과가 없어요.');
            return;
        }
        setStatus(statusEl, '');
        restStops.forEach((restStop) => {
            listEl.appendChild(
                renderResultCard(document, {
                    name: restStop.unitName,
                    routeLabel: restStop.routeName,
                    distanceLabel: Number.isFinite(restStop.distanceMeters)
                        ? formatDistance(restStop.distanceMeters)
                        : '',
                    badges: nearbyBadgesFor(restStop, interest),
                    colorClassByKey: NEARBY_BADGE_COLOR_CLASS_BY_KEY
                })
            );
        });
    }

    function runQuery(name) {
        const trimmedName = (name ?? '').trim();
        if (!origin && trimmedName === '') {
            listEl.innerHTML = '';
            setStatus(statusEl, '');
            return;
        }
        nearbyRequest.load({
            originLat: origin?.latitude,
            originLng: origin?.longitude,
            name: trimmedName,
            interest
        });
    }

    function enterNearbySearch(nextOrigin, nextInterest) {
        origin = nextOrigin;
        interest = nextInterest;

        showScreen(document, 'mode1');
        searchInputEl.value = '';
        listEl.innerHTML = '';
        setStatus(statusEl, '');

        if (origin) {
            emptyStateEl.hidden = true;
            subHeadingEl.hidden = false;
            subHeadingEl.textContent = '내 위치 기준 · 가까운 순';
        } else {
            emptyStateEl.hidden = false;
            subHeadingEl.hidden = true;
        }
        runQuery('');
    }

    let searchDebounceTimer;
    searchInputEl?.addEventListener('input', () => {
        clearTimeout(searchDebounceTimer);
        const query = searchInputEl.value.trim();
        searchDebounceTimer = setTimeout(() => runQuery(query), 250);
    });

    document.getElementById('finderMode1Back')?.addEventListener('click', () => {
        showScreen(document, 'landing');
    });

    return { enterNearbySearch };
}
