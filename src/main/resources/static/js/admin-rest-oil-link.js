import {
    clearOilStationOverride,
    fetchOilLinks,
    linkOilStation,
    searchOilStations,
    unlinkOilStation
} from './admin-rest-oil-link-request.js';

const DEFAULT_DEBOUNCE_MS = 250;

function csrfFrom(source) {
    return {
        headerName: source.dataset.csrfHeader || 'X-CSRF-TOKEN',
        token: source.querySelector('input[name="_csrf"]')?.value || ''
    };
}

export function initializeAdminRestOilLink(document, {
    fetchImpl = fetch,
    confirmImpl = globalThis.confirm,
    onNotice = () => {},
    debounceMs = DEFAULT_DEBOUNCE_MS
} = {}) {
    const csrfSource = document.getElementById('oilLinkCsrfSource');
    const status = document.getElementById('oilLinkStatus');
    const totalCount = document.getElementById('oilLinkTotalCount');
    const missingCount = document.getElementById('oilLinkMissingCount');
    const searchInput = document.getElementById('oilLinkSearchInput');
    const missingOnlyButton = document.getElementById('oilLinkMissingOnlyButton');
    const tableBody = document.getElementById('oilLinkTableBody');
    const modal = document.getElementById('oilLinkModal');
    const modalClose = document.getElementById('oilLinkModalClose');
    const modalTitle = document.getElementById('oilLinkModalTitle');
    const currentList = document.getElementById('oilLinkCurrentList');
    const searchQuery = document.getElementById('oilLinkSearchQuery');
    const searchResults = document.getElementById('oilLinkSearchResults');
    if (!csrfSource || !status || !totalCount || !missingCount || !searchInput || !missingOnlyButton || !tableBody
        || !modal || !modalClose || !modalTitle || !currentList || !searchQuery || !searchResults) {
        return;
    }

    let allRestStops = [];
    let missingOnly = false;
    let currentRestStop = null;
    let searchDebounceTimer;

    function pageCsrf() {
        return csrfFrom(csrfSource);
    }

    function filteredRestStops() {
        const query = searchInput.value.trim();
        return allRestStops.filter((restStop) => {
            if (missingOnly && restStop.linkedOilStation !== null) {
                return false;
            }
            if (query !== '' && !restStop.unitName.includes(query)) {
                return false;
            }
            return true;
        });
    }

    function createRestStopRow(restStop) {
        const row = document.createElement('tr');
        const linkedOilStation = restStop.linkedOilStation;

        const nameCell = document.createElement('td');
        nameCell.textContent = restStop.unitName;

        const routeCell = document.createElement('td');
        routeCell.textContent = restStop.routeName ?? '';

        const oilNamesCell = document.createElement('td');
        oilNamesCell.textContent = linkedOilStation ? linkedOilStation.standardRestName : '없음';

        const badgeCell = document.createElement('td');
        const badge = document.createElement('span');
        badge.className = 'oil-link-badge';
        const locked = linkedOilStation?.adminOverridden ?? false;
        badge.dataset.state = linkedOilStation ? 'linked' : 'missing';
        badge.textContent = (linkedOilStation ? '연결됨' : '연결없음') + (locked ? ' 🔒' : '');
        badgeCell.appendChild(badge);

        const manageCell = document.createElement('td');
        const manageButton = document.createElement('button');
        manageButton.type = 'button';
        manageButton.className = 'oil-link-manage';
        manageButton.textContent = '관리';
        manageButton.addEventListener('click', () => openModal(restStop));
        manageCell.appendChild(manageButton);

        row.appendChild(nameCell);
        row.appendChild(routeCell);
        row.appendChild(oilNamesCell);
        row.appendChild(badgeCell);
        row.appendChild(manageCell);
        return row;
    }

    function renderTable() {
        tableBody.replaceChildren(...filteredRestStops().map((restStop) => createRestStopRow(restStop)));
    }

    function renderSummary() {
        totalCount.textContent = String(allRestStops.length);
        missingCount.textContent = String(
            allRestStops.filter((restStop) => restStop.linkedOilStation === null).length
        );
    }

    async function loadOilLinks() {
        status.textContent = '불러오는 중입니다.';
        const result = await fetchOilLinks(fetchImpl);
        if (result.status !== 'success') {
            status.textContent = '목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.';
            return;
        }
        allRestStops = result.restStops;
        status.textContent = '';
        renderSummary();
        renderTable();
    }

    async function reloadAndRefreshModal() {
        await loadOilLinks();
        if (!currentRestStop) {
            return;
        }
        const updated = allRestStops.find(
            (restStop) => restStop.serviceAreaCode === currentRestStop.serviceAreaCode
        );
        if (!updated) {
            return;
        }
        currentRestStop = updated;
        modalTitle.textContent = updated.unitName;
        renderCurrentLink(updated.linkedOilStation);
    }

    function appendOilStationDetail(item, oil) {
        const name = document.createElement('span');
        name.className = 'oil-link-detail-name';
        name.textContent = oil.standardRestName;
        item.appendChild(name);

        const detail = document.createElement('span');
        detail.className = 'oil-link-detail-meta';
        detail.textContent = [oil.routeName, oil.serviceAreaAddress, oil.direction ? `${oil.direction} 방향` : null]
            .filter((value) => value)
            .join(' · ');
        item.appendChild(detail);
    }

    function createCurrentLinkItem(oil) {
        const item = document.createElement('li');
        appendOilStationDetail(item, oil);

        const unlinkButton = document.createElement('button');
        unlinkButton.type = 'button';
        unlinkButton.textContent = '연결 해제';
        unlinkButton.addEventListener('click', async () => {
            if (!confirmImpl('이 주유소 연결을 해제할까요?')) {
                return;
            }
            const result = await unlinkOilStation(oil.id, pageCsrf(), fetchImpl);
            if (result.status !== 'success') {
                onNotice('연결 해제에 실패했습니다.', 'error');
                return;
            }
            onNotice('연결을 해제했습니다.');
            await reloadAndRefreshModal();
        });
        item.appendChild(unlinkButton);

        if (oil.adminOverridden) {
            const releaseButton = document.createElement('button');
            releaseButton.type = 'button';
            releaseButton.textContent = '자동 매칭으로 되돌리기';
            releaseButton.addEventListener('click', async () => {
                const result = await clearOilStationOverride(oil.id, pageCsrf(), fetchImpl);
                if (result.status !== 'success') {
                    onNotice('잠금 해제에 실패했습니다.', 'error');
                    return;
                }
                onNotice('자동 매칭으로 되돌렸습니다.');
                await reloadAndRefreshModal();
            });
            item.appendChild(releaseButton);
        }

        return item;
    }

    function renderCurrentLink(oilStation) {
        if (!oilStation) {
            const empty = document.createElement('li');
            empty.className = 'oil-link-current-empty';
            empty.textContent = '연결된 주유소가 없습니다';
            currentList.replaceChildren(empty);
            return;
        }
        currentList.replaceChildren(createCurrentLinkItem(oilStation));
    }

    function createSearchResultItem(oil) {
        const item = document.createElement('li');
        appendOilStationDetail(item, oil);

        if (oil.linkedRestStopName) {
            const linkedText = document.createElement('span');
            linkedText.className = 'oil-link-search-linked';
            linkedText.textContent = `${oil.linkedRestStopName}에 이미 연결됨`;
            item.appendChild(linkedText);
        }

        const linkButton = document.createElement('button');
        linkButton.type = 'button';
        linkButton.textContent = '연결';
        linkButton.addEventListener('click', async () => {
            if (!currentRestStop) {
                return;
            }
            const result = await linkOilStation(oil.id, currentRestStop.serviceAreaCode, pageCsrf(), fetchImpl);
            if (result.status !== 'success') {
                onNotice('연결에 실패했습니다.', 'error');
                return;
            }
            onNotice('연결했습니다.');
            searchQuery.value = '';
            searchResults.replaceChildren();
            await reloadAndRefreshModal();
        });
        item.appendChild(linkButton);
        return item;
    }

    async function runSearch(name) {
        const trimmed = name.trim();
        if (trimmed === '') {
            searchResults.replaceChildren();
            return;
        }
        const result = await searchOilStations(trimmed, fetchImpl);
        if (result.status !== 'success') {
            searchResults.replaceChildren();
            return;
        }
        if (result.oilStations.length === 0) {
            const empty = document.createElement('li');
            empty.textContent = '검색 결과가 없습니다';
            searchResults.replaceChildren(empty);
            return;
        }
        searchResults.replaceChildren(...result.oilStations.map((oil) => createSearchResultItem(oil)));
    }

    function openModal(restStop) {
        currentRestStop = restStop;
        modalTitle.textContent = restStop.unitName;
        renderCurrentLink(restStop.linkedOilStation);
        searchQuery.value = '';
        searchResults.replaceChildren();
        modal.showModal();
    }

    searchInput.addEventListener('input', renderTable);

    missingOnlyButton.addEventListener('click', () => {
        missingOnly = !missingOnly;
        missingOnlyButton.setAttribute('aria-pressed', String(missingOnly));
        renderTable();
    });

    modalClose.addEventListener('click', () => {
        modal.close();
        currentRestStop = null;
    });

    searchQuery.addEventListener('input', () => {
        clearTimeout(searchDebounceTimer);
        const value = searchQuery.value;
        if (debounceMs === 0) {
            runSearch(value);
            return;
        }
        searchDebounceTimer = setTimeout(() => runSearch(value), debounceMs);
    });

    loadOilLinks();
}
