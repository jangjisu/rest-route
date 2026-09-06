/**
 * rest-stops-name-search.js — 지도 화면 "휴게소명 검색" 모달. 입력 → API 조회 → 결과가 하나면
 * 바로 선택, 여러 개면 후보 목록 모달을 띄운다. 실제 선택 시 지도 팝업을 열지(좌표 있을 때)
 * 상세 패널만 열지(좌표 없을 때)는 이 모듈이 정하지 않고, 호출하는 쪽이 openRestStopPopupAt/
 * openDetailPanel 콜백으로 위임받는다.
 */
import { closeDialogById, openDialogById } from './utils.js';
import { formatText } from './rest-stop-detail-formatters.js';
import { createCandidateListItem } from './candidate-list-item.js';
import { createRestStopNameSearchRequest } from './rest-stop-name-search-request.js';

export function initRestStopNameSearch(document, { signal, openRestStopPopupAt, openDetailPanel }) {
    const request = createRestStopNameSearchRequest({ onState: renderState });

    function setStatus(message) {
        const status = document.getElementById('restStopNameSearchStatus');
        if (!status) {
            return;
        }
        status.hidden = message === '';
        status.textContent = message;
    }

    function search() {
        const query = document.getElementById('restStopNameSearchInput')?.value.trim() ?? '';
        if (query === '') {
            setStatus('휴게소명을 입력해주세요.');
            return;
        }
        request.load(query);
    }

    function renderState(state) {
        if (state.status === 'loading') {
            setStatus('검색하는 중입니다...');
            return;
        }

        if (state.status === 'success') {
            if (state.restStops.length === 0) {
                setStatus('검색 결과가 없습니다.');
                return;
            }
            if (state.restStops.length === 1) {
                setStatus('');
                selectResult(state.restStops[0]);
                return;
            }
            setStatus('');
            renderCandidates(state.restStops);
            openDialogById('restStopSearchModal');
            return;
        }

        setStatus('검색에 실패했습니다. 잠시 후 다시 시도해주세요.');
    }

    function renderCandidates(restStops) {
        const list = document.getElementById('restStopSearchList');
        if (!list) {
            return;
        }
        list.replaceChildren();
        restStops.forEach((restStop) => list.appendChild(createItem(restStop)));
    }

    function createItem(restStop) {
        return createCandidateListItem(document, {
            itemClassName: 'route-result-item route-candidate-item',
            buttonClassName: 'route-candidate-button',
            primaryClassName: 'route-result-name',
            secondaryClassName: 'route-result-meta',
            primaryText: formatText(restStop?.unitName, '이름 정보 없음'),
            secondaryText: formatText(restStop?.routeName, '노선 정보 없음'),
            onSelect: () => selectResult(restStop)
        });
    }

    function selectResult(restStop) {
        closeDialogById('restStopSearchModal');
        const latitude = Number.parseFloat(restStop.yValue);
        const longitude = Number.parseFloat(restStop.xValue);
        if (Number.isFinite(latitude) && Number.isFinite(longitude)) {
            openRestStopPopupAt(restStop, { latitude, longitude });
            return;
        }
        openDetailPanel(restStop);
    }

    document.getElementById('restStopNameSearchButton')?.addEventListener('click', search, { signal });
    document.getElementById('restStopNameSearchInput')?.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            search();
        }
    }, { signal });
    document.getElementById('restStopSearchModalClose')?.addEventListener('click', () => closeDialogById('restStopSearchModal'), { signal });
    document.getElementById('restStopSearchModal')?.addEventListener('click', (event) => {
        if (event.target === event.currentTarget) {
            closeDialogById('restStopSearchModal');
        }
    }, { signal });
}
