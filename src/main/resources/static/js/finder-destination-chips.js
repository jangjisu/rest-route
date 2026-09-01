/**
 * 목적지 칩 — 사용 로그 기반 "인기 목적지" 데이터가 없어 하드코딩 상수로 관리한다.
 * 칩은 도시명이 아니라 역 이름으로 검색한다(경로 API가 역/시설 단위 검색만 지원). 라벨과
 * destinationQuery를 동일하게 둬서 화면에 보이는 이름과 실제 검색어가 항상 일치하게 한다. 직접
 * 입력한 목적지는 이 목록과 무관하게 place-search 후보 선택을 거친다(finder-app.js).
 */
export const DESTINATION_CHIPS = [
    { label: '부산역', destinationQuery: '부산역' },
    { label: '대전역', destinationQuery: '대전역' },
    { label: '강릉역', destinationQuery: '강릉역' },
    { label: '광주송정역', destinationQuery: '광주송정역' }
];
