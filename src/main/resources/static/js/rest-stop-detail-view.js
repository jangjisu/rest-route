import { closeDialogById, openDialogById, showApiUnavailableAlert } from './utils.js';
import {
    availableDataTags,
    CONVENIENCE_FALLBACK,
    formatAvailability,
    formatFoodBadges,
    formatFoodCost,
    formatFreightOperation,
    formatOilPrice,
    formatOperationTime,
    formatParkingBreakdown,
    formatRefreshedAt,
    formatTotalParkingCount,
    formatSalesRankingMonth,
    formatText,
    hasFoodMenu,
    hasFoodSections,
    hasRenderableRestStopDetail,
    isMissingValue,
    normalizeSalesRankingStoreName,
    orderFoodMenus,
    sortSalesRankingProducts,
    sortSalesRankingStores
} from './rest-stop-detail-formatters.js';
import { renderDetailImage } from './rest-stop-images.js';

export function restStopDetailEmptyMessage(detail, externalUnavailable = false) {
    if (hasRenderableRestStopDetail(detail)) {
        return '';
    }
    return externalUnavailable
        ? '상세 정보를 불러오지 못했습니다.'
        : '이 휴게소의 상세 정보를 준비하고 있습니다.';
}

export function formatEvChargerAvailability(restStop) {
    return restStop?.hasEvCharger === true ? '전기차 충전 가능' : '';
}

export function formatEvChargerCount(count) {
    const numericCount = Number(count);
    if (!Number.isFinite(numericCount) || numericCount <= 0) {
        return '';
    }

    return `${numericCount.toLocaleString()}대`;
}

export function renderOilInfo(oilInfo = {}) {
    const section = document.getElementById('restStopOilSection');
    if (!section) {
        return;
    }

    section.classList.remove('d-none');

    setDetailValue('restStopOilGasolinePrice', oilInfo?.gasolinePrice, '정보 없음', formatOilPrice);
    setDetailValue('restStopOilDieselPrice', oilInfo?.dieselPrice, '정보 없음', formatOilPrice);
    setDetailValue('restStopOilLpgPrice', oilInfo?.lpgPrice, '정보 없음', formatOilPrice);
    setDetailValue('restStopOilCompany', oilInfo?.oilCompany, '정보 없음');
    setDetailValue('restStopOilTelNo', oilInfo?.telNo, '정보 없음');
    renderOilRefreshStatus(oilInfo?.lastRefreshedAt);
    renderOilConveniences(oilInfo?.oilStationConveniences);
}

export function renderThemeBadges(themes) {
    const list = document.getElementById('restStopDetailThemes');
    if (!list) {
        return;
    }

    list.replaceChildren();
    const items = Array.isArray(themes) ? themes : [];
    items.forEach((theme) => {
        const name = typeof theme?.name === 'string' ? theme.name.trim() : '';
        if (name === '') {
            return;
        }

        const badge = document.createElement('li');
        badge.className = 'rest-stop-detail-theme-badge';
        badge.textContent = name;
        const detail = typeof theme?.detail === 'string' ? theme.detail.trim() : '';
        if (detail !== '') {
            badge.title = detail;
        }
        list.appendChild(badge);
    });

    list.classList.toggle('d-none', list.children.length === 0);
}

export function renderEventSection(events) {
    const section = document.getElementById('restStopDetailEventSection');
    const list = document.getElementById('restStopDetailEventList');
    if (!section || !list) {
        return;
    }

    list.replaceChildren();
    const items = Array.isArray(events) ? events : [];
    items.forEach((event) => {
        const name = typeof event?.name === 'string' ? event.name.trim() : '';
        if (name === '') {
            return;
        }

        list.appendChild(createEventItem(event, name));
    });

    section.classList.toggle('d-none', list.children.length === 0);
}

function popupOptionsForState(status) {
    if (status === 'loading') {
        return { status: 'loading' };
    }
    if (status === 'not-found') {
        return { status: 'success', tags: [] };
    }
    return { status: 'error' };
}

function detailStatusMessage(status) {
    if (status === 'loading') {
        return '상세 정보를 불러오는 중입니다.';
    }
    if (status === 'not-found') {
        return '상세 정보가 없습니다.';
    }
    return '상세 정보를 불러오지 못했습니다.';
}

