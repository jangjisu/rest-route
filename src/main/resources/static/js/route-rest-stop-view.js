import { showApiUnavailableAlert } from './utils.js';
import { isMissingValue, formatText } from './rest-stop-detail-formatters.js';
import { createRouteRestStopImage } from './rest-stop-images.js';

const ROUTE_REST_STOP_FILTERS = [
    { key: 'all', label: '전체', title: '경로상 휴게소' },
    { key: 'fuel', label: '주유 가능', title: '주유 가능한 휴게소' },
    { key: 'food', label: '먹거리', title: '먹거리가 있는 휴게소' },
    { key: 'theme', label: '테마', title: '테마가 있는 휴게소' },
    { key: 'event', label: '이벤트', title: '이벤트 진행 중인 휴게소' },
    { key: 'ev', label: 'EV 충전', title: 'EV 충전 가능한 휴게소' }
];

export function formatRouteDuration(durationSeconds) {
    if (!Number.isFinite(durationSeconds) || durationSeconds <= 0) {
        return '';
    }

    const totalMinutes = Math.round(durationSeconds / 60);
    const hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;
    if (hours === 0) {
        return `${minutes}분`;
    }
    if (minutes === 0) {
        return `${hours}시간`;
    }
    return `${hours}시간 ${minutes}분`;
}

export function formatRouteDistance(distanceMeters) {
    if (!Number.isFinite(distanceMeters) || distanceMeters <= 0) {
        return '';
    }

    const km = distanceMeters / 1000;
    return `${km.toFixed(km < 10 ? 1 : 0)}km`;
}

export function formatRouteTollFare(tollFareWon) {
    if (!Number.isFinite(tollFareWon) || tollFareWon <= 0) {
        return '무료';
    }
    return `톨비 ${tollFareWon.toLocaleString()}원`;
}

export function formatRouteOptionSummary(route) {
    const summary = route?.summary;
    if (!summary || typeof summary !== 'object') {
        return '';
    }

    return [
        formatRouteDuration(summary.durationSeconds),
        formatRouteDistance(summary.distanceMeters)
    ]
        .filter((part) => part !== '')
        .join(' · ');
}

export function formatOilPriceDelta(diffFromAverage) {
    if (!Number.isFinite(diffFromAverage)) {
        return null;
    }

    const absoluteDiff = Math.abs(diffFromAverage).toLocaleString('ko-KR');
    if (diffFromAverage < 0) {
        return { text: `(-${absoluteDiff})`, tone: 'cheap' };
    }
    if (diffFromAverage > 0) {
        return { text: `(+${absoluteDiff})`, tone: 'expensive' };
    }
    return { text: '(0)', tone: 'same' };
}

export function formatOilPriceComparison(price, diffFromAverage) {
    if (isMissingValue(price)) {
        return '';
    }

    const delta = formatOilPriceDelta(diffFromAverage);
    if (delta === null) {
        return price;
    }

    return `${price} ${delta.text}`;
}

export function formatRouteComparisonSummary(restStop) {
    const summary = restStop?.comparisonSummary;
    if (!summary || typeof summary !== 'object') {
        return [];
    }

    const priceParts = [
        ['휘발유', summary.gasolinePrice, summary.gasolinePriceDiffFromAverage],
        ['경유', summary.dieselPrice, summary.dieselPriceDiffFromAverage],
        ['LPG', summary.lpgPrice, summary.lpgPriceDiffFromAverage]
    ]
        .map(([label, value, diff]) => [label, formatOilPriceComparison(value, diff)])
        .filter(([, value]) => value !== '')
        .map(([label, value]) => `${label} ${value}`);
    const countParts = [];
    if (Number.isFinite(summary.totalParkingCount) && summary.totalParkingCount > 0) {
        countParts.push(`주차 ${summary.totalParkingCount.toLocaleString()}대`);
    }
    if (Number.isFinite(summary.foodMenuCount) && summary.foodMenuCount > 0) {
        countParts.push(`먹거리 ${summary.foodMenuCount.toLocaleString()}개`);
    }
    if (Number.isFinite(summary.facilityCount) && summary.facilityCount > 0) {
        countParts.push(`시설 ${summary.facilityCount.toLocaleString()}개`);
    }

    return [priceParts, countParts]
        .filter((parts) => parts.length > 0)
        .map((parts) => parts.join(' · '));
}

