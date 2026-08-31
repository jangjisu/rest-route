/**
 * 목적지 칩 — 사용 로그 기반 "인기 목적지" 데이터가 없어 하드코딩 상수로 관리한다.
 * 칩은 도시명이 아니라 역 이름으로 검색한다(경로 API가 역/시설 단위 검색만 지원).
 */
export const DESTINATION_CHIPS = [
    { label: '부산', destinationQuery: '부산역' },
    { label: '대전', destinationQuery: '대전역' },
    { label: '강릉', destinationQuery: '강릉역' },
    { label: '광주', destinationQuery: '광주송정역' }
];

/**
 * 칩 라벨을 실제 destinationQuery로 변환한다. 칩 목록에 없는 라벨(직접 입력한 텍스트)은
 * 입력값을 그대로 destinationQuery로 사용한다.
 */
export function resolveDestinationQuery(label) {
    const trimmed = typeof label === 'string' ? label.trim() : '';
    const matchedChip = DESTINATION_CHIPS.find((chip) => chip.label === trimmed);
    return matchedChip ? matchedChip.destinationQuery : trimmed;
}
