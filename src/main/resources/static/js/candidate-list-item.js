/**
 * candidate-list-item.js — "선택 가능한 후보" li > button > (이름 + 설명) 목록 아이템의 공용
 * 골격. rest-stops-map.js(휴게소 이름 검색·목적지 후보 팝업 2곳)와
 * finder-destination-recommendation.js(목적지 후보 팝업)가 필드명만 다르게 세 번 따로
 * 만들던 구조다.
 *
 * secondaryText가 없으면(빈 문자열/undefined 등) 설명 줄 자체를 만들지 않는다 — 호출하는
 * 쪽이 항상 표시하고 싶으면 formatText 등으로 미리 대체 문구를 채워서 넘기면 된다. 클래스명은
 * 화면마다 스타일이 달라 전부 호출하는 쪽이 정한다.
 */
export function createCandidateListItem(document, {
    itemClassName,
    buttonClassName,
    primaryClassName,
    secondaryClassName,
    primaryText,
    secondaryText,
    onSelect
}) {
    const item = document.createElement('li');
    if (itemClassName) {
        item.className = itemClassName;
    }

    const button = document.createElement('button');
    button.type = 'button';
    if (buttonClassName) {
        button.className = buttonClassName;
    }

    const primary = document.createElement('p');
    if (primaryClassName) {
        primary.className = primaryClassName;
    }
    primary.textContent = primaryText;
    button.appendChild(primary);

    if (secondaryText) {
        const secondary = document.createElement('p');
        if (secondaryClassName) {
            secondary.className = secondaryClassName;
        }
        secondary.textContent = secondaryText;
        button.appendChild(secondary);
    }

    button.addEventListener('click', onSelect);
    item.appendChild(button);

    return item;
}