export function formatNationalOilPriceSummary(summary) {
    if (!summary || typeof summary !== 'object') {
        return null;
    }

    const items = [
        ['휘발유', summary.gasoline],
        ['경유', summary.diesel],
        ['LPG', summary.lpg]
    ]
        .map(([fallbackLabel, item]) => formatNationalOilPriceItem(fallbackLabel, item))
        .filter((item) => item !== null);
    if (items.length === 0) {
        return null;
    }

    return {
        tradeDate: formatText(summary.tradeDate, '오늘'),
        items
    };
}

function formatNationalOilPriceItem(fallbackLabel, item) {
    if (!item || typeof item !== 'object' || isMissingValue(item.price)) {
        return null;
    }

    return {
        label: fallbackLabel,
        price: item.price,
        ...formatNationalOilDailyDiff(item.dailyDiff)
    };
}

function formatNationalOilDailyDiff(value) {
    const diff = Number.parseFloat(value);
    if (!Number.isFinite(diff)) {
        return { dailyDiff: '-', dailyDiffTone: 'same' };
    }

    const absoluteDiff = Math.abs(diff).toLocaleString('ko-KR');
    if (diff < 0) {
        return { dailyDiff: `↓ ${absoluteDiff}원`, dailyDiffTone: 'favorable' };
    }
    if (diff > 0) {
        return { dailyDiff: `↑ ${absoluteDiff}원`, dailyDiffTone: 'unfavorable' };
    }
    return { dailyDiff: '0원', dailyDiffTone: 'same' };
}

export function renderNationalOilPriceState(state) {
    if (state.status === 'success') {
        renderNationalOilPriceSummary(state.data);
        return;
    }

    if (state.status === 'external-unavailable') {
        showApiUnavailableAlert();
    }

    renderNationalOilPriceSummary(null);
}

function renderNationalOilPriceSummary(summary) {
    const container = document.getElementById('routeNationalOilPriceSummary');
    if (!container) {
        return;
    }

    const formattedSummary = formatNationalOilPriceSummary(summary);
    container.replaceChildren();
    container.classList.toggle('d-none', formattedSummary === null);
    if (formattedSummary === null) {
        return;
    }

    const header = document.createElement('div');
    header.className = 'route-national-oil-summary-header';

    const title = document.createElement('p');
    title.className = 'route-national-oil-summary-title';
    title.textContent = '전국 평균 유가';
    header.appendChild(title);

    const date = document.createElement('p');
    date.className = 'route-national-oil-summary-date';
    date.textContent = formattedSummary.tradeDate;
    header.appendChild(date);
    container.appendChild(header);

    const list = document.createElement('dl');
    list.className = 'route-national-oil-summary-list';
    formattedSummary.items.forEach((item) => {
        const chip = document.createElement('div');
        chip.className = 'route-national-oil-chip';

        const term = document.createElement('dt');
        term.textContent = item.label;
        chip.appendChild(term);

        const description = document.createElement('dd');
        const price = document.createElement('span');
        price.className = 'route-national-oil-chip-price';
        price.textContent = item.price;
        description.appendChild(price);

        const diff = document.createElement('span');
        diff.className = `route-national-oil-chip-diff route-national-oil-chip-diff-${item.dailyDiffTone}`;
        diff.textContent = item.dailyDiff;
        description.appendChild(diff);

        chip.appendChild(description);
        list.appendChild(chip);
    });
    container.appendChild(list);
}

function routeRestStopMatchesFilter(restStop, filterKey) {
    const summary = restStop?.comparisonSummary;
    if (filterKey === 'fuel') {
        return Boolean(summary) && [summary.gasolinePrice, summary.dieselPrice, summary.lpgPrice]
            .some((price) => !isMissingValue(price));
    }
    if (filterKey === 'food') {
        return Number(summary?.foodMenuCount) > 0;
    }
    if (filterKey === 'theme') {
        return restStop?.hasTheme === true;
    }
    if (filterKey === 'event') {
        return restStop?.hasEvent === true;
    }
    if (filterKey === 'ev') {
        return restStop?.hasEvCharger === true;
    }
    return true;
}

