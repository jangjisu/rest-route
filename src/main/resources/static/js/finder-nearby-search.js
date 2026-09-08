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

// 실패 지점을 화면 문구만 보고도 구분할 수 있게, 사유별로 다른 안내를 보여준다 — 코드는
// finder-rest-stop-nearby-request.js가 fetch/파싱/HTTP 상태/API 코드/응답 모양 중 어디서
// 걸렸는지 reason으로 넘겨준다.
export function describeNearbyError(state) {
    if (state.reason === 'http') {
        return `서버에 문제가 생겼어요. (상태 코드 ${state.httpStatus})`;
    }
    if (state.reason === 'api') {
        return `요청을 처리하지 못했어요. (코드: ${state.apiCode ?? '알 수 없음'})`;
    }
    if (state.reason === 'invalid-response') {
        return '서버 응답을 처리하지 못했어요.';
    }
    if (state.reason === 'unexpected-shape') {
        return '예상과 다른 응답을 받았어요.';
    }
    if (state.reason === 'network') {
        return '네트워크 연결을 확인해주세요.';
    }
    return '휴게소 목록을 불러오지 못했어요.';
}

export function initializeNearbySearch(document, { openDetail }) {
    const emptyStateEl = document.getElementById('finderMode1EmptyState');
    const subHeadingEl = document.getElementById('finderMode1SubHeading');
    const searchInputEl = document.getElementById('finderMode1SearchInput');
    const statusEl = document.getElementById('finderMode1Status');
    const errorStateEl = document.getElementById('finderMode1ErrorState');
    const errorMessageEl = document.getElementById('finderMode1ErrorMessage');
    const retryButtonEl = document.getElementById('finderMode1Retry');
    const listEl = document.getElementById('finderMode1List');

    let origin = null;
    let interest = null;

    function hideError() {
        errorStateEl.hidden = true;
    }

    function showError(state) {
        errorMessageEl.textContent = describeNearbyError(state);
        errorStateEl.hidden = false;
    }

    const nearbyRequest = createFinderRestStopNearbyRequest({
        onState: (state) => {
            if (state.status === 'loading') {
                hideError();
                setStatus(statusEl, searchInputEl.value.trim() ? '검색 중...' : '불러오는 중...');
                return;
            }
            if (state.status === 'error') {
                listEl.innerHTML = '';
                setStatus(statusEl, '');
                showError(state);
                return;
            }
            hideError();
            renderList(state.restStops);
        }
    });

    retryButtonEl?.addEventListener('click', () => runQuery(searchInputEl.value));

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
                    colorClassByKey: NEARBY_BADGE_COLOR_CLASS_BY_KEY,
                    onSelect: () => openDetail(restStop)
                })
            );
        });
    }

    function runQuery(name) {
        const trimmedName = (name ?? '').trim();
        // "휴게소 이름으로 검색해보세요" 안내는 위치도 검색어도 없을 때만 보여준다 — 위치 없이
        // 들어와서 한 번 보여준 뒤에도 그대로 남아 있던 버그(검색어를 입력해도 안 사라짐)를 고쳤다.
        emptyStateEl.hidden = Boolean(origin) || trimmedName !== '';
        if (!origin && trimmedName === '') {
            listEl.innerHTML = '';
            setStatus(statusEl, '');
            hideError();
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
            subHeadingEl.hidden = false;
            subHeadingEl.textContent = '내 위치 기준 · 가까운 순';
        } else {
            subHeadingEl.hidden = true;
        }
        runQuery(''); // emptyStateEl.hidden도 여기서 origin/검색어 기준으로 같이 정해진다
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