function oilRefreshStatusMessage(status) {
    if (status === 'not-found') {
        return '갱신할 주유소 정보가 없습니다.';
    }

    return '요금 정보를 갱신하지 못했습니다.';
}

function setDetailName(value, fallbackValue) {
    const element = document.getElementById('restStopDetailName');
    if (!element) {
        return;
    }

    element.textContent = formatText(value, formatText(fallbackValue, '이름 정보 없음'));
    element.classList.toggle(
        'rest-stop-detail-missing',
        isMissingValue(value) && isMissingValue(fallbackValue)
    );
}

function setDetailValue(id, rawValue, fallback, formatter = (value) => formatText(value, fallback)) {
    const element = document.getElementById(id);
    if (!element) {
        return;
    }

    const formattedValue = formatter(rawValue);
    element.textContent = formattedValue;
    element.classList.toggle('rest-stop-detail-missing', isMissingValue(rawValue));
}

function renderParkingInfo(compactCount, fullSizeCount, disabledCount) {
    const totalElement = document.getElementById('restStopDetailTotalParking');
    if (totalElement) {
        const allMissing = isMissingValue(compactCount) && isMissingValue(fullSizeCount) && isMissingValue(disabledCount);
        totalElement.textContent = formatTotalParkingCount(compactCount, fullSizeCount, disabledCount);
        totalElement.classList.toggle('rest-stop-detail-missing', allMissing);
    }

    const breakdownElement = document.getElementById('restStopDetailParkingBreakdown');
    if (breakdownElement) {
        const allMissing = isMissingValue(fullSizeCount) && isMissingValue(compactCount) && isMissingValue(disabledCount);
        breakdownElement.textContent = formatParkingBreakdown(fullSizeCount, compactCount, disabledCount);
        breakdownElement.classList.toggle('rest-stop-detail-missing', allMissing);
    }
}

function renderConvenience(facilities) {
    const list = document.getElementById('restStopDetailConvenience');
    const fallback = document.getElementById('restStopDetailConvenienceFallback');
    if (!list || !fallback) {
        return;
    }

    list.replaceChildren();
    const conveniences = Array.isArray(facilities) ? facilities : [];
    conveniences.forEach((convenience) => {
        const item = document.createElement('li');
        item.textContent = convenience;
        list.appendChild(item);
    });

    list.classList.toggle('d-none', conveniences.length === 0);
    fallback.classList.toggle('d-none', conveniences.length > 0);
    fallback.textContent = conveniences.length === 0 ? CONVENIENCE_FALLBACK : '';
}

function renderBooleanStatus(id, value, formatter) {
    const element = document.getElementById(id);
    if (!element) {
        return;
    }

    element.textContent = formatter(value);
    element.classList.toggle('rest-stop-detail-missing', value !== true && value !== false);
}

function renderEvChargerInfo(count) {
    const badge = document.getElementById('restStopDetailEvCharger');
    const value = document.getElementById('restStopDetailEvChargerText');
    if (!badge || !value) {
        return;
    }

    const formattedCount = formatEvChargerCount(count);
    value.textContent = formattedCount;
    badge.classList.toggle('d-none', formattedCount === '');
}

function renderSalesRanking(salesRanking) {
    const section = document.getElementById('restStopSalesRankingSection');
    const month = document.getElementById('restStopSalesRankingMonth');
    const storeColumn = document.getElementById('restStopStoreRankingColumn');
    const productColumn = document.getElementById('restStopProductRankingColumn');
    const storeList = document.getElementById('restStopStoreRankingList');
    const productList = document.getElementById('restStopProductRankingList');
    if (!section || !month || !storeColumn || !productColumn || !storeList || !productList) {
        return;
    }

    const visibleStores = sortSalesRankingStores(salesRanking?.storeRankings);
    const visibleProducts = sortSalesRankingProducts(salesRanking?.products);
    const hasRanking = (visibleStores.length > 0 || visibleProducts.length > 0)
        && !isMissingValue(salesRanking?.baseYearMonth);
    section.classList.toggle('d-none', !hasRanking);
    if (!hasRanking) {
        storeList.replaceChildren();
        productList.replaceChildren();
        storeColumn.classList.add('d-none');
        productColumn.classList.add('d-none');
        month.textContent = '';
        return;
    }

    month.textContent = formatSalesRankingMonth(salesRanking.baseYearMonth);
    storeColumn.classList.toggle('d-none', visibleStores.length === 0);
    productColumn.classList.toggle('d-none', visibleProducts.length === 0);
    storeList.replaceChildren(...visibleStores.map((store) => createSalesRankingItem(store, 'storeName')));
    productList.replaceChildren(...visibleProducts.map((product) => createSalesRankingItem(product, 'productName')));
}