export function filterRouteRestStops(restStops, filterKey) {
    const items = Array.isArray(restStops) ? restStops : [];
    const knownFilter = ROUTE_REST_STOP_FILTERS.some((filter) => filter.key === filterKey);
    if (!knownFilter || filterKey === 'all') {
        return items;
    }
    return items.filter((restStop) => routeRestStopMatchesFilter(restStop, filterKey));
}

export function routeRestStopFilterCounts(restStops) {
    const items = Array.isArray(restStops) ? restStops : [];
    return Object.fromEntries(ROUTE_REST_STOP_FILTERS.map((filter) => [
        filter.key,
        filter.key === 'all' ? items.length : filterRouteRestStops(items, filter.key).length
    ]));
}

export function routeRestStopAvailabilityLabels(restStop) {
    return [
        ['fuel', '주유 가능'],
        ['food', '먹거리'],
        ['theme', '테마'],
        ['event', '이벤트'],
        ['ev', 'EV 충전']
    ]
        .filter(([filterKey]) => routeRestStopMatchesFilter(restStop, filterKey))
        .map(([, label]) => label);
}

export function routeRecommendationLabels(restStop) {
    if (!Array.isArray(restStop?.recommendationTags)) {
        return [];
    }
    return restStop.recommendationTags
        .map((tag) => formatText(tag?.label, ''))
        .filter((label) => label !== '');
}

export function routeRestStopCardBadges(restStop) {
    return [
        ...routeRestStopAvailabilityLabels(restStop)
            .map((label) => ({ label, kind: 'availability' })),
        ...routeRecommendationLabels(restStop)
            .map((label) => ({ label, kind: 'recommendation' }))
    ];
}

export function routeNearbyTrafficBadge(restStop) {
    const traffic = restStop?.nearbyTraffic;
    if (!traffic || typeof traffic !== 'object' || !traffic.key || !traffic.label) {
        return null;
    }
    return { key: traffic.key, label: traffic.label };
}

function routeResultFuelItems(restStop) {
    const summary = restStop?.comparisonSummary;
    if (!summary || typeof summary !== 'object') {
        return [];
    }

    return [
        ['휘발유', summary.gasolinePrice, summary.gasolinePriceDiffFromAverage],
        ['경유', summary.dieselPrice, summary.dieselPriceDiffFromAverage],
        ['LPG', summary.lpgPrice, summary.lpgPriceDiffFromAverage]
    ]
        .map(([label, price, diff]) => ({
            label,
            missing: isMissingValue(price),
            price: formatText(price, 'X'),
            delta: formatOilPriceDelta(diff)
        }));
}

