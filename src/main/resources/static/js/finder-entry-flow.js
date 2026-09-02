/**
 * 랜딩 화면의 두 타일부터 위치 동의 팝업, 연료/EV 관심 팝업까지 — mode1/mode2가 실제 화면
 * (검색·목록)을 그리기 전까지 거치는 공통 진입 절차. mode1/mode2 각각이 무엇을 "그리는지"는 전혀
 * 모르고, 좌표+관심 항목이 정해지면 `onMode1Ready`/`onMode2Ready` 콜백에 넘겨주기만 한다 —
 * 그래야 mode1.js/mode2.js가 이 파일을 몰라도 되고, 이 파일도 mode1.js/mode2.js를 몰라도 된다.
 *
 * 위치 동의(모드별 dialog)와 연료/EV 관심(공유 dialog)에 이미 이번 탭 세션에서 답했으면
 * `finder-session-memory.js` 기억을 보고 팝업 없이 바로 다음 단계로 넘어간다 — 좌표 자체는 여기서
 * 캐시하지 않고 "허용"이었을 때만 매번 새로 받아온다.
 */

import { closeDialogById, openDialogById } from './utils.js';
import { setLoading, setStatus, showScreen } from './finder-render.js';
import {
    getRememberedInterest,
    getRememberedLocationAnswer,
    rememberInterest,
    rememberLocationAnswer,
    resetFinderMemory
} from './finder-session-memory.js';
import { INTEREST_OPTIONS } from './finder-condition.js';

export function initializeFinderEntryFlow(document, { requestCurrentPosition, onMode1Ready, onMode2Ready } = {}) {
    const mode1ErrorEl = document.getElementById('finderPermissionMode1Error');
    const mode2ErrorEl = document.getElementById('finderPermissionMode2Error');
    // 연료 선택 팝업은 mode1/mode2가 공유하므로, 팝업을 열기 전에 어느 쪽으로 이어질지와 그때까지
    // 받아둔 좌표를 표시해둔다.
    let interestPopupTargetMode = 'mode1';
    let pendingOrigin = null;

    document.getElementById('finderEnterMode1')?.addEventListener('click', () => startMode1Entry());
    document.getElementById('finderEnterMode2')?.addEventListener('click', () => startMode2Entry());

    /**
     * 이번 탭 세션에서 이미 위치를 답한 적 있으면(허용/건너뛰기 모두) 팝업을 다시 띄우지 않고 바로
     * 다음 단계로 넘어간다 — 단, 좌표는 여기서 캐시해두지 않고 "허용"이었을 때만 매번 새로 받아온다
     * (위치가 바뀌었을 수 있어서다). 처음 답하는 탭이면 기존과 동일하게 팝업을 띄운다.
     */
    async function startMode1Entry() {
        const remembered = getRememberedLocationAnswer('mode1');
        if (remembered === null) {
            openDialogById('finderPermissionMode1');
            return;
        }
        if (remembered === 'skipped') {
            proceedFromMode1Location(null);
            return;
        }
        setLoading(document, true);
        const result = await requestCurrentPosition();
        setLoading(document, false);
        proceedFromMode1Location(result.granted ? { latitude: result.latitude, longitude: result.longitude } : null);
    }

    async function startMode2Entry() {
        if (getRememberedLocationAnswer('mode2') !== 'granted') {
            openDialogById('finderPermissionMode2');
            return;
        }
        setLoading(document, true);
        const result = await requestCurrentPosition();
        setLoading(document, false);
        if (!result.granted) {
            // 이전엔 허용했지만 이번엔 실패(권한 해제 등) — 재시도할 수 있게 팝업으로 안내한다.
            openDialogById('finderPermissionMode2');
            return;
        }
        proceedFromMode2Location({ latitude: result.latitude, longitude: result.longitude });
    }

    /* ---------- 위치 동의 팝업: 이름·거리로 찾기 ---------- */

    document.getElementById('finderPermissionMode1Close')?.addEventListener('click', () => {
        closeDialogById('finderPermissionMode1');
    });
    document.getElementById('finderPermissionMode1Skip')?.addEventListener('click', () => {
        rememberLocationAnswer('mode1', 'skipped');
        closeDialogById('finderPermissionMode1');
        proceedFromMode1Location(null);
    });
    document.getElementById('finderPermissionMode1Allow')?.addEventListener('click', async () => {
        setStatus(mode1ErrorEl, '');
        setLoading(document, true);
        const result = await requestCurrentPosition();
        setLoading(document, false);

        const origin = result.granted ? { latitude: result.latitude, longitude: result.longitude } : null;
        rememberLocationAnswer('mode1', result.granted ? 'granted' : 'skipped');
        closeDialogById('finderPermissionMode1');
        proceedFromMode1Location(origin);
    });

    function proceedFromMode1Location(origin) {
        interestPopupTargetMode = 'mode1';
        pendingOrigin = origin;
        openInterestPopupOrSkip();
    }

    /* ---------- 위치 동의 팝업: 목적지로 추천받기 ---------- */

    document.getElementById('finderPermissionMode2Close')?.addEventListener('click', () => {
        closeDialogById('finderPermissionMode2');
    });
    document.getElementById('finderPermissionMode2Allow')?.addEventListener('click', async () => {
        setStatus(mode2ErrorEl, '');
        setLoading(document, true);
        const result = await requestCurrentPosition();
        setLoading(document, false);

        if (!result.granted) {
            setStatus(mode2ErrorEl, '위치 확인에 실패했어요. 다시 시도해주세요.');
            return;
        }

        rememberLocationAnswer('mode2', 'granted');
        closeDialogById('finderPermissionMode2');
        proceedFromMode2Location({ latitude: result.latitude, longitude: result.longitude });
    });

    function proceedFromMode2Location(origin) {
        interestPopupTargetMode = 'mode2';
        pendingOrigin = origin;
        openInterestPopupOrSkip();
    }

    /* ---------- 연료 선택 팝업 — mode1/mode2가 공유한다. 위치 동의 팝업 다음에 항상 뜬다 ---------- */

    /** 이번 탭 세션에서 이미 연료/EV 관심을 답한 적 있으면 팝업 없이 그 값을 바로 쓴다. */
    function openInterestPopupOrSkip() {
        const remembered = getRememberedInterest();
        if (remembered === undefined) {
            openDialogById('finderInterestPopup');
            return;
        }
        applyInterest(remembered);
    }

    function applyInterest(interest) {
        if (interestPopupTargetMode === 'mode2') {
            onMode2Ready?.(pendingOrigin, interest);
        } else {
            onMode1Ready?.(pendingOrigin, interest);
        }
    }

    const interestChipsEl = document.getElementById('finderInterestChips');
    INTEREST_OPTIONS.forEach((option) => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'finder-chip';
        button.textContent = option.label;
        button.addEventListener('click', () => {
            closeDialogById('finderInterestPopup');
            rememberInterest(option.key);
            applyInterest(option.key);
        });
        interestChipsEl?.appendChild(button);
    });
    document.getElementById('finderInterestSkip')?.addEventListener('click', () => {
        closeDialogById('finderInterestPopup');
        rememberInterest(null);
        applyInterest(null);
    });
    document.getElementById('finderInterestClose')?.addEventListener('click', () => {
        closeDialogById('finderInterestPopup');
        resetFinderMemory();
        showScreen(document, 'landing');
    });
}
