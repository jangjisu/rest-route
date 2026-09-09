/**
 * 목적지 칩 — 사용 로그 기반 "인기 목적지" 데이터가 없어 하드코딩 상수로 관리한다.
 * 칩은 도시명이 아니라 역 이름으로 지정한다(도시명은 행정구역 중심점이 도로에서 떨어져 있을 수
 * 있어 길찾기 출도착지로 부적합). 여기 이름은 서버의 PopularDestination이 아는 이름과 정확히
 * 같아야 한다 — 서버가 그 이름으로 좌표를 찾으므로, 어긋나면 목적지를 찾지 못한다.
 * 직접 입력한 목적지는 이 목록과 무관하게 place-search 후보 선택을 거쳐 좌표로 넘어간다.
 */
export const DESTINATION_CHIPS = [
    { label: '부산역', destinationName: '부산역' },
    { label: '대전역', destinationName: '대전역' },
    { label: '강릉역', destinationName: '강릉역' },
    { label: '광주송정역', destinationName: '광주송정역' }
];