export function createRouteResultItem(restStop, index, onSelect) {
    const item = document.createElement('li');
    item.className = 'route-result-item';
    item.tabIndex = 0;
    item.setAttribute('role', 'button');
    item.setAttribute('aria-label', `${formatText(restStop?.unitName, '이름 정보 없음')} 상세정보 보기`);

    let appendTarget = item;
    const image = createRouteRestStopImage(document, restStop);
    if (image) {
        item.classList.add('route-result-item-with-image');
        const imageWrapper = document.createElement('div');
        imageWrapper.className = 'route-result-image-wrapper';
        imageWrapper.appendChild(image);
        item.appendChild(imageWrapper);

        appendTarget = document.createElement('div');
        appendTarget.className = 'route-result-content';
        item.appendChild(appendTarget);
    }

    const header = document.createElement('div');
    header.className = 'route-result-item-header';

    const rank = document.createElement('span');
    rank.className = 'route-result-rank';
    rank.textContent = String(index + 1);
    header.appendChild(rank);

    const name = document.createElement('p');
    name.className = 'route-result-name';
    name.textContent = formatText(restStop?.unitName, '이름 정보 없음');
    const heading = document.createElement('div');
    heading.className = 'route-result-heading';
    heading.appendChild(name);

    const nearbyTraffic = routeNearbyTrafficBadge(restStop);
    if (nearbyTraffic) {
        const trafficBadge = document.createElement('span');
        trafficBadge.className = `route-result-traffic route-result-traffic-${nearbyTraffic.key}`;
        trafficBadge.textContent = `인근 ${nearbyTraffic.label}`;
        heading.appendChild(trafficBadge);
    }
    header.appendChild(heading);
    appendTarget.appendChild(header);

    const cardBadges = routeRestStopCardBadges(restStop);
    if (restStop?.hasDirectionAlternative === true || cardBadges.length > 0) {
        const availability = document.createElement('div');
        availability.className = 'route-result-availability';
        if (restStop?.hasDirectionAlternative === true) {
            const badge = document.createElement('span');
            badge.className = 'route-direction-alternative-badge';
            badge.textContent = '상·하행 후보';
            availability.appendChild(badge);
        }
        cardBadges.forEach(({ label, kind }) => {
            const badge = document.createElement('span');
            badge.className = kind === 'availability'
                ? 'route-result-availability-badge'
                : 'route-result-tag';
            badge.textContent = label;
            availability.appendChild(badge);
        });
        appendTarget.appendChild(availability);
    }

    const meta = document.createElement('p');
    meta.className = 'route-result-meta';
    meta.textContent = formatText(restStop?.routeName, '노선 정보 없음');
    appendTarget.appendChild(meta);

    const fuels = routeResultFuelItems(restStop);
    if (fuels.length > 0) {
        const fuelList = document.createElement('div');
        fuelList.className = 'route-result-fuels';
        fuels.forEach((fuel) => {
            const fuelItem = document.createElement('span');
            fuelItem.className = 'route-result-fuel';
            const fuelLabel = document.createElement('span');
            fuelLabel.className = 'route-result-fuel-label';
            fuelLabel.textContent = fuel.label;
            fuelItem.appendChild(fuelLabel);

            const fuelPrice = document.createElement('span');
            fuelPrice.className = 'route-result-fuel-price';
            fuelPrice.classList.toggle('route-result-fuel-price-missing', fuel.missing);
            fuelPrice.textContent = fuel.price;
            fuelItem.appendChild(fuelPrice);

            if (fuel.delta !== null) {
                const delta = document.createElement('span');
                delta.className = `route-result-fuel-delta route-result-fuel-delta-${fuel.delta.tone}`;
                delta.textContent = fuel.delta.text;
                fuelItem.appendChild(delta);
            }
            fuelList.appendChild(fuelItem);
        });
        item.appendChild(fuelList);
    }

    const countSummary = formatRouteComparisonSummary(restStop).at(1);
    if (countSummary) {
        const summary = document.createElement('p');
        summary.className = 'route-result-summary';
        summary.textContent = countSummary;
        appendTarget.appendChild(summary);
    }

    const arrow = document.createElement('span');
    arrow.className = 'route-result-action-arrow';
    arrow.setAttribute('aria-hidden', 'true');
    arrow.textContent = '→';
    item.appendChild(arrow);

    const select = () => onSelect(restStop);
    item.addEventListener('click', select);
    item.addEventListener('keydown', (event) => {
        if (event.key !== 'Enter' && event.key !== ' ') {
            return;
        }
        event.preventDefault();
        select();
    });

    return item;
}

export function renderRouteOptionCards(routes, selectedIndex, onSelectRoute) {
    const container = document.getElementById('routeOptions');
    if (!container) {
        return;
    }

    container.replaceChildren();
    if (routes.length <= 1) {
        container.classList.add('d-none');
        return;
    }

    container.classList.remove('d-none');
    routes.forEach((route, index) => {
        const card = document.createElement('button');
        card.type = 'button';
        card.className = `route-option${index === selectedIndex ? ' selected' : ''}`;
        card.setAttribute('aria-pressed', String(index === selectedIndex));
        card.innerHTML = `
            <span class="route-option-label">경로 ${index + 1}</span>
            <p class="route-option-summary">${formatRouteOptionSummary(route)}</p>
            <p class="route-option-toll">${formatRouteTollFare(route?.summary?.tollFareWon)}</p>
        `;
        card.addEventListener('click', () => onSelectRoute?.(index));
        container.appendChild(card);
    });
}