function createSalesRankingItem(ranking, nameKey) {
    const item = document.createElement('li');
    item.className = 'rest-stop-sales-ranking-item';

    const rank = document.createElement('span');
    rank.className = 'rest-stop-sales-ranking-rank';
    rank.textContent = `${ranking.rank}`;
    item.appendChild(rank);

    const name = document.createElement('span');
    name.className = 'rest-stop-sales-ranking-name';
    name.textContent = nameKey === 'storeName'
        ? normalizeSalesRankingStoreName(ranking[nameKey])
        : ranking[nameKey];
    item.appendChild(name);

    return item;
}

function renderOilRefreshStatus(lastRefreshedAt) {
    const status = document.getElementById('restStopOilRefreshStatus');
    if (!status) {
        return;
    }

    status.textContent = formatRefreshedAt(lastRefreshedAt);
    status.classList.toggle('rest-stop-detail-missing', isMissingValue(lastRefreshedAt));
}

function renderOilConveniences(conveniences) {
    const tags = document.getElementById('restStopOilConvenienceTags');
    const fallback = document.getElementById('restStopOilConvenienceFallback');
    const details = document.getElementById('restStopOilConvenienceDetails');
    if (!tags || !fallback || !details) {
        return;
    }

    tags.replaceChildren();
    details.replaceChildren();

    const oilConveniences = Array.isArray(conveniences) ? conveniences : [];
    oilConveniences.forEach((convenience) => {
        tags.appendChild(createOilConvenienceTag(convenience));
        details.appendChild(createOilConvenienceDetail(convenience));
    });

    const hasConveniences = oilConveniences.length > 0;
    tags.classList.toggle('d-none', !hasConveniences);
    details.classList.toggle('d-none', !hasConveniences);
    fallback.classList.toggle('d-none', hasConveniences);
    fallback.textContent = hasConveniences ? '' : '주유소 편의시설 정보 없음';
}

function createOilConvenienceTag(convenience) {
    const item = document.createElement('li');
    item.textContent = formatText(convenience?.name, '이름 정보 없음');
    return item;
}

function createOilConvenienceDetail(convenience) {
    const item = document.createElement('li');

    const name = document.createElement('p');
    name.className = 'rest-stop-oil-convenience-name';
    name.textContent = formatText(convenience?.name, '이름 정보 없음');
    item.appendChild(name);

    const description = document.createElement('p');
    description.className = 'rest-stop-oil-convenience-description';
    description.textContent = formatText(convenience?.description, '상세 정보 없음');
    description.classList.toggle('rest-stop-detail-missing', isMissingValue(convenience?.description));
    item.appendChild(description);

    const time = document.createElement('p');
    time.className = 'rest-stop-oil-convenience-time';
    time.textContent = formatOperationTime(convenience?.startTime, convenience?.endTime);
    time.classList.toggle(
        'rest-stop-detail-missing',
        isMissingValue(convenience?.startTime) || isMissingValue(convenience?.endTime)
    );
    item.appendChild(time);

    return item;
}

function createEventItem(event, name) {
    const item = document.createElement('li');
    item.className = 'rest-stop-detail-event-item';

    const nameElement = document.createElement('p');
    nameElement.className = 'rest-stop-detail-event-name';
    nameElement.textContent = name;
    item.appendChild(nameElement);

    const period = typeof event?.period === 'string' ? event.period.trim() : '';
    if (period !== '') {
        const periodElement = document.createElement('p');
        periodElement.className = 'rest-stop-detail-event-period';
        periodElement.textContent = period;
        item.appendChild(periodElement);
    }

    const detail = typeof event?.detail === 'string' ? event.detail.trim() : '';
    if (detail !== '') {
        const detailElement = document.createElement('p');
        detailElement.className = 'rest-stop-detail-event-detail';
        detailElement.textContent = detail;
        item.appendChild(detailElement);
    }

    return item;
}

