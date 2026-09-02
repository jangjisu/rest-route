/**
 * "이름·거리로 찾기"/"목적지로 추천받기" 진입 팝업(위치 동의, 연료/EV 관심)을 매번 다시 묻지
 * 않기 위한 탭 세션 기억. sessionStorage를 쓰므로 이 탭을 닫기 전까지만 유지되고(새로고침에는
 * 살아남음), 다른 탭이나 다음 방문에는 이어지지 않는다 — 좌표 자체는 여기서 캐시하지 않고 매번
 * 새로 받아온다(허용 여부만 기억). 잘못 고른 걸 다시 고르는 방법은 탭을 닫았다 새로 여는 것뿐이다
 * (화면 안에서 되돌리는 UI는 의도적으로 만들지 않았다 — "첫 화면으로"만 눌러도 초기화되면 화면을
 * 오갈 때마다 매번 다시 물어보게 돼서 애초에 이 기억을 두는 의미가 없어진다).
 *
 * 저장소는 인자로 주입받는다(기본값 `globalThis.sessionStorage`) — 프라이빗 브라우징 등으로
 * sessionStorage 접근 자체가 막혀 있어도 조용히 무시하고, 테스트에서는 가짜 저장소를 넣을 수 있다.
 */

const LOCATION_KEY_PREFIX = 'finder.locationAnswered.';
const INTEREST_KEY = 'finder.interest';
const INTEREST_SKIPPED_SENTINEL = 'NONE';

function defaultStorage() {
    try {
        return globalThis.sessionStorage;
    } catch {
        return undefined;
    }
}

function read(storage, key) {
    try {
        return (storage ?? defaultStorage())?.getItem(key) ?? null;
    } catch {
        return null;
    }
}

function write(storage, key, value) {
    try {
        (storage ?? defaultStorage())?.setItem(key, value);
    } catch {
        // 저장 실패(프라이빗 브라우징 등)는 매번 다시 묻는 것으로 조용히 폴백한다.
    }
}

/** @param {'nearby-search'|'destination-recommendation'} mode @param {'granted'|'skipped'} answer */
export function rememberLocationAnswer(mode, answer, storage) {
    write(storage, LOCATION_KEY_PREFIX + mode, answer);
}

/** @returns {'granted'|'skipped'|null} null이면 아직 이 탭에서 답한 적 없음. */
export function getRememberedLocationAnswer(mode, storage) {
    return read(storage, LOCATION_KEY_PREFIX + mode);
}

/** @param {string|null} interest 선택한 유종/EV, 건너뛰었으면 null. */
export function rememberInterest(interest, storage) {
    write(storage, INTEREST_KEY, interest ?? INTEREST_SKIPPED_SENTINEL);
}

/**
 * @returns {string|null|undefined} 고른 값, 건너뛰어서 저장된 경우 null, 아직 이 탭에서 답한 적
 * 없으면 undefined — null(건너뛰기 기억)과 undefined(미응답)를 반드시 구분해야 팝업을 다시 띄울지
 * 판단할 수 있다.
 */
export function getRememberedInterest(storage) {
    const value = read(storage, INTEREST_KEY);
    if (value === null) {
        return undefined;
    }
    return value === INTEREST_SKIPPED_SENTINEL ? null : value;
}