function renderDirectionAlternativeNotice(restStops) {
    const notice = document.getElementById('routeDirectionAlternativeNotice');
    if (!notice) {
        return;
    }

    const hasDirectionAlternative = restStops.some((restStop) => restStop?.hasDirectionAlternative === true);
    notice.classList.toggle('d-none', !hasDirectionAlternative);
}

function renderRouteResultTitle(count, filterKey = 'all') {
    const title = document.getElementById('routeResultModalTitle');
    if (!title) {
        return;
    }

    const filter = ROUTE_REST_STOP_FILTERS.find((item) => item.key === filterKey)
        ?? ROUTE_REST_STOP_FILTERS[0];
    title.textContent = `${filter.title} (${count}곳)`;
}

/**
 * 경로 선택 카드, 경로상 휴게소 필터·목록·전국 평균 유가를 전담한다. selectedRouteRestStopFilter
 * 같은 가변 상태는 이 View가 직접 소유하고, 메인 모듈의 전역 상태를 역참조하지 않는다.
 * 목록 항목 선택(onSelectRestStop)과 경로 카드 선택(onSelectRoute)은 메인이 주입하는
 * 콜백으로 처리한다 — 상세 패널을 열거나 지도 마커/폴리라인을 갱신하는 건 메인의 조율 몫이다.
 */
export function createRouteRestStopView({ onSelectRestStop }) {
    let selectedFilter = 'all';
    let lastRestStops = [];
    let lastNationalOilPriceSummary = null;

    function renderList(restStops, nationalOilPriceSummary = lastNationalOilPriceSummary) {
        lastRestStops = Array.isArray(restStops) ? restStops : [];
        lastNationalOilPriceSummary = nationalOilPriceSummary;
        renderCurrentList();
    }

    function setNationalOilPriceSummary(summary) {
        lastNationalOilPriceSummary = summary;
    }

    function reset() {
        selectedFilter = 'all';
    }

    function renderCurrentList() {
        const list = document.getElementById('routeResultList');
        if (!list) {
            return;
        }

        const filteredRestStops = filterRouteRestStops(lastRestStops, selectedFilter);
        renderDirectionAlternativeNotice(lastRestStops);
        renderNationalOilPriceSummary(lastNationalOilPriceSummary);
        renderFilters();
        renderRouteResultTitle(filteredRestStops.length, selectedFilter);
        list.replaceChildren();
        if (filteredRestStops.length === 0) {
            const empty = document.createElement('li');
            empty.className = 'route-result-empty';
            empty.textContent = '선택한 조건에 맞는 휴게소가 없습니다.';
            list.appendChild(empty);
            return;
        }
        filteredRestStops.forEach((restStop, index) =>
            list.appendChild(createRouteResultItem(restStop, index, onSelectRestStop)));
    }

    function renderFilters() {
        const container = document.getElementById('routeRestStopFilters');
        if (!container) {
            return;
        }

        const counts = routeRestStopFilterCounts(lastRestStops);
        container.replaceChildren();
        container.classList.toggle('d-none', counts.all === 0);
        ROUTE_REST_STOP_FILTERS.forEach((filter) => {
            const button = document.createElement('button');
            const selected = filter.key === selectedFilter;
            button.type = 'button';
            button.className = `route-rest-stop-filter${selected ? ' selected' : ''}`;
            button.setAttribute('aria-pressed', String(selected));
            button.textContent = `${filter.label} ${counts[filter.key]}`;
            button.addEventListener('click', () => {
                if (filter.key === selectedFilter) {
                    return;
                }
                selectedFilter = filter.key;
                renderCurrentList();
            });
            container.appendChild(button);
        });
    }

    return {
        renderList,
        setNationalOilPriceSummary,
        reset,
        renderOptionCards: renderRouteOptionCards
    };
}
