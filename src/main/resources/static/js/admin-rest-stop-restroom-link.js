import {
    fetchRestroomLinks,
    linkRestroom,
    searchRestrooms,
    unlinkRestroom
} from './admin-rest-stop-restroom-link-request.js';
import { closeDialogById, csrfFrom, openDialogById } from './admin-common.js';

const DEFAULT_DEBOUNCE_MS = 250;

export function initializeAdminRestStopRestroomLink(document, {
    fetchImpl = fetch,
    confirmImpl = globalThis.confirm,
    onNotice = () => {},
    debounceMs = DEFAULT_DEBOUNCE_MS
} = {}) {
    const csrfSource = document.getElementById('restroomLinkCsrfSource');
    const status = document.getElementById('restroomLinkStatus');
    const totalCount = document.getElementById('restroomLinkTotalCount');
    const missingCount = document.getElementById('restroomLinkMissingCount');
    const searchInput = document.getElementById('restroomLinkSearchInput');
    const missingOnlyButton = document.getElementById('restroomLinkMissingOnlyButton');
    const tableBody = document.getElementById('restroomLinkTableBody');
    const modal = document.getElementById('restroomLinkModal');
    const modalClose = document.getElementById('restroomLinkModalClose');
    const modalTitle = document.getElementById('restroomLinkModalTitle');
    const currentList = document.getElementById('restroomLinkCurrentList');
    const searchQuery = document.getElementById('restroomLinkSearchQuery');
    const searchRoute = document.getElementById('restroomLinkSearchRoute');
    const searchResults = document.getElementById('restroomLinkSearchResults');
    if (!csrfSource || !status || !totalCount || !missingCount || !searchInput || !missingOnlyButton || !tableBody
        || !modal || !modalClose || !modalTitle || !currentList || !searchQuery || !searchRoute || !searchResults) {
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
            if (missingOnly && restStop.linkedRestroom !== null) {
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
        const linkedRestroom = restStop.linkedRestroom;

        const nameCell = document.createElement('td');
        nameCell.textContent = restStop.unitName;

        const routeCell = document.createElement('td');
        routeCell.textContent = restStop.routeName ?? '';

        const restroomNameCell = document.createElement('td');
        restroomNameCell.textContent = linkedRestroom ? linkedRestroom.sourceRestStopName : '없음';

        const badgeCell = document.createElement('td');
        const badge = document.createElement('span');
        badge.className = 'oil-link-badge';
        badge.dataset.state = linkedRestroom ? 'linked' : 'missing';
        badge.textContent = linkedRestroom ? '연결됨' : '연결없음';
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
        row.appendChild(restroomNameCell);
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
            allRestStops.filter((restStop) => restStop.linkedRestroom === null).length
        );
    }

    function populateRouteOptions() {
        const previousValue = searchRoute.value;
        const routeNames = [...new Set(allRestStops.map((restStop) => restStop.routeName).filter(Boolean))].sort(
            (a, b) => a.localeCompare(b, 'ko')
        );
        const defaultOption = document.createElement('option');
        defaultOption.value = '';
        defaultOption.textContent = '전체 노선';
        const routeOptions = routeNames.map((routeName) => {
            const option = document.createElement('option');
            option.value = routeName;
            option.textContent = routeName;
            return option;
        });
        searchRoute.replaceChildren(defaultOption, ...routeOptions);
        if (routeNames.includes(previousValue)) {
            searchRoute.value = previousValue;
        }
    }

    async function loadRestroomLinks() {
        status.textContent = '불러오는 중입니다.';
        const result = await fetchRestroomLinks(fetchImpl);
        if (result.status !== 'success') {
            status.textContent = '목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.';
            return;
        }
        allRestStops = result.restStops;
        status.textContent = '';
        renderSummary();
        renderTable();
        populateRouteOptions();
    }

    async function reloadAndRefreshModal() {
        await loadRestroomLinks();
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
        renderCurrentLink(updated.linkedRestroom);
    }

    function appendRestroomDetail(item, restroom) {
        const name = document.createElement('span');
        name.className = 'oil-link-detail-name';
        name.textContent = restroom.sourceRestStopName;
        item.appendChild(name);

        const detail = document.createElement('span');
        detail.className = 'oil-link-detail-meta';
        detail.textContent = [
            restroom.routeName,
            restroom.maleToiletCount !== null && restroom.maleToiletCount !== undefined
                ? `남 ${restroom.maleToiletCount}개`
                : null,
            restroom.femaleToiletCount !== null && restroom.femaleToiletCount !== undefined
                ? `여 ${restroom.femaleToiletCount}개`
                : null
        ]
            .filter((value) => value)
            .join(' · ');
        item.appendChild(detail);
    }

    function createCurrentLinkItem(restroom) {
        const item = document.createElement('li');
        appendRestroomDetail(item, restroom);

        const unlinkButton = document.createElement('button');
        unlinkButton.type = 'button';
        unlinkButton.textContent = '연결 해제';
        unlinkButton.addEventListener('click', async () => {
            if (!confirmImpl('이 화장실 현황 연결을 해제할까요?')) {
                return;
            }
            const result = await unlinkRestroom(restroom.id, pageCsrf(), fetchImpl);
            if (result.status !== 'success') {
                onNotice('연결 해제에 실패했습니다.', 'error');
                return;
            }
            onNotice('연결을 해제했습니다.');
            await reloadAndRefreshModal();
        });
        item.appendChild(unlinkButton);

        return item;
    }

    function renderCurrentLink(restroom) {
        if (!restroom) {
            const empty = document.createElement('li');
            empty.className = 'oil-link-current-empty';
            empty.textContent = '연결된 화장실 현황이 없습니다';
            currentList.replaceChildren(empty);
            return;
        }
        currentList.replaceChildren(createCurrentLinkItem(restroom));
    }

    function createSearchResultItem(restroom) {
        const item = document.createElement('li');
        appendRestroomDetail(item, restroom);

        if (restroom.linkedRestStopName) {
            const linkedText = document.createElement('span');
            linkedText.className = 'oil-link-search-linked';
            linkedText.textContent = `${restroom.linkedRestStopName}에 이미 연결됨`;
            item.appendChild(linkedText);
        }

        const linkButton = document.createElement('button');
        linkButton.type = 'button';
        linkButton.textContent = '연결';
        linkButton.addEventListener('click', async () => {
            if (!currentRestStop) {
                return;
            }
            const result = await linkRestroom(restroom.id, currentRestStop.serviceAreaCode, pageCsrf(), fetchImpl);
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

    async function runSearch() {
        const name = searchQuery.value.trim();
        const routeName = searchRoute.value;
        if (name === '' && routeName === '') {
            searchResults.replaceChildren();
            return;
        }
        const result = await searchRestrooms(name, routeName, fetchImpl);
        if (result.status !== 'success') {
            searchResults.replaceChildren();
            return;
        }
        if (result.restrooms.length === 0) {
            const empty = document.createElement('li');
            empty.textContent = '검색 결과가 없습니다';
            searchResults.replaceChildren(empty);
            return;
        }
        searchResults.replaceChildren(...result.restrooms.map((restroom) => createSearchResultItem(restroom)));
    }

    function openModal(restStop) {
        openDialogById(document, 'restroomLinkModal', {
            onOpened: () => {
                currentRestStop = restStop;
                modalTitle.textContent = restStop.unitName;
                renderCurrentLink(restStop.linkedRestroom);
                searchQuery.value = '';
                searchRoute.value = restStop.routeName ?? '';
                runSearch();
            }
        });
    }

    searchInput.addEventListener('input', renderTable);

    missingOnlyButton.addEventListener('click', () => {
        missingOnly = !missingOnly;
        missingOnlyButton.setAttribute('aria-pressed', String(missingOnly));
        renderTable();
    });

    modalClose.addEventListener('click', () => {
        closeDialogById(document, 'restroomLinkModal', () => {
            currentRestStop = null;
        });
    });

    searchQuery.addEventListener('input', () => {
        clearTimeout(searchDebounceTimer);
        if (debounceMs === 0) {
            runSearch();
            return;
        }
        searchDebounceTimer = setTimeout(() => runSearch(), debounceMs);
    });

    searchRoute.addEventListener('change', () => {
        runSearch();
    });

    loadRestroomLinks();
}