/**
 * 휴게소 상세 패널의 상태·렌더링을 전담한다. currentDetail/foodExpanded 같은 가변 상태는
 * 이 View가 직접 소유하고, 메인 모듈(rest-stops-map.js)의 전역 상태를 역참조하지 않는다.
 * 지도 팝업 갱신(onPopupUpdate)과 모바일 시트 레이아웃 갱신(onPresentationChange), 실시간
 * 유가 재조회(refreshOilPrice)는 메인이 주입하는 콜백으로 처리한다.
 */
export function createRestStopDetailView({ onPopupUpdate, onPresentationChange, refreshOilPrice }) {
    let currentDetail;
    let selectedRestStopName = '';
    let selectedServiceAreaCode = '';
    let foodExpanded = false;
    let currentFoodMenus = [];
    let currentFoodSections = [];

    function open(restStop) {
        selectedRestStopName = restStop.unitName;
        selectedServiceAreaCode = restStop.serviceAreaCode;
        currentDetail = undefined;
    }

    function renderState(state) {
        const panel = document.getElementById('restStopDetailPanel');
        const status = document.getElementById('restStopDetailStatus');
        const content = document.getElementById('restStopDetailContent');
        if (!panel || !status || !content) {
            return;
        }

        panel.classList.remove('d-none');
        onPresentationChange?.();
        setDetailName(selectedRestStopName);
        panel.setAttribute('aria-busy', state.status === 'loading' ? 'true' : 'false');

        if (state.status === 'success') {
            if (state.externalUnavailable) {
                showApiUnavailableAlert();
            }

            renderDetail(state.data);
            const emptyMessage = restStopDetailEmptyMessage(state.data, state.externalUnavailable);
            status.textContent = emptyMessage;
            status.classList.toggle('d-none', emptyMessage === '');
            content.classList.toggle('d-none', emptyMessage !== '');
            onPopupUpdate?.(
                { unitName: state.data.unitName || selectedRestStopName, routeName: state.data.routeName },
                { status: 'success', tags: availableDataTags(state.data) }
            );
            return;
        }

        if (state.status === 'external-unavailable') {
            showApiUnavailableAlert();
        }

        content.classList.add('d-none');
        status.classList.remove('d-none');
        status.textContent = detailStatusMessage(state.status);
        onPopupUpdate?.({ unitName: selectedRestStopName }, popupOptionsForState(state.status));
    }

    function renderDetail(detail) {
        currentDetail = detail;
        renderDetailImage(document, detail);
        setDetailName(detail.unitName, selectedRestStopName);
        setDetailValue('restStopDetailRoute', detail.routeName, '노선 정보 없음');
        setDetailValue('restStopDetailDirection', detail.direction, '방향 정보 없음');
        setDetailValue('restStopDetailAddress', detail.address, '주소 정보 없음');
        renderConvenience(detail.convenienceFacilities);
        renderBooleanStatus('restStopDetailMaintenance', detail.hasMaintenance, formatAvailability);
        renderBooleanStatus('restStopDetailFreight', detail.allowsTruckParking, formatFreightOperation);
        renderParkingInfo(detail.compactCarParkingCount, detail.fullSizeCarParkingCount, detail.disabledParkingCount);
        renderEvChargerInfo(detail.evChargerCount);
        renderThemeBadges(detail.themes);
        renderEventSection(detail.events);
        renderSalesRanking(detail.salesRanking);
        renderOilInfo(detail.oilInfo);
        renderFoodMenu(detail.foodMenu);
    }

    function renderFoodMenu(foodMenu) {
        const openButton = document.getElementById('restStopFoodOpen');
        if (!openButton) {
            return;
        }

        const visible = hasFoodMenu(foodMenu);
        openButton.classList.toggle('d-none', !visible);
        if (!visible) {
            currentFoodMenus = [];
            currentFoodSections = [];
            closeFoodModal();
            return;
        }

        currentFoodMenus = foodMenu.menus;
        currentFoodSections = Array.isArray(foodMenu.sections) ? foodMenu.sections : [];
        foodExpanded = false;
    }

    function openFoodModal() {
        openDialogById('restStopFoodModal', {
            guard: () => currentFoodMenus.length > 0,
            onOpened: () => {
                foodExpanded = false;
                renderFoodList();
            }
        });
    }

    function closeFoodModal() {
        closeDialogById('restStopFoodModal');
    }

    function renderFoodList() {
        const list = document.getElementById('restStopFoodList');
        if (!list) {
            return;
        }

        const sections = currentFoodSections.filter((section) => Array.isArray(section?.menus) && section.menus.length > 0);
        if (!foodExpanded && sections.length > 0) {
            list.replaceChildren();
            sections.forEach((section) => list.appendChild(createFoodSection(section)));
            renderFoodToggle(currentFoodMenus.length > 0);
            return;
        }

        const recommended = currentFoodMenus.filter((menu) => menu?.recommended);
        const hasRecommended = recommended.length > 0;
        const menus = foodExpanded || !hasRecommended ? orderFoodMenus(currentFoodMenus) : recommended;

        list.replaceChildren();
        menus.forEach((menu) => list.appendChild(createFoodMenuItem(menu)));
        renderFoodToggle(hasFoodSections({ sections }) || (hasRecommended && currentFoodMenus.length > recommended.length));
    }

    function renderFoodToggle(canExpand) {
        const toggle = document.getElementById('restStopFoodToggle');
        if (!toggle) {
            return;
        }

        toggle.classList.toggle('d-none', !canExpand);
        toggle.setAttribute('aria-expanded', foodExpanded ? 'true' : 'false');
        toggle.textContent = foodExpanded ? '추천 메뉴 보기' : '전체 메뉴 보기';
    }

    function createFoodSection(section) {
        const item = document.createElement('li');
        item.className = 'rest-stop-food-section';

        const title = document.createElement('h4');
        title.className = 'rest-stop-food-section-title';
        title.textContent = formatText(section?.title, '추천 메뉴');
        item.appendChild(title);

        const list = document.createElement('ul');
        list.className = 'rest-stop-food-section-list';
        section.menus.forEach((menu) => list.appendChild(createFoodMenuItem(menu)));
        item.appendChild(list);

        return item;
    }

    function createFoodMenuItem(menu) {
        const item = document.createElement('li');
        item.className = 'rest-stop-food-item';

        const name = document.createElement('p');
        name.className = 'rest-stop-food-name';
        name.textContent = formatText(menu?.foodName, '이름 정보 없음');
        formatFoodBadges(menu).forEach((badgeLabel) => name.appendChild(createFoodBadge(badgeLabel)));
        item.appendChild(name);

        const cost = document.createElement('p');
        cost.className = 'rest-stop-food-cost';
        cost.textContent = formatFoodCost(menu?.foodCost);
        item.appendChild(cost);

        if (!isMissingValue(menu?.description)) {
            const description = document.createElement('p');
            description.className = 'rest-stop-food-description';
            description.textContent = menu.description;
            item.appendChild(description);
        }

        return item;
    }

    function createFoodBadge(label) {
        const badge = document.createElement('span');
        badge.className = 'rest-stop-food-badge';
        badge.textContent = label;
        return badge;
    }

    function toggleFoodMenu() {
        foodExpanded = !foodExpanded;
        renderFoodList();
    }

    async function refreshOilInfo() {
        const button = document.getElementById('restStopOilRefreshButton');
        const status = document.getElementById('restStopOilRefreshStatus');
        if (!refreshOilPrice || !button || !status) {
            return;
        }

        button.disabled = true;
        status.textContent = '실시간 요금을 확인하는 중입니다.';

        const result = await refreshOilPrice(selectedServiceAreaCode);

        button.disabled = false;
        if (result.status === 'success') {
            currentDetail = {
                ...currentDetail,
                oilInfo: result.data
            };
            renderOilInfo(result.data);
            return;
        }

        if (result.status === 'external-unavailable') {
            showApiUnavailableAlert();
        }

        status.textContent = oilRefreshStatusMessage(result.status);
        status.classList.add('rest-stop-detail-missing');
    }

    return {
        open,
        renderState,
        toggleFoodMenu,
        openFoodModal,
        closeFoodModal,
        refreshOilInfo,
        getCurrentDetail: () => currentDetail,
        getSelectedServiceAreaCode: () => selectedServiceAreaCode
    };
}
