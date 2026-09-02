/**
 * mode1/mode2가 공유하는 화면 전환·상태 표시·결과 카드 렌더링. 배지 판정 자체(어떤 배지가
 * 붙는지)와 색상 매핑(어느 배지가 무슨 색인지)은 각 모드가 완전히 독립적으로 갖고 있고, 이 모듈은
 * "카드 하나를 어떻게 그리는가"라는 공통 골격만 담당한다. 실제 페이지의 `document`를 그대로 안 쓰고
 * 인자로 받아서, 테스트에서는 가짜 document를 넣어 DOM 없이 검증할 수 있다.
 */

function renderBadges(document, container, badges, colorClassByKey) {
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

export function showScreen(document, screenName) {
    document.querySelectorAll('[data-finder-screen]').forEach((section) => {
        section.hidden = section.dataset.finderScreen !== screenName;
    });
}

export function setLoading(document, isLoading) {
    const overlay = document.getElementById('finderLoadingOverlay');
    if (overlay) {
        overlay.hidden = !isLoading;
    }
}

export function setStatus(element, message) {
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

export function renderResultCard(document, { name, routeLabel, distanceLabel, badges, colorClassByKey, onSelect }) {
    const li = document.createElement('li');
    li.className = 'finder-result-card';
    if (onSelect) {
        li.tabIndex = 0;
        li.setAttribute('role', 'button');
        li.addEventListener('click', onSelect);
        li.addEventListener('keydown', (event) => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                onSelect();
            }
        });
    }

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
    renderBadges(document, badgeRow, badges, colorClassByKey);
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
